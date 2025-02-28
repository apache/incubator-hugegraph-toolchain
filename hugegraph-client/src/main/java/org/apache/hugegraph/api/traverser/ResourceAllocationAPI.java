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

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.graph.GraphAPI;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.traverser.Prediction;
import org.apache.hugegraph.util.E;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceAllocationAPI extends TraversersAPI {

    private static final String RESOURCE_ALLOCATION = "resource_allocation";

    public ResourceAllocationAPI(RestClient client,
                                 String graphSpace,
                                 String graph) {
        super(client, graphSpace, graph);
    }

    @Override
    protected String type() {
        return "resourceallocation";
    }

    public Prediction get(Object vertexId, Object otherId, Direction direction,
                          String label, long degree) {
        this.client.checkApiVersion("0.67", RESOURCE_ALLOCATION);
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
        Map<String, Double> resourceAllocation = result.readObject(Map.class);
        E.checkState(res.getResourceAllocation() != null,
                     "The result doesn't have key '%s'", RESOURCE_ALLOCATION);
        return res;
    }
}
