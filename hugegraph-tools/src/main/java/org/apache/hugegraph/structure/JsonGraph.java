/*
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

package org.apache.hugegraph.structure;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.JsonUtil;
import org.eclipse.jetty.util.ConcurrentHashSet;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class JsonGraph {

    private static final int INIT_VERTEX_CAPACITY = 1_000_000;

    private Map<String, Map<Object, JsonVertex>> tables;

    public JsonGraph() {
        this.tables = new ConcurrentHashMap<>();
    }

    public Set<String> tables() {
        return this.tables.keySet();
    }

    public void put(Vertex vertex) {
        // Add vertex to table of `label`
        Map<Object, JsonVertex> vertices = this.table(vertex.label());
        vertices.put(vertex.id(), JsonVertex.from(vertex));
    }

    public void put(Edge edge) {
        // Find source vertex
        Map<Object, JsonVertex> vertices = this.table(edge.sourceLabel());
        assert vertices != null;
        JsonVertex source = vertices.get(edge.sourceId());
        if (source == null) {
            // Printer.print("Invalid edge without source vertex: %s", edge);
            return;
        }

        // Find target vertex
        vertices = this.table(edge.targetLabel());
        assert vertices != null;
        JsonVertex target = vertices.get(edge.targetId());
        if (target == null) {
            // Printer.print("Invalid edge without target vertex: %s", edge);
            return;
        }

        // Add edge to source&target vertex
        JsonEdge jsonEdge = JsonEdge.from(edge);
        source.addEdge(jsonEdge);
        target.addEdge(jsonEdge);
    }

    public Map<Object, JsonVertex> table(String table) {
        Map<Object, JsonVertex> vertices = this.tables.get(table);
        if (vertices == null) {
            vertices = new ConcurrentHashMap<>(INIT_VERTEX_CAPACITY);
            this.tables.putIfAbsent(table, vertices);
        }
        return this.tables.get(table);
    }

    public static class JsonVertex {

        private Object id;
        private String label;
        private String properties;
        private Set<JsonEdge> edges;

        public JsonVertex() {
            this.edges = new ConcurrentHashSet<>();
        }

        public void addEdge(JsonEdge edge) {
            this.edges.add(edge);
        }

        public Object getId() {
            return this.id;
        }

        public String getLabel() {
            return this.label;
        }

        @JsonRawValue
        public String getProperties() {
            return this.properties;
        }

        public Set<JsonEdge> getEdges() {
            return this.edges;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> properties() {
            return JsonUtil.fromJson(this.properties, Map.class);
        }

        public static JsonVertex from(Vertex v) {
            JsonVertex vertex = new JsonVertex();
            vertex.id = v.id();
            vertex.label = v.label();
            vertex.properties = JsonUtil.toJson(v.properties());
            return vertex;
        }
    }

    public static class JsonEdge {

        private String id;
        private String label;
        private Object source;
        private Object target;
        private String properties;

        public String getId() {
            return this.id;
        }

        public String getLabel() {
            return this.label;
        }

        public Object getSource() {
            return this.source;
        }

        public Object getTarget() {
            return this.target;
        }

        @JsonRawValue
        public String getProperties() {
            return this.properties;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> properties() {
            return JsonUtil.fromJson(this.properties, Map.class);
        }

        public static JsonEdge from(Edge e) {
            JsonEdge edge = new JsonEdge();
            edge.id = e.id();
            edge.label = e.label();
            edge.source = e.sourceId();
            edge.target = e.targetId();
            edge.properties = JsonUtil.toJson(e.properties());
            return edge;
        }
    }
}
