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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hugegraph.structure.graph.Path;

import java.util.List;
import java.util.Map;

public class PathWithMeasure {

    // AllShortestPathsAPI
    @JsonProperty("paths")
    private List<Path> paths;
    @JsonProperty("crosspoints")
    private List<Path> crosspoints;
    @JsonProperty("rays")
    private List<Path> rays;
    @JsonProperty("rings")
    private List<Path> rings;
    @JsonProperty
    private Map<String, Object> measure;

    public List<Path> getPaths() {
        return this.paths;
    }

    public List<Path> getCrosspoints() {
        return this.crosspoints;
    }

    public List<Path> getRays() {
        return this.rays;
    }

    public List<Path> getRings() {
        return this.rings;
    }

    public Map<String, Object> measure() {
        return this.measure;
    }
}
