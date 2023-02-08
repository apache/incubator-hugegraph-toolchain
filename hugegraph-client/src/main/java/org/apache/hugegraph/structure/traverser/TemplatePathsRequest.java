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

import java.util.ArrayList;
import java.util.List;

import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TemplatePathsRequest {

    @JsonProperty("sources")
    private VerticesArgs sources;
    @JsonProperty("targets")
    private VerticesArgs targets;
    @JsonProperty("steps")
    public List<RepeatEdgeStep> steps;
    @JsonProperty("with_ring")
    public boolean withRing;
    @JsonProperty("capacity")
    public long capacity;
    @JsonProperty("limit")
    public int limit;
    @JsonProperty("with_vertex")
    public boolean withVertex;

    private TemplatePathsRequest() {
        this.sources = null;
        this.targets = null;
        this.steps = new ArrayList<>();
        this.withRing = false;
        this.capacity = Traverser.DEFAULT_CAPACITY;
        this.limit = Traverser.DEFAULT_PATHS_LIMIT;
        this.withVertex = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("TemplatePathsRequest{sources=%s,targets=%s," +
                             "steps=%s,withRing=%s,capacity=%s,limit=%s," +
                             "withVertex=%s}", this.sources, this.targets,
                             this.steps, this.withRing, this.capacity,
                             this.limit, this.withVertex);
    }

    public static class Builder {

        private TemplatePathsRequest request;
        private VerticesArgs.Builder sourcesBuilder;
        private VerticesArgs.Builder targetsBuilder;
        private List<RepeatEdgeStep.Builder> stepBuilders;

        private Builder() {
            this.request = new TemplatePathsRequest();
            this.sourcesBuilder = VerticesArgs.builder();
            this.targetsBuilder = VerticesArgs.builder();
            this.stepBuilders = new ArrayList<>();
        }

        public VerticesArgs.Builder sources() {
            return this.sourcesBuilder;
        }

        public VerticesArgs.Builder targets() {
            return this.targetsBuilder;
        }

        public RepeatEdgeStep.Builder steps() {
            RepeatEdgeStep.Builder builder = RepeatEdgeStep.repeatStepBuilder();
            this.stepBuilders.add(builder);
            return builder;
        }

        public Builder withRing(boolean withRing) {
            this.request.withRing = withRing;
            return this;
        }

        public Builder capacity(long capacity) {
            TraversersAPI.checkCapacity(capacity);
            this.request.capacity = capacity;
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

        public TemplatePathsRequest build() {
            this.request.sources = this.sourcesBuilder.build();
            E.checkArgument(this.request.sources != null,
                            "Source vertices can't be null");
            this.request.targets = this.targetsBuilder.build();
            E.checkArgument(this.request.targets != null,
                            "Target vertices can't be null");
            for (RepeatEdgeStep.Builder builder : this.stepBuilders) {
                this.request.steps.add(builder.build());
            }
            E.checkArgument(this.request.steps != null &&
                            !this.request.steps.isEmpty(),
                            "The steps can't be null or empty");
            TraversersAPI.checkCapacity(this.request.capacity);
            TraversersAPI.checkLimit(this.request.limit);
            return this.request;
        }
    }
}
