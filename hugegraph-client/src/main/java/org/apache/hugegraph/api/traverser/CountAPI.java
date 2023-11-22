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

import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.traverser.CountRequest;
import org.apache.hugegraph.util.E;

public class CountAPI extends TraversersAPI {

    private static final String COUNT = "count";

    public CountAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return "count";
    }

    public long post(CountRequest request) {
        this.client.checkApiVersion("0.55", "count");
        RestResult result = this.client.post(this.path(), request);
        @SuppressWarnings("unchecked")
        Map<String, Number> countMap = result.readObject(Map.class);
        E.checkState(countMap.containsKey(COUNT),
                     "The result doesn't have key '%s'", COUNT);
        return countMap.get(COUNT).longValue();
    }
}
