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

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.graph.GraphAPI;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.traverser.Prediction;
import org.apache.hugegraph.structure.traverser.SingleSourceJaccardSimilarityRequest;
import org.apache.hugegraph.util.E;

public class AdamicAdarAPI extends TraversersAPI {

    private static final String AA = "adamic_adar";

    public AdamicAdarAPI(RestClient client, String graphSpace, String graph) {
        super(client, graphSpace, graph);
    }

    @Override
    protected String type() {
        return "adamicadar";
    }

    public Prediction get(Object vertexId, Object otherId, Direction direction,
                          String label, long degree) {
        this.client.checkApiVersion("0.67", AA);
        String vertex = GraphAPI.formatVertexId(vertexId, false);
        String other = GraphAPI.formatVertexId(otherId, false);
        checkDegree(degree);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("vertex", vertex);
        params.put("other", other);
        params.put("direction", direction);
        params.put("label", label);
        params.put("max_degree", degree);
        RestResult result = this.client.get(this.path(), params);
        @SuppressWarnings("unchecked")
        Prediction res = result.readObject(Prediction.class);
        E.checkState(res.getAdamicAdar() != null,
                     "The result doesn't have key '%s'", AA);
        return res;
    }

    /**
     * 20221122 Guangxu Zhang
     * No corresponding interface found in server
     */
    @SuppressWarnings("unchecked")
    public Map<Object, Double> post(SingleSourceJaccardSimilarityRequest
                                    request) {
        this.client.checkApiVersion("0.67", AA);
        RestResult result = this.client.post(this.path(), request);
        return result.readObject(Map.class);
    }
}
