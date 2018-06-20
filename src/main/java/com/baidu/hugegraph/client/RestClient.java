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

package com.baidu.hugegraph.client;

import javax.ws.rs.core.Response;

import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.serializer.PathDeserializer;
import com.baidu.hugegraph.serializer.ShardDeserializer;
import com.baidu.hugegraph.serializer.VertexDeserializer;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Shard;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class RestClient extends com.baidu.hugegraph.rest.RestClient {

    private static final int SECOND = 1000;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Vertex.class, new VertexDeserializer());
        module.addDeserializer(Path.class, new PathDeserializer());
        module.addDeserializer(Shard.class, new ShardDeserializer());
        RestResult.registerModule(module);
    }

    public RestClient(String url, int timeout) {
        super(url, timeout * SECOND);
    }

    public RestClient(String url, String username, String password, int timeout) {
        super(url, username, password, timeout * SECOND);
    }

    protected void checkStatus(Response response, Response.Status... statuses) {
        boolean match = false;
        for (Response.Status status : statuses) {
            if (status.getStatusCode() == response.getStatus()) {
                match = true;
                break;
            }
        }
        if (!match) {
            throw ServerException.fromResponse(response);
        }
    }
}
