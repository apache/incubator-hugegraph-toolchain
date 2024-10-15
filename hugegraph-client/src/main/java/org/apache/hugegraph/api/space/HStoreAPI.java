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

package org.apache.hugegraph.api.space;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.space.HStoreNodeInfo;

import java.util.List;
import java.util.Map;

public class HStoreAPI extends API {
    private static final String PATH = "hstore";

    public HStoreAPI(RestClient client) {
        super(client);
        this.path(this.type());
    }

    @Override
    protected String type() {
        return HugeType.HSTORE.string();
    }

    public List<String> list() {
        RestResult result = this.client.get(this.path());
        return result.readList("nodes", String.class);
    }

    public HStoreNodeInfo get(String id) {
        RestResult result = this.client.get(this.path(), id);
        return result.readObject(HStoreNodeInfo.class);
    }

    public String status() {
        RestResult result = this.client.get(this.path(), "status");
        return (String) result.readObject(Map.class).get("status");
    }

    public void startSplit() {
        this.client.get(this.path(), "split");
    }

    public void nodeStartup(String id) {
        String startupPath = joinPath(this.path(), id, "startup");
        this.client.get(startupPath);
    }

    public void nodeShutdown(String id) {
        String startupPath = joinPath(this.path(), id, "shutdown");
        this.client.get(startupPath);
    }
}
