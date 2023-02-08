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

import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class RingsRaysApiTest extends TraverserApiTest {

    @BeforeClass
    public static void prepareSchemaAndGraph() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initIndexLabel();
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();

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

        // Path length 5
        v1.addEdge("link", v2);
        v2.addEdge("link", v3);
        v3.addEdge("link", v1);
        v3.addEdge("link", v2);
    }

    @Test
    public void testRings() {
        Object markoId = getVertexId("person", "name", "marko");
        List<Path> paths = ringsAPI.get(markoId, Direction.BOTH, null,
                                        2, false, -1L, -1L, -1);
        Assert.assertEquals(0, paths.size());
    }

    @Test
    public void testRingsWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        List<Path> paths = ringsAPI.get(markoId, Direction.BOTH, null,
                                        3, false, -1L, -1L, 1);
        Assert.assertEquals(1, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, lopId, markoId);
        List<Object> path2 = ImmutableList.of(markoId, lopId, joshId, markoId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
    }

    @Test
    public void testRingsWithDepth() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        List<Path> paths = ringsAPI.get(markoId, Direction.BOTH, null,
                                        1, false, -1L, -1L, -1);
        Assert.assertEquals(0, paths.size());

        paths = ringsAPI.get(markoId, Direction.BOTH, null,
                             2, false, -1L, -1L, -1);
        Assert.assertEquals(0, paths.size());

        paths = ringsAPI.get(markoId, Direction.BOTH, null,
                             3, false, -1L, -1L, -1);
        Assert.assertEquals(1, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, lopId, markoId);
        List<Object> path2 = ImmutableList.of(markoId, lopId, joshId, markoId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
    }

    @Test
    public void testRingsWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");

        Assert.assertThrows(ServerException.class, () -> {
            ringsAPI.get(markoId, Direction.BOTH, null,
                         2, false, -1L, 1L, -1);
        }, e -> {
            String expect = "Exceed capacity '1' while finding rings";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testRingsSourceInRing() {
        List<Path> paths = ringsAPI.get(1, Direction.BOTH, null,
                                        3, true, -1L, -1L, -1);
        Assert.assertEquals(1, paths.size());
        List<Object> path1 = ImmutableList.of(1, 3, 2, 1);
        List<Object> path2 = ImmutableList.of(1, 2, 3, 1);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));

        paths = ringsAPI.get(2, Direction.OUT, null,
                             2, true, -1L, -1L, -1);
        Assert.assertEquals(1, paths.size());
        List<Object> path3 = ImmutableList.of(2, 3, 2);
        Assert.assertEquals(path3, paths.get(0).objects());

        paths = ringsAPI.get(2, Direction.BOTH, null,
                             2, true, -1L, -1L, -1);
        Assert.assertEquals(1, paths.size());
        Assert.assertEquals(path3, paths.get(0).objects());
    }

    @Test
    public void testRingsWithoutSourceInRing() {
        List<Path> paths = ringsAPI.get(1, Direction.BOTH, null,
                                        3, false, -1L, -1L, -1);
        Assert.assertEquals(3, paths.size());
        List<Object> path1 = ImmutableList.of(1, 3, 2, 3);
        List<Object> path2 = ImmutableList.of(1, 3, 2, 1);
        List<Object> path3 = ImmutableList.of(1, 2, 3, 1);
        List<Object> path4 = ImmutableList.of(1, 2, 3, 2);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3, path4);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));

        paths = ringsAPI.get(2, Direction.OUT, null,
                             3, false, -1L, -1L, -1);
        Assert.assertEquals(2, paths.size());
        List<Object> path5 = ImmutableList.of(2, 3, 2);
        List<Object> path6 = ImmutableList.of(2, 3, 1, 2);
        expectedPaths = ImmutableList.of(path5, path6);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        paths = ringsAPI.get(2, Direction.BOTH, null,
                             3, false, -1L, -1L, -1);
        Assert.assertEquals(2, paths.size());
        List<Object> path7 = ImmutableList.of(2, 3, 1, 2);
        List<Object> path8 = ImmutableList.of(2, 1, 3, 2);
        expectedPaths = ImmutableList.of(path5, path7, path8);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
    }

    @Test
    public void testRays() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = raysAPI.get(markoId, Direction.OUT, null,
                                       2, -1L, -1L, -1);
        Assert.assertEquals(4, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId);
        List<Object> path3 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path4 = ImmutableList.of(markoId, joshId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3, path4);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));
    }

    @Test
    public void testRaysWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");

        List<Path> paths = raysAPI.get(markoId, Direction.OUT, null,
                                       2, -1L, -1L, 2);
        Assert.assertEquals(2, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
    }

    @Test
    public void testRaysWithDepth() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = raysAPI.get(markoId, Direction.OUT, null,
                                       1, -1L, -1L, -1);
        Assert.assertEquals(3, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId);
        List<Object> path3 = ImmutableList.of(markoId, joshId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));

        paths = raysAPI.get(markoId, Direction.OUT, null,
                            2, -1L, -1L, -1);
        Assert.assertEquals(4, paths.size());
        List<Object> path4 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path5 = ImmutableList.of(markoId, joshId, lopId);
        expectedPaths = ImmutableList.of(path1, path2, path4, path5);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));

        paths = raysAPI.get(markoId, Direction.OUT, null,
                            3, -1L, -1L, -1);
        Assert.assertEquals(4, paths.size());
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));
    }

    @Test
    public void testRaysWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");

        Assert.assertThrows(ServerException.class, () -> {
            raysAPI.get(markoId, Direction.OUT, null,
                        2, -1L, 1L, -1);
        }, e -> {
            String expect = "Exceed capacity '1' while finding rays";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testRaysWithBoth() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = raysAPI.get(markoId, Direction.BOTH, null,
                                       1, -1L, -1L, -1);
        Assert.assertEquals(3, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId);
        List<Object> path3 = ImmutableList.of(markoId, joshId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));

        paths = raysAPI.get(markoId, Direction.BOTH, null,
                            2, -1L, -1L, -1);
        Assert.assertEquals(5, paths.size());
        List<Object> path4 = ImmutableList.of(markoId, vadasId);
        List<Object> path5 = ImmutableList.of(markoId, lopId, joshId);
        List<Object> path6 = ImmutableList.of(markoId, lopId, peterId);
        List<Object> path7 = ImmutableList.of(markoId, joshId, lopId);
        List<Object> path8 = ImmutableList.of(markoId, joshId, rippleId);
        expectedPaths = ImmutableList.of(path4, path5, path6, path7, path8);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(4).objects()));

        paths = raysAPI.get(markoId, Direction.BOTH, null,
                            3, -1L, -1L, -1);
        Assert.assertEquals(5, paths.size());
        List<Object> path9 = ImmutableList.of(markoId, vadasId);
        List<Object> path10 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path11 = ImmutableList.of(markoId, lopId, peterId);
        List<Object> path12 = ImmutableList.of(markoId, joshId, lopId, peterId);
        List<Object> path13 = ImmutableList.of(markoId, lopId,
                                               joshId, rippleId);
        expectedPaths = ImmutableList.of(path9, path10, path11, path12, path13);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(4).objects()));
    }
}
