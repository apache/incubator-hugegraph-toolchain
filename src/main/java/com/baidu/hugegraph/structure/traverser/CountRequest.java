/*
 * Copyright 2017 HugeGraph Authors
 *
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

package com.baidu.hugegraph.structure.traverser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.api.traverser.TraversersAPI;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.constant.Traverser;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CountRequest {

    @JsonProperty("source")
    private Object source;
    @JsonProperty("steps")
    private List<Step> steps;
    @JsonProperty("contains_traversed")
    public boolean containsTraversed;
    @JsonProperty("dedup_size")
    public long dedupSize;

    private CountRequest() {
        this.source = null;
        this.steps = new ArrayList<>();
        this.containsTraversed = false;
        this.dedupSize = Traverser.DEFAULT_DEDUP_SIZE;
    }

    @Override
    public String toString() {
        return String.format("CountRequest{source=%s,steps=%s," +
                             "containsTraversed=%s,dedupSize=%s",
                             this.source, this.steps, this.containsTraversed,
                             this.dedupSize);
    }

    public static class Builder {

        private CountRequest request;
        private List<Step.Builder> stepBuilders;

        public Builder() {
            this.request = new CountRequest();
            this.stepBuilders = new ArrayList<>();
        }

        public Builder source(Object source) {
            E.checkArgumentNotNull(source, "The source can't be null");
            this.request.source = source;
            return this;
        }

        public Step.Builder steps() {
            Step.Builder builder = new Step.Builder();
            this.stepBuilders.add(builder);
            return builder;
        }

        public Builder containsTraversed(boolean containsTraversed) {
            this.request.containsTraversed = containsTraversed;
            return this;
        }

        public Builder dedupSize(long dedupSize) {
            checkDedupSize(dedupSize);
            this.request.dedupSize = dedupSize;
            return this;
        }

        public CountRequest build() {
            E.checkArgumentNotNull(this.request.source,
                                   "The source can't be null");
            for (Step.Builder builder : this.stepBuilders) {
                this.request.steps.add(builder.build());
            }
            E.checkArgument(this.request.steps != null &&
                            !this.request.steps.isEmpty(),
                            "The steps can't be null or empty");
            checkDedupSize(this.request.dedupSize);
            return this.request;
        }
    }

    private static void checkDedupSize(long dedupSize) {
        E.checkArgument(dedupSize >= 0L || dedupSize == API.NO_LIMIT,
                        "The dedup size must be >= 0 or == %s, but got: %s",
                        API.NO_LIMIT, dedupSize);
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
        @JsonProperty("skip_degree")
        private long skipDegree;

        private Step() {
            this.direction = "BOTH";
            this.labels = new ArrayList<>();
            this.properties = new HashMap<>();
            this.degree = Traverser.DEFAULT_DEGREE;
            this.skipDegree = Traverser.DEFAULT_SKIP_DEGREE;
        }

        @Override
        public String toString() {
            return String.format("Step{direction=%s,labels=%s,properties=%s," +
                                 "degree=%s,skipDegree=%s}",
                                 this.direction, this.labels, this.properties,
                                 this.degree, this.skipDegree);
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

            public Builder skipDegree(long skipDegree) {
                TraversersAPI.checkSkipDegree(skipDegree, this.step.degree,
                                              API.NO_LIMIT);
                this.step.skipDegree = skipDegree;
                return this;
            }

            private Step build() {
                TraversersAPI.checkDegree(this.step.degree);
                TraversersAPI.checkSkipDegree(this.step.skipDegree,
                                              this.step.degree, API.NO_LIMIT);
                return this.step;
            }
        }
    }
}
