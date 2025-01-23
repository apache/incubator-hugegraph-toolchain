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

package org.apache.hugegraph.api.vermeer;

import com.google.common.collect.ImmutableMap;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.Map;

public class VermeerAPI extends API {

    private static final String PATH = "vermeer";
    private static final String GET_SYS_CFG =
            "api/v1.0/memt_clu/config/getsyscfg";

    public VermeerAPI(RestClient client) {
        super(client);
    }

    @Override
    protected String type() {
        return HugeType.VERMEER.string();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> sertMap = ImmutableMap.of("sertype", "vermeer");
        RestResult results = this.client.post(GET_SYS_CFG, sertMap);
        return results.readObject(Map.class);
    }

    public Map<String, Object> post(Map<String, Object> params) {
        this.path(PATH);
        RestResult results = this.client.post(this.path(), params);
        return results.readObject(Map.class);
    }

    public Map<String, Object> getGraphsInfo() {
        this.path(String.join("/", PATH, "vermeergraphs"));
        RestResult results = this.client.get(this.path());
        return results.readObject(Map.class);
    }

    public Map<String, Object> getGraphInfoByName(String graphName) {
        this.path(String.join("/", PATH, "vermeergraphs", graphName));
        RestResult results = this.client.get(this.path());
        return results.readObject(Map.class);
    }

    public Map<String, Object> deleteGraphByName(String graphName) {
        this.path(String.join("/", PATH, "vermeergraphs", graphName));
        RestResult results = this.client.delete(this.path(), ImmutableMap.of());
        return results.readObject(Map.class);
    }

    public Map<String, Object> getTasksInfo() {
        this.path(String.join("/", PATH, "vermeertasks"));
        RestResult results = this.client.get(this.path());
        return results.readObject(Map.class);
    }

    public Map<String, Object> getTasksInfoById(String taskId) {
        this.path(
                String.join("/", PATH, "vermeertasks", taskId));
        RestResult results = this.client.get(this.path());
        return results.readObject(Map.class);
    }








}
