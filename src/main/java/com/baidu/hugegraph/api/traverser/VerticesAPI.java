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

package com.baidu.hugegraph.api.traverser;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.api.graph.GraphAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.graph.Shard;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.ImmutableMap;

public class VerticesAPI extends TraversersAPI {

    public VerticesAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "vertices";
    }

    public List<Vertex> list(List<Object> ids) {
        E.checkArgument(ids != null && !ids.isEmpty(),
                        "Ids can't be null or empty");

        List<String> stringIds = new ArrayList<>(ids.size());
        for (Object id : ids) {
            stringIds.add(GraphAPI.formatVertexId(id, false));
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ids", stringIds);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), Vertex.class);
    }

    public List<Shard> shards(long splitSize) {
        String path = Paths.get(this.path(), "shards").toString();
        Map<String, Object> params = ImmutableMap.of("split_size", splitSize);
        RestResult result = this.client.get(path, params);
        return result.readList("shards", Shard.class);
    }

    public List<Vertex> scan(Shard shard) {
        String path = Paths.get(this.path(), "scan").toString();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start", shard.start());
        params.put("end", shard.end());
        RestResult result = this.client.get(path, params);
        return result.readList(this.type(), Vertex.class);
    }
}

