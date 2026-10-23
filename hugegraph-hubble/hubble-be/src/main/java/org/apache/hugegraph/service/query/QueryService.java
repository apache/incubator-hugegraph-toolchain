/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.service.query;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.AdjacentQuery;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.entity.query.JsonView;
import org.apache.hugegraph.entity.query.TableView;
import org.apache.hugegraph.entity.query.TypedResult;
import org.apache.hugegraph.entity.query.GremlinResult.Type;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.IllegalGremlinException;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.Result;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.GremlinUtil;
import org.apache.hugegraph.util.ReadOnlyCypher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QueryService {

    private static final Set<String> ILLEGAL_GREMLIN_EXCEPTIONS = ImmutableSet.of(
            "groovy.lang.MissingPropertyException",
            "groovy.lang.MissingMethodException",
            "groovy.lang.MissingFieldException",
            "groovy.lang.MissingClassException",
            "groovy.lang.IncorrectClosureArgumentsException",
            "org.codehaus.groovy.control.CompilationFailedException",
            "org.codehaus.groovy.control.MultipleCompilationErrorsException",
            "org.codehaus.groovy.runtime.metaclass.MethodSelectionException"
    );

    public static final Set<String> CONDITION_OPERATORS = ImmutableSet.of(
            "eq", "gt", "gte", "lt", "lte", "neq"
    );

    public static final int GRAPH_VERTEX_LIMIT = 1000;

    private static final String TIMEOUT_EXCEPTION =
            "java.net.SocketTimeoutException";

    private static final String CONN_REFUSED_MSG = "Connection refused";

    @Autowired
    private HugeConfig config;
    @Autowired
    private VertexLabelService vlService;

    public ResultSet executeQueryCount(HugeClient client, String query) {

        log.debug("Execute Gremlin count query");
        // Execute gremlin query
        ResultSet resultSet = this.executeGremlin(query, client);
        return resultSet;
    }

    public GremlinResult executeGremlinQuery(HugeClient client, GremlinQuery query) {

        log.debug("Execute Gremlin query");
        String gremlin = this.optimize(query.getContent());
        boolean defaultLimitApplied = !gremlin.equals(query.getContent());
        if (query.isPreview()) {
            // Stop ordinary read traversals upstream. The extra row tells the
            // browser whether its bounded preview omits further results.
            gremlin = GremlinUtil.optimizePreviewLimit(gremlin, GRAPH_VERTEX_LIMIT + 1);
        }
        log.debug("Gremlin query optimized");
        // Execute gremlin query
        ResultSet resultSet = this.executeGremlin(gremlin, client);
        GremlinResult result = this.buildQueryResult(resultSet, client, query.isPreview());
        result.setDefaultLimitApplied(defaultLimitApplied);
        int defaultLimit = this.config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT);
        result.setDefaultLimit(query.isPreview() ? Math.min(defaultLimit, GRAPH_VERTEX_LIMIT) : defaultLimit);
        return result;
    }

    public JsonView executeSingleGremlinQuery(HugeClient client, GremlinQuery query) {

        log.debug("Execute single Gremlin query");
        String gremlin = this.optimize(query.getContent());
        log.debug("Single Gremlin query optimized");
        // Execute gremlin query
        ResultSet resultSet = this.executeGremlin(gremlin, client);
        // Scan data, vote the result type
        TypedResult typedResult = this.parseResults(resultSet);
        // Build json view
        JsonView jsonView = new JsonView(typedResult.getData());
        return jsonView;
    }

    public GremlinResult executeCypherQuery(HugeClient client, String query) {
        return this.executeCypherQuery(client, query, false);
    }

    public GremlinResult executeCypherQuery(HugeClient client, String query, boolean preview) {
        int defaultLimit = preview ? this.config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT) : 0;
        ReadOnlyCypher.PreviewQuery limited = preview ?
                ReadOnlyCypher.preview(query, defaultLimit, GRAPH_VERTEX_LIMIT + 1) : null;
        ResultSet resultSet = this.executeCypher(limited == null ? query : limited.query(), client);
        GremlinResult result = this.buildQueryResult(resultSet, client, preview);
        if (limited != null) {
            result.setDefaultLimitApplied(limited.defaultLimitApplied());
            result.setDefaultLimit(Math.min(defaultLimit, GRAPH_VERTEX_LIMIT));
        }
        return result;
    }

    private GremlinResult buildQueryResult(ResultSet resultSet, HugeClient client, boolean preview) {
        // Scan data, vote the result type
        TypedResult typedResult = this.parseResults(resultSet,
                                  preview ? GRAPH_VERTEX_LIMIT : Integer.MAX_VALUE);
        QueryPropertyPreview propertyPreview = preview ? new QueryPropertyPreview() : null;
        if (propertyPreview != null) {
            typedResult = propertyPreview.project(typedResult);
        }
        boolean resultTruncated = typedResult.isTruncated();
        // All views consume the same parsed query data. Graph
        // enrichment can introduce new SDK objects; project only those once.
        GraphView graphView = this.buildGraphView(typedResult, client, resultTruncated, preview);
        if (propertyPreview != null) {
            graphView = propertyPreview.project(graphView);
        }
        JsonView jsonView = new JsonView(typedResult.getData());
        TableView tableView = this.buildTableView(typedResult);
        return GremlinResult.builder()
                            .type(typedResult.getType())
                            .jsonView(jsonView)
                            .tableView(tableView)
                            .graphView(graphView)
                            .propertiesTruncated(propertyPreview != null && propertyPreview.truncated())
                            .propertyPreviewLimit(preview ? QueryPropertyPreview.STRING_LIMIT : 0)
                            .propertyPreviewVertexIds(propertyPreview == null ? null :
                                                      propertyPreview.partialElementIds(graphView.getVertices()))
                            .propertyPreviewEdgeIds(propertyPreview == null ? null :
                                                    propertyPreview.partialElementIds(graphView.getEdges()))
                            .resultTruncated(resultTruncated)
                            .resultLimit(preview ? GRAPH_VERTEX_LIMIT : 0)
                            .graphTruncated(graphView.isTruncated())
                            .graphVertexLimit(preview ? GRAPH_VERTEX_LIMIT : 0)
                            .graphEdgeLimit(preview ? this.config.get(HubbleOptions.GREMLIN_EDGES_TOTAL_LIMIT) : 0)
                            .build();
    }

    private ResultSet executeCypher(String cypher, HugeClient client) {
        try {
            return client.cypher().cypher(cypher);
        } catch (ServerException e) {
            String exception = e.exception();
            log.error("Cypher execute failed with HTTP status {}", e.status());
            if (ILLEGAL_GREMLIN_EXCEPTIONS.contains(exception)) {
                throw new IllegalGremlinException("gremlin.illegal-statemnt", e,
                        e.message());
            }
            throw this.serverQueryException(e);
        } catch (ClientException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                String message = cause.getMessage();
                if (message != null && message.startsWith(TIMEOUT_EXCEPTION)) {
                    throw new InternalException("gremlin.execute.timeout", e,
                            message);
                }
                if (message != null && message.contains(CONN_REFUSED_MSG)) {
                    throw new InternalException("gremlin.connection.refused", e,
                            message);
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("Cypher execute failed with {}",
                      e.getClass().getSimpleName());
            throw new ExternalException("gremlin.execute.failed", e,
                    e.getMessage());
        }
    }

    public Long executeGremlinAsyncTask(HugeClient client, GremlinQuery query) {

        log.debug("Execute asynchronous Gremlin query");
        // Execute optimized gremlin query
        GremlinRequest request = new GremlinRequest(query.getContent());
        try {
            return client.gremlin().executeAsTask(request);
        } catch (ServerException e) {
            throw this.serverQueryException(e);
        }
    }

    public Long executeCypherAsyncTask(HugeClient client, String query) {

        log.debug("Execute asynchronous Cypher query");
        // Execute optimized gremlin query
        try {
            return client.cypher().executeAsTask(query);
        } catch (ServerException e) {
            throw this.serverQueryException(e);
        }
    }

    public GremlinResult expandVertex(HugeClient client, AdjacentQuery query) {

        // Build gremlin query
        String gremlin = this.buildGremlinQuery(client, query);
        log.debug("Execute vertex expansion query");
        // Execute gremlin query
        ResultSet resultSet = this.executeGremlin(gremlin, client);

        Map<Object, Vertex> vertices = new LinkedHashMap<>();
        List<Edge> edges = new ArrayList<>(resultSet.size());
        for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
            Path path = iter.next().getPath();
            List<Object> objects = path.objects();
            Ex.check(objects.size() == 3,
                     "The expanded vertex path must contain 3 objects, " +
                     "but got %s", objects.size());
            Edge edge = (Edge) objects.get(1);
            Vertex vertex = (Vertex) objects.get(2);
            // Filter vertices and edges that existed in query
            if (query.retainEdge(edge)) {
                edges.add(edge);
            }
            if (query.retainVertex(vertex)) {
                vertices.putIfAbsent(vertex.id(), vertex);
            }
        }
        // Parallel edges share a neighbor; spend its property budget only once.
        QueryPropertyPreview propertyPreview = new QueryPropertyPreview();
        GraphView graphView = propertyPreview.project(new GraphView(vertices.values(), edges));
        return GremlinResult.builder()
                            .type(Type.PATH)
                            .graphView(graphView)
                            .propertiesTruncated(propertyPreview.truncated())
                            .propertyPreviewLimit(QueryPropertyPreview.STRING_LIMIT)
                            .propertyPreviewVertexIds(propertyPreview.partialElementIds(graphView.getVertices()))
                            .propertyPreviewEdgeIds(propertyPreview.partialElementIds(graphView.getEdges()))
                            .build();
    }

    private String optimize(String content) {
        // Optimize each gremlin statemnts
        int limit = this.config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT);
        // The statement splitting is quote-aware, see GremlinUtil
        return GremlinUtil.optimizeLimit(content, limit);
    }

    private ResultSet executeGremlin(String gremlin, HugeClient client) {
        try {
            return client.gremlin().gremlin(gremlin).execute();
        } catch (ServerException e) {
            String exception = e.exception();
            log.error("Gremlin execute failed with HTTP status {}", e.status());
            if (ILLEGAL_GREMLIN_EXCEPTIONS.contains(exception)) {
                throw new IllegalGremlinException("gremlin.illegal-statemnt", e,
                                                  e.message());
            }
            throw this.serverQueryException(e);
        } catch (ClientException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                String message = cause.getMessage();
                if (message != null && message.startsWith(TIMEOUT_EXCEPTION)) {
                    throw new InternalException("gremlin.execute.timeout", e,
                                                message);
                }
                if (message != null && message.contains(CONN_REFUSED_MSG)) {
                    throw new InternalException("gremlin.connection.refused", e,
                                                message);
                }
            }
            throw e;
        } catch (Exception e) {
            log.error("Gremlin execute failed with {}",
                      e.getClass().getSimpleName());
            throw new ExternalException("gremlin.execute.failed", e,
                                        e.getMessage());
        }
    }

    private ExternalException serverQueryException(ServerException exception) {
        int status = exception.status() > 0 ? exception.status() : 400;
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return new ExternalException(HttpStatus.BAD_GATEWAY.value(),
                                         "gremlin.server.authentication-failed",
                                         exception);
        }
        if (status == 503) {
            return new ExternalException(status, "gremlin.server.unavailable",
                                         exception);
        }
        String detail = exception.message();
        if (StringUtils.isBlank(detail)) {
            detail = exception.cause();
        }
        if (StringUtils.isBlank(detail)) {
            detail = exception.exception();
        }
        if (StringUtils.isBlank(detail)) {
            detail = "HTTP " + status;
        }
        return new ExternalException(status, "gremlin.execute.failed",
                                     exception, detail);
    }

    private TypedResult parseResults(ResultSet resultSet) {
        return this.parseResults(resultSet, Integer.MAX_VALUE);
    }

    private TypedResult parseResults(ResultSet resultSet, int limit) {
        if (resultSet == null) {
            return new TypedResult(Type.EMPTY, null);
        }
        Iterator<Result> iter = resultSet.iterator();
        if (!iter.hasNext()) {
            return new TypedResult(Type.EMPTY, null);
        }

        Map<Type, Integer> typeVotes = new HashMap<>();
        List<Object> typedData = new ArrayList<>(Math.min(resultSet.size(), limit));
        int rows = 0;
        boolean truncated = false;
        while (iter.hasNext() && rows++ < limit && typedData.size() < limit) {
            Result result = iter.next();
            if (result == null) {
                // NOTE: null value doesn't vote
                continue;
            }
            Object object = result.getObject();
            if (object instanceof Map<?, ?> row) {
                List<Object> elements = unwrapCypherElements(row);
                if (elements != null) {
                    for (Object element : elements) {
                        if (typedData.size() >= limit) {
                            truncated = true;
                            break;
                        }
                        Type elementType = element instanceof Vertex ?
                                           Type.VERTEX : Type.EDGE;
                        typeVotes.compute(elementType,
                                          (k, v) -> v == null ? 1 : v + 1);
                        typedData.add(element);
                    }
                    continue;
                }
            }
            Type type;
            if (object instanceof Vertex) {
                type = Type.VERTEX;
            } else if (object instanceof Edge) {
                type = Type.EDGE;
            } else if (object instanceof Path) {
                type = Type.PATH;
            } else {
                type = Type.GENERAL;
            }
            typeVotes.compute(type, (k, v) -> v == null ? 1 : v + 1);
            typedData.add(object);
        }

        Type type;
        if (typeVotes.isEmpty()) {
            type = Type.EMPTY;
        } else {
            // Find the key with max value
            type = Collections.max(typeVotes.entrySet(),
                                   Comparator.comparingInt(Map.Entry::getValue))
                              .getKey();
        }
        return new TypedResult(type, typedData, truncated || iter.hasNext());
    }

    private TableView buildTableView(TypedResult typedResult) {
        List<Object> data = typedResult.getData();
        if (CollectionUtils.isEmpty(data)) {
            return new TableView();
        }

        return switch (typedResult.getType()) {
            case EMPTY -> new TableView();
            case GENERAL -> new TableView(TableView.GENERAL_HEADER,
                    data.stream().map(object -> ImmutableMap.of("result", object))
                        .collect(Collectors.toCollection(ArrayList::new)));
            case VERTEX -> new TableView(TableView.VERTEX_HEADER,
                    data.stream().filter(Vertex.class::isInstance)
                        .collect(Collectors.toCollection(ArrayList::new)));
            case EDGE -> new TableView(TableView.EDGE_HEADER,
                    data.stream().filter(Edge.class::isInstance)
                        .collect(Collectors.toCollection(ArrayList::new)));
            case PATH -> this.buildPathTableView(data);
        };
    }

    private TableView buildPathTableView(List<Object> data) {
        List<Object> paths = new ArrayList<>(data.size());
        for (Object object : data) {
            if (object instanceof Path path) {
                List<Object> ids = new ArrayList<>(path.size());
                for (Object element : path.objects()) {
                    if (element instanceof Vertex vertex) {
                        ids.add(vertex.id());
                    } else if (element instanceof Edge edge) {
                        ids.add(edge.id());
                    } else {
                        ids.add(element);
                    }
                }
                paths.add(ImmutableMap.of("path", ids));
            }
        }
        return new TableView(TableView.PATH_HEADER, paths);
    }

    /**
     * Cypher rows arrive as a map keyed by the RETURN variables whose values
     * are _type-tagged node/relationship maps. Unwrap them into detached
     * Vertex/Edge instances so such results can render as a graph; return
     * null when the row is not purely graph elements.
     */
    private static List<Object> unwrapCypherElements(Map<?, ?> row) {
        if (row.isEmpty()) {
            return null;
        }
        List<Object> elements = new ArrayList<>(row.size());
        for (Object value : row.values()) {
            Object element = toGraphElement(value);
            if (element == null) {
                return null;
            }
            elements.add(element);
        }
        return elements;
    }

    private static Object toGraphElement(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Object type = map.get("_type");
        if ("node".equals(type)) {
            Vertex vertex = new Vertex(String.valueOf(map.get("_label")));
            copyElementProperties(map, vertex::property);
            vertex.id(map.get("_id"));
            return vertex;
        }
        if ("relationship".equals(type)) {
            Edge edge = new Edge(String.valueOf(map.get("_label")));
            edge.id(String.valueOf(map.get("_id")));
            edge.sourceId(map.get("_outV"));
            edge.targetId(map.get("_inV"));
            copyElementProperties(map, edge::property);
            return edge;
        }
        return null;
    }

    private static void copyElementProperties(Map<?, ?> map,
                                              BiConsumer<String, Object> setter) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.startsWith("_") || entry.getValue() == null) {
                continue;
            }
            setter.accept(key, entry.getValue());
        }
    }

    private GraphView buildGraphView(TypedResult result, HugeClient client,
                                     boolean resultTruncated, boolean preview) {
        List<Object> data = result.getData();
        if (!result.getType().isGraph() || CollectionUtils.isEmpty(data)) {
            return new GraphView();
        }

        int vertexLimit = preview ? GRAPH_VERTEX_LIMIT : Integer.MAX_VALUE;
        int edgeLimit = preview ? this.config.get(HubbleOptions.GREMLIN_EDGES_TOTAL_LIMIT) : Integer.MAX_VALUE;
        Map<Object, Vertex> vertices = new LinkedHashMap<>();
        Map<String, Edge> edges = new LinkedHashMap<>();
        boolean truncated = resultTruncated;
        for (Object object : data) {
            List<?> elements = object instanceof Path path ?
                               path.objects() :
                               Collections.singletonList(object);
            for (Object element : elements) {
                if (element instanceof Vertex vertex) {
                    if (vertices.containsKey(vertex.id()) ||
                        vertices.size() < vertexLimit) {
                        vertices.putIfAbsent(vertex.id(), vertex);
                    } else {
                        truncated = true;
                    }
                } else if (element instanceof Edge edge) {
                    if (edges.containsKey(edge.id()) || edges.size() < edgeLimit) {
                        edges.putIfAbsent(edge.id(), edge);
                    } else {
                        truncated = true;
                    }
                } else if (object instanceof Path) {
                    return new GraphView();
                }
            }
        }

        try {
            if (!edges.isEmpty()) {
                // Paths already contain most or all endpoints. Fetch only the
                // missing endpoints, within the requested graph view budget.
                vertices.putAll(this.verticesOfEdge(edges, vertices, client, vertexLimit));
            } else if (!vertices.isEmpty() && !truncated) {
                // Preserve full raw query data without decorating oversized
                // vertex-only results through additional graph scans.
                if (vertices.size() > GRAPH_VERTEX_LIMIT) {
                    truncated = true;
                } else {
                    EdgePreview linked = this.edgesOfVertex(vertices, client);
                    edges = linked.edges();
                    truncated = linked.truncated();
                }
            }
        } catch (Exception e) {
            // Decoration must not discard the result of the user's query.
            log.warn("Failed to enrich graph view with linked elements", e);
            truncated = true;
        }

        int previousEdges = edges.size();
        if (preview) {
            final Map<Object, Vertex> previewVertices = vertices;
            edges.values().removeIf(edge -> !previewVertices.containsKey(edge.sourceId()) ||
                                            !previewVertices.containsKey(edge.targetId()));
        }
        GraphView view = new GraphView(vertices.values(), edges.values());
        view.setTruncated(truncated || previousEdges != edges.size());
        return view;
    }

    private String buildGremlinQuery(HugeClient client, AdjacentQuery query) {
        int degreeLimit = this.config.get(
                          HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT);

        Object id = this.getRealVertexId(client, query);
        StringBuilder sb = new StringBuilder("g.V(");
        // vertex id
        sb.append(GremlinUtil.escapeId(id)).append(")");
        // direction
        String direction = query.getDirection() != null ?
                           query.getDirection().name() :
                           Direction.BOTH.name();
        sb.append(".toE(").append(direction);
        // edge label
        if (query.getEdgeLabel() != null) {
            sb.append(", ").append(GremlinUtil.escape(query.getEdgeLabel()))
              .append(")");
        } else {
            sb.append(")");
        }
        if (query.getConditions() != null) {
            // properties
            for (AdjacentQuery.Condition condition : query.getConditions()) {
                // key
                sb.append(".has(").append(GremlinUtil.escape(condition.getKey()))
                  .append(", ");
                // value
                sb.append(this.checkConditionOperator(condition.getOperator()))
                  .append("(")
                  .append(GremlinUtil.escape(this.checkConditionValue(
                          condition.getValue())))
                    .append(")");
                sb.append(")");
            }
        }
        // limit
        sb.append(".limit(").append(degreeLimit).append(")");
        // other vertex
        sb.append(".otherV().path()");
        return sb.toString();
    }

    private String checkConditionOperator(String operator) {
        Ex.check(CONDITION_OPERATORS.contains(operator),
                 "common.param.should-belong-to", "condition.operator",
                 CONDITION_OPERATORS);
        return operator;
    }

    private Object checkConditionValue(Object value) {
        Ex.check(value instanceof String || value instanceof Number ||
                 value instanceof Boolean,
                 "common.param.should-belong-to", "condition.value",
                 "String/Number/Boolean");
        return value;
    }

    private Object getRealVertexId(HugeClient client, AdjacentQuery query) {
        VertexLabelEntity entity = this.vlService.get(query.getVertexLabel(),
                                                      client);
        IdStrategy idStrategy = entity.getIdStrategy();
        String rawVertexId = query.getVertexId();
        try {
            if (idStrategy == IdStrategy.AUTOMATIC ||
                idStrategy == IdStrategy.CUSTOMIZE_NUMBER) {
                return Long.parseLong(rawVertexId);
            } else if (idStrategy == IdStrategy.CUSTOMIZE_UUID) {
                return UUID.fromString(rawVertexId);
            }
        } catch (Exception e) {
            throw new ExternalException("gremlin.convert-vertex-id.failed", e,
                                        rawVertexId, idStrategy);
        }

        Ex.check(idStrategy == IdStrategy.PRIMARY_KEY ||
                 idStrategy == IdStrategy.CUSTOMIZE_STRING,
                 "Unsupported vertex id strategy: %s", idStrategy);
        return rawVertexId;
    }

    private record EdgePreview(Map<String, Edge> edges, boolean truncated) {
    }

    private EdgePreview edgesOfVertex(Map<Object, Vertex> vertices,
                                            HugeClient client) {
        final HugeConfig config = this.config;
        int batchSize = config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS);
        int edgeLimit = config.get(HubbleOptions.GREMLIN_EDGES_TOTAL_LIMIT);
        int degreeLimit = config.get(HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT);

        Set<Object> vertexIds = vertices.keySet();
        Map<String, Edge> edges = new LinkedHashMap<>();
        Map<Object, Integer> degrees = new HashMap<>();
        boolean truncated = false;
        for (List<Object> batch : Iterables.partition(vertexIds, batchSize)) {
            if (edges.size() >= edgeLimit) {
                // Do not query further batches just to prove completeness.
                truncated = true;
                break;
            }
            List<String> escapedIds = batch.stream()
                                          .map(GremlinUtil::escapeId)
                                          .collect(Collectors.toList());
            String ids = StringUtils.join(escapedIds, ",");
            int batchLimit = edgeLimit - edges.size();
            String gremlin = String.format("g.V(%s).bothE().dedup().limit(%s)",
                                           ids, batchLimit + 1);
            ResultSet resultSet = client.gremlin().gremlin(gremlin).execute();
            truncated |= resultSet.size() > batchLimit;
            for (Iterator<Result> iter = resultSet.iterator();
                 iter.hasNext() && edges.size() < edgeLimit;) {
                Object object = iter.next().getObject();
                if (!(object instanceof Edge)) {
                    continue;
                }
                Edge edge = (Edge) object;
                Object source = edge.sourceId();
                Object target = edge.targetId();
                if (edges.containsKey(edge.id()) || !vertexIds.contains(source) ||
                    !vertexIds.contains(target)) {
                    continue;
                }
                int sourceDegree = degrees.getOrDefault(source, 0);
                int targetDegree = degrees.getOrDefault(target, 0);
                int sourceIncrement = source.equals(target) ? 2 : 1;
                if (sourceDegree + sourceIncrement > degreeLimit ||
                    targetDegree + 1 > degreeLimit) {
                    truncated = true;
                    continue;
                }
                edges.put(edge.id(), edge);
                degrees.put(source, sourceDegree + 1);
                degrees.merge(target, 1, Integer::sum);
            }
        }
        return new EdgePreview(edges, truncated);
    }

    private Map<Object, Vertex> verticesOfEdge(Map<String, Edge> edges,
                                               Map<Object, Vertex> existing,
                                               HugeClient client, int vertexLimit) {
        int batchSize = this.config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS);
        int remaining = vertexLimit - existing.size();
        Set<Object> vertexIds = new LinkedHashSet<>();
        for (Edge edge : edges.values()) {
            for (Object id : new Object[]{edge.sourceId(), edge.targetId()}) {
                if (!existing.containsKey(id) && vertexIds.size() < remaining) {
                    vertexIds.add(id);
                }
            }
        }

        Map<Object, Vertex> vertices = new LinkedHashMap<>();
        for (List<Object> batch : Iterables.partition(vertexIds, batchSize)) {
            List<String> escapedIds = batch.stream()
                                          .map(GremlinUtil::escapeId)
                                          .collect(Collectors.toList());
            String ids = StringUtils.join(escapedIds, ",");
            ResultSet resultSet = client.gremlin().gremlin("g.V(" + ids + ")").execute();
            for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
                Vertex vertex = iter.next().getVertex();
                if (vertexIds.contains(vertex.id()) && vertices.size() < remaining) {
                    vertices.put(vertex.id(), vertex);
                }
            }
        }
        return vertices;
    }
}
