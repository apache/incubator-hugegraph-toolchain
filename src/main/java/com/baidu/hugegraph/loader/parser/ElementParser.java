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

package com.baidu.hugegraph.loader.parser;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.source.ElementSource;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public abstract class ElementParser<GE extends GraphElement>
       implements Iterator<GE> {

    private static final int VERTEX_ID_LIMIT = 128;
    private static final String ID_CHARSET = "UTF-8";

    private final InputReader reader;

    private final HugeClient client;
    private final Table<HugeType, String, SchemaElement> schemas;

    ElementParser(InputReader reader, LoadOptions options) {
        this.reader = reader;
        this.client = HugeClientWrapper.get(options);
        this.schemas = HashBasedTable.create();
        this.reader.init();
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
        String line = this.reader().line();
        try {
            return this.parse(this.filterFields(this.reader().next()));
        } catch (IllegalArgumentException e) {
            throw new ParseException(line, e.getMessage());
        }
    }

    protected abstract GE parse(Map<String, Object> keyValues);

    protected abstract boolean isIdField(String fieldName);

    protected Map<String, Object> filterFields(Map<String, Object> keyValues) {
        for (String field : this.source().ignoredFields()) {
            keyValues.remove(field);
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

    protected String spliceVertexId(VertexLabel vertexLabel,
                                    List<Object> primaryValues) {
        E.checkArgument(vertexLabel.primaryKeys().size() == primaryValues.size(),
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
        Object value = DataTypeUtil.convert(rawValue, pKey);
        E.checkArgument(value != null,
                        "The value '%s' can't convert to class %s " +
                        "with cardinality %s",
                        rawValue, pKey.dataType().clazz(), pKey.cardinality());
        return value;
    }

    public static boolean isAutomatic(IdStrategy idStrategy) {
        return idStrategy == IdStrategy.AUTOMATIC;
    }

    public static boolean isCustomize(IdStrategy idStrategy) {
        return idStrategy == IdStrategy.CUSTOMIZE_STRING ||
               idStrategy == IdStrategy.CUSTOMIZE_NUMBER;
    }

    public static boolean isPrimaryKey(IdStrategy idStrategy) {
        return idStrategy == IdStrategy.PRIMARY_KEY;
    }
}
