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
import java.util.Map;

import org.apache.hugegraph.api.graph.GraphAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.traverser.WeightedPaths;
import org.apache.hugegraph.util.E;

public class SingleSourceShortestPathAPI extends TraversersAPI {

    public SingleSourceShortestPathAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "singlesourceshortestpath";
    }

    public WeightedPaths get(Object sourceId, Direction direction, String label,
                             String weight, long degree, long skipDegree,
                             long capacity, int limit, boolean withVertex) {
        this.client.checkApiVersion("0.51", "single source shortest path");
        String source = GraphAPI.formatVertexId(sourceId, false);

        E.checkNotNull(weight, "weight");
        checkDegree(degree);
        checkCapacity(capacity);
        checkSkipDegree(skipDegree, degree, capacity);
        checkLimit(limit);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("source", source);
        params.put("direction", direction);
        params.put("label", label);
        params.put("weight", weight);
        params.put("max_degree", degree);
        params.put("skip_degree", skipDegree);
        params.put("capacity", capacity);
        params.put("limit", limit);
        params.put("with_vertex", withVertex);
        RestResult result = this.client.get(this.path(), params);
        return result.readObject(WeightedPaths.class);
    }
}
