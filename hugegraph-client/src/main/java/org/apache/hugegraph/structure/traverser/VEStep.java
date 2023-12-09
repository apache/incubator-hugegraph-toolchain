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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VEStep {

    @JsonProperty("label")
    public String label;

    @JsonProperty("properties")
    public Map<String, Object> properties;

    protected VEStep() {
        this.properties = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("VEStepEntity{label=%s,properties=%s}",
                             this.label, this.properties);
    }

    public static class Builder {

        protected VEStep veStep;

        private Builder() {
            this.veStep = new VEStep();
        }

        public VEStep.Builder label(String label) {
            this.veStep.label = label;
            return this;
        }

        public VEStep.Builder properties(Map<String, Object> properties) {
            this.veStep.properties = properties;
            return this;
        }

        public VEStep.Builder properties(String key, Object value) {
            this.veStep.properties.put(key, value);
            return this;
        }

        public VEStep build() {
            return this.veStep;
        }

    }
}
