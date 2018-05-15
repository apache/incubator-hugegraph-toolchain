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
import java.util.Map;

import com.baidu.hugegraph.api.graphs.GraphsAPI;
import com.baidu.hugegraph.client.RestClient;

public class GraphsManager {

    private GraphsAPI graphsAPI;

    public GraphsManager(RestClient client) {
        this.graphsAPI = new GraphsAPI(client);
    }

    public Map<String, String> getGraph(String graph) {
        return this.graphsAPI.get(graph);
    }

    public List<String> listGraph() {
        return this.graphsAPI.list();
    }

    public void restoring(String graph, boolean restoring) {
        this.graphsAPI.restoring(graph, restoring);
    }

    public boolean restoring(String graph) {
        return this.graphsAPI.restoring(graph);
    }

    public void clear(String graph, String message) {
        this.graphsAPI.clear(graph, message);
    }
}
