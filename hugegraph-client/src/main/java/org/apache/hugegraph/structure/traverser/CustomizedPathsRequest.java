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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomizedPathsRequest {

    @JsonProperty("sources")
    private VerticesArgs sources;
    @JsonProperty("steps")
    private List<Step> steps;
    @JsonProperty("sort_by")
    private SortBy sortBy;
    @JsonProperty("capacity")
    private long capacity;
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("with_vertex")
    private boolean withVertex;

    private CustomizedPathsRequest() {
        this.sources = null;
        this.steps = new ArrayList<>();
        this.sortBy = SortBy.NONE;
        this.capacity = Traverser.DEFAULT_CAPACITY;
        this.limit = Traverser.DEFAULT_PATHS_LIMIT;
        this.withVertex = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("CustomizedPathsRequest{sourceVertex=%s,steps=%s," +
                             "sortBy=%s,capacity=%s,limit=%s," +
                             "withVertex=%s}", this.sources, this.steps,
                             this.sortBy, this.capacity, this.limit,
                             this.withVertex);
    }

    public static class Builder {

        private CustomizedPathsRequest request;
        private VerticesArgs.Builder sourcesBuilder;
        private List<Step.Builder> stepBuilders;

        private Builder() {
            this.request = new CustomizedPathsRequest();
            this.sourcesBuilder = VerticesArgs.builder();
            this.stepBuilders = new ArrayList<>();
        }

        public VerticesArgs.Builder sources() {
            return this.sourcesBuilder;
        }

        public Step.Builder steps() {
            Step.Builder builder = new Step.Builder();
            this.stepBuilders.add(builder);
            return builder;
        }

        public Builder sortBy(SortBy sortBy) {
            this.request.sortBy = sortBy;
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

        public CustomizedPathsRequest build() {
            this.request.sources = this.sourcesBuilder.build();
            for (Step.Builder builder : this.stepBuilders) {
                this.request.steps.add(builder.build());
            }
            E.checkArgument(this.request.sources != null,
                            "Source vertices can't be null");
            E.checkArgument(this.request.steps != null &&
                            !this.request.steps.isEmpty(),
                            "Steps can't be null or empty");
            TraversersAPI.checkCapacity(this.request.capacity);
            TraversersAPI.checkLimit(this.request.limit);
            return this.request;
        }
    }

    public static class Step {

        @JsonProperty("direction")
        private String direction;
        @JsonProperty("labels")
        private List<String> labels;
        @JsonProperty("properties")
        private Map<String, Object> properties;
        @JsonProperty("weight_by")
        private String weightBy;
        @JsonProperty("default_weight")
        private double defaultWeight;
        @JsonProperty("degree")
        private long degree;
        @JsonProperty("sample")
        private long sample;

        private Step() {
            this.direction = null;
            this.labels = new ArrayList<>();
            this.properties = new HashMap<>();
            this.weightBy = null;
            this.defaultWeight = Traverser.DEFAULT_WEIGHT;
            this.degree = Traverser.DEFAULT_MAX_DEGREE;
            this.sample = Traverser.DEFAULT_SAMPLE;
        }

        @Override
        public String toString() {
            return String.format("Step{direction=%s,labels=%s,properties=%s," +
                                 "weightBy=%s,defaultWeight=%s,degree=%s," +
                                 "sample=%s}", this.direction, this.labels,
                                 this.properties, this.weightBy,
                                 this.defaultWeight, this.degree, this.sample);
        }

        public static class Builder {

            private Step step;

            private Builder() {
                this.step = new Step();
            }

            public Builder direction(Direction direction) {
                this.step.direction = direction.toString();
                return this;
            }

            public Builder labels(List<String> labels) {
                this.step.labels.addAll(labels);
                return this;
            }

            public Builder labels(String... labels) {
                this.step.labels.addAll(Arrays.asList(labels));
                return this;
            }

            public Builder properties(Map<String, Object> properties) {
                this.step.properties = properties;
                return this;
            }

            public Builder properties(String key, Object value) {
                this.step.properties.put(key, value);
                return this;
            }

            public Builder weightBy(String property) {
                this.step.weightBy = property;
                return this;
            }

            public Builder defaultWeight(double weight) {
                this.step.defaultWeight = weight;
                return this;
            }

            public Builder degree(long degree) {
                TraversersAPI.checkDegree(degree);
                this.step.degree = degree;
                return this;
            }

            public Builder sample(int sample) {
                E.checkArgument(sample == API.NO_LIMIT || sample > 0,
                                "The sample must be > 0 or == -1, but got: %s",
                                sample);
                this.step.sample = sample;
                return this;
            }

            private Step build() {
                TraversersAPI.checkDegree(this.step.degree);
                E.checkArgument(this.step.sample > 0 ||
                                this.step.sample == API.NO_LIMIT,
                                "The sample must be > 0 or == -1, but got: %s",
                                this.step.sample);
                E.checkArgument(this.step.degree == API.NO_LIMIT ||
                                this.step.degree >= this.step.sample,
                                "Degree must be greater than or equal to " +
                                "sample, but got degree %s and sample %s",
                                this.step.degree, this.step.sample);
                return this.step;
            }
        }
    }

    public enum SortBy {
        INCR,
        DECR,
        NONE
    }
}
