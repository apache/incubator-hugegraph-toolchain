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

package org.apache.hugegraph.api.traverser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Edges;
import org.apache.hugegraph.structure.graph.Shard;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableMap;

public class EdgesAPI extends TraversersAPI {

    public EdgesAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "edges";
    }

    public List<Edge> list(List<String> ids) {
        E.checkArgument(ids != null && !ids.isEmpty(),
                        "Ids can't be null or empty");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ids", ids);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), Edge.class);
    }

    public List<Shard> shards(long splitSize) {
        String path = String.join(PATH_SPLITOR, this.path(), "shards");
        Map<String, Object> params = ImmutableMap.of("split_size", splitSize);
        RestResult result = this.client.get(path, params);
        return result.readList("shards", Shard.class);
    }

    public Edges scan(Shard shard, String page, long pageLimit) {
        E.checkArgument(shard != null, "Shard can't be null");
        String path = String.join(PATH_SPLITOR, this.path(), "scan");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start", shard.start());
        params.put("end", shard.end());
        params.put("page", page);
        params.put("page_limit", pageLimit);
        RestResult result = this.client.get(path, params);
        return result.readObject(Edges.class);
    }
}

