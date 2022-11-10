/*
 * Copyright 2017 HugeGraph Authors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.IllegalGremlinException;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.AdjacentQuery;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.entity.query.GremlinResult.Type;
import org.apache.hugegraph.entity.query.JsonView;
import org.apache.hugegraph.entity.query.TableView;
import org.apache.hugegraph.entity.query.TypedResult;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.Result;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.GremlinUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class GremlinQueryService {

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

    private static final String TIMEOUT_EXCEPTION =
            "java.net.SocketTimeoutException";

    private static final String CONN_REFUSED_MSG = "Connection refused";

    @Autowired
    private HugeConfig config;
    @Autowired
    private HugeClientPoolService poolService;
    @Autowired
    private VertexLabelService vlService;

    private HugeClient getClient(int connId) {
        return this.poolService.getOrCreate(connId);
    }

    public GremlinResult executeQuery(int connId, GremlinQuery query) {
        HugeClient client = this.getClient(connId);

        log.debug("The original gremlin ==> {}", query.getContent());
        String gremlin = this.optimize(query.getContent());
        log.debug("The optimized gremlin ==> {}", gremlin);
        // Execute gremlin query
        ResultSet resultSet = this.executeGremlin(gremlin, client);
        // Scan data, vote the result type
        TypedResult typedResult = this.parseResults(resultSet);
        // Build json view
        JsonView jsonView = new JsonView(typedResult.getData());
        // Build table view
        TableView tableView = this.buildTableView(typedResult);
        // Build graph view
        GraphView graphView = this.buildGraphView(typedResult, client);
        return GremlinResult.builder()
                            .type(typedResult.getType())
                            .jsonView(jsonView)
                            .tableView(tableView)
                            .graphView(graphView)
                            .build();
    }

    public Long executeAsyncTask(int connId, GremlinQuery query) {
        HugeClient client = this.getClient(connId);

        log.debug("The async gremlin ==> {}", query.getContent());
        // Execute optimized gremlin query
        GremlinRequest request = new GremlinRequest(query.getContent());
        return client.gremlin().executeAsTask(request);
    }

    public GremlinResult expandVertex(int connId, AdjacentQuery query) {
        HugeClient client = this.getClient(connId);

        // Build gremlin query
        String gremlin = this.buildGremlinQuery(connId, query);
        log.debug("expand vertex gremlin ==> {}", gremlin);
        // Execute gremlin query
        ResultSet resultSet = this.executeGremlin(gremlin, client);

        List<Vertex> vertices = new ArrayList<>(resultSet.size());
        List<Edge> edges = new ArrayList<>(resultSet.size());
        for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
            Path path = iter.next().getPath();
            List<Object> objects = path.objects();
            assert objects.size() == 3;
            Edge edge = (Edge) objects.get(1);
            Vertex vertex = (Vertex) objects.get(2);
            // Filter vertices and edges that existed in query
            if (query.retainEdge(edge)) {
                edges.add(edge);
            }
            if (query.retainVertex(vertex)) {
                vertices.add(vertex);
            }
        }
        // Build graph view
        GraphView graphView = new GraphView(vertices, edges);
        return GremlinResult.builder()
                            .type(Type.PATH)
                            .graphView(graphView)
                            .build();
    }

    private String optimize(String content) {
        // Optimize each gremlin statemnts
        int limit = this.config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT);
        String[] originalParts = StringUtils.split(content, ";");
        String[] optimizeParts = new String[originalParts.length];
        for (int i = 0; i < originalParts.length; i++) {
            String part = originalParts[i];
            optimizeParts[i] = GremlinUtil.optimizeLimit(part, limit);
        }
        return StringUtils.join(optimizeParts, ";");
    }

    private ResultSet executeGremlin(String gremlin, HugeClient client) {
        try {
            return client.gremlin().gremlin(gremlin).execute();
        } catch (ServerException e) {
            String exception = e.exception();
            log.error("Gremlin execute failed: {}", exception);
            if (ILLEGAL_GREMLIN_EXCEPTIONS.contains(exception)) {
                throw new IllegalGremlinException("gremlin.illegal-statemnt", e,
                                                  e.message());
            }
            throw new ExternalException("gremlin.execute.failed", e, e.message());
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
            log.error("Gremlin execute failed", e);
            throw new ExternalException("gremlin.execute.failed", e,
                                        e.getMessage());
        }
    }

    private TypedResult parseResults(ResultSet resultSet) {
        if (resultSet == null) {
            return new TypedResult(Type.EMPTY, null);
        }
        Iterator<Result> iter = resultSet.iterator();
        if (!iter.hasNext()) {
            return new TypedResult(Type.EMPTY, null);
        }

        Map<Type, Integer> typeVotes = new HashMap<>();
        List<Object> typedData = new ArrayList<>(resultSet.size());
        while (iter.hasNext()) {
            Result result = iter.next();
            if (result == null) {
                // NOTE: null value doesn't vote
                continue;
            }
            Object object = result.getObject();
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
        return new TypedResult(type, typedData);
    }

    private TableView buildTableView(TypedResult typedResult) {
        List<Object> data = typedResult.getData();
        if (CollectionUtils.isEmpty(data)) {
            return TableView.EMPTY;
        }

        switch (typedResult.getType()) {
            case EMPTY:
                return TableView.EMPTY;
            case GENERAL:
                // result
                List<Object> results = new ArrayList<>(data.size());
                data.forEach(object -> {
                    results.add(ImmutableMap.of("result", object));
                });
                return new TableView(TableView.GENERAL_HEADER, results);
            case VERTEX:
                // id, label, properties
                List<Object> vertices = new ArrayList<>(data.size());
                data.forEach(object -> {
                    if (object instanceof Vertex) {
                        vertices.add(object);
                    }
                });
                return new TableView(TableView.VERTEX_HEADER, vertices);
            case EDGE:
                // id, label, source, target, properties
                List<Object> edges = new ArrayList<>(data.size());
                data.forEach(object -> {
                    if (object instanceof Edge) {
                        edges.add(object);
                    }
                });
                return new TableView(TableView.EDGE_HEADER, edges);
            case PATH:
                // path, only fill vertex/edge id
                List<Object> paths = new ArrayList<>(data.size());
                data.forEach(object -> {
                    if (object instanceof Path) {
                        Path path = (Path) object;
                        List<Object> ids = new ArrayList<>();
                        path.objects().forEach(element -> {
                            if (element instanceof Vertex) {
                                ids.add(((Vertex) element).id());
                            } else if (element instanceof Edge) {
                                ids.add(((Edge) element).id());
                            } else {
                                ids.add(element);
                            }
                        });
                        paths.add(ImmutableMap.of("path", ids));
                    }
                });
                return new TableView(TableView.PATH_HEADER, paths);
            default:
                throw new AssertionError(String.format(
                          "Unknown result type '%s'", typedResult.getType()));
        }
    }

    private GraphView buildGraphView(TypedResult result, HugeClient client) {
        List<Object> data = result.getData();
        if (!result.getType().isGraph() || CollectionUtils.isEmpty(data)) {
            return GraphView.EMPTY;
        }

        Map<Object, Vertex> vertices = new HashMap<>();
        Map<String, Edge> edges = new HashMap<>();
        for (Object object : data) {
            if (object instanceof Vertex) {
                Vertex vertex = (Vertex) object;
                vertices.put(vertex.id(), vertex);
            } else if (object instanceof Edge) {
                Edge edge = (Edge) object;
                edges.put(edge.id(), edge);
            } else if (object instanceof Path) {
                List<Object> elements = ((Path) object).objects();
                for (Object element : elements) {
                    if (element instanceof Vertex) {
                        Vertex vertex = (Vertex) element;
                        vertices.put(vertex.id(), vertex);
                    } else if (element instanceof Edge) {
                        Edge edge = (Edge) element;
                        edges.put(edge.id(), edge);
                    } else {
                        return GraphView.EMPTY;
                    }
                }
            }
        }

        if (!edges.isEmpty()) {
            if (vertices.isEmpty()) {
                vertices = this.verticesOfEdge(result, edges, client);
            } else {
                // TODO: reduce the number of requests
                vertices.putAll(this.verticesOfEdge(result, edges, client));
            }
        } else {
            if (!vertices.isEmpty()) {
                edges = this.edgesOfVertex(result, vertices, client);
            }
        }

        if (!edges.isEmpty()) {
            Ex.check(!vertices.isEmpty(),
                     "gremlin.edges.linked-vertex.not-exist");
        }
        return new GraphView(vertices.values(), edges.values());
    }

    private String buildGremlinQuery(int connId, AdjacentQuery query) {
        int degreeLimit = this.config.get(
                          HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT);

        Object id = this.getRealVertexId(connId, query);
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
            sb.append(", '").append(query.getEdgeLabel()).append("')");
        } else {
            sb.append(")");
        }
        if (query.getConditions() != null) {
            // properties
            for (AdjacentQuery.Condition condition : query.getConditions()) {
                // key
                sb.append(".has('").append(condition.getKey()).append("', ");
                // value
                sb.append(condition.getOperator()).append("(")
                  .append(GremlinUtil.escape(condition.getValue())).append(")");
                sb.append(")");
            }
        }
        // limit
        sb.append(".limit(").append(degreeLimit).append(")");
        // other vertex
        sb.append(".otherV().path()");
        return sb.toString();
    }

    private Object getRealVertexId(int connId, AdjacentQuery query) {
        VertexLabelEntity entity = this.vlService.get(query.getVertexLabel(),
                                                      connId);
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

        assert idStrategy == IdStrategy.PRIMARY_KEY ||
               idStrategy == IdStrategy.CUSTOMIZE_STRING;
        return rawVertexId;
    }

    private Map<String, Edge> edgesOfVertex(TypedResult result,
                                            Map<Object, Vertex> vertices,
                                            HugeClient client) {
        final HugeConfig config = this.config;
        int batchSize = config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS);
        int edgeLimit = config.get(HubbleOptions.GREMLIN_EDGES_TOTAL_LIMIT);
        int degreeLimit = config.get(HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT);

        Set<Object> vertexIds = vertices.keySet();
        Map<String, Edge> edges = new HashMap<>(vertexIds.size());
        Iterables.partition(vertexIds, batchSize).forEach(batch -> {
            List<String> escapedIds = batch.stream()
                                           .map(GremlinUtil::escapeId)
                                           .collect(Collectors.toList());
            String ids = StringUtils.join(escapedIds, ",");
            // Any better way to find two vertices has linked?
            String gremlin;
            if (result.getType().isPath()) {
                // If result type is path, the vertices count not too much in theory
                gremlin = String.format("g.V(%s).bothE().local(limit(%s)).dedup()",
                                        ids, degreeLimit);
            } else {
                gremlin = String.format("g.V(%s).bothE().dedup().limit(%s)",
                                        ids, edgeLimit);
            }
            ResultSet resultSet = client.gremlin().gremlin(gremlin).execute();
            // The edges count for per vertex
            Map<Object, Integer> degrees = new HashMap<>(resultSet.size());
            for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
                Edge edge = iter.next().getEdge();
                Object source = edge.sourceId();
                Object target = edge.targetId();
                // only add the interconnected edges of the found vertices
                if (!vertexIds.contains(source) || !vertexIds.contains(target)) {
                    continue;
                }
                edges.put(edge.id(), edge);
                if (edges.size() >= edgeLimit) {
                    break;
                }

                int deg = degrees.compute(source, (k, v) -> v == null ? 1 : v + 1);
                if (deg >= degreeLimit) {
                    break;
                }
                deg = degrees.compute(target, (k, v) -> v == null ? 1 : v + 1);
                if (deg >= degreeLimit) {
                    break;
                }
            }
        });
        return edges;
    }

    private Map<Object, Vertex> verticesOfEdge(TypedResult result,
                                               Map<String, Edge> edges,
                                               HugeClient client) {
        int batchSize = this.config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS);

        Set<Object> vertexIds = new HashSet<>(edges.size() * 2);
        edges.values().forEach(edge -> {
            vertexIds.add(edge.sourceId());
            vertexIds.add(edge.targetId());
        });

        Map<Object, Vertex> vertices = new HashMap<>(vertexIds.size());
        Iterables.partition(vertexIds, batchSize).forEach(batch -> {
            List<String> escapedIds = batch.stream()
                                           .map(GremlinUtil::escapeId)
                                           .collect(Collectors.toList());
            String ids = StringUtils.join(escapedIds, ",");
            String gremlin = String.format("g.V(%s)", ids);
            ResultSet resultSet = client.gremlin().gremlin(gremlin).execute();
            for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
                Vertex vertex = iter.next().getVertex();
                vertices.put(vertex.id(), vertex);
            }
        });
        return vertices;
    }
}
