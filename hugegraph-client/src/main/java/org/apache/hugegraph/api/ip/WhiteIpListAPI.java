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

package org.apache.hugegraph.api.ip;

import com.google.common.collect.ImmutableMap;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.Map;

public class WhiteIpListAPI extends API {
    private static final String PATH = "whiteiplist";

    public WhiteIpListAPI(RestClient client) {
        super(client);
        this.path(PATH);
    }

    @Override
    protected String type() {
        return HugeType.WHITE_IP_LIST.string();
    }

    public Map<String, Object> list() {
        RestResult result = this.client.get(this.path());
        return result.readObject(Map.class);
    }

    public Map<String, Object> batch(Map<String, Object> action) {
        RestResult result = this.client.post(this.path(), action);
        return result.readObject(Map.class);

    }

    public Map<String, Object> update(boolean status) {
        Map<String, Object> params = ImmutableMap.of("status", status);
        RestResult result = this.client.put(this.path(), null, params);
        return result.readObject(Map.class);
    }
}
