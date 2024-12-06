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

package org.apache.hugegraph.structure.traverser;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Steps {

    @JsonProperty("direction")
    protected Direction direction;
    @JsonProperty("edge_steps")
    public List<StepEntity> eSteps;
    @JsonProperty("vertex_steps")
    public List<StepEntity> vSteps;
    @JsonProperty("max_degree")
    protected long maxDegree;
    @JsonProperty("skip_degree")
    protected long skipDegree;

    protected Steps() {
        this.direction = Direction.BOTH;
        this.eSteps = new ArrayList<>();
        this.vSteps = new ArrayList<>();
        this.maxDegree = Traverser.DEFAULT_MAX_DEGREE;
        this.skipDegree = Traverser.DEFAULT_SKIP_DEGREE;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("Steps{direction=%s,eSteps=%s,vSteps=%s," +
                             "maxDegree=%s,skipDegree=%s}",
                             this.direction, this.eSteps, this.vSteps,
                             this.maxDegree, this.skipDegree);
    }

    public static class Builder {

        protected Steps step;

        private Builder() {
            this.step = new Steps();
        }

        public Steps.Builder direction(Direction direction) {
            this.step.direction = direction;
            return this;
        }

        public Steps.Builder edgeSteps(List<StepEntity> eSteps) {
            this.step.eSteps = eSteps;
            return this;
        }

        public Steps.Builder edgeSteps(StepEntity stepEntity) {
            this.step.eSteps.add(stepEntity);
            return this;
        }

        public Steps.Builder vertexSteps(List<StepEntity> vSteps) {
            this.step.vSteps = vSteps;
            return this;
        }

        public Steps.Builder vertexSteps(StepEntity stepEntity) {
            this.step.vSteps.add(stepEntity);
            return this;
        }

        public Steps.Builder degree(long degree) {
            TraversersAPI.checkDegree(degree);
            this.step.maxDegree = degree;
            return this;
        }

        public Steps.Builder skipDegree(long skipDegree) {
            TraversersAPI.checkSkipDegree(skipDegree, this.step.maxDegree,
                                          API.NO_LIMIT);
            this.step.skipDegree = skipDegree;
            return this;
        }

        public Steps build() {
            TraversersAPI.checkDegree(this.step.maxDegree);
            TraversersAPI.checkSkipDegree(this.step.skipDegree,
                                          this.step.maxDegree, API.NO_LIMIT);
            return this.step;
        }
    }

    public static class StepEntity {
        @JsonProperty("label")
        public String label;

        @JsonProperty("properties")
        public Map<String, Object> properties;

        public StepEntity() {
        }

        public StepEntity(String label) {
            this.label = label;
        }

        public StepEntity(String label, Map<String, Object> properties) {
            this.label = label;
            this.properties = properties;
        }

        @Override
        public String toString() {
            return String.format("StepEntity{label=%s,properties=%s}",
                    this.label, this.properties);
        }
    }
}
