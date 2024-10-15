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

package org.apache.hugegraph.api.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.client.api.API;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.List;
import java.util.Map;

public class GraphSketchAPI extends API {
    private static final String GRAPH_SKETCH_PATH = "graphspaces/%s/graphs/%s/sketch";

    public GraphSketchAPI(RestClient client, String graphSpace, String graph) {
        super(client);
        this.path(GRAPH_SKETCH_PATH, graphSpace, graph);
    }

    @Override
    protected String type() {
        return HugeType.GRAPHS.string();
    }

    public Object getSketch() {
        return this.client.get(this.path()).readObject(Map.class);
    }

    public Object createSketchTask(JsonSketchTask sketchTask) {
        return this.client.post(this.path(), sketchTask).readObject(Map.class);
    }

    @NoArgsConstructor
    @Data
    @AllArgsConstructor
    @Builder
    public static class JsonSketchTask {
        @JsonProperty("username")
        public String username;
        @JsonProperty("password")
        public String password;
        @JsonProperty("algorithms")
        public List<String> algorithms;

        @JsonProperty("afsUri")
        public String afsUri;
        @JsonProperty("afsDir")
        public String afsDir;

        @JsonProperty("afsUser")
        public String afsUser;
        @JsonProperty("afsPassword")
        public String afsPassword;
    }
}
