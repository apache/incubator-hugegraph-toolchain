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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrosspointsRequest {

    @JsonProperty("sources")
    private VerticesArgs sources;
    @JsonProperty("path_patterns")
    private List<PathPattern> pathPatterns;
    @JsonProperty("capacity")
    private long capacity;
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("with_path")
    private boolean withPath;
    @JsonProperty("with_vertex")
    private boolean withVertex;

    private CrosspointsRequest() {
        this.sources = null;
        this.pathPatterns = new ArrayList<>();
        this.capacity = Traverser.DEFAULT_CAPACITY;
        this.limit = Traverser.DEFAULT_CROSSPOINT_LIMIT;
        this.withPath = false;
        this.withVertex = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("CrosspointsRequest{sourceVertex=%s," +
                             "pathPatterns=%s,capacity=%s,limit=%s," +
                             "withPath=%s,withVertex=%s}",
                             this.sources, this.pathPatterns, this.capacity,
                             this.limit, this.withPath, this.withVertex);
    }

    public static class Builder {

        private CrosspointsRequest request;
        private VerticesArgs.Builder sourcesBuilder;
        private List<PathPattern.Builder> pathPatternBuilders;

        private Builder() {
            this.request = new CrosspointsRequest();
            this.sourcesBuilder = VerticesArgs.builder();
            this.pathPatternBuilders = new ArrayList<>();
        }

        public VerticesArgs.Builder sources() {
            return this.sourcesBuilder;
        }

        public PathPattern.Builder pathPatterns() {
            PathPattern.Builder builder = new PathPattern.Builder();
            this.pathPatternBuilders.add(builder);
            return builder;
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

        public Builder withPath(boolean withPath) {
            this.request.withPath = withPath;
            return this;
        }

        public Builder withVertex(boolean withVertex) {
            this.request.withVertex = withVertex;
            return this;
        }

        public CrosspointsRequest build() {
            this.request.sources = this.sourcesBuilder.build();
            for (PathPattern.Builder builder : this.pathPatternBuilders) {
                this.request.pathPatterns.add(builder.build());
            }
            E.checkArgument(this.request.sources != null,
                            "Source vertices can't be null");
            E.checkArgument(this.request.pathPatterns != null &&
                            !this.request.pathPatterns.isEmpty(),
                            "Steps can't be null or empty");
            TraversersAPI.checkCapacity(this.request.capacity);
            TraversersAPI.checkLimit(this.request.limit);
            return this.request;
        }
    }

    public static class PathPattern {

        @JsonProperty("steps")
        private List<Step> steps;

        private PathPattern() {
            this.steps = new ArrayList<>();
        }

        public static class Builder {

            private PathPattern pathPattern;
            private List<Step.Builder> stepBuilders;

            private Builder() {
                this.pathPattern = new PathPattern();
                this.stepBuilders = new ArrayList<>();
            }

            public Step.Builder steps() {
                Step.Builder builder = new Step.Builder();
                this.stepBuilders.add(builder);
                return builder;
            }

            private PathPattern build() {
                for (Step.Builder builder : this.stepBuilders) {
                    this.pathPattern.steps.add(builder.build());
                }
                E.checkArgument(this.pathPattern.steps != null &&
                                !this.pathPattern.steps.isEmpty(),
                                "Steps of path pattern can't be empty");
                return this.pathPattern;
            }
        }
    }

    public static class Step {

        @JsonProperty("direction")
        private String direction;
        @JsonProperty("labels")
        private List<String> labels;
        @JsonProperty("properties")
        private Map<String, Object> properties;
        @JsonProperty("degree")
        private long degree;

        private Step() {
            this.direction = null;
            this.labels = new ArrayList<>();
            this.properties = new HashMap<>();
            this.degree = Traverser.DEFAULT_MAX_DEGREE;
        }

        @Override
        public String toString() {
            return String.format("Step{direction=%s,labels=%s,properties=%s," +
                                 "degree=%s}", this.direction, this.labels,
                                 this.properties, this.degree);
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
                this.step.labels = labels;
                return this;
            }

            public Builder labels(String label) {
                this.step.labels.add(label);
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

            public Builder degree(long degree) {
                TraversersAPI.checkDegree(degree);
                this.step.degree = degree;
                return this;
            }

            private Step build() {
                TraversersAPI.checkDegree(this.step.degree);
                return this.step;
            }
        }
    }
}
