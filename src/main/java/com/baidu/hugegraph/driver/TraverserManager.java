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

package com.baidu.hugegraph.driver;

import java.util.List;

import com.baidu.hugegraph.api.traverser.KneighborAPI;
import com.baidu.hugegraph.api.traverser.KoutAPI;
import com.baidu.hugegraph.api.traverser.ShortestPathAPI;
import com.baidu.hugegraph.api.traverser.VerticesAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.graph.Vertex;

public class TraverserManager {

    private ShortestPathAPI shortestPathAPI;
    private KoutAPI koutAPI;
    private KneighborAPI kneighborAPI;
    private VerticesAPI verticesAPI;

    public TraverserManager(RestClient client, String graph) {
        this.shortestPathAPI = new ShortestPathAPI(client, graph);
        this.koutAPI = new KoutAPI(client, graph);
        this.kneighborAPI = new KneighborAPI(client, graph);
        this.verticesAPI = new VerticesAPI(client, graph);
    }

    public List<Object> shortestPath(Object sourceId, Object targetId,
                                     Direction direction, int maxDepth) {
        return this.shortestPathAPI.get(sourceId, targetId, direction,
                                        null, maxDepth);
    }

    public List<Object> shortestPath(Object sourceId, Object targetId,
                                     Direction direction, String label,
                                     int maxDepth) {
        return this.shortestPathAPI.get(sourceId, targetId, direction,
                                        label, maxDepth);
    }

    public List<Object> kout(Object sourceId, Direction direction, int depth) {
        return this.koutAPI.get(sourceId, direction, null, depth, true);
    }

    public List<Object> kout(Object sourceId, Direction direction,
                             String label, int depth, boolean nearest) {
        return this.koutAPI.get(sourceId, direction, label, depth, nearest);
    }

    public List<Object> kneighbor(Object sourceId, Direction direction,
                                  int depth) {
        return this.kneighborAPI.get(sourceId, direction, null, depth);
    }

    public List<Object> kneighbor(Object sourceId, Direction direction,
                                  String label, int depth) {
        return this.kneighborAPI.get(sourceId, direction, label, depth);
    }

    public List<Vertex> vertices(List<Object> ids) {
        return this.verticesAPI.get(ids);
    }
}
