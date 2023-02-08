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

package org.apache.hugegraph.unit;

import static org.apache.hugegraph.structure.graph.UpdateStrategy.INTERSECTION;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.UpdateStrategy;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BatchElementRequestTest extends BaseUnitTest {

    @Test
    public void testVertexRequestBuildOK() {
        List<Vertex> vertices = ImmutableList.of(createVertex());
        Map<String, UpdateStrategy> strategies = ImmutableMap.of("set",
                                                                 INTERSECTION);

        BatchVertexRequest req;
        req = new BatchVertexRequest.Builder().vertices(vertices)
                                              .updatingStrategies(strategies)
                                              .createIfNotExist(true)
                                              .build();

        Assert.assertNotNull(req);
        Object list = Whitebox.getInternalState(req, "vertices");
        Assert.assertEquals(vertices, list);
        Object map = Whitebox.getInternalState(req, "updateStrategies");
        Assert.assertEquals(strategies, map);
        Object created = Whitebox.getInternalState(req, "createIfNotExist");
        Assert.assertEquals(true, created);
    }

    @Test
    public void testVertexEmptyUpdateStrategy() {
        List<Vertex> vertices = ImmutableList.of(createVertex());
        Map<String, UpdateStrategy> strategies = ImmutableMap.of();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new BatchVertexRequest.Builder().vertices(vertices)
                                            .updatingStrategies(strategies)
                                            .createIfNotExist(true)
                                            .build();
        });
    }

    @Test
    public void testVertexNotSupportedUpdateParameter() {
        List<Vertex> vertices = ImmutableList.of(createVertex());
        Map<String, UpdateStrategy> strategies = ImmutableMap.of("set",
                                                                 INTERSECTION);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new BatchVertexRequest.Builder().vertices(vertices)
                                            .updatingStrategies(strategies)
                                            .createIfNotExist(false)
                                            .build();
        });
    }

    @Test
    public void testEdgeRequestBuildOK() {
        List<Edge> edges = ImmutableList.of(createEdge());
        Map<String, UpdateStrategy> strategies = ImmutableMap.of("set",
                                                                 INTERSECTION);

        BatchEdgeRequest req;
        req = new BatchEdgeRequest.Builder().edges(edges)
                                            .updatingStrategies(strategies)
                                            .checkVertex(false)
                                            .createIfNotExist(true)
                                            .build();

        Assert.assertNotNull(req);
        Object list = Whitebox.getInternalState(req, "edges");
        Assert.assertEquals(edges, list);
        Object map = Whitebox.getInternalState(req, "updateStrategies");
        Assert.assertEquals(strategies, map);
        Object checked = Whitebox.getInternalState(req, "checkVertex");
        Assert.assertEquals(false, checked);
        Object created = Whitebox.getInternalState(req, "createIfNotExist");
        Assert.assertEquals(true, created);
    }

    @Test
    public void testEdgeEmptyUpdateStrategy() {
        List<Edge> edges = ImmutableList.of(createEdge());
        Map<String, UpdateStrategy> strategies = ImmutableMap.of();

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new BatchEdgeRequest.Builder().edges(edges)
                                          .updatingStrategies(strategies)
                                          .checkVertex(false)
                                          .createIfNotExist(true)
                                          .build();
        });
    }

    @Test
    public void testEdgeNotSupportedUpdateParameter() {
        List<Edge> edges = ImmutableList.of(createEdge());
        Map<String, UpdateStrategy> strategies = ImmutableMap.of("set",
                                                                 INTERSECTION);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new BatchEdgeRequest.Builder().edges(edges)
                                          .updatingStrategies(strategies)
                                          .checkVertex(false)
                                          .createIfNotExist(false)
                                          .build();
        });
    }

    private static Vertex createVertex() {
        Vertex vertex = new Vertex("object");
        vertex.id("object:1");
        vertex.property("name", 1);
        vertex.property("price", 2);
        return vertex;
    }

    private static Edge createEdge() {
        Edge edge = new Edge("updates");
        edge.id("object:1>updates>>object:2");
        edge.sourceId("object:1");
        edge.sourceLabel("object");
        edge.targetId("object:2");
        edge.targetLabel("object");
        edge.property("price", 1);
        return edge;
    }
}
