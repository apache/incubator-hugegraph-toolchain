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

public class AllShortestPathsApiTest extends TraverserApiTest {

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
        Vertex v19 = graph().addVertex(T.LABEL, "node", T.ID, 19);
        Vertex v20 = graph().addVertex(T.LABEL, "node", T.ID, 20);
        Vertex v21 = graph().addVertex(T.LABEL, "node", T.ID, 21);
        Vertex v22 = graph().addVertex(T.LABEL, "node", T.ID, 22);
        Vertex v23 = graph().addVertex(T.LABEL, "node", T.ID, 23);
        Vertex v24 = graph().addVertex(T.LABEL, "node", T.ID, 24);
        Vertex v25 = graph().addVertex(T.LABEL, "node", T.ID, 25);
        Vertex v26 = graph().addVertex(T.LABEL, "node", T.ID, 26);
        Vertex v27 = graph().addVertex(T.LABEL, "node", T.ID, 27);
        Vertex v28 = graph().addVertex(T.LABEL, "node", T.ID, 28);

        // Path length 5
        v1.addEdge("link", v2);
        v2.addEdge("link", v3);
        v3.addEdge("link", v4);
        v4.addEdge("link", v5);
        v5.addEdge("link", v6);

        v1.addEdge("link", v25);
        v25.addEdge("link", v26);
        v26.addEdge("link", v27);
        v27.addEdge("link", v28);
        v28.addEdge("link", v6);

        // Path length 4
        v1.addEdge("link", v7);
        v7.addEdge("link", v8);
        v8.addEdge("link", v9);
        v9.addEdge("link", v6);

        // Path length 3
        v1.addEdge("link", v10);
        v10.addEdge("link", v11);
        v11.addEdge("link", v6);

        v1.addEdge("link", v19);
        v19.addEdge("link", v20);
        v20.addEdge("link", v6);

        v1.addEdge("link", v21);
        v21.addEdge("link", v22);
        v22.addEdge("link", v6);

        v1.addEdge("link", v23);
        v23.addEdge("link", v24);
        v24.addEdge("link", v6);

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
    public void testAllShortestPath() {
        List<Path> paths = allShortestPathsAPI.get(1, 6, Direction.BOTH,
                                                   null, 6, -1L, 0L, -1L);
        Assert.assertEquals(4, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 10, 11, 6),
                ImmutableList.of(1, 19, 20, 6),
                ImmutableList.of(1, 21, 22, 6),
                ImmutableList.of(1, 23, 24, 6)
        );
        for (Path path : paths){
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testAllShortestPathWithLabel() {
        List<Path> paths = allShortestPathsAPI.get(1, 6, Direction.BOTH,
                                                   "link", 6, -1L, 0L,
                                                   -1L);
        Assert.assertEquals(4, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 10, 11, 6),
                ImmutableList.of(1, 19, 20, 6),
                ImmutableList.of(1, 21, 22, 6),
                ImmutableList.of(1, 23, 24, 6)
        );
        for (Path path : paths){
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testAllShortestPathWithDegree() {
        List<Path> paths = allShortestPathsAPI.get(1, 6, Direction.OUT,
                                                   null, 6, 1L, 0L, -1L);
        /*
         * Following results can be guaranteed in RocksDB backend,
         * but different results exist in table type backend(like Cassandra).
         */
        Assert.assertEquals(1, paths.size());
        List<Object> expected = ImmutableList.of(1, 2, 3, 4, 5, 6);
        Assert.assertEquals(expected, paths.iterator().next().objects());
    }

    @Test
    public void testAllShortestPathWithCapacity() {
        List<Path> paths = allShortestPathsAPI.get(1, 6, Direction.BOTH,
                                                   null, 6, -1L, 0L, -1L);
        Assert.assertEquals(4, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(1, 10, 11, 6),
                ImmutableList.of(1, 19, 20, 6),
                ImmutableList.of(1, 21, 22, 6),
                ImmutableList.of(1, 23, 24, 6)
        );
        for (Path path : paths){
            Assert.assertTrue(expected.contains(path.objects()));
        }
        Assert.assertThrows(ServerException.class, () -> {
            allShortestPathsAPI.get(1, 6, Direction.BOTH, null, 6, 6,
                                    0L, 10L);
        }, e -> {
            String expect = "Exceed capacity '10' while finding shortest path";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testAllShortestPathWithMaxDepth() {
        List<Path> paths = allShortestPathsAPI.get(14, 6, Direction.BOTH,
                                                   null, 4, 5L, 0L, 19L);
        Assert.assertEquals(1, paths.size());
        Path path = paths.get(0);
        Assert.assertEquals(ImmutableList.of(14, 7, 8, 9, 6), path.objects());

        paths = allShortestPathsAPI.get(14, 6, Direction.BOTH,
                                        null, 3, 5L, 0L, 19L);
        Assert.assertEquals(0, paths.size());
    }

    @Test
    public void testAllShortestPathWithIllegalArgs() {
        // The max depth shouldn't be 0 or negative
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, -1, 1L, 0L, 2L);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            shortestPathAPI.get("a", "b", Direction.BOTH,
                                null, 0, 1L, 0L, 2L);
        });

        // The degree shouldn't be 0 or negative but NO_LIMIT(-1)
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, 5, 0L, 0L, 2L);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, 5, -3L, 0L, 2L);
        });

        // The skipped degree shouldn't be negative
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, 5, 1L, -1L, 2L);
        });

        // The skipped degree shouldn't be >= capacity
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, 5, 1L, 2L, 2L);
        });

        // The skipped degree shouldn't be < degree
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, 5, 3L, 2L, 10L);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            allShortestPathsAPI.get("a", "b", Direction.BOTH,
                                    null, 5, -1L, 2L, 10L);
        });
    }
}
