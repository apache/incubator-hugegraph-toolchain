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

import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.struct.EdgeStruct;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;

public class EdgeBuilder extends ElementBuilder<Edge> {

    private final EdgeStruct struct;
    private final EdgeLabel edgeLabel;
    private final VertexLabel sourceLabel;
    private final VertexLabel targetLabel;

    public EdgeBuilder(LoadContext context, EdgeStruct struct) {
        super(context, struct);
        this.struct = struct;
        this.edgeLabel = this.getEdgeLabel(struct.label());
        this.sourceLabel = this.getVertexLabel(this.edgeLabel.sourceLabel());
        this.targetLabel = this.getVertexLabel(this.edgeLabel.targetLabel());
        // Ensure that the source/target id fileds are matched with id strategy
        this.checkIdFields(this.sourceLabel, this.struct.sourceFields());
        this.checkIdFields(this.targetLabel, this.struct.targetFields());
    }

    @Override
    public EdgeStruct struct() {
        return this.struct;
    }

    @Override
    protected SchemaLabel getSchemaLabel() {
        return this.edgeLabel;
    }

    @Override
    protected Edge build(Map<String, Object> keyValues) {
        Edge edge = new Edge(this.struct.label());
        // Must add source/target vertex id
        edge.sourceId(this.buildVertexId(this.sourceLabel,
                                         this.struct.sourceFields(),
                                         keyValues));
        edge.targetId(this.buildVertexId(this.targetLabel,
                                         this.struct.targetFields(),
                                         keyValues));
        // Must add source/target vertex label
        edge.sourceLabel(this.sourceLabel.name());
        edge.targetLabel(this.targetLabel.name());
        // Add properties
        this.addProperties(edge, keyValues);
        return edge;
    }

    @Override
    protected boolean isIdField(String fieldName) {
        return this.struct.sourceFields().contains(fieldName) ||
               this.struct.targetFields().contains(fieldName);
    }

    private Object buildVertexId(VertexLabel vertexLabel,
                                 List<String> fieldNames,
                                 Map<String, Object> keyValues) {
        IdStrategy idStrategy = vertexLabel.idStrategy();
        List<String> primaryKeys = vertexLabel.primaryKeys();
        Object[] primaryValues = new Object[primaryKeys.size()];
        for (String fieldName : fieldNames) {
            if (!keyValues.containsKey(fieldName)) {
                continue;
            }
            Object fieldValue = keyValues.get(fieldName);
            this.checkFieldValue(fieldName, fieldValue);
            Object mappedValue = this.mappingFieldValueIfNeeded(fieldName,
                                                                fieldValue);
            /*
             * Check vertex id length when the id strategy of source/target
             * label is CUSTOMIZE_STRING, just return when CUSTOMIZE_NUMBER
             */
            if (idStrategy.isCustomizeString()) {
                E.checkArgument(mappedValue instanceof String,
                                "The field value must be String if there " +
                                "is no value mapping, same as the value " +
                                "after mapping, but got %s(%s) -> %s(%s)",
                                fieldValue, fieldValue.getClass(),
                                mappedValue, mappedValue.getClass());
                String id = (String) mappedValue;
                checkVertexIdLength(id);
                return id;
            } else if (idStrategy.isCustomizeNumber()) {
                return DataTypeUtil.parseNumber(mappedValue);
            } else if (idStrategy.isCustomizeUuid()) {
                return DataTypeUtil.parseUUID(mappedValue);
            } else {
                // The id strategy of source/target label must be PRIMARY_KEY
                String key = this.struct.mappingField(fieldName);
                if (primaryKeys.contains(key)) {
                    int index = primaryKeys.indexOf(key);
                    Object value = this.validatePropertyValue(key, mappedValue);
                    primaryValues[index] = value;
                }
            }
        }

        String id = spliceVertexId(vertexLabel, primaryValues);
        checkVertexIdLength(id);
        return id;
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
            throw new IllegalArgumentException(
                      "Unsupported AUTOMATIC id strategy for hugegraph-loader");
        }
    }
}
