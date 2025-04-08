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

import java.util.Set;

import org.apache.hugegraph.util.E;

import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Traverser;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EgonetRequest {

    @JsonProperty("sources")
    public Set<Object> sources;
    @JsonProperty("steps")
    public Steps steps;
    @JsonProperty("max_depth")
    public int maxDepth;
    @JsonProperty("limit")
    public long limit = Traverser.DEFAULT_LIMIT;
    @JsonProperty("count_only")
    public boolean countOnly = false;
    @JsonProperty("with_vertex")
    public boolean withVertex = false;
    @JsonProperty("with_path")
    public boolean withPath = false;
    @JsonProperty("with_edge")
    public boolean withEdge = false;

    private EgonetRequest() {
        this.sources = null;
        this.steps = null;
        this.maxDepth = Traverser.DEFAULT_MAX_DEPTH;
        this.limit = Traverser.DEFAULT_PATHS_LIMIT;
        this.countOnly = false;
        this.withVertex = false;
        this.withPath = false;
        this.withEdge = false;
    }

    public static EgonetRequest.Builder builder() {
        return new EgonetRequest.Builder();
    }

    @Override
    public String toString() {
        return String.format("EgonetRequest{sources=%s,steps=%s," +
                             "maxDepth=%s,limit=%s,countOnly=%s," +
                             "withVertex=%s,withPath=%s,withEdge=%s}",
                             this.sources, this.steps, this.maxDepth,
                             this.limit, this.countOnly, this.withVertex,
                             this.withPath, this.withEdge);
    }

    public static class Builder {

        private EgonetRequest request;
        private Steps.Builder stepsBuilder;

        private Builder() {
            this.request = new EgonetRequest();
            this.stepsBuilder = Steps.builder();
        }

        public Builder source(Set<Object> sources) {
            E.checkNotNull(sources, "sources");
            this.request.sources = sources;
            return this;
        }

        public Steps.Builder steps() {
            Steps.Builder builder = Steps.builder();
            this.stepsBuilder = builder;
            return builder;
        }

        public Builder maxDepth(int maxDepth) {
            TraversersAPI.checkPositive(maxDepth, "max depth");
            this.request.maxDepth = maxDepth;
            return this;
        }

        public Builder countOnly(boolean countOnly) {
            this.request.countOnly = countOnly;
            return this;
        }

        public Builder limit(long limit) {
            TraversersAPI.checkLimit(limit);
            this.request.limit = limit;
            return this;
        }

        public Builder withVertex(boolean withVertex) {
            this.request.withVertex = withVertex;
            return this;
        }

        public Builder withPath(boolean withPath) {
            this.request.withPath = withPath;
            return this;
        }

        public Builder withEdge(boolean withEdge) {
            this.request.withEdge = withEdge;
            return this;
        }

        public EgonetRequest build() {
            E.checkArgument(this.request.sources != null &&
                            !this.request.sources.isEmpty(),
                            "The sources can't be null or empty");
            this.request.steps = this.stepsBuilder.build();
            E.checkNotNull(this.request.steps, "steps");
            TraversersAPI.checkPositive(this.request.maxDepth, "max depth");
            TraversersAPI.checkLimit(this.request.limit);
            if (this.request.countOnly) {
                E.checkArgument(!this.request.withVertex &&
                                !this.request.withPath && !this.request.withEdge,
                                "Can't return vertex, edge or path " +
                                "when count only is true");
            }
            return this.request;
        }
    }
}
