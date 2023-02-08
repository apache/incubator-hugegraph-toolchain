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

package org.apache.hugegraph.api.traverser;

import java.util.List;

import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class SameNeighborsApiTest extends TraverserApiTest {

    @BeforeClass
    public static void initShortestPathGraph() {
        schema().vertexLabel("node")
                .useCustomizeNumberId()
                .ifNotExist()
                .create();

        schema().edgeLabel("link")
                .sourceLabel("node").targetLabel("node")
                .ifNotExist()
                .create();
        schema().edgeLabel("relateTo")
                .sourceLabel("node").targetLabel("node")
                .ifNotExist()
                .create();

        Vertex v1 = graph().addVertex(T.LABEL, "node", T.ID, 1);
        Vertex v2 = graph().addVertex(T.LABEL, "node", T.ID, 2);
        Vertex v3 = graph().addVertex(T.LABEL, "node", T.ID, 3);
        Vertex v4 = graph().addVertex(T.LABEL, "node", T.ID, 4);
        Vertex v5 = graph().addVertex(T.LABEL, "node", T.ID, 5);
        Vertex v6 = graph().addVertex(T.LABEL, "node", T.ID, 6);
        Vertex v7 = graph().addVertex(T.LABEL, "node", T.ID, 7);
        Vertex v8 = graph().addVertex(T.LABEL, "node", T.ID, 8);
        Vertex v9 = graph().addVertex(T.LABEL, "node", T.ID, 9);
        Vertex v10 = graph().addVertex(T.LABEL, "node", T.ID, 10);

        v1.addEdge("link", v3);
        v2.addEdge("link", v3);
        v4.addEdge("link", v1);
        v4.addEdge("link", v2);

        v1.addEdge("relateTo", v5);
        v2.addEdge("relateTo", v5);
        v6.addEdge("relateTo", v1);
        v6.addEdge("relateTo", v2);

        v1.addEdge("link", v7);
        v8.addEdge("link", v1);
        v2.addEdge("link", v9);
        v10.addEdge("link", v2);
    }

    @Test
    public void testSameNeighbors() {
        List<Object> neighbors = sameNeighborsAPI.get(1, 2, Direction.BOTH,
                                                      null, -1, -1);
        Assert.assertEquals(4, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(3, 4, 5, 6)));
    }

    @Test
    public void testSameNeighborsWithDirection() {
        List<Object> neighbors = sameNeighborsAPI.get(1, 2, Direction.OUT,
                                                      null, -1, -1);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(3, 5)));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.IN,
                                         null, -1, -1);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(4, 6)));
    }

    @Test
    public void testSameNeighborsWithLabel() {
        List<Object> neighbors = sameNeighborsAPI.get(1, 2, Direction.BOTH,
                                                      "link", -1, -1);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(3, 4)));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.OUT, "link", -1, -1);
        Assert.assertEquals(1, neighbors.size());

        Assert.assertTrue(neighbors.contains(3));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.IN,
                                         "link", -1, -1);
        Assert.assertEquals(1, neighbors.size());

        Assert.assertTrue(neighbors.contains(4));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.BOTH,
                                         "relateTo", -1, -1);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(5, 6)));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.OUT,
                                         "relateTo", -1, -1);
        Assert.assertEquals(1, neighbors.size());

        Assert.assertTrue(neighbors.contains(5));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.IN,
                                         "relateTo", -1, -1);
        Assert.assertEquals(1, neighbors.size());

        Assert.assertTrue(neighbors.contains(6));
    }

    @Test
    public void testSameNeighborsWithDegree() {
        List<Object> neighbors = sameNeighborsAPI.get(1, 2, Direction.OUT,
                                                      null, 6, -1);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(3, 5)));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.OUT,
                                         null, 1, -1);
        Assert.assertEquals(1, neighbors.size());

        Assert.assertTrue(neighbors.contains(3));
    }

    @Test
    public void testSameNeighborsWithLimit() {
        List<Object> neighbors = sameNeighborsAPI.get(1, 2, Direction.BOTH,
                                                      "link", 6, -1);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(3, 4)));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.BOTH,
                                         "link", 6, 2);
        Assert.assertEquals(2, neighbors.size());

        Assert.assertTrue(neighbors.containsAll(ImmutableList.of(3, 4)));

        neighbors = sameNeighborsAPI.get(1, 2, Direction.BOTH,
                                         "link", 6, 1);
        Assert.assertEquals(1, neighbors.size());

        Assert.assertTrue(neighbors.contains(3));
    }
}
