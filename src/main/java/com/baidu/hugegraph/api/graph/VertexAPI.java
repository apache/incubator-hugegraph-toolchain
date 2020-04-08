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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;

import com.baidu.hugegraph.api.graph.structure.BatchVertexRequest;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.exception.NotAllCreatedException;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.graph.Vertices;
import com.google.common.collect.ImmutableMap;

public class VertexAPI extends GraphAPI {

    public VertexAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.VERTEX.string();
    }

    public Vertex create(Vertex vertex) {
        RestResult result = this.client.post(this.path(), vertex);
        return result.readObject(Vertex.class);
    }

    public List<Object> create(List<Vertex> vertices) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Encoding", BATCH_ENCODING);
        RestResult result = this.client.post(this.batchPath(), vertices,
                                             headers);
        List<Object> ids = result.readList(Object.class);
        if (vertices.size() != ids.size()) {
            throw new NotAllCreatedException(
                      "Not all vertices are successfully created, " +
                      "expect '%s', the actual is '%s'",
                      ids, vertices.size(), ids.size());
        }
        return ids;
    }

    public List<Vertex> update(BatchVertexRequest request) {
        this.client.checkApiVersion("0.45", "batch property update");
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Encoding", BATCH_ENCODING);
        RestResult result = this.client.put(this.batchPath(), null,
                                            request, headers);
        return result.readList(this.type(), Vertex.class);
    }

    public Vertex append(Vertex vertex) {
        String id = GraphAPI.formatVertexId(vertex.id());
        Map<String, Object> params = ImmutableMap.of("action", "append");
        RestResult result = this.client.put(this.path(), id, vertex, params);
        return result.readObject(Vertex.class);
    }

    public Vertex eliminate(Vertex vertex) {
        String id = GraphAPI.formatVertexId(vertex.id());
        Map<String, Object> params = ImmutableMap.of("action", "eliminate");
        RestResult result = this.client.put(this.path(), id, vertex, params);
        return result.readObject(Vertex.class);
    }

    public Vertex get(Object id) {
        String vertexId = GraphAPI.formatVertexId(id);
        RestResult result = this.client.get(this.path(), vertexId);
        return result.readObject(Vertex.class);
    }

    public Vertices list(int limit) {
        return this.list(null, null, 0, null, limit);
    }

    public Vertices list(String label, Map<String, Object> properties,
                         int offset, String page, int limit) {
        return this.list(label, properties, false, offset, page, limit);
    }

    public Vertices list(String label, Map<String, Object> properties,
                         boolean keepP, int offset, String page, int limit) {
        checkOffset(offset);
        checkLimit(limit, "Limit");
        String props = GraphAPI.formatProperties(properties);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("label", label);
        params.put("properties", props);
        params.put("keep_start_p", keepP);
        params.put("offset", offset);
        params.put("limit", limit);
        params.put("page", page);
        RestResult result = this.client.get(this.path(), params);
        return result.readObject(Vertices.class);
    }

    public void delete(Object id) {
        String vertexId = GraphAPI.formatVertexId(id);
        this.client.delete(this.path(), vertexId);
    }
}
