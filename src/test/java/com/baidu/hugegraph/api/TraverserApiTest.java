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

package com.baidu.hugegraph.api;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TraverserApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchemaAndGraph() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();
    }

    @Test
    public void testShortestPath() {
        Object markoId = getVertexId("person", "name", "marko");
        Object lopId = getVertexId("software", "name", "lop");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        Path path = shortestPathAPI.get(markoId, lopId, Direction.OUT,
                                        null, 3);
        Assert.assertEquals(2, path.size());
        Assert.assertEquals(personId + ":marko", path.objects().get(0));
        Assert.assertEquals(softwareId + ":lop", path.objects().get(1));
    }

    @Test
    public void testPaths() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = pathsAPI.get(markoId, rippleId, Direction.BOTH,
                                        null, 3, 10);
        Assert.assertEquals(2, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path2 = ImmutableList.of(markoId, lopId, joshId, rippleId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
    }

    @Test
    public void testPathsWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = pathsAPI.get(markoId, rippleId, Direction.BOTH,
                                        null, 3, 1);
        Assert.assertEquals(1, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        Assert.assertEquals(path1, paths.get(0).objects());
    }

    @Test
    public void testCrosspoints() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        List<Path> paths = crosspointsAPI.get(markoId, peterId, Direction.OUT,
                                             null, 3, 10);
        Assert.assertEquals(2, paths.size());
        Path crosspoint1 = new Path(lopId,
                                    ImmutableList.of(markoId, lopId, peterId));
        Path crosspoint2 = new Path(lopId, ImmutableList.of(markoId, joshId,
                                                            lopId, peterId));

        List<Path> crosspoints = ImmutableList.of(crosspoint1, crosspoint2);
        Assert.assertTrue(crosspoints.contains(paths.get(0)));
        Assert.assertTrue(crosspoints.contains(paths.get(1)));
    }

    @Test
    public void testKoutNearest() {
        Object markoId = getVertexId("person", "name", "marko");

        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.OUT,
                                            null, 2, true);
        Assert.assertEquals(1, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutAll() {
        Object markoId = getVertexId("person", "name", "marko");

        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.OUT,
                                            null, 2, false);
        Assert.assertEquals(2, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutBothNearest() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.BOTH,
                                            null, 2, true);
        Assert.assertEquals(2, vertices.size());
        Assert.assertTrue(vertices.contains(personId + ":peter"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutBothAll() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.BOTH,
                                            null, 2, false);
        Assert.assertEquals(5, vertices.size());
        Assert.assertTrue(vertices.contains(personId + ":marko"));
        Assert.assertTrue(vertices.contains(personId + ":josh"));
        Assert.assertTrue(vertices.contains(personId + ":peter"));
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKneighbor() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = kneighborAPI.get(markoId, Direction.OUT,
                                                 null, 2);
        Assert.assertEquals(5, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
        Assert.assertTrue(vertices.contains(personId + ":vadas"));
        Assert.assertTrue(vertices.contains(personId + ":josh"));
        Assert.assertTrue(vertices.contains(personId + ":marko"));
    }

    @Test
    public void testVertices() {
        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Object> ids = ImmutableList.of(markoId, vadasId, joshId,
                                            peterId, lopId, rippleId);
        List<Vertex> vertices = verticesAPI.get(ids);

        Assert.assertEquals(6, vertices.size());

        Assert.assertEquals(markoId, vertices.get(0).id());
        Assert.assertEquals(vadasId, vertices.get(1).id());
        Assert.assertEquals(joshId, vertices.get(2).id());
        Assert.assertEquals(peterId, vertices.get(3).id());
        Assert.assertEquals(lopId, vertices.get(4).id());
        Assert.assertEquals(rippleId, vertices.get(5).id());

        Map<String, Object> props = ImmutableMap.of("name", "josh",
                                                    "city", "Beijing",
                                                    "age", 32);
        Assert.assertEquals(props, vertices.get(2).properties());
    }
}
