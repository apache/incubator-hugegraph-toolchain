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

package com.baidu.hugegraph.api.graph;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.exception.ClientException;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public abstract class GraphAPI extends API {

    private static final String PATH = "graphs/%s/graph/%s";

    protected static final String CHARSET = "UTF-8";
    protected static final String BATCH_ENCODING = "gzip";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String batchPath;

    public GraphAPI(RestClient client, String graph) {
        super(client);
        this.path(String.format(PATH, graph, type()));
        this.batchPath = String.format("%s/%s", this.path, "batch");
    }

    public String batchPath() {
        return this.batchPath;
    }

    public static String formatVertexId(Object id) {
        return formatVertexId(id, false);
    }

    public static String formatVertexId(Object id, boolean allowNull) {
        if (!allowNull) {
            E.checkArgumentNotNull(id, "The vertex id can't be null");
        } else {
            if (id == null) {
                return null;
            }
        }
        E.checkArgument(id instanceof String || id instanceof Number,
                        "The vertex id must be either 'string' or " +
                        "'number', but got '%s'", id);
        return JsonUtil.toJson(id);
    }

    public static String formatProperties(Map<String, Object> properties) {
        if (properties == null) {
            properties = ImmutableMap.of();
        }
        try {
            String props = MAPPER.writeValueAsString(properties);
            return URLEncoder.encode(props, CHARSET);
        } catch (JsonProcessingException e) {
            throw new ClientException(String.format(
                      "Failed to serialize properties '%s'", properties));
        } catch (UnsupportedEncodingException e) {
            throw new ClientException(String.format(
                      "Failed to encode properties '%s'", properties));
        }
    }
}
