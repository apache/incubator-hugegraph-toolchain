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

package org.apache.hugegraph.spark.connector.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.spark.connector.constant.DataTypeEnum;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"label"})
public abstract class ElementMapping implements Serializable {

    @JsonProperty("label")
    private String label;

    @JsonProperty("field_mapping")
    private Map<String, String> mappingFields;

    @JsonProperty("value_mapping")
    private Map<String, Map<String, Object>> mappingValues;

    @JsonProperty("selected")
    private Set<String> selectedFields;

    @JsonProperty("ignored")
    private Set<String> ignoredFields;

    @JsonProperty("batch_size")
    private int batchSize;

    public ElementMapping() {
        this.mappingFields = new HashMap<>();
        this.mappingValues = new HashMap<>();
        this.selectedFields = new HashSet<>();
        this.ignoredFields = new HashSet<>();
        this.batchSize = 0;
    }

    public abstract DataTypeEnum type();

    public void check() throws IllegalArgumentException {
        this.mappingFields.values().forEach(value -> E.checkArgument(value != null,
                                                                     "The value in field_mapping " +
                                                                     "can't be null"));
        this.mappingValues.values()
                          .forEach(m -> m.values().forEach(value -> E.checkArgument(value != null,
                                                           "The value in " +
                                                           "value_mapping can't be null")));
    }

    public String label() {
        return this.label;
    }

    public void label(String label) {
        this.label = label;
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

    public int batchSize() {
        return this.batchSize;
    }

    public void batchSize(int batchSize) {
        this.batchSize = batchSize;
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
}
