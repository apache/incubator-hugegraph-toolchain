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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.structure.graph;

import java.util.HashMap;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.structure.GraphElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;


public class Vertex extends GraphElement {

    // Hold a graphManager object for addEdge().
    private GraphManager manager;

    @JsonCreator
    public Vertex(@JsonProperty("label") String label) {
        this.label = label;
        this.properties = new HashMap<>();
        this.type = "vertex";
    }

    public Edge addEdge(String label, Vertex vertex, Object... properties) {
        Preconditions.checkNotNull(label,
                "The edge label can not be null.");
        Preconditions.checkNotNull(vertex,
                "The target vertex can not be null.");
        return this.manager.addEdge(this, label, vertex, properties);
    }

    public void manager(GraphManager manager) {
        this.manager = manager;
    }

    @Override
    public Vertex property(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{id=%s, label=%s, properties=%s}",
                this.id,
                this.label,
                this.properties);
    }
}
