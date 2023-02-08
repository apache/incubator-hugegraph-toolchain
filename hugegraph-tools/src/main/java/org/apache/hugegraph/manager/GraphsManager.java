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

package org.apache.hugegraph.manager;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.base.ToolClient;
import org.apache.hugegraph.base.ToolManager;
import org.apache.hugegraph.structure.constant.GraphMode;

public class GraphsManager extends ToolManager {

    public GraphsManager(ToolClient.ConnectionInfo info) {
        super(info, "graphs");
    }

    public Map<String, String> create(String name, String config) {
        return this.client.graphs().createGraph(name, config);
    }

    public Map<String, String> clone(String name, String cloneGraphName) {
        return this.client.graphs().cloneGraph(name, cloneGraphName);
    }

    public List<String> list() {
        return this.client.graphs().listGraph();
    }

    public Map<String, String> get(String graph) {
        return this.client.graphs().getGraph(graph);
    }

    public void clear(String graph, String confirmMessage) {
        this.client.graphs().clearGraph(graph, confirmMessage);
    }

    public void drop(String graph, String confirmMessage) {
        this.client.graphs().dropGraph(graph, confirmMessage);
    }

    public void mode(String graph, GraphMode mode) {
        this.client.graphs().mode(graph, mode);
    }

    public GraphMode mode(String graph) {
        return this.client.graphs().mode(graph);
    }
}
