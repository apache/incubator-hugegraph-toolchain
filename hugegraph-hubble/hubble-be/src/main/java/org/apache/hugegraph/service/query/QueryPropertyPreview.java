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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.query.TypedResult;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;

final class QueryPropertyPreview {

    static final int STRING_LIMIT = 256;
    private static final int COLLECTION_LIMIT = 20;
    private static final int DEPTH_LIMIT = 4;

    private final Map<Object, Object> copies = new IdentityHashMap<>();
    private boolean truncated;

    boolean truncated() {
        return this.truncated;
    }

    TypedResult project(TypedResult result) {
        if (result.getData() == null) {
            return result;
        }
        List<Object> data = new ArrayList<>(result.getData().size());
        for (Object value : result.getData()) {
            data.add(this.value(value, 0));
        }
        return new TypedResult(result.getType(), data, result.isTruncated());
    }

    GraphView project(GraphView graph) {
        if (graph.getVertices() == null || graph.getEdges() == null) {
            return graph;
        }
        List<Vertex> vertices = new ArrayList<>(graph.getVertices().size());
        graph.getVertices().forEach(vertex -> vertices.add((Vertex) this.value(vertex, 0)));
        List<Edge> edges = new ArrayList<>(graph.getEdges().size());
        graph.getEdges().forEach(edge -> edges.add((Edge) this.value(edge, 0)));
        GraphView preview = new GraphView(vertices, edges);
        preview.setTruncated(graph.isTruncated());
        return preview;
    }

    private Object value(Object value, int depth) {
        if (this.copies.containsKey(value)) {
            return this.copies.get(value);
        }
        if (value instanceof Vertex) {
            Vertex original = (Vertex) value;
            Vertex copy = new Vertex(original.label());
            copy.id(original.id());
            return this.element(original, copy);
        }
        if (value instanceof Edge) {
            Edge original = (Edge) value;
            Edge copy = new Edge(original.label());
            copy.id(original.id());
            copy.sourceId(original.sourceId());
            copy.targetId(original.targetId());
            copy.sourceLabel(original.sourceLabel());
            copy.targetLabel(original.targetLabel());
            return this.element(original, copy);
        }
        if (value instanceof Path) {
            Path original = (Path) value;
            Path copy = new Path();
            this.copies.put(original, copy);
            copy.labels(original.labels().toArray());
            Object crosspoint = original.crosspoint();
            copy.crosspoint(crosspoint instanceof GraphElement ? this.value(crosspoint, 0) : crosspoint);
            for (Object object : original.objects()) {
                copy.objects(this.value(object, 0));
            }
            return copy;
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            if (bytes.length > STRING_LIMIT) {
                this.truncated = true;
                return "[binary preview: " + bytes.length + " bytes omitted]";
            }
            return bytes.clone();
        }
        if (value instanceof String) {
            String text = (String) value;
            if (text.length() > STRING_LIMIT) {
                int end = STRING_LIMIT;
                if (Character.isHighSurrogate(text.charAt(end - 1)) &&
                    Character.isLowSurrogate(text.charAt(end))) {
                    end--;
                }
                this.truncated = true;
                return text.substring(0, end) + "… [preview " + end + "/" +
                       text.length() + " chars]";
            }
        }
        if (value instanceof Map || value instanceof List || value instanceof Set) {
            if (depth >= DEPTH_LIMIT) {
                this.truncated = true;
                return "[preview: nested value omitted]";
            }
        }
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            Map<?, ?> original = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : original.entrySet()) {
                if (copy.size() == COLLECTION_LIMIT) {
                    this.truncated = true;
                    break;
                }
                copy.put(entry.getKey(), this.value(entry.getValue(), depth + 1));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (copy.size() == COLLECTION_LIMIT) {
                    this.truncated = true;
                    break;
                }
                copy.add(this.value(item, depth + 1));
            }
            return copy;
        }
        if (value instanceof Set) {
            Set<Object> copy = new LinkedHashSet<>();
            int visited = 0;
            for (Object item : (Set<?>) value) {
                if (visited++ == COLLECTION_LIMIT) {
                    this.truncated = true;
                    break;
                }
                copy.add(this.value(item, depth + 1));
            }
            return copy;
        }
        return value;
    }

    private GraphElement element(GraphElement original, GraphElement copy) {
        this.copies.put(original, copy);
        // SDK query elements have an attached manager: property() on them can
        // write to Server. Only populate the detached copy's property map.
        for (Map.Entry<String, Object> entry : original.properties().entrySet()) {
            if (copy.properties().size() == COLLECTION_LIMIT) {
                this.truncated = true;
                break;
            }
            copy.properties().put(entry.getKey(), this.value(entry.getValue(), 0));
        }
        return copy;
    }
}
