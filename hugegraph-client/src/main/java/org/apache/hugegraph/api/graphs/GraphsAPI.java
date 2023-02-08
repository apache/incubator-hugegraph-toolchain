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

package org.apache.hugegraph.api.graphs;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;

import org.apache.commons.lang3.StringUtils;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.exception.InvalidResponseException;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.structure.constant.GraphReadMode;
import org.apache.hugegraph.structure.constant.HugeType;

import com.google.common.collect.ImmutableMap;

public class GraphsAPI extends API {

    private static final String DELIMITER = "/";
    private static final String MODE = "mode";
    private static final String GRAPH_READ_MODE = "graph_read_mode";
    private static final String CLEAR = "clear";

    private static final String CONFIRM_MESSAGE = "confirm_message";

    public GraphsAPI(RestClient client) {
        super(client);
        this.path(this.type());
    }

    @Override
    protected String type() {
        return HugeType.GRAPHS.string();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> create(String name, String cloneGraphName,
                                      String configText) {
        this.client.checkApiVersion("0.67", "dynamic graph add");
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
        Map<String, Object> params = null;
        if (StringUtils.isNotEmpty(cloneGraphName)) {
            params = ImmutableMap.of("clone_graph_name", cloneGraphName);
        }
        RestResult result = this.client.post(joinPath(this.path(), name),
                                             configText, headers, params);
        return result.readObject(Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(Map.class);
    }

    public List<String> list() {
        RestResult result = this.client.get(this.path());
        return result.readList(this.type(), String.class);
    }

    public void clear(String graph, String message) {
        this.client.delete(joinPath(this.path(), graph, CLEAR),
                           ImmutableMap.of(CONFIRM_MESSAGE, message));
    }

    public void drop(String graph, String message) {
        this.client.checkApiVersion("0.67", "dynamic graph delete");
        this.client.delete(joinPath(this.path(), graph),
                           ImmutableMap.of(CONFIRM_MESSAGE, message));
    }

    public void mode(String graph, GraphMode mode) {
        // NOTE: Must provide id for PUT. If use "graph/mode", "/" will
        // be encoded to "%2F". So use "mode" here although inaccurate.
        this.client.put(joinPath(this.path(), graph, MODE), null, mode);
    }

    public GraphMode mode(String graph) {
        RestResult result = this.client.get(joinPath(this.path(), graph), MODE);
        @SuppressWarnings("unchecked")
        Map<String, String> mode = result.readObject(Map.class);
        String value = mode.get(MODE);
        if (value == null) {
            throw new InvalidResponseException("Invalid response, expect 'mode' in response");
        }
        try {
            return GraphMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidResponseException("Invalid GraphMode value '%s'", value);
        }
    }

    public void readMode(String graph, GraphReadMode readMode) {
        this.client.checkApiVersion("0.59", "graph read mode");
        // NOTE: Must provide id for PUT. If use "graph/graph_read_mode", "/"
        // will be encoded to "%2F". So use "graph_read_mode" here although
        // inaccurate.
        this.client.put(joinPath(this.path(), graph, GRAPH_READ_MODE),
                        null, readMode);
    }

    public GraphReadMode readMode(String graph) {
        this.client.checkApiVersion("0.59", "graph read mode");
        RestResult result = this.client.get(joinPath(this.path(), graph),
                                            GRAPH_READ_MODE);
        @SuppressWarnings("unchecked")
        Map<String, String> readMode = result.readObject(Map.class);
        String value = readMode.get(GRAPH_READ_MODE);
        if (value == null) {
            throw new InvalidResponseException("Invalid response, expect 'graph_read_mode' " +
                                               "in response");
        }
        try {
            return GraphReadMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidResponseException("Invalid GraphReadMode value '%s'", value);
        }
    }

    private static String joinPath(String path, String graph) {
        return String.join(DELIMITER, path, graph);
    }

    private static String joinPath(String path, String graph, String action) {
        return String.join(DELIMITER, path, graph, action);
    }
}
