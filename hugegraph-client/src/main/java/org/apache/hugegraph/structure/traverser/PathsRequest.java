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

public class PathsRequest {

    @JsonProperty("sources")
    private VerticesArgs sources;
    @JsonProperty("targets")
    private VerticesArgs targets;
    @JsonProperty("step")
    public EdgeStep step;
    @JsonProperty("max_depth")
    public int depth;
    @JsonProperty("nearest")
    public boolean nearest = false;
    @JsonProperty("capacity")
    public long capacity = Traverser.DEFAULT_CAPACITY;
    @JsonProperty("limit")
    public int limit = Traverser.DEFAULT_LIMIT;
    @JsonProperty("with_vertex")
    public boolean withVertex = false;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("PathRequest{sources=%s,targets=%s,step=%s," +
                             "maxDepth=%s,nearest=%s,capacity=%s," +
                             "limit=%s,withVertex=%s}",
                             this.sources, this.targets, this.step, this.depth,
                             this.nearest, this.capacity,
                             this.limit, this.withVertex);
    }

    public static class Builder {

        private PathsRequest request;
        private EdgeStep.Builder stepBuilder;
        private VerticesArgs.Builder sourcesBuilder;
        private VerticesArgs.Builder targetsBuilder;

        private Builder() {
            this.request = new PathsRequest();
            this.stepBuilder = EdgeStep.builder();
            this.sourcesBuilder = VerticesArgs.builder();
            this.targetsBuilder = VerticesArgs.builder();
        }

        public VerticesArgs.Builder sources() {
            return this.sourcesBuilder;
        }

        public VerticesArgs.Builder targets() {
            return this.targetsBuilder;
        }

        public EdgeStep.Builder step() {
            EdgeStep.Builder builder = EdgeStep.builder();
            this.stepBuilder = builder;
            return builder;
        }

        public PathsRequest.Builder maxDepth(int maxDepth) {
            TraversersAPI.checkPositive(maxDepth, "max depth");
            this.request.depth = maxDepth;
            return this;
        }

        public PathsRequest.Builder nearest(boolean nearest) {
            this.request.nearest = nearest;
            return this;
        }

        public PathsRequest.Builder capacity(long capacity) {
            TraversersAPI.checkCapacity(capacity);
            this.request.capacity = capacity;
            return this;
        }

        public PathsRequest.Builder limit(int limit) {
            TraversersAPI.checkLimit(limit);
            this.request.limit = limit;
            return this;
        }

        public PathsRequest.Builder withVertex(boolean withVertex) {
            this.request.withVertex = withVertex;
            return this;
        }

        public PathsRequest build() {
            this.request.sources = this.sourcesBuilder.build();
            E.checkArgument(this.request.sources != null,
                            "Source vertices can't be null");
            this.request.targets = this.targetsBuilder.build();
            E.checkArgument(this.request.targets != null,
                            "Target vertices can't be null");
            this.request.step = this.stepBuilder.build();
            E.checkNotNull(this.request.step, "The steps can't be null");
            TraversersAPI.checkPositive(this.request.depth, "max depth");
            TraversersAPI.checkCapacity(this.request.capacity);
            TraversersAPI.checkLimit(this.request.limit);
            return this.request;
        }
    }
}
