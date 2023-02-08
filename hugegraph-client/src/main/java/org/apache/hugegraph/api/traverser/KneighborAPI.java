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
import org.apache.hugegraph.structure.traverser.Kneighbor;
import org.apache.hugegraph.structure.traverser.KneighborRequest;

public class KneighborAPI extends TraversersAPI {

    public KneighborAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "kneighbor";
    }

    public List<Object> get(Object sourceId, Direction direction,
                            String label, int depth, long degree, int limit) {
        String source = GraphAPI.formatVertexId(sourceId, false);

        checkPositive(depth, "Depth of k-neighbor");
        checkDegree(degree);
        checkLimit(limit);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("source", source);
        params.put("direction", direction);
        params.put("label", label);
        params.put("max_depth", depth);
        params.put("max_degree", degree);
        params.put("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList("vertices", Object.class);
    }

    public Kneighbor post(KneighborRequest request) {
        this.client.checkApiVersion("0.58", "customized kneighbor");
        RestResult result = this.client.post(this.path(), request);
        return result.readObject(Kneighbor.class);
    }
}

