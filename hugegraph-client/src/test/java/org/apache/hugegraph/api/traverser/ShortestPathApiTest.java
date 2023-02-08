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

import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ShortestPathApiTest extends TraverserApiTest {

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
        Vertex v11 = graph().addVertex(T.LABEL, "node", T.ID, 11);
        Vertex v12 = graph().addVertex(T.LABEL, "node", T.ID, 12);
        Vertex v13 = graph().addVertex(T.LABEL, "node", T.ID, 13);
        Vertex v14 = graph().addVertex(T.LABEL, "node", T.ID, 14);
        Vertex v15 = graph().addVertex(T.LABEL, "node", T.ID, 15);
        Vertex v16 = graph().addVertex(T.LABEL, "node", T.ID, 16);
        Vertex v17 = graph().addVertex(T.LABEL, "node", T.ID, 17);
        Vertex v18 = graph().addVertex(T.LABEL, "node", T.ID, 18);

        // Path length 5
        v1.addEdge("link", v2);
        v2.addEdge("link", v3);
        v3.addEdge("link", v4);
        v4.addEdge("link", v5);
        v5.addEdge("link", v6);

        // Path length 4
        v1.addEdge("link", v7);
        v7.addEdge("link", v8);
        v8.addEdge("link", v9);
        v9.addEdge("link", v6);

        // Path length 3
        v1.addEdge("link", v10);
        v10.addEdge("link", v11);
        v11.addEdge("link", v6);

        // Add other 3 neighbor for v7
        v7.addEdge("link", v12);
        v7.addEdge("link", v13);
        v7.addEdge("link", v14);

        // Add other 4 neighbor for v10
        v10.addEdge("link", v15);
        v10.addEdge("link", v16);
        v10.addEdge("link", v17);
        v10.addEdge("link", v18);
    }

    @Test
    public void testShortestPath() {
        Path path = shortestPathAPI.get(1, 6, Direction.BOTH,
                                        null, 6, -1L, 0L, -1L);
        Assert.assertEquals(4, path.size());
        Assert.assertEquals(ImmutableList.of(1, 10, 11, 6), path.objects());
    }

    @Test
    public void testShortestPathWithLabel() {
        Path path = shortestPathAPI.get(1, 6, Direction.BOTH,
                                        "link", 6, -1L, 0L, -1L);
        Assert.assertEquals(4, path.size());
        Assert.assertEquals(ImmutableList.of(1, 10, 11, 6), path.objects());
    }

    @Test
    public void testShortestPathWithDegree() {
        Path path = shortestPathAPI.get(1, 6, Direction.OUT,
                                        null, 6, 1L, 0L, -1L);
        /*
         * Following results can be guaranteed in RocksDB backend,
         * but different results exist in table type backend(like Cassandra).
         */
        Assert.assertEquals(6, path.size());
        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4, 5, 6), path.objects());
    }

    @Test
    public void testShortestPathWithCapacity() {
        Path path = shortestPathAPI.get(14, 6, Direction.BOTH,
                                        null, 6, 5L, 0L, 19L);
        Assert.assertEquals(5, path.size());
        Assert.assertEquals(ImmutableList.of(14, 7, 8, 9, 6), path.objects());

        Assert.assertThrows(ServerException.class, () -> {
            shortestPathAPI.get(14, 6, Direction.BOTH, null, 6, 1L, 0L, 2L);
        }, e -> {
            String expect = "Exceed capacity '2' while finding shortest path";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testShortestPathWithMaxDepth() {
        Path path = shortestPathAPI.get(14, 6, Direction.BOTH,
                                        null, 4, 5L, 0L, 19L);
        Assert.assertEquals(5, path.size());
        Assert.assertEquals(ImmutableList.of(14, 7, 8, 9, 6), path.objects());

        path = shortestPathAPI.get(14, 6, Direction.BOTH,
                                   null, 3, 5L, 0L, 19L);
        Assert.assertEquals(0, path.size());
    }

    @Test
    public void testShortestPathWithSkipDegree() {
        // Path length 5 with min degree 3(v1 degree is 3)
        List<Object> path1 = ImmutableList.of(1, 2, 3, 4, 5, 6);
        // Path length 4 with middle degree 4(v7 degree is 4)
        List<Object> path2 = ImmutableList.of(1, 7, 8, 9, 6);
        // Path length 3 with max degree 5(v10 degree is 5)
        List<Object> path3 = ImmutableList.of(1, 10, 11, 6);

        // (skipped degree == degree) > max degree
        Path path = shortestPathAPI.get(1, 6, Direction.OUT,
                                        null, 5, 6L, 6L, -1L);
        Assert.assertEquals(4, path.size());
        Assert.assertEquals(path3, path.objects());

        // (skipped degree == degree) == max degree
        path = shortestPathAPI.get(1, 6, Direction.OUT,
                                   null, 5, 5L, 5L, -1L);
        Assert.assertEquals(5, path.size());
        Assert.assertEquals(path2, path.objects());

        // min degree < (skipped degree == degree) == middle degree < max degree
        path = shortestPathAPI.get(1, 6, Direction.OUT,
                                   null, 5, 4L, 4L, -1L);
        Assert.assertEquals(6, path.size());
        Assert.assertEquals(path1, path.objects());

        // (skipped degree == degree) <= min degree
        path = shortestPathAPI.get(1, 6, Direction.OUT,
                                   null, 5, 3L, 3L, -1L);
        Assert.assertEquals(0, path.size());

        // Skipped degree > max degree, degree <= min degree
        path = shortestPathAPI.get(1, 6, Direction.OUT,
                                   null, 5, 3L, 6L, -1L);
        Assert.assertTrue(path.size() == 4 ||
                          path.size() == 5 ||
                          path.size() == 6);
        List<List<Object>> paths = ImmutableList.of(path1, path2, path3);
        Assert.assertTrue(paths.contains(path.objects()));

        // Skipped degree > max degree, min degree < degree < max degree
        path = shortestPathAPI.get(1, 6, Direction.OUT,
                                   null, 5, 4L, 6L, -1L);
        Assert.assertTrue(path.size() == 4 || path.size() == 5);
        Assert.assertTrue(path2.equals(path.objects()) ||
                          path3.equals(path.objects()));

        // Skipped degree > max degree, degree >= max degree
        path = shortestPathAPI.get(1, 6, Direction.OUT,
                                   null, 5, 5L, 6L, -1L);
        Assert.assertEquals(4, path.size());
        Assert.assertEquals(path3, path.objects());
    }

    @Test
    public void testShortestPathWithIllegalArgs() {
        // The max depth shouldn't be 0 or negative
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, -1, 1L, 0L, 2L);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 0, 1L, 0L, 2L);
        });

        // The degree shouldn't be 0 or negative but NO_LIMIT(-1)
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 5, 0L, 0L, 2L);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 5, -3L, 0L, 2L);
        });

        // The skipped degree shouldn't be negative
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 5, 1L, -1L, 2L);
        });

        // The skipped degree shouldn't be >= capacity
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 5, 1L, 2L, 2L);
        });

        // The skipped degree shouldn't be < degree
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 5, 3L, 2L, 10L);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 5, -1L, 2L, 10L);
        });
    }
}
