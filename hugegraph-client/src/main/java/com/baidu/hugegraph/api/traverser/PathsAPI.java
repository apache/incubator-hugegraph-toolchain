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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.api.graph.GraphAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.traverser.PathsRequest;
import com.baidu.hugegraph.structure.traverser.PathsWithVertices;

public class PathsAPI extends TraversersAPI {

    public PathsAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "paths";
    }

    public List<Path> get(Object sourceId, Object targetId,
                          Direction direction, String label,
                          int maxDepth, long degree, long capacity,
                          long limit) {
        String source = GraphAPI.formatVertexId(sourceId, false);
        String target = GraphAPI.formatVertexId(targetId, false);

        checkPositive(maxDepth, "Max depth of path");
        checkDegree(degree);
        checkCapacity(capacity);
        checkLimit(limit);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("source", source);
        params.put("target", target);
        params.put("direction", direction);
        params.put("label", label);
        params.put("max_depth", maxDepth);
        params.put("max_degree", degree);
        params.put("capacity", capacity);
        params.put("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList("paths", Path.class);
    }

    public PathsWithVertices post(PathsRequest request) {
        this.client.checkApiVersion("0.58", "paths with property filter");
        RestResult result = this.client.post(this.path(), request);
        return result.readObject(PathsWithVertices.class);
    }
}
