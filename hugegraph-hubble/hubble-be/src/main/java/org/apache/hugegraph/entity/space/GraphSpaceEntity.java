/*
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

package org.apache.hugegraph.entity.space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.structure.space.GraphSpace;
import org.apache.hugegraph.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphSpaceEntity extends GraphSpace {
    @JsonIgnore
    private boolean computeCpuLimitProvided;
    @JsonIgnore
    private boolean computeMemoryLimitProvided;

    @JsonProperty("graphspace_admin")
    public List<String> graphspaceAdmin = new ArrayList<>();

    @JsonProperty("statistic")
    public Map<String, Object> statistic = new HashMap<>();

    public GraphSpaceEntity() {
    }

    @Override
    @JsonProperty("compute_cpu_limit")
    public void setComputeCpuLimit(int value) {
        super.setComputeCpuLimit(value);
        this.computeCpuLimitProvided = true;
    }

    @Override
    @JsonProperty("compute_memory_limit")
    public void setComputeMemoryLimit(int value) {
        super.setComputeMemoryLimit(value);
        this.computeMemoryLimitProvided = true;
    }

    @Override
    public Object convertReq() {
        // Preserve omission so Server can apply update semantics atomically.
        Map<?, ?> request = JsonUtil.fromJson(JsonUtil.toJson(super.convertReq()), Map.class);
        if (!this.computeCpuLimitProvided) {
            request.remove("compute_cpu_limit");
        }
        if (!this.computeMemoryLimitProvided) {
            request.remove("compute_memory_limit");
        }
        return request;
    }

    public static GraphSpaceEntity fromGraphSpace(GraphSpace graphSpace) {
        return JsonUtil.fromJson(JsonUtil.toJson(graphSpace), GraphSpaceEntity.class);
    }

    public GraphSpace convertGraphSpace() {
        // Generate GraphSpace instance From GraphSpaceEntity
        return this;
    }

    public Map<String, Object> getStatistic() {
        return statistic;
    }

    public void setStatistic(Map<String, Object> statistic) {
        this.statistic = statistic;
    }
}
