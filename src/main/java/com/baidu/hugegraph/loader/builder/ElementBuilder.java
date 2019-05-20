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

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.progress.ElementProgress;
import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.InputReaderFactory;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.graph.ElementSource;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.constant.AutoCloseableIterator;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public abstract class ElementBuilder<GE extends GraphElement>
                implements AutoCloseableIterator<GE> {

    private static final Logger LOG = Log.logger(ElementBuilder.class);

    private static final int VERTEX_ID_LIMIT = 128;

    private final InputReader reader;
    private final HugeClient client;
    private final Table<HugeType, String, SchemaElement> schemas;

    public ElementBuilder(ElementSource source, LoadOptions options) {
        this.reader = InputReaderFactory.create(source.input());
        this.client = HugeClientWrapper.get(options);
        this.schemas = HashBasedTable.create();
    }

    public abstract ElementSource source();

    public void init() {
        try {
            this.reader.init();
        } catch (Exception e) {
            throw new LoadException("Failed to init input reader", e);
        }
    }

    public InputReader reader() {
        return this.reader;
    }

    public void progress(ElementProgress oldProgress,
                         ElementProgress newProgress) {
        ElementSource source = this.source();
        InputProgress oldInputProgress = oldProgress.get(source);
        if (oldInputProgress == null) {
            oldInputProgress = new InputProgress(source);
        }
        InputProgress newInputProgress = newProgress.get(source);
        this.reader.progress(oldInputProgress, newInputProgress);
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
            throw new ParseException(line.rawLine(), e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            this.reader.close();
        } catch (Exception e) {
            LOG.warn("Failed to close builder for {} with exception {}",
                     this.source(), e);
        }
    }

    protected abstract GE build(Map<String, Object> keyValues);

    protected abstract boolean isIdField(String fieldName);

    protected Map<String, Object> filterFields(Map<String, Object> keyValues) {
        // Retain selected fileds or remove ignored fields
        if (!this.source().selectedFields().isEmpty()) {
            keyValues.keySet().retainAll(this.source().selectedFields());
        } else if (!this.source().ignoredFields().isEmpty()) {
            keyValues.keySet().removeAll(this.source().ignoredFields());
        }

        SchemaLabel schemaLabel = this.getSchemaLabel();
        Set<String> nullableKeys = schemaLabel.nullableKeys();
        Set<Object> nullValues = this.source().nullValues();
        if (!nullableKeys.isEmpty() && !nullValues.isEmpty()) {
            Iterator<Map.Entry<String, Object>> itor = keyValues.entrySet()
                                                                .iterator();
            itor.forEachRemaining(entry -> {
                String key = this.source().mappingField(entry.getKey());
                Object val = entry.getValue();
                if (nullableKeys.contains(key) && nullValues.contains(val)) {
                    itor.remove();
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
            String key = this.source().mappingField(fieldName);
            fieldValue = this.mappingFieldValueIfNeeded(fieldName, fieldValue);
            Object value = this.validatePropertyValue(key, fieldValue);

            element.property(key, value);
        }
    }

    protected PropertyKey getPropertyKey(String name) {
        SchemaElement schema = this.schemas.get(HugeType.PROPERTY_KEY, name);
        if (schema == null) {
            schema = this.client.schema().getPropertyKey(name);
        }
        E.checkState(schema != null, "The property key %s doesn't exist", name);
        this.schemas.put(HugeType.PROPERTY_KEY, name, schema);
        return (PropertyKey) schema;
    }

    protected VertexLabel getVertexLabel(String name) {
        SchemaElement schema = this.schemas.get(HugeType.VERTEX_LABEL, name);
        if (schema == null) {
            schema = this.client.schema().getVertexLabel(name);
        }
        E.checkState(schema != null, "The vertex label %s doesn't exist", name);
        this.schemas.put(HugeType.VERTEX_LABEL, name, schema);
        return (VertexLabel) schema;
    }

    protected EdgeLabel getEdgeLabel(String name) {
        SchemaElement schema = this.schemas.get(HugeType.EDGE_LABEL, name);
        if (schema == null) {
            schema = this.client.schema().getEdgeLabel(name);
        }
        E.checkState(schema != null, "The edge label %s doesn't exist", name);
        this.schemas.put(HugeType.EDGE_LABEL, name, schema);
        return (EdgeLabel) schema;
    }

    protected abstract SchemaLabel getSchemaLabel();

    protected void checkFieldValue(String fieldName, Object fieldValue) {
        if (this.source().mappingValues().isEmpty() ||
            !this.source().mappingValues().containsKey(fieldName)) {
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
        if (this.source().mappingValues().isEmpty()) {
            return fieldValue;
        }
        String fieldStrValue = String.valueOf(fieldValue);
        return this.source().mappingValue(fieldName, fieldStrValue);
    }

    protected String spliceVertexId(VertexLabel vertexLabel,
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

    protected void checkVertexIdLength(String id) {
        E.checkArgument(id.getBytes(Constants.CHARSET).length <= VERTEX_ID_LIMIT,
                        "Vertex id length limit is '%s', '%s' exceeds it",
                        VERTEX_ID_LIMIT, id);
    }

    protected Object validatePropertyValue(String key, Object rawValue) {
        PropertyKey pKey = this.getPropertyKey(key);
        InputSource inputSource = this.source().input();
        return DataTypeUtil.convert(rawValue, pKey, inputSource);
    }

    protected static long parseNumberId(Object idValue) {
        if (idValue instanceof Number) {
            return ((Number) idValue).longValue();
        } else if (idValue instanceof String) {
            return Long.parseLong((String) idValue);
        } else {
            throw new IllegalArgumentException(String.format(
                      "The id value must can be casted to Long, " +
                      "but got %s(%s)", idValue, idValue.getClass().getName()));
        }
    }
}
