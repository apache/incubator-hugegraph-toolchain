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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VESteps {

    @JsonProperty("direction")
    public Direction direction;
    @JsonAlias("degree")
    @JsonProperty("max_degree")
    public long maxDegree;
    @JsonProperty("skip_degree")
    public long skipDegree;
    @JsonProperty("vertex_steps")
    public List<VEStep> vSteps;
    @JsonProperty("edge_steps")
    public List<VEStep> eSteps;

    protected VESteps() {
        this.direction = Direction.BOTH;
        this.maxDegree = Traverser.DEFAULT_MAX_DEGREE;
        this.skipDegree = Traverser.DEFAULT_SKIP_DEGREE;
        this.vSteps = new ArrayList<>();
        this.eSteps = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("Steps{direction=%s,maxDegree=%s," +
                             "skipDegree=%s,vSteps=%s,eSteps=%s}",
                             this.direction, this.maxDegree,
                             this.skipDegree, this.vSteps, this.eSteps);
    }

    public static class Builder {

        protected VESteps steps;

        private Builder() {
            this.steps = new VESteps();
        }

        public VESteps.Builder direction(Direction direction) {
            this.steps.direction = direction;
            return this;
        }

        public VESteps.Builder vSteps(List<VEStep> vSteps) {
            this.steps.vSteps = vSteps;
            return this;
        }

        public VESteps.Builder addVStep(String label, Map<String, Object> properties) {
            VEStep vStep = VEStep.builder()
                                 .label(label)
                                 .properties(properties)
                                 .build();
            this.steps.vSteps.add(vStep);
            return this;
        }

        public VESteps.Builder addVStep(String label) {
            return this.addVStep(label, Collections.emptyMap());
        }

        public VESteps.Builder eSteps(List<VEStep> eSteps) {
            this.steps.eSteps = eSteps;
            return this;
        }

        public VESteps.Builder addEStep(String label, Map<String, Object> properties) {
            VEStep eStep = VEStep.builder()
                                 .label(label)
                                 .properties(properties)
                                 .build();
            this.steps.eSteps.add(eStep);
            return this;
        }

        public VESteps.Builder addEStep(String label) {
            return this.addEStep(label, Collections.emptyMap());
        }

        public VESteps.Builder degree(long degree) {
            TraversersAPI.checkDegree(degree);
            this.steps.maxDegree = degree;
            return this;
        }

        public VESteps.Builder skipDegree(long skipDegree) {
            TraversersAPI.checkSkipDegree(skipDegree, this.steps.maxDegree,
                                          API.NO_LIMIT);
            this.steps.skipDegree = skipDegree;
            return this;
        }

        public VESteps build() {
            TraversersAPI.checkDegree(this.steps.maxDegree);
            TraversersAPI.checkSkipDegree(this.steps.skipDegree,
                                          this.steps.maxDegree, API.NO_LIMIT);
            return this.steps;
        }
    }
}
