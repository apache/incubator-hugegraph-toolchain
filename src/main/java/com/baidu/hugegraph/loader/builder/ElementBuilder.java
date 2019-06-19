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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.AutoCloseableIterator;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.InputReaderFactory;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public abstract class ElementBuilder<GE extends GraphElement>
                implements AutoCloseableIterator<GE> {

    private static final Logger LOG = Log.logger(ElementBuilder.class);

    private final SchemaCache schema;
    private final InputReader reader;

    public ElementBuilder(LoadContext context, ElementStruct struct) {
        this.schema = new SchemaCache(context);
        this.reader = InputReaderFactory.create(struct.input());
        this.init(context, struct);
    }

    public abstract ElementStruct struct();

    public ElemType type() {
        return this.struct().type();
    }

    private void init(LoadContext context, ElementStruct struct) {
        try {
            this.reader.init(context, struct);
        } catch (Exception e) {
            throw new LoadException("Failed to init input reader", e);
        }
    }

    @Override
    public boolean hasNext() {
        return this.reader.hasNext();
    }

    @Override
    public GE next() {
        Line line = this.reader.next();
        Map<String, Object> keyValues = line.toMap();
        try {
            keyValues = this.filterFields(keyValues);
            return this.build(keyValues);
        } catch (IllegalArgumentException e) {
            throw new ParseException(line.rawLine(), e);
        }
    }

    @Override
    public void close() {
        try {
            this.reader.close();
        } catch (Exception e) {
            LOG.warn("Failed to close builder for {} with exception {}",
                     this.struct(), e);
        }
    }

    protected abstract SchemaLabel getSchemaLabel();

    protected abstract GE build(Map<String, Object> keyValues);

    protected abstract boolean isIdField(String fieldName);

    protected Map<String, Object> filterFields(Map<String, Object> keyValues) {
        // Retain selected fileds or remove ignored fields
        if (!this.struct().selectedFields().isEmpty()) {
            keyValues.keySet().retainAll(this.struct().selectedFields());
        } else if (!this.struct().ignoredFields().isEmpty()) {
            keyValues.keySet().removeAll(this.struct().ignoredFields());
        }

        SchemaLabel schemaLabel = this.getSchemaLabel();
        Set<String> nullableKeys = schemaLabel.nullableKeys();
        Set<Object> nullValues = this.struct().nullValues();
        if (!nullableKeys.isEmpty() && !nullValues.isEmpty()) {
            Iterator<Map.Entry<String, Object>> iter = keyValues.entrySet()
                                                                .iterator();
            iter.forEachRemaining(entry -> {
                String key = this.struct().mappingField(entry.getKey());
                Object val = entry.getValue();
                if (nullableKeys.contains(key) && nullValues.contains(val)) {
                    iter.remove();
                }
            });
        }
        return keyValues;
    }

    protected void addProperties(GE element, Map<String, Object> keyValues) {
        for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            this.checkFieldValue(fieldName, fieldValue);

            if (this.isIdField(fieldName)) {
                continue;
            }
            String key = this.struct().mappingField(fieldName);
            fieldValue = this.mappingFieldValueIfNeeded(fieldName, fieldValue);
            Object value = this.validatePropertyValue(key, fieldValue);

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
        InputSource inputSource = this.struct().input();
        return DataTypeUtil.convert(rawValue, pKey, inputSource);
    }

    protected void checkFieldValue(String fieldName, Object fieldValue) {
        if (this.struct().mappingValues().isEmpty() ||
            !this.struct().mappingValues().containsKey(fieldName)) {
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
        if (this.struct().mappingValues().isEmpty()) {
            return fieldValue;
        }
        String fieldStrValue = String.valueOf(fieldValue);
        return this.struct().mappingValue(fieldName, fieldStrValue);
    }

    protected static String spliceVertexId(VertexLabel vertexLabel,
                                           Object[] primaryValues) {
        E.checkArgument(vertexLabel.primaryKeys().size() == primaryValues.length,
                        "Missing some primary key columns, expect %s, " +
                        "but only got %s for vertex label '%s'",
                        vertexLabel.primaryKeys(), primaryValues, vertexLabel);

        StringBuilder vertexId = new StringBuilder();
        StringBuilder vertexKeysId = new StringBuilder();
        String[] searchList = new String[]{":", "!"};
        String[] replaceList = new String[]{"`:", "`!"};
        for (Object value : primaryValues) {
            String pkValue = String.valueOf(value);
            pkValue = StringUtils.replaceEach(pkValue, searchList, replaceList);
            vertexKeysId.append(pkValue);
            vertexKeysId.append("!");
        }

        vertexId.append(vertexLabel.id()).append(":").append(vertexKeysId);
        vertexId.deleteCharAt(vertexId.length() - 1);
        return vertexId.toString();
    }

    protected static void checkVertexIdLength(String id) {
        E.checkArgument(id.getBytes(Constants.CHARSET).length <=
                        Constants.VERTEX_ID_LIMIT,
                        "The vertex id length limit is '%s', '%s' exceeds it",
                        Constants.VERTEX_ID_LIMIT, id);
    }

    protected static long parseNumberId(Object idValue) {
        if (idValue instanceof Number) {
            return ((Number) idValue).longValue();
        } else if (idValue instanceof String) {
            return Long.parseLong((String) idValue);
        }
        throw new IllegalArgumentException(String.format(
                  "The id value must can be casted to Long, but got %s(%s)",
                  idValue, idValue.getClass().getName()));
    }
}
