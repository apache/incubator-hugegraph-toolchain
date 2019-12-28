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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.LongEncoding;
import com.google.common.collect.Maps;

public abstract class ElementBuilder {

    private static final CharsetEncoder ENCODER =
                         Constants.CHARSET.newEncoder();
    private static final ByteBuffer BUFFER =
                         ByteBuffer.allocate(Constants.VERTEX_ID_LIMIT);

    private final InputStruct struct;
    private final SchemaCache schema;

    public ElementBuilder(LoadContext context, InputStruct struct) {
        this.struct = struct;
        this.schema = new SchemaCache(context);
    }

    public abstract ElementMapping mapping();

    public abstract GraphElement build(Map<String, Object> keyValues);

    protected abstract SchemaLabel schemaLabel();

    protected abstract boolean isIdField(String fieldName);

    /**
     * Retain only the key-value pairs needed by the current vertex or edge
     */
    protected Map<String, Object> filterFields(Map<String, Object> keyValues) {
        // select and ignore not allowed to be true at the same time
        boolean selectFields = !this.mapping().selectedFields().isEmpty();
        boolean ignoreFields = !this.mapping().ignoredFields().isEmpty();

        Set<String> nullableKeys = this.schemaLabel().nullableKeys();
        Set<Object> nullValues = this.mapping().nullValues();

        return Maps.filterEntries(keyValues, entry -> {
            assert entry != null;
            String key = entry.getKey();
            Object val = entry.getValue();
            // Retain selected fields or remove ignored fields
            if (selectFields) {
                return this.mapping().selectedFields().contains(key);
            } else if (ignoreFields) {
                return !this.mapping().ignoredFields().contains(key);
            }
            String mappedKey = this.mapping().mappingField(key);
            return !nullableKeys.contains(mappedKey) || !nullValues.contains(val);
        });
    }

    protected void addProperties(GraphElement element,
                                 Map<String, Object> keyValues) {
        for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            this.checkFieldValue(key, value);
            value = this.validatePropertyValue(key, value);

            element.property(key, value);
        }
    }

    protected PropertyKey getPropertyKey(String name) {
        return this.schema.getPropertyKey(name);
    }

    protected VertexLabel getVertexLabel(String name) {
        return this.schema.getVertexLabel(name);
    }

    protected EdgeLabel getEdgeLabel(String name) {
        return this.schema.getEdgeLabel(name);
    }

    protected Object validatePropertyValue(String key, Object rawValue) {
        PropertyKey pKey = this.getPropertyKey(key);
        InputSource inputSource = this.struct.input();
        return DataTypeUtil.convert(rawValue, pKey, inputSource);
    }

    protected void checkFieldValue(String fieldName, Object fieldValue) {
        if (this.mapping().mappingValues().isEmpty() ||
            !this.mapping().mappingValues().containsKey(fieldName)) {
            return;
        }
        // NOTE: The nullable values has been filtered before this
        E.checkArgument(fieldValue != null, "The field value can't be null");
        E.checkArgument(DataTypeUtil.isSimpleValue(fieldValue),
                        "The field value must be simple type, actual is '%s'",
                        fieldValue.getClass());
    }

    protected Object mappingFieldValueIfNeeded(String fieldName,
                                               Object fieldValue) {
        if (this.mapping().mappingValues().isEmpty()) {
            return fieldValue;
        }
        String fieldStrValue = String.valueOf(fieldValue);
        return this.mapping().mappingValue(fieldName, fieldStrValue);
    }

    protected static String spliceVertexId(VertexLabel vertexLabel,
                                           Object[] primaryValues) {
        List<String> primaryKeys = vertexLabel.primaryKeys();
        E.checkArgument(primaryKeys.size() == primaryValues.length,
                        "Missing some primary key columns, expect %s, " +
                        "but only got %s for vertex label '%s'",
                        primaryKeys, primaryValues, vertexLabel);

        StringBuilder vertexId = new StringBuilder();
        StringBuilder vertexKeysId = new StringBuilder();
        for (int i = 0; i < primaryValues.length; i++) {
            Object value = primaryValues[i];
            E.checkArgument(value != null,
                            "Make sure the value of the primary key '%s' is " +
                            "not empty, or check whether the headers or " +
                            "field_mapping are configured correctly",
                            primaryKeys.get(i));
            String pkValue;
            if (value instanceof Number || value instanceof Date) {
                pkValue = LongEncoding.encodeNumber(value);
            } else {
                pkValue = String.valueOf(value);
            }
            if (StringUtils.containsAny(pkValue, Constants.SEARCH_LIST)) {
                pkValue = StringUtils.replaceEach(pkValue,
                                                  Constants.SEARCH_LIST,
                                                  Constants.TARGET_LIST);
            }
            vertexKeysId.append(pkValue);
            vertexKeysId.append("!");
        }

        vertexId.append(vertexLabel.id()).append(":").append(vertexKeysId);
        vertexId.deleteCharAt(vertexId.length() - 1);
        return vertexId.toString();
    }

    protected static void checkVertexIdLength(String id) {
        ENCODER.reset();
        BUFFER.clear();
        CoderResult result = ENCODER.encode(CharBuffer.wrap(id.toCharArray()),
                                            BUFFER, true);
        E.checkArgument(result.isUnderflow(),
                        "The vertex id length limit is '%s', '%s' exceeds it",
                        Constants.VERTEX_ID_LIMIT, id);
    }
}
