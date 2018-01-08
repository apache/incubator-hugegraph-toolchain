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

import java.util.concurrent.ConcurrentHashMap;

import com.baidu.hugegraph.exception.InvalidOperationException;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Edge extends GraphElement {

    @JsonProperty("id")
    private String id;
    @JsonProperty("outV")
    private Object source;
    @JsonProperty("inV")
    private Object target;
    @JsonProperty("outVLabel")
    private String sourceLabel;
    @JsonProperty("inVLabel")
    private String targetLabel;

    @JsonCreator
    public Edge(@JsonProperty("label") String label) {
        this.label = label;
        this.properties = new ConcurrentHashMap<>();
        this.type = "edge";
    }

    public String id() {
        return this.id;
    }

    public void id(String id) {
        this.id = id;
    }

    public Object source() {
        return this.source;
    }

    public void source(Object source) {
        this.source = source;
    }

    public Edge source(Vertex source) {
        this.source = source.id();
        this.sourceLabel = source.label();
        return this;
    }

    public Object target() {
        return this.target;
    }

    public void target(Object target) {
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
        E.checkNotNull(key, "The property name can not be null");
        E.checkNotNull(value, "The property value can not be null");
        if (this.fresh()) {
            return (Edge) super.property(key, value);
        } else {
            return this.setProperty(key, value);
        }
    }

    @Override
    protected Edge setProperty(String key, Object value) {
        Edge edge = new Edge(this.label);
        edge.id(this.id);
        edge.source(this.source);
        edge.target(this.target);
        edge.property(key, value);
        // NOTE: append can also b used to update property
        edge = this.manager.appendEdgeProperty(edge);

        super.property(key, edge.property(key));
        return this;
    }

    @Override
    public Edge removeProperty(String key) {
        E.checkNotNull(key, "The property name can not be null");
        if (!this.properties.containsKey(key)) {
            throw new InvalidOperationException(
                      "The edge '%s' doesn't have the property '%s'",
                      this.id, key);
        }
        Edge edge = new Edge(this.label);
        edge.id(this.id);
        edge.source(this.source);
        edge.target(this.target);

        Object value = this.properties.get(key);
        edge.property(key, value);
        this.manager.eliminateEdgeProperty(edge);

        this.properties().remove(key);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{id=%s, source=%s, sourceLabel=%s, " +
                             "target=%s, targetLabel=%s, " +
                             "label=%s, properties=%s}",
                this.id, this.source, this.sourceLabel,
                this.target, this.targetLabel,
                this.label, this.properties);
    }
}
