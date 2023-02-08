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

package org.apache.hugegraph.api.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.exception.NotAllCreatedException;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Edges;

import com.google.common.collect.ImmutableMap;

import jakarta.ws.rs.core.MultivaluedHashMap;

public class EdgeAPI extends GraphAPI {

    public EdgeAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.EDGE.string();
    }

    public Edge create(Edge edge) {
        RestResult result = this.client.post(this.path(), edge);
        return result.readObject(Edge.class);
    }

    public List<String> create(List<Edge> edges, boolean checkVertex) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Encoding", BATCH_ENCODING);
        Map<String, Object> params = ImmutableMap.of("check_vertex",
                                                     checkVertex);
        RestResult result = this.client.post(this.batchPath(), edges,
                                             headers, params);
        List<String> ids = result.readList(String.class);
        if (edges.size() != ids.size()) {
            throw new NotAllCreatedException("Not all edges are successfully created, " +
                                             "expect '%s', the actual is '%s'",
                                             ids, edges.size(), ids.size());
        }
        return ids;
    }

    public List<Edge> update(BatchEdgeRequest request) {
        this.client.checkApiVersion("0.45", "batch property update");
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Encoding", BATCH_ENCODING);
        RestResult result = this.client.put(this.batchPath(), null,
                                            request, headers);
        return result.readList(this.type(), Edge.class);
    }

    public Edge append(Edge edge) {
        String id = edge.id();
        Map<String, Object> params = ImmutableMap.of("action", "append");
        RestResult result = this.client.put(this.path(), id, edge, params);
        return result.readObject(Edge.class);
    }

    public Edge eliminate(Edge edge) {
        String id = edge.id();
        Map<String, Object> params = ImmutableMap.of("action", "eliminate");
        RestResult result = this.client.put(this.path(), id, edge, params);
        return result.readObject(Edge.class);
    }

    public Edge get(String id) {
        RestResult result = this.client.get(this.path(), id);
        return result.readObject(Edge.class);
    }

    public Edges list(int limit) {
        return this.list(null, null, null, null, 0, null, limit);
    }

    public Edges list(Object vertexId, Direction direction,
                      String label, Map<String, Object> properties,
                      int offset, String page, int limit) {
        return this.list(vertexId, direction, label, properties, false,
                         offset, page, limit);
    }

    public Edges list(Object vertexId, Direction direction, String label,
                      Map<String, Object> properties, boolean keepP,
                      int offset, String page, int limit) {
        checkOffset(offset);
        checkLimit(limit, "Limit");
        String vid = GraphAPI.formatVertexId(vertexId, true);
        String props = GraphAPI.formatProperties(properties);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("vertex_id", vid);
        params.put("direction", direction);
        params.put("label", label);
        params.put("properties", props);
        params.put("keep_start_p", keepP);
        params.put("offset", offset);
        params.put("limit", limit);
        params.put("page", page);
        RestResult result = this.client.get(this.path(), params);
        return result.readObject(Edges.class);
    }

    public void delete(String id) {
        this.client.delete(this.path(), id);
    }
}
