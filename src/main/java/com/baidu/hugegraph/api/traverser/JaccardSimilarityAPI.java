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
import java.util.Map;

import com.baidu.hugegraph.api.graph.GraphAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.util.E;

public class JaccardSimilarityAPI extends TraversersAPI {

    private static final String JACCARD_SIMILARITY = "jaccard_similarity";

    public JaccardSimilarityAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "jaccardsimilarity";
    }

    public double get(Object vertexId, Object otherId, Direction direction,
                      String label, long degree) {
        this.client.checkApiVersion("0.51", "jaccard similarity");
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
        Map<String, Double> jaccard = result.readObject(Map.class);
        E.checkState(jaccard.containsKey(JACCARD_SIMILARITY),
                     "The result doesn't have key '%s'", JACCARD_SIMILARITY);
        return jaccard.get(JACCARD_SIMILARITY);
    }
}
