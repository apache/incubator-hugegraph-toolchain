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

package org.apache.hugegraph.api.space;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang.StringUtils;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.space.GraphSpace;
import org.apache.hugegraph.util.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraphSpaceAPI extends API {

    private static final String PATH = "graphspaces";
    private static final String DELIMITER = "/";

    public GraphSpaceAPI(RestClient client) {
        super(client);
        this.path(PATH);
    }

    private static String joinPath(String path, String id) {
        return String.join(DELIMITER, path, id);
    }

    @Override
    protected String type() {
        return HugeType.GRAPHSPACES.string();
    }

    public GraphSpace create(GraphSpace graphSpace) {
        this.client.checkApiVersion("0.67", "dynamic graph space add");
        Object obj = graphSpace.convertReq();
        RestResult result = this.client.post(this.path(), obj);
        return result.readObject(GraphSpace.class);
    }

    public GraphSpace get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(GraphSpace.class);
    }

    public List<String> list() {
        RestResult result = this.client.get(this.path());

        return result.readList(this.type(), String.class);
    }

    public List<Map<String, Object>> listProfile(String prefix) {
        String profilePath = joinPath(this.path(), "profile");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("prefix", prefix);
        RestResult result = this.client.get(profilePath, params);
        List<Map> results = result.readList(Map.class);
        List<Map<String, Object>> profiles = new ArrayList<>();
        for (Object entry : results) {
            profiles.add(JsonUtil.fromJson(JsonUtil.toJson(entry), Map.class));
        }
        return profiles;
    }

    public Map<String, String> setDefault(String name) {
        String defaultPath = joinPath(this.path(), name, "default");
        RestResult result = this.client.get(defaultPath);
        return result.readObject(Map.class);
    }

    public Map<String, String> getDefault() {
        String defaultPath = joinPath(this.path(), "default");
        RestResult result = this.client.get(defaultPath);
        return result.readObject(Map.class);
    }

    public Map<String, String> setDefaultRole(String name, String user,
                                              String role, String graph) {
        String path = joinPath(this.path(), name, "role");
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("role", role);
        if (StringUtils.isNotEmpty(graph)) {
            params.put("graph", graph);
        }
        RestResult result = this.client.post(path, params);
        return result.readObject(Map.class);
    }

    public boolean checkDefaultRole(String name, String user,
                                    String role, String graph) {
        String path = joinPath(this.path(), name, "role");
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("role", role);
        if (StringUtils.isNotEmpty(graph)) {
            params.put("graph", graph);
        }
        RestResult result = this.client.get(path, params);
        return (boolean) result.readObject(Map.class).getOrDefault("check",
                                                                   false);
    }

    public Map<String, String> deleteDefaultRole(String name, String user,
                                                 String role, String graph) {
        String path = joinPath(this.path(), name, "role");
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("role", role);
        if (StringUtils.isNotEmpty(graph)) {
            params.put("graph", graph);
        }
        RestResult result = this.client.delete(path, params);
        return result.readObject(Map.class);
    }

    public void delete(String name) {
        this.client.delete(joinPath(this.path(), name),
                           ImmutableMap.of());
    }

    public GraphSpace update(GraphSpace graphSpace) {
        Object obj = graphSpace.convertReq();
        RestResult result = this.client.put(this.path(),
                                            graphSpace.getName(),
                                            ImmutableMap.of("action", "update",
                                                            "update",
                                                            obj));

        return result.readObject(GraphSpace.class);
    }
}
