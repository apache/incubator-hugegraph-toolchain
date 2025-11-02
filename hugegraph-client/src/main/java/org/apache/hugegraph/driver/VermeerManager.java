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

import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.vermeer.VermeerAPI;

public class VermeerManager {

    private final VermeerAPI vermeerAPI;

    public VermeerManager(RestClient client) {
        this.vermeerAPI = new VermeerAPI(client);
    }

    public Map<String, Object> getStatus() {
        return this.vermeerAPI.getStatus();
    }

    public Map<String, Object> post(Map<String, Object> params) {
        return this.vermeerAPI.post(params);
    }

    public Map<String, Object> getGraphsInfo() {
        return this.vermeerAPI.getGraphsInfo();
    }

    public Map<String, Object> getGraphInfoByName(String graphName) {
        return this.vermeerAPI.getGraphInfoByName(graphName);
    }

    public Map<String, Object> deleteGraphByName(String graphName) {
        return this.vermeerAPI.deleteGraphByName(graphName);
    }

    public Map<String, Object> getTasksInfo() {
        return this.vermeerAPI.getTasksInfo();
    }

    public Map<String, Object> getTasksInfoById(String taskId) {
        return this.vermeerAPI.getTasksInfoById(taskId);
    }

}
