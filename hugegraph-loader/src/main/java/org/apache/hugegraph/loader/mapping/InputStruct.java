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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.apache.hugegraph.loader.constant.Checkable;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;

@JsonPropertyOrder({"id", "skip", "input", "vertices", "edges"})
public class InputStruct implements Checkable, Serializable {

    public static final InputStruct EMPTY = new InputStruct(ImmutableList.of(),
                                                            ImmutableList.of());

    @JsonProperty("id")
    private String id;
    @JsonProperty("skip")
    private boolean skip;
    @JsonProperty("input")
    private InputSource input;
    @JsonProperty("vertices")
    private List<VertexMapping> vertices;
    @JsonProperty("edges")
    private List<EdgeMapping> edges;

    @JsonCreator
    public InputStruct(@JsonProperty("vertices") List<VertexMapping> vertices,
                       @JsonProperty("edges") List<EdgeMapping> edges) {
        this.vertices = vertices != null ? vertices : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
    }

    @Override
    public void check() throws IllegalArgumentException {
        E.checkArgument(!StringUtils.isEmpty(this.id),
                        "The mapping.id can't be null or empty");
        E.checkArgument(this.input != null, "The mapping.input can't be null");
        this.input.check();
        E.checkArgument(!this.vertices.isEmpty() || !this.edges.isEmpty(),
                        "The mapping.vertices and mapping.edges can't be " +
                        "empty at same time, need specify at least one");
        this.vertices.forEach(VertexMapping::check);
        this.edges.forEach(EdgeMapping::check);

        this.vertices.forEach(vm -> vm.checkFieldsValid(this.input));
        this.edges.forEach(em -> em.checkFieldsValid(this.input));
    }

    public String id() {
        return this.id;
    }

    public void id(String id) {
        this.id = id;
    }

    public boolean skip() {
        return this.skip;
    }

    public InputSource input() {
        return this.input;
    }

    public void input(InputSource input) {
        this.input = input;
    }

    public List<VertexMapping> vertices() {
        return this.vertices;
    }

    public List<EdgeMapping> edges() {
        return this.edges;
    }

    public void add(ElementMapping mapping) {
        if (mapping.type().isVertex()) {
            this.vertices.add((VertexMapping) mapping);
        } else {
            this.edges.add((EdgeMapping) mapping);
        }
    }

    public InputStruct extractVertexStruct() {
        if (this.vertices.isEmpty()) {
            return EMPTY;
        }
        InputStruct struct = new InputStruct(this.vertices, ImmutableList.of());
        struct.id = this.id;
        struct.skip = this.skip;
        struct.input = this.input;
        return struct;
    }

    public InputStruct extractEdgeStruct() {
        if (this.edges.isEmpty()) {
            return EMPTY;
        }
        InputStruct struct = new InputStruct(ImmutableList.of(), this.edges);
        struct.id = this.id;
        struct.skip = this.skip;
        struct.input = this.input;
        return struct;
    }

    @Override
    public String toString() {
        return String.format("InputStruct{id=%s, input=%s}",
                             this.id, this.input);
    }
}
