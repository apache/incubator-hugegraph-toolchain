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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.driver;

import java.util.List;

import com.baidu.hugegraph.api.graph.EdgeAPI;
import com.baidu.hugegraph.api.graph.VertexAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.exception.ClientException;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;


public class GraphManager {

    private VertexAPI vertexApi;
    private EdgeAPI edgeApi;

    public GraphManager(String url, String graph) {
        RestClient client = new RestClient(url);
        this.vertexApi = new VertexAPI(client, graph);
        this.edgeApi = new EdgeAPI(client, graph);
    }

    public Vertex addVertex(Vertex vertex) {
        if (vertex.id() != null) {
            // TODO: Need to perform exception.
            throw new ClientException(String.format("Not allowed "
                    + "to custom id for vertex: '%s'", vertex));
        }
        vertex = this.vertexApi.create(vertex);
        vertex.manager(this);
        return vertex;
    }

    public Vertex addVertex(Object... keyValues) {
        String label = getLabelValue(keyValues);
        Vertex vertex = new Vertex(label);
        this.attachProperties(vertex, keyValues);
        vertex = this.vertexApi.create(vertex);
        vertex.manager(this);
        return vertex;
    }

    public Vertex getVertex(String vertexId) {
        Vertex vertex = this.vertexApi.get(vertexId);
        vertex.manager(this);
        return vertex;
    }

    public List<Vertex> addVertices(List<Vertex> vertices) {
        List<String> ids = this.vertexApi.create(vertices);
        assert vertices.size() == ids.size();
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).id(ids.get(i));
        }
        return vertices;
    }

    public List<Vertex> getVertices() {
        return this.getVertices(-1);
    }

    public List<Vertex> getVertices(int limit) {
        List<Vertex> vertices = this.vertexApi.list(limit);
        for (Vertex vertex : vertices) {
            vertex.manager(this);
        }
        return vertices;
    }

    public void removeVertex(String vertexId) {
        this.vertexApi.delete(vertexId);
    }

    public Edge addEdge(Edge edge) {
        if (edge.id() != null) {
            throw new ClientException(String.format("Not allowed "
                    + "to custom id for edge: '%s'", edge));
        }
        return this.edgeApi.create(edge);
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
        return this.edgeApi.create(edge);
    }

    public Edge getEdge(String edgeId) {
        return this.edgeApi.get(edgeId);
    }

    public List<Edge> addEdges(List<Edge> edges) {
        return this.addEdges(edges, true);
    }

    public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
        List<String> ids = this.edgeApi.create(edges, checkVertex);
        assert edges.size() == ids.size();
        for (int i = 0; i < edges.size(); i++) {
            edges.get(i).id(ids.get(i));
        }
        return edges;
    }

    public List<Edge> getEdges() {
        return this.getEdges(-1);
    }

    public List<Edge> getEdges(int limit) {
        return this.edgeApi.list(limit);
    }

    public void removeEdge(String edgeId) {
        this.edgeApi.delete(edgeId);
    }

    private String getLabelValue(Object... keyValues) {
        String labelValue = null;
        for (int i = 0; i < keyValues.length; i = i + 2) {
            if (keyValues[i].equals(T.label)) {
                if (!(keyValues[i + 1] instanceof String)) {
                    throw new IllegalArgumentException(String.format(
                            "Expected a string value as the vertex label " +
                            "argument, but got: %s", labelValue));
                }
                labelValue = (String) keyValues[i + 1];
                break;
            }
        }
        return labelValue;
    }

    public void attachProperties(GraphElement element, Object... properties) {
        for (int i = 0; i < properties.length; i = i + 2) {
            if (!properties[i].equals(T.label)) {
                element.property((String) properties[i], properties[i + 1]);
            }
        }
    }
}
