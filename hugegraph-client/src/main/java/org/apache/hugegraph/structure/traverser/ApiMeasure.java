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

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiMeasure {

    @JsonProperty("edge_iterations")
    private Long edgeIters;
    @JsonProperty("vertice_iterations")
    private Long verticeIters;
    @JsonProperty("cost(ns)")
    private Long totalTime;

    public Long edgeIters() {
        return this.edgeIters;
    }

    public Long verticeIters() {
        return this.verticeIters;
    }

    public Long totalTime() {
        return this.totalTime;
    }
}
