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

package com.baidu.hugegraph.api.graph;

import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.graph.Edge;
import com.google.common.collect.ImmutableMap;


public class EdgeAPI extends GraphAPI {

    public EdgeAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.EDGE.string();
    }

    public Edge create(Edge edge) {
        RestResult result = this.client.post(path(), edge);
        return result.readObject(Edge.class);
    }

    public List<String> create(List<Edge> edges, boolean checkVertex) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Encoding", BATCH_ENCODING);
        RestResult result = this.client.post(batchPath(), edges, headers,
                ImmutableMap.of("checkVertex", checkVertex));
        return result.readList(String.class);
    }

    public Edge get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(Edge.class);
    }

    public List<Edge> list(int limit) {
        RestResult result = this.client.get(path(),
                ImmutableMap.of("limit", limit));
        return result.readList(type(), Edge.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
