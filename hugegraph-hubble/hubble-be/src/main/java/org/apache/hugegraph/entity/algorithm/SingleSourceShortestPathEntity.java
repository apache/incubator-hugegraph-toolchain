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

package org.apache.hugegraph.entity.algorithm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.Traverser;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleSourceShortestPathEntity {

    @JsonProperty("source")
    private Object source;

    @JsonProperty("direction")
    private Direction direction;

    @JsonProperty("weight")
    private String weight;

    @JsonProperty("label")
    private String label;

    @JsonProperty("max_degree")
    private long maxDegree = Traverser.DEFAULT_MAX_DEGREE;

    @JsonProperty("skip_degree")
    private long skipDegree;

    @JsonProperty("capacity")
    private long capacity = Traverser.DEFAULT_CAPACITY;

    @JsonProperty("with_vertex")
    private boolean withVertex;

    @JsonProperty("limit")
    private long limit = Traverser.DEFAULT_PATHS_LIMIT;
}
