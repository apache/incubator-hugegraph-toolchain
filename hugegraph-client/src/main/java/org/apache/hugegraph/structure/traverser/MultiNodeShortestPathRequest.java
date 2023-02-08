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

import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MultiNodeShortestPathRequest {

    @JsonProperty("vertices")
    public VerticesArgs vertices;
    @JsonProperty("step")
    public EdgeStep step;
    @JsonProperty("max_depth")
    public int maxDepth;
    @JsonProperty("capacity")
    public long capacity;
    @JsonProperty("with_vertex")
    public boolean withVertex;

    private MultiNodeShortestPathRequest() {
        this.vertices = null;
        this.step = null;
        this.maxDepth = Traverser.DEFAULT_MAX_DEPTH;
        this.capacity = Traverser.DEFAULT_CAPACITY;
        this.withVertex = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("MultiNodeShortestPathRequest{vertices=%s," +
                             "step=%s,maxDepth=%s,capacity=%s,withVertex=%s}",
                             this.vertices, this.step, this.maxDepth,
                             this.capacity, this.withVertex);
    }

    public static class Builder {

        private MultiNodeShortestPathRequest request;
        private VerticesArgs.Builder verticesBuilder;
        private EdgeStep.Builder stepBuilder;

        private Builder() {
            this.request = new MultiNodeShortestPathRequest();
            this.verticesBuilder = VerticesArgs.builder();
            this.stepBuilder = EdgeStep.builder();
        }

        public VerticesArgs.Builder vertices() {
            return this.verticesBuilder;
        }

        public EdgeStep.Builder step() {
            EdgeStep.Builder builder = EdgeStep.builder();
            this.stepBuilder = builder;
            return builder;
        }

        public Builder maxDepth(int maxDepth) {
            TraversersAPI.checkPositive(maxDepth, "max depth");
            this.request.maxDepth = maxDepth;
            return this;
        }

        public Builder capacity(long capacity) {
            TraversersAPI.checkCapacity(capacity);
            this.request.capacity = capacity;
            return this;
        }

        public Builder withVertex(boolean withVertex) {
            this.request.withVertex = withVertex;
            return this;
        }

        public MultiNodeShortestPathRequest build() {
            this.request.vertices = this.verticesBuilder.build();
            E.checkArgument(this.request.vertices != null,
                            "The vertices can't be null");
            this.request.step = this.stepBuilder.build();
            E.checkArgument(this.request.step != null,
                            "The step can't be null");
            TraversersAPI.checkPositive(this.request.maxDepth, "max depth");
            TraversersAPI.checkCapacity(this.request.capacity);
            return this.request;
        }
    }
}
