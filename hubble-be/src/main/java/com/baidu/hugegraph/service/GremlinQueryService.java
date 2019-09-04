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

package com.baidu.hugegraph.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.entity.AdjacentQuery;
import com.baidu.hugegraph.entity.GremlinQuery;
import com.baidu.hugegraph.entity.GremlinResult;
import com.baidu.hugegraph.entity.GremlinResult.GraphView;
import com.baidu.hugegraph.entity.GremlinResult.Type;
import com.baidu.hugegraph.options.HubbleOptions;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.gremlin.Result;
import com.baidu.hugegraph.structure.gremlin.ResultSet;
import com.baidu.hugegraph.util.GremlinUtil;
import com.google.common.collect.Iterables;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GremlinQueryService {

    @Autowired
    private HugeConfig config;
    @Autowired
    private HugeClientPoolService poolService;

    private HugeClient getClient(Integer connectionId) {
        return this.poolService.getOrCreate(connectionId);
    }

    public GremlinResult executeQuery(GremlinQuery query) {
        HugeClient client = this.getClient(query.getConnectionId());

        String gremlin = this.optimize(query.getContent());
        log.debug("gremlin ==> {}", gremlin);
        // Execute gremlin query
        ResultSet resultSet = client.gremlin().gremlin(gremlin).execute();
        Type resultType = this.parseResultType(resultSet);
        // Build the graph view
        GraphView graphView = !resultType.isGraph() ? GraphView.EMPTY :
                              this.buildGraphView(resultSet, client);
        return GremlinResult.builder()
                            .type(resultType)
                            .data(resultSet.data())
                            .graphView(graphView)
                            .build();
    }

    public GremlinResult expandVertex(AdjacentQuery query) {
        HugeClient client = this.getClient(query.getConnectionId());

        // Build gremlin query
        String gremlin = this.buildGremlinQuery(query);
        log.debug("gremlin ==> {}", gremlin);
        // Execute gremlin query
        ResultSet resultSet = client.gremlin().gremlin(gremlin).execute();

        List<Vertex> vertices = new ArrayList<>(resultSet.size());
        List<Edge> edges = new ArrayList<>(resultSet.size());
        for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
            Path path = iter.next().getPath();
            List<Object> objects = path.objects();
            assert objects.size() == 3;
            edges.add((Edge) objects.get(1));
            vertices.add((Vertex) objects.get(2));
        }
        GraphView graphView = new GraphView(vertices, edges);
        return GremlinResult.builder()
                            .type(Type.PATH)
                            .data(resultSet.data())
                            .graphView(graphView)
                            .build();
    }

    private String optimize(String content) {
        // Remove the trailing redundant semicolon
        String gremlin = StringUtils.stripEnd(content, ";");
        int limit = this.config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT);
        return GremlinUtil.optimizeLimit(gremlin, limit);
    }

    private Type parseResultType(ResultSet resultSet) {
        if (resultSet == null) {
            return Type.EMPTY;
        }
        Iterator<Result> iter = resultSet.iterator();
        if (!iter.hasNext()) {
            return Type.EMPTY;
        }
        Result result = iter.next();
        if (result == null) {
            return Type.EMPTY;
        }
        Object object = result.getObject();
        if (object instanceof Vertex) {
            return Type.VERTEX;
        } else if (object instanceof Edge) {
            return Type.EDGE;
        } else if (object instanceof Path) {
            return Type.PATH;
        } else {
            return Type.GENERAL;
        }
    }

    private GraphView buildGraphView(ResultSet resultSet, HugeClient client) {
        GraphView graphView = new GraphView();
        if (resultSet.data() == null || resultSet.data().isEmpty()) {
            return graphView;
        }

        Map<Object, Vertex> vertices = new HashMap<>();
        Map<String, Edge> edges = new HashMap<>();
        for (Iterator<Result> iter = resultSet.iterator(); iter.hasNext();) {
            Result result = iter.next();
            Object object = result.getObject();
            if (object instanceof Vertex) {
                Vertex vertex = result.getVertex();
                vertices.put(vertex.id(), vertex);
            } else if (object instanceof Edge) {
                Edge edge = result.getEdge();
                edges.put(edge.id(), edge);
            } else if (object instanceof Path) {
                List<Object> elements = ((Path) object).objects();
                for (Object element : elements) {
                    if (element instanceof Vertex) {
                        Vertex vertex = (Vertex) element;
                        vertices.put(vertex.id(), vertex);
                    } else {
                        Edge edge = (Edge) element;
                        edges.put(edge.id(), edge);
                    }
                }
            }
        }

        if (!edges.isEmpty()) {
            if (vertices.isEmpty()) {
                vertices = this.verticesOfEdge(edges, client);
            } else {
                // TODO: reduce the number of requests
                vertices.putAll(this.verticesOfEdge(edges, client));
            }
        } else {
            if (!vertices.isEmpty()) {
                edges = this.edgesOfVertex(vertices, client);
            }
        }
        graphView.setVertices(vertices.values());
        graphView.setEdges(edges.values());
        return graphView;
    }

    private String buildGremlinQuery(AdjacentQuery query) {
        int degreeLimit = this.config.get(
                          HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT);
        StringBuilder sb = new StringBuilder("g.V(");
        // vertex id
        sb.append(GremlinUtil.escapeId(query.getVertexId())).append(")");
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

    private Map<String, Edge> edgesOfVertex(Map<Object, Vertex> vertices,
                                            HugeClient client) {
        int edgeLimit = this.config.get(HubbleOptions.GREMLIN_EDGES_TOTAL_LIMIT);
        int batchSize = this.config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS);
        int degreeLimit = this.config.get(
                          HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT);

        Set<Object> vertexIds = vertices.keySet();
        Map<String, Edge> edges = new HashMap<>(vertexIds.size());
        Iterables.partition(vertexIds, batchSize).forEach(batch -> {
            List<String> escapedIds = batch.stream()
                                           .map(GremlinUtil::escapeId)
                                           .collect(Collectors.toList());
            String ids = StringUtils.join(escapedIds, ",");
            // Exist better way?
            String gremlin = String.format("g.V(%s).bothE().dedup()" +
                                           ".limit(800000)", ids);
            ResultSet rs = client.gremlin().gremlin(gremlin).execute();
            // The edges count for per vertex
            Map<Object, Integer> degrees = new HashMap<>(rs.size());
            for (Iterator<Result> iter = rs.iterator(); iter.hasNext();) {
                Edge edge = iter.next().getEdge();
                Object source = edge.sourceId();
                Object target = edge.targetId();
                // only add the interconnected edges of the found vertices
                if (!vertexIds.contains(source) ||
                    !vertexIds.contains(target)) {
                    continue;
                }

                int degree = degrees.computeIfAbsent(source, k -> 0);
                degrees.put(source, ++degree);
                if (degree >= degreeLimit) {
                    break;
                }
                degree = degrees.computeIfAbsent(target, k -> 0);
                degrees.put(target, ++degree);
                if (degree >= degreeLimit) {
                    break;
                }

                edges.put(edge.id(), edge);
                if (edges.size() >= edgeLimit) {
                    break;
                }
            }
        });
        return edges;
    }

    private Map<Object, Vertex> verticesOfEdge(Map<String, Edge> edges,
                                               HugeClient client) {
        int batchSize = this.config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS);

        Set<Object> vertexIds = new HashSet<>(edges.size() * 2);
        edges.values().forEach(edge -> {
            vertexIds.add(edge.sourceId());
            vertexIds.add(edge.targetId());
        });

        Map<Object, Vertex> vertices = new HashMap<>(vertexIds.size());
        Iterables.partition(vertexIds, batchSize).forEach(batch -> {
            List<Vertex> results = client.traverser().vertices(batch);
            results.forEach(vertex -> vertices.put(vertex.id(), vertex));
        });
        return vertices;
    }
}
