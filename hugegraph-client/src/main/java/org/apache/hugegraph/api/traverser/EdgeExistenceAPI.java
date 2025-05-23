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


import org.apache.hugegraph.api.graph.GraphAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.logging.log4j.util.Strings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EdgeExistenceAPI extends TraversersAPI {

    public EdgeExistenceAPI(RestClient client, String graphSpace, String graph) {
        super(client, graphSpace, graph);
    }

    @Override
    protected String type() {
        return "edgeexist";
    }

    public List<Edge> get(Object sourceId, Object targetId, String edgeLabel,
                          String sortValues, int limit) {
        this.client.checkApiVersion("0.70", "count");
        String source = GraphAPI.formatVertexId(sourceId, false);
        String target = GraphAPI.formatVertexId(targetId, false);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("source", source);
        params.put("target", target);
        params.put("label", edgeLabel);
        if (!Strings.isEmpty(sortValues)) {
            params.put("sort_values", sortValues);
        }
        params.put("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList("edges", Edge.class);
    }
}
