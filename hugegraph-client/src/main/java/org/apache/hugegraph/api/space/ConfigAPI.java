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

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.List;
import java.util.Map;

public class ConfigAPI extends API {

    private static final String PATH = "graphspaces/%s/configs/rest";

    public ConfigAPI(RestClient client, String graphSpace) {
        super(client);
        this.path(String.format(PATH, graphSpace));
    }

    @Override
    protected String type() {
        return HugeType.CONFIGS.string();
    }

    public List<String> listConfigOptions() {
        RestResult result = client.get(this.path(), "config-fields");
        return result.readList("fields", String.class);
    }

    public Map<String, Object> get(String serviceName) {
        RestResult result = client.get(this.path(), serviceName);

        return result.readObject(Map.class);
    }

    public Map<String, Object> add(String serviceName,
                                   Map<String, Object> configs) {
        ImmutableMap<String, Object> data
                = ImmutableMap.of("name", serviceName, "config", configs);
        RestResult result = client.post(this.path(), data);

        return result.readObject(Map.class);
    }

    public Map<String, Object> update(String serviceName,
                                      Map<String, Object> config) {
        RestResult result = client.put(this.path(), serviceName, config);

        return result.readObject(Map.class);
    }

    public void delete(String serviceName) {
        client.delete(this.path(), serviceName);
    }
}
