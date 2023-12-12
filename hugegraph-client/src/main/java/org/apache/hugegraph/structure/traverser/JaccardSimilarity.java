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

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JaccardSimilarity {

    @JsonProperty("jaccard_similarity")
    private Map<Object, Double> similarsMap;
    @JsonProperty
    private ApiMeasure measure;

    public Map<Object, Double> similarsMap() {
        return this.similarsMap;
    }

    public ApiMeasure measure() {
        return this.measure;
    }

    public int size() {
        return this.similarsMap.size();
    }

    public Set<Object> keySet() {
        return this.similarsMap.keySet();
    }

    public Double get(Object key) {
        return this.similarsMap.get(key);
    }
}
