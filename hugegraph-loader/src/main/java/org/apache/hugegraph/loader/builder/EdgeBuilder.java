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

package org.apache.hugegraph.loader.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.SchemaLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableList;

import org.apache.spark.sql.Row;

public class EdgeBuilder extends ElementBuilder<Edge> {

    private final EdgeMapping mapping;
    private final EdgeLabel edgeLabel;
    private final VertexLabel sourceLabel;
    private final VertexLabel targetLabel;
    private final Collection<String> nonNullKeys;
    // Used to optimize access performance
    private VertexIdsIndex vertexIdsIndex;
    private String[] lastNames;

    public EdgeBuilder(LoadContext context, InputStruct struct,
                       EdgeMapping mapping) {
        super(context, struct);
        this.mapping = mapping;
        this.edgeLabel = this.getEdgeLabel(this.mapping.label());
        this.sourceLabel = this.getVertexLabel(this.edgeLabel.sourceLabel());
        this.targetLabel = this.getVertexLabel(this.edgeLabel.targetLabel());
        this.nonNullKeys = this.nonNullableKeys(this.edgeLabel);
        // Ensure that the source/target id fields are matched with id strategy
        this.checkIdFields(this.sourceLabel, this.mapping.sourceFields());
        this.checkIdFields(this.targetLabel, this.mapping.targetFields());

        this.vertexIdsIndex = null;
    }

    @Override
    public EdgeMapping mapping() {
        return this.mapping;
    }

    @Override
    public List<Edge> build(String[] names, Object[] values) {
        if (this.vertexIdsIndex == null ||
            !Arrays.equals(this.lastNames, names)) {
            this.vertexIdsIndex = this.extractVertexIdsIndex(names);
        }
        this.lastNames = names;
        EdgeKVPairs kvPairs = this.newEdgeKVPairs();
        kvPairs.source.extractFromEdge(names, values,
                                       this.vertexIdsIndex.sourceIndexes);
        kvPairs.target.extractFromEdge(names, values,
                                       this.vertexIdsIndex.targetIndexes);
        kvPairs.extractProperties(names, values);

        List<Vertex> sources = kvPairs.source.buildVertices(false);
        List<Vertex> targets = kvPairs.target.buildVertices(false);
        if (sources.isEmpty() || targets.isEmpty()) {
            return ImmutableList.of();
        }
        E.checkArgument(sources.size() == 1 || targets.size() == 1 ||
                        sources.size() == targets.size(),
                        "The elements number of source and target must be: " +
                        "1 to n, n to 1, n to n");
        int size = Math.max(sources.size(), targets.size());
        List<Edge> edges = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Vertex source = i < sources.size() ?
                            sources.get(i) : sources.get(0);
            Vertex target = i < targets.size() ?
                            targets.get(i) : targets.get(0);
            Edge edge = new Edge(this.mapping.label());
            edge.source(source);
            edge.target(target);
            // Add properties
            this.addProperties(edge, kvPairs.properties);
            this.checkNonNullableKeys(edge);
            edges.add(edge);
        }
        return edges;
    }

    @Override
    public List<Edge> build(Row row) {
        String[] names = row.schema().fieldNames();
        Object[] values = new Object[row.size()];
        for (int i = 0; i < row.size(); i++) {
            values[i] = row.get(i);
        }
        if (this.vertexIdsIndex == null ||
            !Arrays.equals(this.lastNames, names)) {
            this.vertexIdsIndex = this.extractVertexIdsIndex(names);
        }

        this.lastNames = names;
        EdgeKVPairs kvPairs = this.newEdgeKVPairs();
        kvPairs.source.extractFromEdge(names, values, this.vertexIdsIndex.sourceIndexes);
        kvPairs.target.extractFromEdge(names, values, this.vertexIdsIndex.targetIndexes);
        kvPairs.extractProperties(names, values);

        List<Vertex> sources = kvPairs.source.buildVertices(false);
        List<Vertex> targets = kvPairs.target.buildVertices(false);
        if (sources.isEmpty() || targets.isEmpty()) {
            return ImmutableList.of();
        }
        E.checkArgument(sources.size() == 1 || targets.size() == 1 ||
                        sources.size() == targets.size(),
                        "The elements number of source and target must be: " +
                        "1 to n, n to 1, n to n");
        int size = Math.max(sources.size(), targets.size());
        List<Edge> edges = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Vertex source = i < sources.size() ?
                            sources.get(i) : sources.get(0);
            Vertex target = i < targets.size() ?
                            targets.get(i) : targets.get(0);
            Edge edge = new Edge(this.mapping.label());
            edge.source(source);
            edge.target(target);
            // Add properties
            this.addProperties(edge, kvPairs.properties);
            this.checkNonNullableKeys(edge);
            edges.add(edge);
        }
        return edges;
    }

    private EdgeKVPairs newEdgeKVPairs() {
        EdgeKVPairs kvPairs = new EdgeKVPairs();
        kvPairs.source = this.newKVPairs(this.sourceLabel,
                                         this.mapping.unfoldSource());
        kvPairs.target = this.newKVPairs(this.targetLabel,
                                         this.mapping.unfoldTarget());
        return kvPairs;
    }

    @Override
    public SchemaLabel schemaLabel() {
        return this.edgeLabel;
    }

    @Override
    protected Collection<String> nonNullableKeys() {
        return this.nonNullKeys;
    }

    @Override
    protected boolean isIdField(String fieldName) {
        return this.mapping.sourceFields().contains(fieldName) ||
               this.mapping.targetFields().contains(fieldName);
    }

    private void checkIdFields(VertexLabel vertexLabel, List<String> fields) {
        if (vertexLabel.idStrategy().isCustomize()) {
            E.checkArgument(fields.size() == 1,
                            "The source/target field can contains only one " +
                            "column when id strategy is CUSTOMIZE");
        } else if (vertexLabel.idStrategy().isPrimaryKey()) {
            E.checkArgument(fields.size() >= 1,
                            "The source/target field must contains some " +
                            "columns when id strategy is CUSTOMIZE");
        } else {
            throw new IllegalArgumentException("Unsupported AUTOMATIC id strategy " +
                                               "for hugegraph-loader");
        }
    }

    public class EdgeKVPairs {

        // No general properties
        private VertexKVPairs source;
        private VertexKVPairs target;
        // General properties
        private Map<String, Object> properties;

        public void extractProperties(String[] names, Object[] values) {
            // General properties
            this.properties = new HashMap<>();
            Set<String> props = schemaLabel().properties();
            for (int i = 0; i < names.length; i++) {
                String fieldName = names[i];
                Object fieldValue = values[i];
                if (!retainField(fieldName, fieldValue)) {
                    continue;
                }

                String key = mapping.mappingField(fieldName);
                if (isIdField(fieldName) &&
                    !props.contains(fieldName) && !props.contains(key)) {
                    continue;
                }

                Object value = mappingValue(fieldName, fieldValue);
                this.properties.put(key, value);
            }
        }
    }

    private VertexIdsIndex extractVertexIdsIndex(String[] names) {
        VertexIdsIndex index = new VertexIdsIndex();
        index.sourceIndexes = new int[this.mapping.sourceFields().size()];
        int idx = 0;
        for (String field : this.mapping.sourceFields()) {
            for (int pos = 0; pos < names.length; pos++) {
                String name = names[pos];
                if (field.equals(name)) {
                    index.sourceIndexes[idx++] = pos;
                }
            }
        }

        index.targetIndexes = new int[this.mapping.targetFields().size()];
        idx = 0;
        for (String field : this.mapping.targetFields()) {
            for (int pos = 0; pos < names.length; pos++) {
                String name = names[pos];
                if (field.equals(name)) {
                    index.targetIndexes[idx++] = pos;
                }
            }
        }
        return index;
    }

    private static class VertexIdsIndex {

        private int[] sourceIndexes;
        private int[] targetIndexes;
    }
}
