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

package org.apache.hugegraph.api.traverser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.graph.GraphAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Direction;

public class SameNeighborsAPI extends TraversersAPI {

    private static final String SAME_NEIGHBORS = "same_neighbors";

    public SameNeighborsAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "sameneighbors";
    }

    public List<Object> get(Object vertexId, Object otherId,
                            Direction direction, String label,
                            long degree, int limit) {
        this.client.checkApiVersion("0.51", "same neighbors");
        String vertex = GraphAPI.formatVertexId(vertexId, false);
        String other = GraphAPI.formatVertexId(otherId, false);
        checkDegree(degree);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("vertex", vertex);
        params.put("other", other);
        params.put("direction", direction);
        params.put("label", label);
        params.put("max_degree", degree);
        params.put("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(SAME_NEIGHBORS, Object.class);
    }
}
