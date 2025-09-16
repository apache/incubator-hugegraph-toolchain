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

import java.util.Collection;
import java.util.List;

import org.apache.hugegraph.util.E;

import org.apache.hugegraph.loader.constant.LoaderStruct;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.filter.util.ShortIdConfig;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.SchemaLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;

public class VertexBuilder extends ElementBuilder<Vertex> {

    private final VertexMapping mapping;
    private final VertexLabel vertexLabel;
    private final Collection<String> nonNullKeys;
    private final ShortIdConfig shortIdConfig;

    public VertexBuilder(LoadContext context, InputStruct struct,
                         VertexMapping mapping) {
        super(context, struct);
        this.mapping = mapping;
        this.vertexLabel = this.getVertexLabel(this.mapping.label());
        this.shortIdConfig = context.options().getShortIdConfig(this.mapping.label());
        this.nonNullKeys = this.nonNullableKeys(this.vertexLabel);
        // Ensure the id field is matched with id strategy
        this.checkIdField();
    }

    @Override
    public VertexMapping mapping() {
        return this.mapping;
    }

    @Override
    public List<Vertex> build(String[] names, Object[] values) {
        VertexKVPairs kvPairs = null;
        // 如果是 Vertex OLAP 属性，则需要 VertexOlapKVPairs 解析
        if (this.verifyOlapVertexBuilder()) {
            kvPairs = new VertexOlapKVPairs(vertexLabel);
        } else {
            kvPairs = this.newKVPairs(this.vertexLabel,
                                      this.mapping.unfold());
        }

        kvPairs.headerCaseSensitive(this.headerCaseSensitive());
        kvPairs.extractFromVertex(names, values);
        return kvPairs.buildVertices(true);
    }

    @Override
    protected SchemaLabel schemaLabel() {
        return this.vertexLabel;
    }

    @Override
    protected Collection<String> nonNullableKeys() {
        return this.nonNullKeys;
    }

    @Override
    protected boolean isIdField(String fieldName) {
        if (this.headerCaseSensitive()) {
            return fieldName.equals(this.mapping.idField());
        } else {
            return fieldName.equalsIgnoreCase(this.mapping.idField());
        }
    }

    private void checkIdField() {
        // OLAP 属性解析 不需要进行判断
        if (this.verifyOlapVertexBuilder()) {
            return;
        }
        String name = this.vertexLabel.name();
        if (this.vertexLabel.idStrategy().isCustomize()) {
            E.checkState(this.mapping.idField() != null || shortIdConfig != null,
                         "The id field can't be empty or null when " +
                         "id strategy is '%s' for vertex label '%s'",
                         this.vertexLabel.idStrategy(), name);
        } else if (this.vertexLabel.idStrategy().isPrimaryKey()) {
            E.checkState(this.mapping.idField() == null,
                         "The id field must be empty or null when " +
                         "id strategy is '%s' for vertex label '%s'",
                         this.vertexLabel.idStrategy(), name);
        } else {
            // The id strategy is automatic
            throw new IllegalArgumentException(
                    "Unsupported AUTOMATIC id strategy for hugegraph-loader");
        }
    }

    /**
     * 确认是否 是 OLAP 属性
     * @return
     */
    public boolean verifyOlapVertexBuilder() {
        return LoaderStruct.OLAP_VERTEX_ID.equals(this.mapping.idField());
    }
}
