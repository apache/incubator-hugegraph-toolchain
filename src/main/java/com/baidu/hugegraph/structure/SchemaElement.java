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

package com.baidu.hugegraph.structure;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SchemaElement extends Element {

    @JsonProperty("name")
    protected String name;
    @JsonProperty("properties")
    protected Set<String> properties;
    @JsonProperty("check_exist")
    protected boolean checkExist;

    public SchemaElement(String name) {
        this.name = name;
        this.properties = new HashSet<>();
        this.checkExist = true;
    }

    public String name() {
        return this.name;
    }

    public Set<String> properties() {
        return this.properties;
    }

    public boolean checkExits() {
        return this.checkExist;
    }

}
