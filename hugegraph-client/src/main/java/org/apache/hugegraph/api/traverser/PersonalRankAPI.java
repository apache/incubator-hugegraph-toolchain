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

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Traverser;
import org.apache.hugegraph.structure.traverser.Ranks;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersonalRankAPI extends TraversersAPI {

    public PersonalRankAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "personalrank";
    }

    public Ranks post(Request request) {
        RestResult result = this.client.post(this.path(), request);
        return result.readObject(Ranks.class);
    }

    public static class Request {

        @JsonProperty("source")
        private Object source;
        @JsonProperty("label")
        private String label;
        @JsonProperty("alpha")
        private double alpha = Traverser.DEFAULT_ALPHA;
        @JsonProperty("max_degree")
        public long degree = Traverser.DEFAULT_MAX_DEGREE;
        @JsonProperty("limit")
        private int limit = Traverser.DEFAULT_LIMIT;
        @JsonProperty("max_depth")
        private int maxDepth = 5;
        @JsonProperty("with_label")
        private WithLabel withLabel = WithLabel.BOTH_LABEL;
        @JsonProperty("sorted")
        private boolean sorted = true;

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return String.format("Request{source=%s,label=%s,alpha=%s," +
                                 "degree=%s,limit=%s,maxDepth=%s," +
                                 "withLabel=%s,sorted=%s}",
                                 this.source, this.label, this.alpha,
                                 this.degree, this.limit, this.maxDepth,
                                 this.withLabel, this.sorted);
        }

        public enum WithLabel {
            SAME_LABEL,
            OTHER_LABEL,
            BOTH_LABEL
        }

        public static class Builder {

            private Request request;

            private Builder() {
                this.request = new Request();
            }

            public Builder source(Object source) {
                E.checkArgument(source != null,
                                "The source of request for personal rank can't be null");
                this.request.source = source;
                return this;
            }

            public Builder label(String label) {
                E.checkArgument(label != null,
                                "The label of request for personal rank can't be null");
                this.request.label = label;
                return this;
            }

            public Builder alpha(double alpha) {
                TraversersAPI.checkAlpha(alpha);
                this.request.alpha = alpha;
                return this;
            }

            public Builder degree(long degree) {
                TraversersAPI.checkDegree(degree);
                this.request.degree = degree;
                return this;
            }

            public Builder limit(int limit) {
                TraversersAPI.checkLimit(limit);
                this.request.limit = limit;
                return this;
            }

            public Builder maxDepth(int maxDepth) {
                E.checkArgument(maxDepth > 0 &&
                                maxDepth <= Traverser.DEFAULT_MAX_DEPTH,
                                "The max depth must be in range (0, %s], but got: %s",
                                Traverser.DEFAULT_MAX_DEPTH, maxDepth);
                this.request.maxDepth = maxDepth;
                return this;
            }

            public Builder withLabel(WithLabel withLabel) {
                this.request.withLabel = withLabel;
                return this;
            }

            public Builder sorted(boolean sorted) {
                this.request.sorted = sorted;
                return this;
            }

            public Request build() {
                E.checkArgument(this.request.source != null,
                                "Source vertex can't be null");
                E.checkArgument(this.request.label != null,
                                "The label of rank request " +
                                "for personal rank can't be null");
                TraversersAPI.checkAlpha(this.request.alpha);
                TraversersAPI.checkDegree(this.request.degree);
                TraversersAPI.checkLimit(this.request.limit);
                E.checkArgument(this.request.maxDepth > 0 &&
                                this.request.maxDepth <= Traverser.DEFAULT_MAX_DEPTH,
                                "The max depth must be in range (0, %s], but got: %s",
                                Traverser.DEFAULT_MAX_DEPTH, this.request.maxDepth);
                return this.request;
            }
        }
    }
}
