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
import java.util.Collections;
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
    static final int VALUE_LIMIT = 10000;
    static final int PATH_LIMIT = 1000;
    private static final String VALUE_OMITTED = "[preview: value budget exhausted]";
    private static final String NESTED_OMITTED = "[preview: nested value omitted]";
    private static final int COLLECTION_LIMIT = 20;
    private static final int DEPTH_LIMIT = 4;

    private boolean truncated;
    private boolean structureTruncated;
    private int remainingValues = VALUE_LIMIT;
    // GraphView wraps vertices but retains their property map. Recognize only
    // maps created for our detached copies, including through those wrappers.
    // Caching original SDK objects/maps would let repeated rows evade the budget.
    private final Set<Map<String, Object>> detachedProperties =
            Collections.newSetFromMap(new IdentityHashMap<>());

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
        return new TypedResult(result.getType(), data,
                               result.isTruncated() || this.structureTruncated);
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
        if (value instanceof GraphElement &&
            this.detachedProperties.contains(((GraphElement) value).properties())) {
            return value;
        }
        boolean available = this.takeValue();
        if (value instanceof Vertex) {
            Vertex original = (Vertex) value;
            Vertex copy = new Vertex(original.label());
            copy.id(original.id());
            return this.element(original, copy, depth);
        }
        if (value instanceof Edge) {
            Edge original = (Edge) value;
            Edge copy = new Edge(original.label());
            copy.id(original.id());
            copy.sourceId(original.sourceId());
            copy.targetId(original.targetId());
            copy.sourceLabel(original.sourceLabel());
            copy.targetLabel(original.targetLabel());
            return this.element(original, copy, depth);
        }
        if (value instanceof Path) {
            return this.path((Path) value, depth, available);
        }
        if (!available) {
            return VALUE_OMITTED;
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
                return NESTED_OMITTED;
            }
        }
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            Map<?, ?> original = (Map<?, ?>) value;
            int visited = 0;
            for (Map.Entry<?, ?> entry : original.entrySet()) {
                if (visited++ == COLLECTION_LIMIT) {
                    this.truncated = true;
                    break;
                }
                if (this.omitKey(entry.getKey())) {
                    continue;
                }
                boolean exhausted = this.remainingValues == 0;
                copy.put(entry.getKey(), this.value(entry.getValue(), depth + 1));
                if (exhausted) {
                    break;
                }
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
                boolean exhausted = this.remainingValues == 0;
                copy.add(this.value(item, depth + 1));
                if (exhausted) {
                    break;
                }
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
                boolean exhausted = this.remainingValues == 0;
                copy.add(this.value(item, depth + 1));
                if (exhausted) {
                    break;
                }
            }
            return copy;
        }
        return value;
    }

    private boolean takeValue() {
        if (this.remainingValues == 0) {
            this.truncated = true;
            return false;
        }
        this.remainingValues--;
        return true;
    }

    private Path path(Path original, int depth, boolean available) {
        Object crosspoint = original.crosspoint();
        boolean nestedCrosspoint = crosspoint instanceof Path || crosspoint instanceof Map ||
                                   crosspoint instanceof List ||
                                   crosspoint instanceof Set || crosspoint instanceof byte[];
        boolean descend = available && depth < DEPTH_LIMIT;
        if (crosspoint instanceof GraphElement) {
            crosspoint = this.value(crosspoint, depth + 1);
        } else if (nestedCrosspoint) {
            // Crosspoint IDs remain exact; arbitrary nested values share the
            // same depth and work budget as the path objects and labels.
            crosspoint = descend ? this.value(crosspoint, depth + 1) : NESTED_OMITTED;
            if (!descend) {
                this.truncated = true;
            }
        }
        List<Object> objects = new ArrayList<>();
        List<Object> labels = new ArrayList<>();
        List<Object> originalObjects = original.objects();
        List<Object> originalLabels = original.labels();
        int slots = Math.min(PATH_LIMIT, Math.max(originalObjects.size(), originalLabels.size()));
        for (int i = 0; descend && i < slots && this.remainingValues > 0; i++) {
            if (i < originalObjects.size()) {
                objects.add(this.value(originalObjects.get(i), depth + 1));
            }
            if (i < originalLabels.size() && this.remainingValues > 0) {
                labels.add(this.value(originalLabels.get(i), depth + 1));
            }
        }
        if (objects.size() < originalObjects.size() || labels.size() < originalLabels.size()) {
            this.truncated = true;
            this.structureTruncated = true;
        }
        // SDK Path stores CopyOnWriteArrayLists. Build each prefix in one batch
        // rather than copying its growing backing array for every object.
        Path copy = new Path(crosspoint, objects);
        copy.labels(labels.toArray());
        return copy;
    }

    private boolean omitKey(Object key) {
        if (key instanceof String && ((String) key).length() > STRING_LIMIT) {
            // Renaming a map key changes its meaning and can merge distinct
            // entries. Omit it explicitly instead of manufacturing a new key.
            this.truncated = true;
            return true;
        }
        return false;
    }

    private GraphElement element(GraphElement original, GraphElement copy, int depth) {
        // SDK query elements have an attached manager: property() on them can
        // write to Server. Only populate the detached copy's property map.
        this.detachedProperties.add(copy.properties());
        if (depth >= DEPTH_LIMIT) {
            this.truncated |= !original.properties().isEmpty();
            return copy;
        }
        int visited = 0;
        for (Map.Entry<String, Object> entry : original.properties().entrySet()) {
            if (visited++ == COLLECTION_LIMIT) {
                this.truncated = true;
                break;
            }
            if (!this.omitKey(entry.getKey())) {
                boolean exhausted = this.remainingValues == 0;
                copy.properties().put(entry.getKey(), this.value(entry.getValue(), depth + 1));
                if (exhausted) {
                    break;
                }
            }
        }
        return copy;
    }
}
