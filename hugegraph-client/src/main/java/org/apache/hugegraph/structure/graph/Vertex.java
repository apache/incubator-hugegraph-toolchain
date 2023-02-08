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

import java.util.Map;

import org.apache.hugegraph.exception.InvalidOperationException;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Vertex extends GraphElement {

    @JsonProperty("id")
    private Object id;

    @JsonCreator
    public Vertex(@JsonProperty("label") String label) {
        this.label = label;
        this.type = "vertex";
    }

    @Override
    public Object id() {
        return this.id;
    }

    public void id(Object id) {
        this.id = id;
    }

    public Edge addEdge(String label, Vertex vertex, Object... properties) {
        E.checkNotNull(label, "The edge label can not be null.");
        E.checkNotNull(vertex, "The target vertex can not be null.");
        return this.manager.addEdge(this, label, vertex, properties);
    }

    public Edge addEdge(String label, Vertex vertex,
                        Map<String, Object> properties) {
        E.checkNotNull(label, "The edge label can not be null.");
        E.checkNotNull(vertex, "The target vertex can not be null.");
        return this.manager.addEdge(this, label, vertex, properties);
    }

    @Override
    public Vertex property(String key, Object value) {
        E.checkNotNull(key, "The property name can not be null");
        E.checkNotNull(value, "The property value can not be null");
        if (this.fresh()) {
            return (Vertex) super.property(key, value);
        } else {
            return this.setProperty(key, value);
        }
    }

    @Override
    protected Vertex setProperty(String key, Object value) {
        Vertex vertex = new Vertex(this.label);
        vertex.id(this.id);
        vertex.property(key, value);
        // NOTE: append can also be used to update property
        vertex = this.manager.appendVertexProperty(vertex);

        super.property(key, vertex.property(key));
        return this;
    }

    @Override
    public Vertex removeProperty(String key) {
        E.checkNotNull(key, "The property name can not be null");
        if (!this.properties.containsKey(key)) {
            throw new InvalidOperationException("The vertex '%s' doesn't have the property '%s'",
                                                this.id, key);
        }
        Vertex vertex = new Vertex(this.label);
        vertex.id(this.id);
        Object value = this.properties.get(key);
        vertex.property(key, value);
        this.manager.eliminateVertexProperty(vertex);

        this.properties().remove(key);
        return this;
    }

    @Override
    public String toString() {
        return String.format("{id=%s, label=%s, properties=%s}",
                             this.id, this.label, this.properties);
    }
}
