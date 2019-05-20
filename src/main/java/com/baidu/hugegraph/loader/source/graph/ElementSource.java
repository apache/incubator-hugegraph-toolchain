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

package com.baidu.hugegraph.loader.source.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.baidu.hugegraph.loader.constant.Unique;
import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.HashUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ElementSource implements Unique<String>, Checkable {

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
    public String uniqueKey() {
        String code = HashUtil.hash(JsonUtil.toJson(this));
        return this.label + "-" + code;
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
