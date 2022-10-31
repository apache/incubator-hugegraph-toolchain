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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.util.DataTypeUtil;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.SchemaLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.LongEncoding;
import com.google.common.collect.ImmutableList;
import org.apache.spark.sql.Row;

public abstract class ElementBuilder<GE extends GraphElement> {

    private final InputStruct struct;
    private final SchemaCache schema;

    // NOTE: CharsetEncoder is not thread safe
    private final CharsetEncoder encoder;
    private final ByteBuffer buffer;

    public ElementBuilder(LoadContext context, InputStruct struct) {
        this.struct = struct;
        this.schema = context.schemaCache();
        this.encoder = Constants.CHARSET.newEncoder();
        this.buffer = ByteBuffer.allocate(Constants.VERTEX_ID_LIMIT);
    }

    public abstract ElementMapping mapping();

    public abstract List<GE> build(String[] names, Object[] values);
    public abstract List<GE> build( Row row);

    public abstract SchemaLabel schemaLabel();

    protected abstract Collection<String> nonNullableKeys();

    protected abstract boolean isIdField(String fieldName);

    @SuppressWarnings("unchecked")
    protected Collection<String> nonNullableKeys(SchemaLabel schemaLabel) {
        return CollectionUtils.subtract(schemaLabel.properties(),
                                        schemaLabel.nullableKeys());
    }

    protected VertexKVPairs newKVPairs(VertexLabel vertexLabel,
                                       boolean unfold) {
        IdStrategy idStrategy = vertexLabel.idStrategy();
        if (idStrategy.isCustomize()) {
            if (unfold) {
                return new VertexFlatIdKVPairs(vertexLabel);
            } else {
                return new VertexIdKVPairs(vertexLabel);
            }
        } else {
            assert idStrategy.isPrimaryKey();
            if (unfold) {
                return new VertexFlatPkKVPairs(vertexLabel);
            } else {
                return new VertexPkKVPairs(vertexLabel);
            }
        }
    }

    /**
     * Retain only the key-value pairs needed by the current vertex or edge
     */
    protected boolean retainField(String fieldName, Object fieldValue) {
        ElementMapping mapping = this.mapping();
        Set<String> selectedFields = mapping.selectedFields();
        Set<String> ignoredFields = mapping.ignoredFields();
        // Retain selected fields or remove ignored fields
        if (!selectedFields.isEmpty() && !selectedFields.contains(fieldName)) {
            return false;
        }
        if (!ignoredFields.isEmpty() && ignoredFields.contains(fieldName)) {
            return false;
        }
        String mappedKey = mapping.mappingField(fieldName);
        Set<String> nullableKeys = this.schemaLabel().nullableKeys();
        Set<Object> nullValues = mapping.nullValues();
        if (nullableKeys.isEmpty() || nullValues.isEmpty()) {
            return true;
        }
        return !nullableKeys.contains(mappedKey) ||
               !nullValues.contains(fieldValue);
    }

    protected void addProperty(GraphElement element, String key, Object value) {
        this.addProperty(element, key, value, true);
    }

    protected void addProperty(GraphElement element, String key, Object value,
                               boolean needConvert) {
        if (needConvert) {
            value = this.convertPropertyValue(key, value);
        }
        element.property(key, value);
    }

    protected void addProperties(GraphElement element,
                                 Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            this.checkFieldValue(key, value);
            value = this.convertPropertyValue(key, value);

            element.property(key, value);
        }
    }

    protected void checkNonNullableKeys(GraphElement element) {
        Set<String> keys = element.properties().keySet();
        // Check whether passed all non-null property
        Collection<String> requiredKeys = this.nonNullableKeys();
        if (!keys.containsAll(requiredKeys)) {
            @SuppressWarnings("unchecked")
            Collection<String> missed = CollectionUtils.subtract(requiredKeys,
                                                                 keys);
            E.checkArgument(false, "All non-null property keys %s of '%s' " +
                            "must be setted, but missed keys %s",
                            requiredKeys, this.schemaLabel().name(), missed);
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

    protected Object mappingValue(String fieldName, Object fieldValue) {
        if (this.mapping().mappingValues().isEmpty()) {
            return fieldValue;
        }
        String fieldStrValue = String.valueOf(fieldValue);
        return this.mapping().mappingValue(fieldName, fieldStrValue);
    }

    private void customizeId(VertexLabel vertexLabel, Vertex vertex,
                             String idField, Object idValue) {
        E.checkArgumentNotNull(idField, "The vertex id field can't be null");
        E.checkArgumentNotNull(idValue, "The vertex id value can't be null");
        IdStrategy idStrategy = vertexLabel.idStrategy();
        if (idStrategy.isCustomizeString()) {
            String id = (String) idValue;
            this.checkVertexIdLength(id);
            vertex.id(id);
        } else if (idStrategy.isCustomizeNumber()) {
            Long id = DataTypeUtil.parseNumber(idField, idValue);
            vertex.id(id);
        } else {
            assert idStrategy.isCustomizeUuid();
            UUID id = DataTypeUtil.parseUUID(idField, idValue);
            vertex.id(id);
        }
    }

    private Object convertPropertyValue(String key, Object rawValue) {
        PropertyKey propertyKey = this.getPropertyKey(key);
        InputSource inputSource = this.struct.input();
        return DataTypeUtil.convert(rawValue, propertyKey, inputSource);
    }

    private void checkFieldValue(String fieldName, Object fieldValue) {
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

    private boolean vertexIdEmpty(VertexLabel vertexLabel, Vertex vertex) {
        IdStrategy idStrategy = vertexLabel.idStrategy();
        if (idStrategy.isCustomizeString()) {
            Object vertexId = vertex.id();
            return vertexId == null || StringUtils.isEmpty((String) vertexId);
        }
        return false;
    }

    private void checkPrimaryValuesValid(VertexLabel vertexLabel,
                                         Object[] primaryValues) {
        List<String> primaryKeys = vertexLabel.primaryKeys();
        E.checkArgument(primaryKeys.size() == primaryValues.length,
                        "Missing some primary key values, expect %s, " +
                        "but only got %s for vertex label '%s'",
                        primaryKeys, Arrays.toString(primaryValues),
                        vertexLabel);
        for (int i = 0; i < primaryKeys.size(); i++) {
            E.checkArgument(primaryValues[i] != null,
                            "Make sure the value of the primary key '%s' is " +
                            "not empty, or check whether the headers or " +
                            "field_mapping are configured correctly",
                            primaryKeys.get(i));
        }
    }

    private String spliceVertexId(VertexLabel vertexLabel,
                                  Object... primaryValues) {
        StringBuilder vertexId = new StringBuilder();
        StringBuilder vertexKeysId = new StringBuilder();
        for (int i = 0; i < primaryValues.length; i++) {
            Object value = primaryValues[i];
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

    private void checkVertexIdLength(String id) {
        this.encoder.reset();
        this.buffer.clear();
        CoderResult r = this.encoder.encode(CharBuffer.wrap(id.toCharArray()),
                                            this.buffer, true);
        E.checkArgument(r.isUnderflow(),
                        "The vertex id length exceeds limit %s : '%s'",
                        Constants.VERTEX_ID_LIMIT, id);
    }

    private boolean isEmptyPkValue(Object pkValue) {
        if (pkValue == null) {
            return true;
        }
        if (pkValue instanceof String) {
            String pkValueStr = (String) pkValue;
            return pkValueStr.isEmpty();
        }
        return false;
    }

    public abstract class VertexKVPairs {

        public final VertexLabel vertexLabel;
        // General properties
        public Map<String, Object> properties;

        public VertexKVPairs(VertexLabel vertexLabel) {
            this.vertexLabel = vertexLabel;
            this.properties = null;
        }

        public abstract void extractFromVertex(String[] names,
                                               Object[] values);

        public abstract void extractFromEdge(String[] names, Object[] values,
                                             int[] fieldIndexes);

        public abstract List<Vertex> buildVertices(boolean withProperty);

        public List<Object> splitField(String key, Object value) {
            return DataTypeUtil.splitField(key, value, struct.input());
        }
    }

    public class VertexIdKVPairs extends VertexKVPairs {

        // The idField(raw field), like: id
        private String idField;
        // The single idValue(mapped), like: A -> 1
        private Object idValue;

        public VertexIdKVPairs(VertexLabel vertexLabel) {
            super(vertexLabel);
        }

        @Override
        public void extractFromVertex(String[] names, Object[] values) {
            // General properties
            this.properties = new HashMap<>();
            for (int i = 0; i < names.length; i++) {
                String fieldName = names[i];
                Object fieldValue = values[i];
                if (!retainField(fieldName, fieldValue)) {
                    continue;
                }
                if (isIdField(fieldName)) {
                    this.idField = fieldName;
                    this.idValue = mappingValue(fieldName, fieldValue);
                } else {
                    String key = mapping().mappingField(fieldName);
                    Object value = mappingValue(fieldName, fieldValue);
                    this.properties.put(key, value);
                }
            }
        }

        @Override
        public void extractFromEdge(String[] names, Object[] values,
                                    int[] fieldIndexes) {
            assert fieldIndexes.length == 1;
            String fieldName = names[fieldIndexes[0]];
            Object fieldValue = values[fieldIndexes[0]];
            this.idField = fieldName;
            this.idValue = mappingValue(fieldName, fieldValue);
        }

        @Override
        public List<Vertex> buildVertices(boolean withProperty) {
            Vertex vertex = new Vertex(vertexLabel.name());
            customizeId(vertexLabel, vertex, this.idField, this.idValue);
            if (vertexIdEmpty(vertexLabel, vertex)) {
                return ImmutableList.of();
            }
            if (withProperty) {
                String key = mapping().mappingField(this.idField);
                // The id field is also used as a general property
                if (vertexLabel.properties().contains(key)) {
                    addProperty(vertex, key, this.idValue);
                }
                addProperties(vertex, this.properties);
                checkNonNullableKeys(vertex);
            }
            return ImmutableList.of(vertex);
        }
    }

    public class VertexFlatIdKVPairs extends VertexKVPairs {

        // The idField(raw field), like: id
        private String idField;
        /*
         * The multiple idValues(spilted and mapped)
         * like: A|B|C -> [1,2,3]
         */
        private List<Object> idValues;

        public VertexFlatIdKVPairs(VertexLabel vertexLabel) {
            super(vertexLabel);
        }

        @Override
        public void extractFromVertex(String[] names, Object[] values) {
            // General properties
            this.properties = new HashMap<>();
            for (int i = 0; i < names.length; i++) {
                String fieldName = names[i];
                Object fieldValue = values[i];
                if (!retainField(fieldName, fieldValue)) {
                    continue;
                }
                if (isIdField(fieldName)) {
                    this.idField = fieldName;
                    List<Object> rawIdValues = splitField(fieldName,
                                                          fieldValue);
                    this.idValues = rawIdValues.stream().map(rawIdValue -> {
                        return mappingValue(fieldName, rawIdValue);
                    }).collect(Collectors.toList());
                } else {
                    String key = mapping().mappingField(fieldName);
                    Object value = mappingValue(fieldName, fieldValue);
                    this.properties.put(key, value);
                }
            }
        }

        @Override
        public void extractFromEdge(String[] names, Object[] values,
                                    int[] fieldIndexes) {
            assert fieldIndexes.length == 1;
            String fieldName = names[fieldIndexes[0]];
            Object fieldValue = values[fieldIndexes[0]];
            this.idField = fieldName;
            List<Object> rawIdValues = splitField(fieldName, fieldValue);
            this.idValues = rawIdValues.stream().map(rawIdValue -> {
                return mappingValue(fieldName, rawIdValue);
            }).collect(Collectors.toList());
        }

        @Override
        public List<Vertex> buildVertices(boolean withProperty) {
            List<Vertex> vertices = new ArrayList<>(this.idValues.size());
            for (Object idValue : this.idValues) {
                Vertex vertex = new Vertex(vertexLabel.name());
                customizeId(vertexLabel, vertex, this.idField, idValue);
                if (vertexIdEmpty(vertexLabel, vertex)) {
                    continue;
                }
                if (withProperty) {
                    String key = mapping().mappingField(this.idField);
                    // The id field is also used as a general property
                    if (vertexLabel.properties().contains(key)) {
                        addProperty(vertex, key, idValue);
                    }
                    addProperties(vertex, this.properties);
                    checkNonNullableKeys(vertex);
                }
                vertices.add(vertex);
            }
            return vertices;
        }
    }

    public class VertexPkKVPairs extends VertexKVPairs {

        /*
         * The primary key names(mapped), allowed multiple
         * like: [p_name,p_age] -> [name,age]
         */
        private List<String> pkNames;
        /*
         * The primary values(mapped), length is the same as pkNames
         * like: [m,2] -> [marko,18]
         */
        private Object[] pkValues;

        public VertexPkKVPairs(VertexLabel vertexLabel) {
            super(vertexLabel);
        }

        @Override
        public void extractFromVertex(String[] names, Object[] values) {
            List<String> primaryKeys = this.vertexLabel.primaryKeys();
            this.pkNames = primaryKeys;
            this.pkValues = new Object[primaryKeys.size()];
            // General properties
            this.properties = new HashMap<>();
            for (int i = 0; i < names.length; i++) {
                String fieldName = names[i];
                Object fieldValue = values[i];
                if (!retainField(fieldName, fieldValue)) {
                    continue;
                }
                String key = mapping().mappingField(fieldName);
                if (primaryKeys.contains(key)) {
                    // Don't put priamry key/values into general properties
                    int index = primaryKeys.indexOf(key);
                    Object pkValue = mappingValue(fieldName, fieldValue);
                    this.pkValues[index] = pkValue;
                } else {
                    Object value = mappingValue(fieldName, fieldValue);
                    this.properties.put(key, value);
                }
            }
        }

        @Override
        public void extractFromEdge(String[] names, Object[] values,
                                    int[] fieldIndexes) {
            this.pkNames = new ArrayList<>(fieldIndexes.length);
            for (int fieldIndex : fieldIndexes) {
                String fieldName = names[fieldIndex];
                String mappingField = mapping().mappingField(fieldName);
                this.pkNames.add(mappingField);
            }
            List<String> primaryKeys = this.vertexLabel.primaryKeys();
            E.checkArgument(ListUtils.isEqualList(this.pkNames, primaryKeys),
                            "Make sure the the primary key fields %s are " +
                            "not empty, or check whether the headers or " +
                            "field_mapping are configured correctly",
                            primaryKeys);
            this.pkValues = new Object[this.pkNames.size()];
            for (int i = 0; i < fieldIndexes.length; i++) {
                String fieldName = names[fieldIndexes[i]];
                Object fieldValue = values[fieldIndexes[i]];
                Object pkValue = mappingValue(fieldName, fieldValue);
                this.pkValues[i] = pkValue;
            }
        }

        @Override
        public List<Vertex> buildVertices(boolean withProperty) {
            checkPrimaryValuesValid(vertexLabel, this.pkValues);
            for (int i = 0; i < this.pkNames.size(); i++) {
                if (isEmptyPkValue(this.pkValues[i])) {
                    return ImmutableList.of();
                }
                Object pkValue = convertPropertyValue(this.pkNames.get(i),
                                                      this.pkValues[i]);
                this.pkValues[i] = pkValue;
            }
            String id = spliceVertexId(vertexLabel, this.pkValues);
            checkVertexIdLength(id);

            Vertex vertex = new Vertex(vertexLabel.name());
            // NOTE: withProperty is true means that parsing vertex
            if (withProperty) {
                for (int i = 0; i < this.pkNames.size(); i++) {
                    addProperty(vertex, this.pkNames.get(i),
                                this.pkValues[i], false);
                }
                addProperties(vertex, this.properties);
                checkNonNullableKeys(vertex);
            }
            vertex.id(id);
            return ImmutableList.of(vertex);
        }
    }

    public class VertexFlatPkKVPairs extends VertexKVPairs {

        /*
         * The primary key name(mapped), must be single
         * like: p_name -> name
         */
        private String pkName;
        /*
         * The primary values(splited and mapped)
         * like: m|v -> [marko,vadas]
         */
        private List<Object> pkValues;

        public VertexFlatPkKVPairs(VertexLabel vertexLabel) {
            super(vertexLabel);
        }

        @Override
        public void extractFromVertex(String[] names, Object[] values) {
            List<String> primaryKeys = vertexLabel.primaryKeys();
            E.checkArgument(primaryKeys.size() == 1,
                            "In case unfold is true, just supported " +
                            "a single primary key");
            this.pkName = primaryKeys.get(0);
            // General properties
            this.properties = new HashMap<>();
            boolean handledPk = false;
            for (int i = 0; i < names.length; i++) {
                String fieldName = names[i];
                Object fieldValue = values[i];
                if (!retainField(fieldName, fieldValue)) {
                    continue;
                }
                String key = mapping().mappingField(fieldName);
                if (!handledPk && primaryKeys.contains(key)) {
                    // Don't put priamry key/values into general properties
                    List<Object> rawPkValues = splitField(fieldName,
                                                          fieldValue);
                    this.pkValues = rawPkValues.stream().map(rawPkValue -> {
                        return mappingValue(fieldName, rawPkValue);
                    }).collect(Collectors.toList());
                    handledPk = true;
                } else {
                    Object value = mappingValue(fieldName, fieldValue);
                    this.properties.put(key, value);
                }
            }
        }

        @Override
        public void extractFromEdge(String[] names, Object[] values,
                                    int[] fieldIndexes) {
            List<String> primaryKeys = vertexLabel.primaryKeys();
            E.checkArgument(fieldIndexes.length == 1 && primaryKeys.size() == 1,
                            "In case unfold is true, just supported " +
                            "a single primary key");
            String fieldName = names[fieldIndexes[0]];
            this.pkName = mapping().mappingField(fieldName);
            String primaryKey = primaryKeys.get(0);
            E.checkArgument(this.pkName.equals(primaryKey),
                            "Make sure the the primary key field '%s' is " +
                            "not empty, or check whether the headers or " +
                            "field_mapping are configured correctly",
                            primaryKey);
            Object fieldValue = values[fieldIndexes[0]];
            List<Object> rawPkValues = splitField(fieldName, fieldValue);
            this.pkValues = rawPkValues.stream().map(rawPkValue -> {
                return mappingValue(fieldName, rawPkValue);
            }).collect(Collectors.toList());
        }

        @Override
        public List<Vertex> buildVertices(boolean withProperty) {
            E.checkArgument(this.pkValues != null,
                            "The primary values shouldn't be null");
            List<Vertex> vertices = new ArrayList<>(this.pkValues.size());
            for (Object pkValue : this.pkValues) {
                if (isEmptyPkValue(pkValue)) {
                    continue;
                }
                pkValue = convertPropertyValue(this.pkName, pkValue);
                String id = spliceVertexId(vertexLabel, pkValue);
                checkVertexIdLength(id);

                Vertex vertex = new Vertex(vertexLabel.name());
                // NOTE: withProperty is true means that parsing vertex
                if (withProperty) {
                    addProperty(vertex, this.pkName, pkValue, false);
                    addProperties(vertex, this.properties);
                    checkNonNullableKeys(vertex);
                }
                vertex.id(id);
                vertices.add(vertex);
            }
            return vertices;
        }
    }
}
