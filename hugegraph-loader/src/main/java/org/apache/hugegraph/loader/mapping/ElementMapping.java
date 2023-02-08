/*
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

package org.apache.hugegraph.loader.mapping;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.loader.constant.Checkable;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.constant.ElemType;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.structure.graph.UpdateStrategy;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableSet;

@JsonPropertyOrder({"label", "skip"})
public abstract class ElementMapping implements Checkable, Serializable {

    @JsonProperty("label")
    private String label;
    @JsonProperty("skip")
    private boolean skip;
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
    @JsonProperty("update_strategies")
    private Map<String, UpdateStrategy> updateStrategies;
    @JsonProperty("batch_size")
    private long batchSize;

    public ElementMapping() {
        this.skip = false;
        this.mappingFields = new HashMap<>();
        this.mappingValues = new HashMap<>();
        this.selectedFields = new HashSet<>();
        this.ignoredFields = new HashSet<>();
        this.nullValues = ImmutableSet.of(Constants.EMPTY_STR);
        this.updateStrategies = new HashMap<>();
        this.batchSize = 500;
    }

    public abstract ElemType type();

    @Override
    public void check() throws IllegalArgumentException {
        E.checkArgument(this.label != null && !this.label.isEmpty(),
                        "The label can't be null or empty");
        E.checkArgument(this.selectedFields.isEmpty() ||
                        this.ignoredFields.isEmpty(),
                        "Not allowed to specify selected(%s) and ignored(%s) " +
                        "fields at the same time, at least one of them " +
                        "must be empty", selectedFields, ignoredFields);
        this.mappingFields.values().forEach(value -> {
            E.checkArgument(value != null,
                            "The value in field_mapping can't be null");
        });
        this.mappingValues.values().forEach(m -> {
            m.values().forEach(value -> {
                E.checkArgument(value != null,
                                "The value in value_mapping can't be null");
            });
        });
    }

    public void checkFieldsValid(InputSource source) {
        if (source.header() == null) {
            return;
        }
        List<String> header = Arrays.asList(source.header());
        if (!this.selectedFields.isEmpty()) {
            E.checkArgument(new HashSet<>(header).containsAll(this.selectedFields),
                            "The all keys %s of selected must be existed " +
                            "in header %s", this.selectedFields, header);
        }
        if (!this.ignoredFields.isEmpty()) {
            E.checkArgument(new HashSet<>(header).containsAll(this.ignoredFields),
                            "The all keys %s of ignored must be existed " +
                            "in header %s", this.ignoredFields, header);
        }
        if (!this.mappingFields.isEmpty()) {
            E.checkArgument(new HashSet<>(header).containsAll(this.mappingFields.keySet()),
                            "The all keys %s of field_mapping must be " +
                            "existed in header",
                            this.mappingFields.keySet(), header);
        }
        if (!this.mappingValues.isEmpty()) {
            E.checkArgument(new HashSet<>(header).containsAll(this.mappingValues.keySet()),
                            "The all keys %s of value_mapping must be " +
                            "existed in header",
                            this.mappingValues.keySet(), header);
        }
    }

    public String label() {
        return this.label;
    }

    public void label(String label) {
        this.label = label;
    }

    public boolean skip() {
        return this.skip;
    }

    public void skip(boolean skip) {
        this.skip = skip;
    }

    public Map<String, String> mappingFields() {
        return this.mappingFields;
    }

    public void mappingFields(Map<String, String> mappingFields) {
        this.mappingFields = mappingFields;
    }

    public String mappingField(String fieldName) {
        if (this.mappingFields.isEmpty()) {
            return fieldName;
        }
        String mappingName = this.mappingFields.get(fieldName);
        return mappingName != null ? mappingName : fieldName;
    }

    public Map<String, Map<String, Object>> mappingValues() {
        return this.mappingValues;
    }

    public void mappingValues(Map<String, Map<String, Object>> mappingValues) {
        this.mappingValues = mappingValues;
    }

    public Object mappingValue(String fieldName, String rawValue) {
        if (this.mappingValues.isEmpty()) {
            return rawValue;
        }
        Object mappingValue = rawValue;
        Map<String, Object> values = this.mappingValues.get(fieldName);
        if (values != null) {
            Object value = values.get(rawValue);
            if (value != null) {
                mappingValue = value;
            }
        }
        return mappingValue;
    }

    public long batchSize() {
        return this.batchSize;
    }

    public Set<String> selectedFields() {
        return this.selectedFields;
    }

    public void selectedFields(Set<String> selectedFields) {
        this.selectedFields = selectedFields;
    }

    public Set<String> ignoredFields() {
        return this.ignoredFields;
    }

    public void ignoredFields(Set<String> ignoredFields) {
        this.ignoredFields = ignoredFields;
    }

    public Set<Object> nullValues() {
        return this.nullValues;
    }

    public void nullValues(Set<Object> nullValues) {
        this.nullValues = nullValues;
    }

    public Map<String, UpdateStrategy> updateStrategies() {
        return this.updateStrategies;
    }

    public void updateStrategies(Map<String, UpdateStrategy> updateStrategies) {
        this.updateStrategies = updateStrategies;
    }
}
