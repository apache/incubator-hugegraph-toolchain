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

package org.apache.hugegraph.structure.graph;

import org.apache.hugegraph.util.SplicingIdGenerator;
import org.apache.hugegraph.exception.InvalidOperationException;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Edge extends GraphElement {

    @JsonProperty("id")
    private String id;
    @JsonProperty("outV")
    private Object sourceId;
    @JsonProperty("inV")
    private Object targetId;
    @JsonProperty("outVLabel")
    private String sourceLabel;
    @JsonProperty("inVLabel")
    private String targetLabel;

    private Vertex source;
    private Vertex target;
    private String name;

    @JsonCreator
    public Edge(@JsonProperty("label") String label) {
        this.label = label;
        this.type = "edge";
        this.sourceId = null;
        this.targetId = null;
        this.sourceLabel = null;
        this.targetLabel = null;
        this.source = null;
        this.target = null;
        this.name = null;
    }

    @Override
    public String id() {
        return this.id;
    }

    public void id(String id) {
        this.id = id;
    }

    public Object sourceId() {
        if (this.sourceId == null && this.source != null) {
            this.sourceId = this.source.id();
        }
        if (this.sourceId == null) {
            throw new InvalidOperationException("Must set source vertex id or add vertices " +
                                                "before add edges");
        }
        return this.sourceId;
    }

    public void sourceId(Object sourceId) {
        E.checkArgumentNotNull(sourceId, "The source vertex id can't be null");
        this.sourceId = sourceId;
    }

    public Edge source(Vertex source) {
        if (source.id() == null) {
            this.source = source;
        }
        this.sourceId = source.id();
        this.sourceLabel = source.label();
        return this;
    }

    public Object targetId() {
        if (this.targetId == null && this.target != null) {
            this.targetId = this.target.id();
        }
        if (this.targetId == null) {
            throw new InvalidOperationException("Must set target vertex id or add vertices " +
                                                "before add edges");
        }
        return this.targetId;
    }

    public void targetId(Object targetId) {
        E.checkArgumentNotNull(targetId, "The target vertex id can't be null");
        this.targetId = targetId;
    }

    public Edge target(Vertex target) {
        if (target.id() == null) {
            this.target = target;
        }
        this.targetId = target.id();
        this.targetLabel = target.label();
        return this;
    }

    public boolean linkedVertex(Object vertexId) {
        return this.sourceId.equals(vertexId) ||
               this.targetId.equals(vertexId);
    }

    public boolean linkedVertex(Vertex vertex) {
        return this.linkedVertex(vertex.id());
    }

    public String sourceLabel() {
        return this.sourceLabel;
    }

    public void sourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String targetLabel() {
        return this.targetLabel;
    }

    public void targetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public String name() {
        if (this.name == null) {
            String[] idParts = SplicingIdGenerator.split(this.id);
            E.checkState(idParts.length == 4,
                         "The edge id must be formatted by 4 parts, " +
                         "actual is %s", idParts.length);
            this.name = idParts[2];
        }
        return this.name;
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
        edge.sourceId(this.sourceId);
        edge.targetId(this.targetId);
        edge.property(key, value);
        // NOTE: append can also be used to update property
        edge = this.manager.appendEdgeProperty(edge);

        super.property(key, edge.property(key));
        return this;
    }

    @Override
    public Edge removeProperty(String key) {
        E.checkNotNull(key, "The property name can not be null");
        if (!this.properties.containsKey(key)) {
            throw new InvalidOperationException("The edge '%s' doesn't have the property '%s'",
                                                this.id, key);
        }
        Edge edge = new Edge(this.label);
        edge.id(this.id);
        edge.sourceId(this.sourceId);
        edge.targetId(this.targetId);

        Object value = this.properties.get(key);
        edge.property(key, value);
        this.manager.eliminateEdgeProperty(edge);

        this.properties().remove(key);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{id=%s, sourceId=%s, sourceLabel=%s, targetId=%s, " +
                             "targetLabel=%s, label=%s, properties=%s}",
                             this.id, this.sourceId, this.sourceLabel, this.targetId,
                             this.targetLabel, this.label, this.properties);
    }
}
