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
import java.util.List;

import org.apache.hugegraph.util.E;

import org.apache.hugegraph.api.traverser.TraversersAPI;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SameNeighborsBatchRequest {

    @JsonProperty("vertex_list")
    private List<List<Object>> vertexList;
    @JsonProperty("direction")
    private String direction;
    @JsonProperty("label")
    private String label;
    @JsonProperty("max_degree")
    public long maxDegree = Traverser.DEFAULT_MAX_DEGREE;
    @JsonProperty("limit")
    public long limit = Traverser.DEFAULT_ELEMENTS_LIMIT;

    private SameNeighborsBatchRequest() {
        this.vertexList = new ArrayList<>();
        this.direction = null;
        this.label = null;
        this.maxDegree = Traverser.DEFAULT_MAX_DEGREE;
        this.limit = Traverser.DEFAULT_ELEMENTS_LIMIT;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {

        return String.format("SameNeighborsBatchRequest{vertex_list=%s,direction=%s,label=%s," +
                             "max_degree=%d,limit=%d", this.vertexList, this.direction,
                             this.label, this.maxDegree, this.limit);
    }

    public static class Builder {

        private final SameNeighborsBatchRequest request;

        private Builder() {
            this.request = new SameNeighborsBatchRequest();
        }

        public Builder vertex(Object vertexId, Object otherId) {
            List<Object> vertexPair = new ArrayList<>();
            vertexPair.add(vertexId);
            vertexPair.add(otherId);
            this.request.vertexList.add(vertexPair);
            return this;
        }

        public Builder direction(Direction direction) {
            this.request.direction = direction.toString();
            return this;
        }

        public Builder label(String label) {
            this.request.label = label;
            return this;
        }

        public Builder maxDegree(long maxDegree) {
            TraversersAPI.checkDegree(maxDegree);
            this.request.maxDegree = maxDegree;
            return this;
        }

        public Builder limit(long limit) {
            TraversersAPI.checkLimit(limit);
            this.request.limit = limit;
            return this;
        }

        public SameNeighborsBatchRequest build() {
            E.checkArgument(this.request.vertexList != null &&
                            !this.request.vertexList.isEmpty(),
                            "The vertex_list can't be null or empty");
            TraversersAPI.checkDegree(this.request.maxDegree);
            TraversersAPI.checkLimit(this.request.limit);
            return this.request;
        }
    }
}
