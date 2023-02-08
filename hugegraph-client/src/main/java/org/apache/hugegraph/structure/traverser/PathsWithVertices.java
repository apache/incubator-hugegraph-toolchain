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

package org.apache.hugegraph.structure.traverser;

import java.util.List;
import java.util.Set;

import org.apache.hugegraph.structure.graph.Vertex;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PathsWithVertices {

    @JsonProperty
    private List<Paths> paths;
    @JsonProperty
    private Set<Vertex> vertices;

    public List<Paths> paths() {
        return this.paths;
    }

    public Set<Vertex> vertices() {
        return this.vertices;
    }

    public static class Paths {

        @JsonProperty
        private List<Object> objects;
        @JsonProperty
        private List<Double> weights;

        public List<Object> objects() {
            return this.objects;
        }

        public List<Double> weights() {
            return this.weights;
        }
    }
}
