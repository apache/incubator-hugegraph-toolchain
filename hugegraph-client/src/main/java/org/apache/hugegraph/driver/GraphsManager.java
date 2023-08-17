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

package org.apache.hugegraph.driver;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.graphs.GraphsAPI;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.structure.constant.GraphReadMode;
import org.apache.hugegraph.client.RestClient;

public class GraphsManager {

    private GraphsAPI graphsAPI;

    public GraphsManager(RestClient client) {
        this.graphsAPI = new GraphsAPI(client);
    }

    public Map<String, String> createGraph(String name, String configText) {
        return this.graphsAPI.create(name, null, configText);
    }

    public Map<String, String> cloneGraph(String name, String cloneGraphName) {
        return this.graphsAPI.create(name, cloneGraphName, null);
    }

    public Map<String, String> cloneGraph(String name, String cloneGraphName,
                                          String configText) {
        return this.graphsAPI.create(name, cloneGraphName, configText);
    }

    public Map<String, String> getGraph(String graph) {
        return this.graphsAPI.get(graph);
    }

    public List<String> listGraph() {
        return this.graphsAPI.list();
    }

    public void clearGraph(String graph, String message) {
        this.graphsAPI.clear(graph, message);
    }

    public void dropGraph(String graph, String message) {
        this.graphsAPI.drop(graph, message);
    }

    public void mode(String graph, GraphMode mode) {
        this.graphsAPI.mode(graph, mode);
    }

    public GraphMode mode(String graph) {
        return this.graphsAPI.mode(graph);
    }

    public void readMode(String graph, GraphReadMode readMode) {
        this.graphsAPI.readMode(graph, readMode);
    }

    public GraphReadMode readMode(String graph) {
        return this.graphsAPI.readMode(graph);
    }
}
