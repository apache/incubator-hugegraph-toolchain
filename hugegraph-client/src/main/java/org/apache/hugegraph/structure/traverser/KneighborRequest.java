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

public class KneighborRequest {

    @JsonProperty("source")
    private Object source;
    @JsonProperty("steps")
    public VESteps steps;
    @JsonProperty("max_depth")
    public int maxDepth;
    @JsonProperty("count_only")
    public boolean countOnly;
    @JsonProperty("limit")
    public int limit;
    @JsonProperty("with_vertex")
    public boolean withVertex;
    @JsonProperty("with_path")
    public boolean withPath;
    @JsonProperty("with_edge")
    public boolean withEdge;

    private KneighborRequest() {
        this.source = null;
        this.steps = null;
        this.maxDepth = Traverser.DEFAULT_MAX_DEPTH;
        this.countOnly = false;
        this.limit = Traverser.DEFAULT_PATHS_LIMIT;
        this.withVertex = false;
        this.withPath = false;
        this.withEdge = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("KneighborRequest{source=%s,steps=%s,maxDepth=%s" +
                             "countOnly=%s,limit=%s,withVertex=%s,withPath=%s,withEdge=%s}",
                             this.source, this.steps, this.maxDepth,
                             this.countOnly, this.limit, this.withVertex,
                             this.withPath, this.withEdge);
    }

    public static class Builder {

        private KneighborRequest request;
        private VESteps.Builder stepBuilder;

        private Builder() {
            this.request = new KneighborRequest();
            this.stepBuilder = VESteps.builder();
        }

        public Builder source(Object source) {
            E.checkNotNull(source, "source");
            this.request.source = source;
            return this;
        }

        public VESteps.Builder steps() {
            VESteps.Builder builder = VESteps.builder();
            this.stepBuilder = builder;
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

        public Builder limit(int limit) {
            TraversersAPI.checkLimit(limit);
            this.request.limit = limit;
            return this;
        }

        public Builder withVertex(boolean withVertex) {
            this.request.withVertex = withVertex;
            return this;
        }

        public Builder withEdge(boolean withEdge) {
            this.request.withEdge = withEdge;
            return this;
        }

        public Builder withPath(boolean withPath) {
            this.request.withPath = withPath;
            return this;
        }

        public KneighborRequest build() {
            E.checkNotNull(this.request.source, "The source can't be null");
            this.request.steps = this.stepBuilder.build();
            E.checkNotNull(this.request.steps, "steps");
            TraversersAPI.checkPositive(this.request.maxDepth, "max depth");
            TraversersAPI.checkLimit(this.request.limit);
            if (this.request.countOnly) {
                E.checkArgument(!this.request.withVertex &&
                                !this.request.withPath,
                                "Can't return vertex or path " +
                                "when count only is true");
            }
            return this.request;
        }
    }
}
