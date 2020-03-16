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
import java.util.Iterator;
import java.util.List;
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
import com.baidu.hugegraph.loader.struct.EdgeStruct;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.struct.VertexStruct;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.baidu.hugegraph.util.LongEncoding;

public abstract class ElementBuilder<GE extends GraphElement>
                implements AutoCloseableIterator<Record<GE>> {

    private static final Logger LOG = Log.logger(ElementBuilder.class);

    private static final CharsetEncoder ENCODER =
                         Constants.CHARSET.newEncoder();
    private static final ByteBuffer BUFFER =
                         ByteBuffer.allocate(Constants.VERTEX_ID_LIMIT);

    private final SchemaCache schema;
    private final InputReader reader;

    public ElementBuilder(LoadContext context, ElementStruct struct) {
        this.schema = new SchemaCache(context);
        this.reader = InputReaderFactory.create(struct.input());
        this.init(context, struct);
    }

    public static ElementBuilder<?> of(LoadContext context,
                                       ElementStruct struct) {
        if (struct.type().isVertex()) {
            return new VertexBuilder(context, (VertexStruct) struct);
        } else {
            assert struct.type().isEdge();
            return new EdgeBuilder(context, (EdgeStruct) struct);
        }
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
    public Record<GE> next() {
        Line line = this.reader.next();
        Map<String, Object> keyValues = line.keyValues();
        try {
            keyValues = this.filterFields(keyValues);
            GE element = this.build(keyValues);
            return Record.of(line.rawLine(), element);
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

    public long confirmOffset() {
        return this.reader.confirmOffset();
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
        Set<String> properties = this.getSchemaLabel().properties();
        for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            this.checkFieldValue(fieldName, fieldValue);
            String key = this.struct().mappingField(fieldName);

            // If the ID field is also a property, retained it
            if (this.isIdField(fieldName) &&
                !properties.contains(fieldName) && !properties.contains(key)) {
                continue;
            }
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
