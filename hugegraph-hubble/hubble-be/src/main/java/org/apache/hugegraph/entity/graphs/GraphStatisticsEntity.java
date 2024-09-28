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

package org.apache.hugegraph.entity.graphs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphStatisticsEntity {
    @JsonProperty("storage")
    public long storage;
    @JsonProperty("vertex_count")
    public String vertexCount;
    @JsonProperty("edge_count")
    public String edgeCount;
    @JsonProperty("update_time")
    public String updateTime;
    @JsonProperty("vertices")
    public Map<String, Object> vertices;
    @JsonProperty("edges")
    public Map<String, Object> edges;

    public static GraphStatisticsEntity emptyEntity() {
        GraphStatisticsEntity empty = new GraphStatisticsEntity();
        empty.setStorage(0);
        empty.setVertexCount("0");
        empty.setEdgeCount("0");
        empty.setVertices(ImmutableMap.of());
        empty.setEdges(ImmutableMap.of());
        empty.setUpdateTime("1970-01-01 00:00:00.000");
        return empty;
    }
}
