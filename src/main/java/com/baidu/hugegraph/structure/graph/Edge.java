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

package com.baidu.hugegraph.structure.graph;

import java.util.HashMap;

import com.baidu.hugegraph.structure.GraphElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Edge extends GraphElement {

    @JsonProperty("outV")
    private String source;
    @JsonProperty("inV")
    private String target;
    @JsonProperty("outVLabel")
    private String sourceLabel;
    @JsonProperty("inVLabel")
    private String targetLabel;

    @JsonCreator
    public Edge(@JsonProperty("label") String label) {
        this.label = label;
        this.properties = new HashMap<>();
        this.type = "edge";
    }

    public String source() {
        return this.source;
    }

    public void source(String source) {
        this.source = source;
    }

    public Edge source(Vertex source) {
        this.source = source.id();
        this.sourceLabel = source.label();
        return this;
    }

    public String target() {
        return this.target;
    }

    public void target(String target) {
        this.target = target;
    }

    public Edge target(Vertex target) {
        this.target = target.id();
        this.targetLabel = target.label();
        return this;
    }

    public String sourceLabel() {
        return sourceLabel;
    }

    public void sourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String targetLabel() {
        return targetLabel;
    }

    public void targetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    @Override
    public Edge property(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{id=%s, "
                        + "source=%s, sourceLabel=%s, "
                        + "target=%s, targetLabel=%s, "
                        + "label=%s, properties=%s}",
                this.id,
                this.source, this.sourceLabel,
                this.target, this.targetLabel,
                this.label, this.properties);
    }
}
