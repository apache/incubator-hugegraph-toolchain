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

import java.util.Map;

import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.uri.UriComponent.Type;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public abstract class GraphAPI extends API {

    private static final String PATH = "graphs/%s/graph/%s";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String batchPath;

    public GraphAPI(RestClient client, String graph) {
        super(client);
        this.path = String.format(PATH, graph, type());
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
                        "The vertex id must be either String or " +
                        "Number, but got '%s'", id);
        return JsonUtil.toJson(id);
    }

    public static String formatProperties(Map<String, Object> properties) {
        if (properties == null) {
            properties = ImmutableMap.of();
        }
        String json;
        try {
            json = MAPPER.writeValueAsString(properties);
        } catch (JsonProcessingException e) {
            throw new ClientException("Failed to serialize properties '%s'",
                                      properties);
        }
        /*
         * Don't use UrlEncoder.encode, it encoded the space as `+`,
         * which will invalidate the jersey's automatic decoding
         * because it considers the space to be encoded as `%2F`
         */
        return encode(json);
    }

    public static String encode(String raw) {
        return UriComponent.encode(raw, Type.QUERY_PARAM_SPACE_ENCODED);
    }
}
