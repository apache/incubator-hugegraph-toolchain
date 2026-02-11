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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphCloneEntity {
    @JsonProperty("graphspace")
    public String graphSpace;
    @JsonProperty("name")
    public String name;
    @JsonProperty("nickname")
    public String nickname;
    @JsonProperty("load_data")
    public int loadData = 0;

    public Map<String, Object> convertMap(String graphSpace, String graph) {
        Map<String, Object> params = new HashMap<>(4);

        String name = (this.name == null) ? graph : this.name;
        String nickname = (this.nickname == null) ? graph : this.nickname;
        String space = (this.graphSpace == null) ? graphSpace : this.graphSpace;

        Map<String, Object> configs = new HashMap<>();
        configs.put("backend", "hstore");
        configs.put("serializer", "binary");
        configs.put("name", name);
        configs.put("nickname", nickname);
        configs.put("search.text_analyzer", "jieba");
        configs.put("search.text_analyzer_mode", "INDEX");

        params.put("configs", configs);
        params.put("create", true);
        params.put("init_schema", true);
        params.put("graphspace", space);
        params.put("load_data", (boolean) (this.loadData == 1));
        return params;
    }
}
