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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.api.graphs.GraphsAPI;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.structure.constant.GraphReadMode;
import org.apache.hugegraph.client.RestClient;

public class GraphsManager {

    private GraphsAPI graphsAPI;

    public GraphsManager(RestClient client, String graphSpace) {
        this.graphsAPI = new GraphsAPI(client, graphSpace);
    }

    public Map<String, String> createGraph(String name, String config) {
        return this.graphsAPI.create(name, config);
    }

    public Map<String, String> createGraph(String name, File file) {
        String config;
        try {
            config = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ClientException("Failed to read config file: %s", file);
        }
        return this.graphsAPI.create(name, config);
    }

    public Map<String, String> getGraph(String graph) {
        return this.graphsAPI.get(graph);
    }

    public List<String> listGraph() {
        return this.graphsAPI.list();
    }

    public List<Map<String, Object>> listProfile() {
        return this.graphsAPI.listProfile(null);
    }

    public List<Map<String, Object>> listProfile(String prefix) {
        return this.graphsAPI.listProfile(prefix);
    }

    public Map<String, String> setDefault(String name) {
        return this.graphsAPI.setDefault(name);
    }

    public Map<String, String> unSetDefault(String name) {
        return this.graphsAPI.unSetDefault(name);
    }

    public Map<String, String> getDefault() {
        return this.graphsAPI.getDefault();
    }

    public void clear(String graph) {
        this.graphsAPI.clear(graph);
    }

    public void clear(String graph, boolean clearSchema) {
        this.graphsAPI.clear(graph, clearSchema);
    }

    public void update(String graph, String nickname) {
        this.graphsAPI.update(graph, nickname);
    }

    public void remove(String graph) {
        this.graphsAPI.delete(graph);
    }

    public void reload(String graph) {
        this.graphsAPI.reload(graph);
    }

    public void reload() {
        this.graphsAPI.reload();
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

    public String clone(String graph, Map<String, Object> body) {
        return this.graphsAPI.clone(graph, body);
    }
}
