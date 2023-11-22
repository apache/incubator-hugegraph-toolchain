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

package org.apache.hugegraph.api.traverser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.structure.traverser.Ranks;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NeighborRankAPI extends TraversersAPI {

    public NeighborRankAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "neighborrank";
    }

    public List<Ranks> post(Request request) {
        RestResult result = this.client.post(this.path(), request);
        return result.readList("ranks", Ranks.class);
    }

    public static class Request {

        @JsonProperty("source")
        private Object source;
        @JsonProperty("steps")
        private List<Step> steps;
        @JsonProperty("alpha")
        private double alpha;
        @JsonProperty("capacity")
        private long capacity;

        private Request() {
            this.source = null;
            this.steps = new ArrayList<>();
            this.alpha = Traverser.DEFAULT_ALPHA;
            this.capacity = Traverser.DEFAULT_CAPACITY;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return String.format("Request{source=%s,steps=%s,alpha=%s," +
                                 "capacity=%s}", this.source, this.steps,
                                 this.alpha, this.capacity);
        }

        public static class Builder {

            private Request request;
            private List<Step.Builder> stepBuilders;

            private Builder() {
                this.request = new Request();
                this.stepBuilders = new ArrayList<>();
            }

            public Builder source(Object source) {
                E.checkArgument(source != null,
                                "The label of request for neighbor rank can't be null");
                this.request.source = source;
                return this;
            }

            public Step.Builder steps() {
                Step.Builder builder = new Step.Builder();
                this.stepBuilders.add(builder);
                return builder;
            }

            public Builder alpha(double alpha) {
                TraversersAPI.checkAlpha(alpha);
                this.request.alpha = alpha;
                return this;
            }

            public Builder capacity(long capacity) {
                TraversersAPI.checkCapacity(capacity);
                this.request.capacity = capacity;
                return this;
            }

            public Request build() {
                for (Step.Builder builder : this.stepBuilders) {
                    this.request.steps.add(builder.build());
                }
                E.checkArgument(this.request.source != null,
                                "Source vertex can't be null");
                E.checkArgument(this.request.steps != null &&
                                !this.request.steps.isEmpty(),
                                "Steps can't be null or empty");
                TraversersAPI.checkCapacity(this.request.capacity);
                TraversersAPI.checkAlpha(this.request.alpha);
                return this.request;
            }
        }

        public static class Step {

            @JsonProperty("direction")
            private String direction;
            @JsonProperty("labels")
            private List<String> labels;
            @JsonProperty("degree")
            private long degree;
            @JsonProperty("top")
            private int top;

            private Step() {
                this.direction = null;
                this.labels = new ArrayList<>();
                this.degree = Traverser.DEFAULT_MAX_DEGREE;
                this.top = (int) Traverser.DEFAULT_PATHS_LIMIT;
            }

            @Override
            public String toString() {
                return String.format("Step{direction=%s,labels=%s,degree=%s," +
                                     "top=%s}", this.direction, this.labels,
                                     this.degree, this.top);
            }

            public static class Builder {

                private Step step;

                private Builder() {
                    this.step = new Step();
                }

                public Step.Builder direction(Direction direction) {
                    this.step.direction = direction.toString();
                    return this;
                }

                public Step.Builder labels(List<String> labels) {
                    this.step.labels.addAll(labels);
                    return this;
                }

                public Step.Builder labels(String... labels) {
                    this.step.labels.addAll(Arrays.asList(labels));
                    return this;
                }

                public Step.Builder degree(long degree) {
                    TraversersAPI.checkDegree(degree);
                    this.step.degree = degree;
                    return this;
                }

                public Step.Builder top(int top) {
                    E.checkArgument(top > 0 && top <= Traverser.DEFAULT_MAX_TOP,
                                    "The top of each layer can't exceed %s",
                                    Traverser.DEFAULT_MAX_TOP);
                    this.step.top = top;
                    return this;
                }

                private Step build() {
                    TraversersAPI.checkDegree(this.step.degree);
                    E.checkArgument(this.step.top > 0 &&
                                    this.step.top <= Traverser.DEFAULT_MAX_TOP,
                                    "The top of each layer can't exceed %s",
                                    Traverser.DEFAULT_MAX_TOP);
                    return this.step;
                }
            }
        }
    }
}
