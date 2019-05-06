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

package com.baidu.hugegraph.loader.source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ElementSource implements Checkable {

    @JsonProperty("label")
    private String label;
    @JsonProperty("input")
    private InputSource input;
    @JsonProperty("field_mapping")
    private Map<String, String> mappingFields;
    @JsonProperty("value_mapping")
    private Map<String, Map<String, Object>> mappingValues;
    @JsonProperty("selected")
    private Set<String> selectedFields;
    @JsonProperty("ignored")
    private Set<String> ignoredFields;
    @JsonProperty("null_values")
    private Set<Object> nullValues;

    public ElementSource() {
        this.mappingFields = new HashMap<>();
        this.mappingValues = new HashMap<>();
        this.selectedFields = new HashSet<>();
        this.ignoredFields = new HashSet<>();
        this.nullValues = new HashSet<>();
    }

    @Override
    public void check() throws IllegalArgumentException {
        E.checkArgument(this.label != null && !this.label.isEmpty(),
                        "The label can't be null or empty");
        this.input.check();
        E.checkArgument(this.selectedFields.isEmpty() ||
                        this.ignoredFields.isEmpty(),
                        "Not allowed to specify selected(%s) and ignored(%s) " +
                        "fields at the same time, at least one of them " +
                        "must be empty", selectedFields, ignoredFields);
        /*
         * The json file source will deserialize value in real data class
         * (1 -> Integer, "1" -> String), but value_mapping can only use
         * String as key("1": "xxx"), we don't know which value (1 and "1")
         * should mapped by key "1". This situation will also occur in jdbc.
         * This situation doesn't appear in the text file, because all value
         * will be parsed into String(1 -> "1", "1" -> "\"1\""), the key of
         * value_mapping can distinguish them.
         */
        if (this.input.type() == SourceType.FILE ||
            this.input.type() == SourceType.HDFS) {
            FileSource fileSource = (FileSource) this.input;
            if (fileSource.format() == FileFormat.JSON) {
                E.checkArgument(this.mappingValues.isEmpty(),
                                "Not allowed to specify value_mapping when " +
                                "the format of input source is JSON");
            }
        } else if (this.input.type() == SourceType.JDBC) {
            E.checkArgument(this.mappingValues.isEmpty(),
                            "Not allowed to specify value_mapping when " +
                            "the input source is JDBC");
        }
        this.mappingValues.values().forEach(m -> {
            m.values().forEach(value -> {
                E.checkArgumentNotNull(value, "The mapped value can't be null");
            });
        });
    }

    public String label() {
        return this.label;
    }

    public InputSource input() {
        return this.input;
    }

    public Map<String, String> mappingFields() {
        return this.mappingFields;
    }

    public String mappingField(String fieldName) {
        String mappingName = fieldName;
        if (this.mappingFields.containsKey(fieldName)) {
            mappingName = this.mappingFields.get(fieldName);
        }
        return mappingName;
    }

    public Map<String, Map<String, Object>> mappingValues() {
        return this.mappingValues;
    }

    public Object mappingValue(String fieldName, String rawValue) {
        Object mappingValue = rawValue;
        if (this.mappingValues.containsKey(fieldName)) {
            Map<String, Object> values = this.mappingValues.get(fieldName);
            if (values.containsKey(rawValue)) {
                mappingValue = values.get(rawValue);
            }
        }
        return mappingValue;
    }

    public Set<String> selectedFields() {
        return this.selectedFields;
    }

    public Set<String> ignoredFields() {
        return this.ignoredFields;
    }

    public Set<Object> nullValues() {
        return this.nullValues;
    }
}
