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

package com.baidu.hugegraph.driver;

import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.api.graph.EdgeAPI;
import com.baidu.hugegraph.api.graph.VertexAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.exception.InvalidOperationException;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;

public class GraphManager {

    private VertexAPI vertexApi;
    private EdgeAPI edgeApi;

    public GraphManager(RestClient client, String graph) {
        this.vertexApi = new VertexAPI(client, graph);
        this.edgeApi = new EdgeAPI(client, graph);
    }

    public Vertex addVertex(Vertex vertex) {
        vertex = this.vertexApi.create(vertex);
        this.attachManager(vertex);
        return vertex;
    }

    public Vertex addVertex(Object... keyValues) {
        String label = this.getValue(T.label, keyValues);
        Vertex vertex = new Vertex(label);
        vertex.id(this.getValue(T.id, keyValues));
        this.attachProperties(vertex, keyValues);
        return this.addVertex(vertex);
    }

    public Vertex getVertex(String vertexId) {
        Vertex vertex = this.vertexApi.get(vertexId);
        this.attachManager(vertex);
        return vertex;
    }

    public List<Vertex> addVertices(List<Vertex> vertices) {
        List<String> ids = this.vertexApi.create(vertices);
        for (int i = 0; i < vertices.size(); i++) {
            Vertex vertex = vertices.get(i);
            vertex.id(ids.get(i));
            this.attachManager(vertex);
        }
        return vertices;
    }

    public List<Vertex> listVertices() {
        return this.listVertices(-1);
    }

    public List<Vertex> listVertices(int limit) {
        return this.listVertices(null, null, limit);
    }

    public List<Vertex> listVertices(String label) {
        return this.listVertices(label, null, -1);
    }

    public List<Vertex> listVertices(String label, int limit) {
        return this.listVertices(label, null, limit);
    }

    public List<Vertex> listVertices(String label,
                                     Map<String, Object> properties) {
        return this.listVertices(label, properties, -1);
    }

    public List<Vertex> listVertices(String label,
                                     Map<String, Object> properties,
                                     int limit) {
        List<Vertex> vertices = this.vertexApi.list(label, properties, limit);
        for (Vertex vertex : vertices) {
            this.attachManager(vertex);
        }
        return vertices;
    }

    public void removeVertex(String vertexId) {
        this.vertexApi.delete(vertexId);
    }

    public Vertex appendVertexProperty(Vertex vertex) {
        vertex = this.vertexApi.append(vertex);
        this.attachManager(vertex);
        return vertex;
    }

    public Vertex eliminateVertexProperty(Vertex vertex) {
        vertex = this.vertexApi.eliminate(vertex);
        this.attachManager(vertex);
        return vertex;
    }

    public Edge addEdge(Edge edge) {
        if (edge.id() != null) {
            throw new InvalidOperationException(
                      "Not allowed to custom id for edge: '%s'", edge);
        }
        edge = this.edgeApi.create(edge);
        this.attachManager(edge);
        return edge;
    }

    public Edge addEdge(Vertex source, String label, Vertex target,
                        Object... properties) {
        return this.addEdge(source.id(), label, target.id(), properties);
    }

    public Edge addEdge(String sourceId, String label, String targetId,
                        Object... properties) {
        Edge edge = new Edge(label);
        edge.source(sourceId);
        edge.target(targetId);
        this.attachProperties(edge, properties);
        return this.addEdge(edge);
    }

    public Edge getEdge(String edgeId) {
        Edge edge = this.edgeApi.get(edgeId);
        this.attachManager(edge);
        return edge;
    }

    public List<Edge> addEdges(List<Edge> edges) {
        return this.addEdges(edges, true);
    }

    public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
        List<String> ids = this.edgeApi.create(edges, checkVertex);
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            edge.id(ids.get(i));
            this.attachManager(edge);
        }
        return edges;
    }

    public List<Edge> listEdges() {
        return this.listEdges(-1);
    }

    public List<Edge> listEdges(int limit) {
        return this.getEdges(null, null, null, null, limit);
    }

    public List<Edge> listEdges(String label) {
        return this.getEdges(null, null, label, null, -1);
    }

    public List<Edge> listEdges(String label, int limit) {
        return this.getEdges(null, null, label, null, limit);
    }

    public List<Edge> listEdges(String label,
                                Map<String, Object> properties) {
        return this.getEdges(null, null, label, properties, -1);
    }

    public List<Edge> listEdges(String label,
                                Map<String, Object> properties,
                                int limit) {
        return this.getEdges(null, null, label, properties, limit);
    }

    public List<Edge> getEdges(String vertexId) {
        return this.getEdges(vertexId, Direction.BOTH, null, null, -1);
    }

    public List<Edge> getEdges(String vertexId, int limit) {
        return this.getEdges(vertexId, Direction.BOTH, null, null, limit);
    }

    public List<Edge> getEdges(String vertexId, Direction direction) {
        return this.getEdges(vertexId, direction, null, null, -1);
    }

    public List<Edge> getEdges(String vertexId,
                               Direction direction,
                               int limit) {
        return this.getEdges(vertexId, direction, null, null, limit);
    }

    public List<Edge> getEdges(String vertexId,
                               Direction direction,
                               String label) {
        return this.getEdges(vertexId, direction, label, null, -1);
    }

    public List<Edge> getEdges(String vertexId,
                               Direction direction,
                               String label,
                               int limit) {
        return this.getEdges(vertexId, direction, label, null, limit);
    }

    public List<Edge> getEdges(String vertexId,
                               Direction direction,
                               String label,
                               Map<String, Object> properties) {
        return this.getEdges(vertexId, direction, label, properties, -1);
    }

    public List<Edge> getEdges(String vertexId,
                               Direction direction,
                               String label,
                               Map<String, Object> properties,
                               int limit) {
        List<Edge> edges = this.edgeApi.list(vertexId, direction, label,
                                             properties, limit);
        for (Edge edge : edges) {
            this.attachManager(edge);
        }
        return edges;
    }

    public void removeEdge(String edgeId) {
        this.edgeApi.delete(edgeId);
    }

    public Edge appendEdgeProperty(Edge edge) {
        edge = this.edgeApi.append(edge);
        this.attachManager(edge);
        return edge;
    }

    public Edge eliminateEdgeProperty(Edge edge) {
        edge = this.edgeApi.eliminate(edge);
        this.attachManager(edge);
        return edge;
    }

    private String getValue(String key, Object... keyValues) {
        E.checkArgument((keyValues.length & 0x01) == 0,
                        "The number of parameters must be even");
        String value = null;
        for (int i = 0; i < keyValues.length; i = i + 2) {
            if (keyValues[i].equals(key)) {
                if (!(keyValues[i + 1] instanceof String)) {
                    throw new IllegalArgumentException(String.format(
                              "Expect a string value as the vertex label " +
                              "argument, but got: %s", keyValues[i + 1]));
                }
                value = (String) keyValues[i + 1];
                break;
            }
        }
        return value;
    }

    private void attachProperties(GraphElement element, Object... properties) {
        E.checkArgument((properties.length & 0x01) == 0,
                        "The number of properties must be even");
        for (int i = 0; i < properties.length; i = i + 2) {
            if (!properties[i].equals(T.id) &&
                !properties[i].equals(T.label)) {
                element.property((String) properties[i], properties[i + 1]);
            }
        }
    }

    private void attachManager(GraphElement element) {
        element.attachManager(this);
    }
}
