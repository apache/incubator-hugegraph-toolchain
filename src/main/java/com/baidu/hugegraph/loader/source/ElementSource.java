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

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ElementSource {

    @JsonProperty("label")
    private String label;
    @JsonProperty("input")
    private InputSource input;
    @JsonProperty("mapping")
    private Map<String, String> mappingFields;
    @JsonProperty("ignored")
    private Set<String> ignoredFields;

    public ElementSource(String label, InputSource input,
                         Map<String, String> mappingFields,
                         Set<String> ignoredFields) {
        this.label = label;
        this.input = input;
        this.mappingFields = mappingFields;
        this.ignoredFields = ignoredFields;
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

    public Set<String> ignoredFields() {
        return this.ignoredFields;
    }
}
