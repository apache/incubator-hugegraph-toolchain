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

package com.baidu.hugegraph.loader.builder;

import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.loader.LoadContext;
import com.baidu.hugegraph.loader.struct.VertexStruct;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;

public class VertexBuilder extends ElementBuilder<Vertex> {

    private final VertexStruct struct;
    private final VertexLabel vertexLabel;

    public VertexBuilder(LoadContext context, VertexStruct struct) {
        super(context, struct);
        this.struct = struct;
        this.vertexLabel = this.getVertexLabel(struct.label());
        // Ensure the id field is matched with id strategy
        this.checkIdField();
    }

    @Override
    public VertexStruct struct() {
        return this.struct;
    }

    @Override
    protected SchemaLabel getSchemaLabel() {
        return this.vertexLabel;
    }

    @Override
    protected Vertex build(Map<String, Object> keyValues) {
        Vertex vertex = new Vertex(this.struct.label());
        // Assign or check id if need
        this.assignIdIfNeed(vertex, keyValues);
        // Add properties
        this.addProperties(vertex, keyValues);
        return vertex;
    }

    @Override
    protected boolean isIdField(String fieldName) {
        return fieldName.equals(this.struct.idField());
    }

    private void assignIdIfNeed(Vertex vertex, Map<String, Object> keyValues) {
        // The id strategy must be CUSTOMIZE/PRIMARY_KEY via 'checkIdField()'
        IdStrategy idStrategy = this.vertexLabel.idStrategy();
        if (idStrategy.isCustomize()) {
            assert this.struct.idField() != null;
            Object idValue = keyValues.get(this.struct.idField());
            E.checkArgument(idValue != null,
                            "The value of id field '%s' can't be null",
                            this.struct.idField());

            if (idStrategy.isCustomizeString()) {
                String id = (String) idValue;
                this.checkVertexIdLength(id);
                vertex.id(id);
            } else {
                assert idStrategy.isCustomizeNumber();
                Long id = parseNumberId(idValue);
                vertex.id(id);
            }
        } else {
            assert this.vertexLabel.idStrategy().isPrimaryKey();
            List<String> primaryKeys = this.vertexLabel.primaryKeys();
            Object[] primaryValues = new Object[primaryKeys.size()];
            for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                this.checkFieldValue(fieldName, fieldValue);

                String key = this.struct.mappingField(fieldName);
                if (!primaryKeys.contains(key)) {
                    continue;
                }
                this.mappingFieldValueIfNeeded(fieldName, fieldValue);
                Object value = this.validatePropertyValue(key, fieldValue);

                int index = primaryKeys.indexOf(key);
                primaryValues[index] = value;
            }
            String id = this.spliceVertexId(this.vertexLabel, primaryValues);
            this.checkVertexIdLength(id);
        }
    }

    private void checkIdField() {
        String name = this.vertexLabel.name();
        if (this.vertexLabel.idStrategy().isCustomize()) {
            E.checkState(this.struct.idField() != null,
                         "The id field can't be empty or null when " +
                         "id strategy is CUSTOMIZE for vertex label '%s'",
                         name);
        } else if (this.vertexLabel.idStrategy().isPrimaryKey()) {
            E.checkState(this.struct.idField() == null,
                         "The id field must be empty or null when " +
                         "id strategy is PRIMARY_KEY for vertex label '%s'",
                         name);
        } else {
            // The id strategy is automatic
            throw new IllegalArgumentException(
                      "Unsupported AUTOMATIC id strategy for hugegraph-loader");
        }
    }
}
