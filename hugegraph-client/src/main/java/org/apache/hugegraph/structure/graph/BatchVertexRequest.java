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

package org.apache.hugegraph.structure.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BatchVertexRequest {

    @JsonProperty("vertices")
    private List<Vertex> vertices;
    @JsonProperty("update_strategies")
    private Map<String, UpdateStrategy> updateStrategies;
    @JsonProperty("create_if_not_exist")
    private boolean createIfNotExist;

    public BatchVertexRequest() {
        this.vertices = null;
        this.updateStrategies = null;
        this.createIfNotExist = true;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("BatchVertexRequest{vertices=%s," +
                             "updateStrategies=%s,createIfNotExist=%s}",
                             this.vertices, this.updateStrategies,
                             this.createIfNotExist);
    }

    public static class Builder {

        private BatchVertexRequest req;

        public Builder() {
            this.req = new BatchVertexRequest();
        }

        public Builder vertices(List<Vertex> vertices) {
            this.req.vertices = vertices;
            return this;
        }

        public Builder updatingStrategies(Map<String, UpdateStrategy> map) {
            this.req.updateStrategies = new HashMap<>(map);
            return this;
        }

        public Builder updatingStrategy(String property,
                                        UpdateStrategy strategy) {
            this.req.updateStrategies.put(property, strategy);
            return this;
        }

        public Builder createIfNotExist(boolean createIfNotExist) {
            this.req.createIfNotExist = createIfNotExist;
            return this;
        }

        public BatchVertexRequest build() {
            E.checkArgumentNotNull(req, "BatchVertexRequest can't be null");
            E.checkArgumentNotNull(req.vertices,
                                   "Parameter 'vertices' can't be null");
            E.checkArgument(req.updateStrategies != null &&
                            !req.updateStrategies.isEmpty(),
                            "Parameter 'update_strategies' can't be empty");
            E.checkArgument(req.createIfNotExist,
                            "Parameter 'create_if_not_exist' " +
                            "dose not support false now");
            return this.req;
        }
    }
}
