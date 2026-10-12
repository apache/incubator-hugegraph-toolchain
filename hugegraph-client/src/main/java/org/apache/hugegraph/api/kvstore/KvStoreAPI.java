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

package org.apache.hugegraph.api.kvstore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.graph.Shard;
import org.apache.hugegraph.structure.graph.Vertices;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableMap;

public class KvStoreAPI extends API {

    private static final String PATH = "kvstore";
    private static String batchPath = "";

    public KvStoreAPI(RestClient client) {
        super(client);
        this.path(PATH);
        batchPath = String.join("/", this.path(), "batch");
    }

    @Override
    protected String type() {
        return "kvstore";
    }

    public List<Shard> shards(long splitSize) {
        String path = String.join(PATH_SPLITOR, this.path(), "shards");
        Map<String, Object> params = ImmutableMap.of("split_size", splitSize);
        RestResult result = this.client.get(path, params);
        return result.readList("shards", Shard.class);
    }

    public Vertices scan(Shard shard, String page, long pageLimit) {
        E.checkArgument(shard != null, "Shard can't be null");
        String path = String.join(PATH_SPLITOR, this.path(), "scan");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start", shard.start());
        params.put("end", shard.end());
        params.put("page", page);
        params.put("page_limit", pageLimit);
        RestResult result = this.client.get(path, params);
        return result.readObject(Vertices.class);
    }

    public Map<String, Object> setBatch(Map<String, Object> data) {
        RestResult result = this.client.post(batchPath, data);
        return result.readObject(Map.class);
    }
}
