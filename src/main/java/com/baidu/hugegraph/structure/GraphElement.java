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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.ReflectionUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
              include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({@JsonSubTypes.Type(value = Vertex.class, name = "vertex"),
               @JsonSubTypes.Type(value = Edge.class, name = "edge")})
public abstract class GraphElement extends Element {

    @JsonProperty("id")
    protected String id;
    @JsonProperty("label")
    protected String label;
    @JsonProperty("type")
    protected String type;
    @JsonProperty("properties")
    protected Map<String, Object> properties;

    public String id() {
        return this.id;
    }

    public void id(String id) {
        this.id = id;
    }

    public String label() {
        return this.label;
    }

    public String type() {
        return this.type;
    }

    public GraphElement property(String name, Object value) {
        E.checkArgumentNotNull(name, "property name");
        E.checkArgumentNotNull(value, "property value");

        Class<?> clazz = value.getClass();
        E.checkArgument(ReflectionUtil.isSimpleType(clazz) ||
                        clazz.equals(UUID.class) ||
                        clazz.equals(Timestamp.class) ||
                        value instanceof List ||
                        value instanceof Set,
                        "Invalid property value type: '%s'", clazz);

        this.properties.put(name, value);
        return this;
    }

    public Map<String, Object> properties() {
        return this.properties;
    }
}
