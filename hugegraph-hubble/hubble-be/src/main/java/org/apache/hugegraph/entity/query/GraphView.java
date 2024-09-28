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

package org.apache.hugegraph.entity.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.entity.graph.VertexQueryEntity;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphView {

    public static final GraphView EMPTY = new GraphView();
    private static final int STATISTICS_ROWS = 10;
    private static final int TOP_DEGREE_NUMBER = 10;

    @JsonProperty("vertices")
    private Collection<VertexQueryEntity> vertices;
    @JsonProperty("edges")
    private Collection<Edge> edges;
    @JsonProperty("statistics")
    private Map<String, Object> statistics = new HashMap<>();

    public GraphView(Collection<Vertex> vertices,
                     Collection<Edge> edges) {
        Collection<VertexQueryEntity> vertexQueryEntities =
                new ArrayList<>(vertices.size());
        for (Vertex vertex: vertices) {
            vertexQueryEntities.add(VertexQueryEntity.fromVertex(vertex));
        }
        this.vertices = vertexQueryEntities;
        this.edges = edges;
        this.statistics = fillStatistics(vertexQueryEntities, edges);
    }

    private Map<String, Object> fillStatistics(
            Collection<VertexQueryEntity> vertexList,
            Collection<Edge> edges) {
        // 悬空边: 顶点展开或者图本身有悬空边
        // incidence_vertices, etc..
        Map<Object, VertexQueryEntity> vertices =
                new HashMap<>(vertexList.size());
        for (VertexQueryEntity vertex: vertexList) {
            vertices.put(vertex.id(), vertex);
        }

        fillVertexStatistics(vertices, edges);
        Map<Object, Integer> degrees = getDegrees(edges, vertices);

        Map<String, Object> statistics = new HashMap<>(STATISTICS_ROWS);
        List<Map<String, Object>> highestDegreeVertices =
                getHighestDegreeVertices(degrees);
        List<Object> isolatedVertices = getIsolatedVertices(degrees);
        List<Map<String, Object>> vertexLabelList = getVertexLabelList(vertices);
        List<String> isolatedEdges = getIsolatedEdges(edges, vertices);
        List<Map<String, Object>> edgeLabelList = getEdgeLabelList(edges);

        statistics.put("highest_degree_vertices", highestDegreeVertices);
        statistics.put("isolated_vertices", isolatedVertices);
        statistics.put("isolated_edges", isolatedEdges);
        statistics.put("vertex_label", vertexLabelList);
        statistics.put("edge_label", edgeLabelList);
        return statistics;
    }

    private Map<Object, Integer> getDegrees(Collection<Edge> edges,
                                            Map<Object, VertexQueryEntity> vertices) {
        Map<Object, Integer> degrees = new HashMap<>(vertices.size());
        for (Map.Entry<Object, VertexQueryEntity> entry: vertices.entrySet()) {
            degrees.put(entry.getKey(), 0);
        }

        for (Edge edge: edges) {
            // 悬空边: 会为相邻顶点计算度
            degrees.compute(edge.sourceId(), (k, v) -> v == null ? 1 : v + 1);
            degrees.compute(edge.targetId(), (k, v) -> v == null ? 1 : v + 1);
        }
        return degrees;
    }

    private void fillVertexStatistics(Map<Object, VertexQueryEntity> vertices,
                                      Collection<Edge> edges) {
        // init
        for (VertexQueryEntity vertex: vertices.values()) {
            if (vertex.getStatistics().get("incidence_vertices") == null) {
                Set<Object> incidenceVertices = new HashSet<>(edges.size());
                vertex.getStatistics().put("incidence_vertices",
                                           incidenceVertices);
            }
        }

        // calculate
        for (Edge edge: edges) {
            Object source = edge.sourceId();
            Object target = edge.targetId();

            if (vertices.get(source) == null || vertices.get(target) == null) {
                continue;
            }
            Set<Object> incidenceVertices =
                    (Set<Object>) vertices.get(source)
                                          .getStatistics()
                                          .get("incidence_vertices");
            incidenceVertices.add(target);
            incidenceVertices =
                    (Set<Object>) vertices.get(target)
                                          .getStatistics()
                                          .get("incidence_vertices");
            incidenceVertices.add(source);
        }
    }

    private List<Map<String, Object>> getVertexLabelList(Map<Object,
            VertexQueryEntity> vertices) {
        Map<String, Integer> vertexLabels = new HashMap<>(vertices.size());
        for (Map.Entry<Object, VertexQueryEntity> entry: vertices.entrySet()) {
            VertexQueryEntity vertex = entry.getValue();
            vertexLabels.compute(vertex.label(), (k, v) -> v == null ? 1 : v + 1);
        }
        return vertexLabels.entrySet().stream().map(e -> {
            Map<String, Object> res = new HashMap<>();
            res.put("label", e.getKey());
            res.put("count", e.getValue());
            return res;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getEdgeLabelList(Collection<Edge> edges) {
        Map<String, Integer> edgeLabels = new HashMap<>(edges.size());
        for (Edge edge: edges) {
            edgeLabels.compute(edge.label(), (k, v) -> v == null ? 1 : v + 1);
        }
        return edgeLabels.entrySet().stream().map(e -> {
            Map<String, Object> res = new HashMap<>();
            res.put("label", e.getKey());
            res.put("count", e.getValue());
            return res;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getHighestDegreeVertices(Map<Object,
            Integer> degrees) {
        return degrees.entrySet().stream()
                      .sorted(Comparator.comparing(e -> e.getValue(),
                                                   Comparator.reverseOrder()))
                      .map(e -> {
                          Map<String, Object> res = new HashMap<>();
                          res.put("id", e.getKey());
                          res.put("degree", e.getValue());
                          return res;
                      }).collect(Collectors.toList())
                      .subList(0, Math.min(TOP_DEGREE_NUMBER,
                                           degrees.size()));
    }

    private List<Object> getIsolatedVertices(Map<Object, Integer> degrees) {
        return degrees.entrySet().stream()
                      .filter(e -> e.getValue() == 0)
                      .map(e -> e.getKey())
                      .collect(Collectors.toList());
    }

    private List<String> getIsolatedEdges(
            Collection<Edge> edges, Map<Object, VertexQueryEntity> vertices) {
        List<String> isolatedEdges = new ArrayList<>(edges.size());
        for (Edge edge: edges) {
            Object source = edge.sourceId();
            Object target = edge.targetId();

            if (vertices.get(source) == null || vertices.get(target) == null) {
                continue;
            }
            Set<Object> sourceIncidenceVertices =
                    (Set<Object>) vertices.get(source)
                                          .getStatistics()
                                          .get("incidence_vertices");
            Set<Object> targetIncidenceVertices =
                    (Set<Object>) vertices.get(target)
                                          .getStatistics()
                                          .get("incidence_vertices");

            if (sourceIncidenceVertices.size() == 1 &&
                targetIncidenceVertices.size() == 1) {
                isolatedEdges.add(edge.id());
            }
        }
        return isolatedEdges;
    }
}
