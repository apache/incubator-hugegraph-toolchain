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

package com.baidu.hugegraph.api.variables;

import java.util.Map;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.google.common.collect.ImmutableMap;

public class VariablesAPI extends API {

    private static final String PATH = "graphs/%s/%s";

    public VariablesAPI(RestClient client, String graph) {
        super(client);
        this.path(String.format(PATH, graph, this.type()));
    }

    @Override
    protected String type() {
        return HugeType.VARIABLES.string();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String key) {
        RestResult result = this.client.get(path(), key);
        return result.readObject(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> set(String key, Object value) {
        value = ImmutableMap.of("data", value);
        RestResult result = this.client.put(this.path(), key, value);
        return result.readObject(Map.class);
    }

    public void remove(String key) {
        this.client.delete(path(), key);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> all() {
        RestResult result = this.client.get(path());
        return result.readObject(Map.class);
    }
}
