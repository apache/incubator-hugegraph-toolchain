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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RanksWithMeasure {

    @JsonProperty("personal_rank")
    private Ranks personalRank;
    @JsonProperty
    private List<Ranks> ranks;
    @JsonProperty
    private Map<String, Object> measure;

    public RanksWithMeasure() {
    }

    public Ranks personalRanks() {
        return this.personalRank;
    }

    public List<Ranks> ranks() {
        return this.ranks;
    }

    public Map<String, Object> measure() {
        return this.measure;
    }

    public void setPersonalRank(Ranks personalRank) {
        if (this.personalRank == null) {
            this.personalRank = personalRank;
        }
    }
}
