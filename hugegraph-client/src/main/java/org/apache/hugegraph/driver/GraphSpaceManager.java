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

import org.apache.hugegraph.api.space.GraphSpaceAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.space.GraphSpace;

public class GraphSpaceManager {

    private final GraphSpaceAPI graphSpaceAPI;

    public GraphSpaceManager(RestClient client) {
        this.graphSpaceAPI = new GraphSpaceAPI(client);
    }

    public List<String> listGraphSpace() {
        return this.graphSpaceAPI.list();
    }

    public List<Map<String, Object>> listProfile() {
        return this.graphSpaceAPI.listProfile(null);
    }

    public List<Map<String, Object>> listProfile(String prefix) {
        return this.graphSpaceAPI.listProfile(prefix);
    }

    public Map<String, String> setDefault(String name) {
        return this.graphSpaceAPI.setDefault(name);
    }

    public Map<String, String> getDefault() {
        return this.graphSpaceAPI.getDefault();
    }

    public GraphSpace getGraphSpace(String name) {
        return this.graphSpaceAPI.get(name);
    }

    public GraphSpace createGraphSpace(GraphSpace graphSpace) {
        return this.graphSpaceAPI.create(graphSpace);
    }

    public void deleteGraphSpace(String name) {
        this.graphSpaceAPI.delete(name);
    }

    public GraphSpace updateGraphSpace(GraphSpace graphSpace) {
        return this.graphSpaceAPI.update(graphSpace);
    }

    public Map<String, String> setDefaultRole(String name, String user,
                                              String role) {
        return this.graphSpaceAPI.setDefaultRole(name, user, role, "");
    }

    public Map<String, String> setDefaultRole(String name, String user,
                                              String role, String graph) {
        return this.graphSpaceAPI.setDefaultRole(name, user, role, graph);
    }

    public boolean checkDefaultRole(String name, String user,
                                    String role) {
        return this.graphSpaceAPI.checkDefaultRole(name, user, role, "");
    }

    public boolean checkDefaultRole(String name, String user,
                                    String role, String graph) {
        return this.graphSpaceAPI.checkDefaultRole(name, user, role, graph);
    }

    public Map<String, String> deleteDefaultRole(String name, String user,
                                                 String role) {
        return this.graphSpaceAPI.deleteDefaultRole(name, user, role, "");
    }

    public Map<String, String> deleteDefaultRole(String name, String user,
                                                 String role, String graph) {
        return this.graphSpaceAPI.deleteDefaultRole(name, user, role, graph);
    }
}
