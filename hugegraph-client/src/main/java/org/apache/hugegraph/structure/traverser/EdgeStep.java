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

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EdgeStep {

    @JsonProperty("direction")
    protected Direction direction;
    @JsonProperty("labels")
    protected List<String> labels;
    @JsonProperty("properties")
    protected Map<String, Object> properties;
    @JsonProperty("degree")
    protected long degree;
    @JsonProperty("skip_degree")
    protected long skipDegree;

    protected EdgeStep() {
        this.direction = Direction.BOTH;
        this.labels = new ArrayList<>();
        this.properties = new HashMap<>();
        this.degree = Traverser.DEFAULT_MAX_DEGREE;
        this.skipDegree = Traverser.DEFAULT_SKIP_DEGREE;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("EdgeStep{direction=%s,labels=%s,properties=%s," +
                             "degree=%s,skipDegree=%s}",
                             this.direction, this.labels, this.properties,
                             this.degree, this.skipDegree);
    }

    public static class Builder {

        protected EdgeStep step;

        private Builder() {
            this.step = new EdgeStep();
        }

        public EdgeStep.Builder direction(Direction direction) {
            this.step.direction = direction;
            return this;
        }

        public EdgeStep.Builder labels(List<String> labels) {
            this.step.labels = labels;
            return this;
        }

        public EdgeStep.Builder labels(String label) {
            this.step.labels.add(label);
            return this;
        }

        public EdgeStep.Builder properties(Map<String, Object> properties) {
            this.step.properties = properties;
            return this;
        }

        public EdgeStep.Builder properties(String key, Object value) {
            this.step.properties.put(key, value);
            return this;
        }

        public EdgeStep.Builder degree(long degree) {
            TraversersAPI.checkDegree(degree);
            this.step.degree = degree;
            return this;
        }

        public EdgeStep.Builder skipDegree(long skipDegree) {
            TraversersAPI.checkSkipDegree(skipDegree, this.step.degree,
                                          API.NO_LIMIT);
            this.step.skipDegree = skipDegree;
            return this;
        }

        public EdgeStep build() {
            TraversersAPI.checkDegree(this.step.degree);
            TraversersAPI.checkSkipDegree(this.step.skipDegree,
                                          this.step.degree, API.NO_LIMIT);
            return this.step;
        }
    }
}
