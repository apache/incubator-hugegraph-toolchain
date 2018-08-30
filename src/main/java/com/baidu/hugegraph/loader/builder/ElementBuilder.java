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

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.InputReaderFactory;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.ElementSource;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.util.AutoCloseableIterator;
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
    private static final String ID_CHARSET = "UTF-8";

    private final InputReader reader;
    private final HugeClient client;
    private final Table<HugeType, String, SchemaElement> schemas;

    public ElementBuilder(ElementSource source, LoadOptions options) {
        this.reader = InputReaderFactory.create(source.input());
        try {
            this.reader.init();
        } catch (Exception e) {
            throw new LoadException("Failed to init input reader", e);
        }
        this.client = HugeClientWrapper.get(options);
        this.schemas = HashBasedTable.create();
    }

    public abstract ElementSource source();

    public InputReader reader() {
        return this.reader;
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
            return this.build(this.filterFields(keyValues));
        } catch (IllegalArgumentException e) {
            throw new ParseException(line.rawLine(), e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        this.reader.close();
    }

    protected abstract GE build(Map<String, Object> keyValues);

    protected abstract boolean isIdField(String fieldName);

    private Map<String, Object> filterFields(Map<String, Object> keyValues) {
        for (String field : this.source().ignoredFields()) {
            keyValues.remove(field);
        }

        SchemaLabel schemaLabel = this.getSchemaLabel();
        Set<String> nullableKeys = schemaLabel.nullableKeys();
        Set<Object> nullValues = this.source().nullValues();
        if (!nullableKeys.isEmpty() && !nullValues.isEmpty()) {
            Iterator<Map.Entry<String, Object>> itor = keyValues.entrySet()
                                                                .iterator();
            itor.forEachRemaining(entry -> {
                String key = entry.getKey();
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
            if (this.isIdField(fieldName)) {
                continue;
            }
            String key = this.source().mappingField(fieldName);
            Object value = this.validatePropertyValue(key, fieldValue);

            element.property(key, value);
        }
    }

    protected PropertyKey getPropertyKey(String name) {
        SchemaElement schema = this.schemas.get(HugeType.PROPERTY_KEY, name);
        if (schema == null) {
            schema = this.client.schema().getPropertyKey(name);
        }
        if (schema == null) {
            throw new IllegalStateException(
                      String.format("The property key %s doesn't exist", name));
        } else {
            this.schemas.put(HugeType.PROPERTY_KEY, name, schema);
        }
        return (PropertyKey) schema;
    }

    protected VertexLabel getVertexLabel(String name) {
        SchemaElement schema = this.schemas.get(HugeType.VERTEX_LABEL, name);
        if (schema == null) {
            schema = this.client.schema().getVertexLabel(name);
        }
        if (schema == null) {
            throw new IllegalStateException(
                      String.format("The vertex label %s doesn't exist", name));
        } else {
            this.schemas.put(HugeType.VERTEX_LABEL, name, schema);
        }
        return (VertexLabel) schema;
    }

    protected EdgeLabel getEdgeLabel(String name) {
        SchemaElement schema = this.schemas.get(HugeType.EDGE_LABEL, name);
        if (schema == null) {
            schema = this.client.schema().getEdgeLabel(name);
        }
        if (schema == null) {
            throw new IllegalStateException(
                      String.format("The edge label %s doesn't exist", name));
        } else {
            this.schemas.put(HugeType.EDGE_LABEL, name, schema);
        }
        return (EdgeLabel) schema;
    }

    protected abstract SchemaLabel getSchemaLabel();

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
        try {
            E.checkArgument(id.getBytes(ID_CHARSET).length <= VERTEX_ID_LIMIT,
                            "Vertex id length limit is '%s', '%s' exceeds it",
                            VERTEX_ID_LIMIT, id);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Object validatePropertyValue(String key, Object rawValue) {
        PropertyKey pKey = this.getPropertyKey(key);
        InputSource inputSource = this.source().input();
        Object value = DataTypeUtil.convert(rawValue, pKey, inputSource);
        E.checkArgument(value != null,
                        "The value '%s' can't convert to class %s " +
                        "with cardinality %s",
                        rawValue, pKey.dataType().clazz(), pKey.cardinality());
        return value;
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
