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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.api.traverser.structure.CrosspointsRequest;
import com.baidu.hugegraph.api.traverser.structure.CustomizedCrosspoints;
import com.baidu.hugegraph.api.traverser.structure.CustomizedPaths;
import com.baidu.hugegraph.api.traverser.structure.PathsRequest;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Edges;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Shard;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.graph.Vertices;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static com.baidu.hugegraph.structure.constant.Traverser.DEFAULT_PAGE_LIMIT;

public class TraverserApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchemaAndGraph() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initIndexLabel();
        BaseApiTest.initVertex();
        BaseApiTest.initEdge();
    }

    @Test
    public void testShortestPath() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");

        Path path = shortestPathAPI.get(markoId, rippleId, Direction.BOTH,
                                        null, 3, -1L, -1L);
        Assert.assertEquals(3, path.size());
        Assert.assertEquals(markoId, path.objects().get(0));
        Assert.assertEquals(joshId, path.objects().get(1));
        Assert.assertEquals(rippleId, path.objects().get(2));
    }

    @Test
    public void testShortestPathWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");

        Assert.assertThrows(ServerException.class, () -> {
            shortestPathAPI.get(markoId, rippleId, Direction.BOTH,
                                null, 3, -1L, 1L);
        });
    }

    @Test
    public void testPaths() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = pathsAPI.get(markoId, rippleId, Direction.BOTH,
                                        null, 3, -1L, -1L, 10);
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
                                        null, 3, -1L, -1L, 1);
        Assert.assertEquals(1, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        Assert.assertEquals(path1, paths.get(0).objects());
    }

    @Test
    public void testPathsWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");

        Assert.assertThrows(ServerException.class, () -> {
            pathsAPI.get(markoId, rippleId, Direction.BOTH,
                         null, 3, -1L, 2L, 1);
        });
    }

    @Test
    public void testCrosspoints() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        List<Path> paths = crosspointsAPI.get(markoId, peterId, Direction.OUT,
                                              null, 3, -1L, -1L, 10);
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
    public void testCrosspointsWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");
        Object peterId = getVertexId("person", "name", "peter");

        Assert.assertThrows(ServerException.class, () -> {
            crosspointsAPI.get(markoId, peterId, Direction.OUT,
                               null, 3, -1L, 2L, 10);
        });
    }

    @Test
    public void testKoutNearest() {
        Object markoId = getVertexId("person", "name", "marko");

        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.OUT,
                                            null, 2, true, -1L, -1L, -1L);
        Assert.assertEquals(1, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutAll() {
        Object markoId = getVertexId("person", "name", "marko");

        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.OUT, null,
                                            2, false, -1L, -1L, -1L);
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
                                            null, 2, true, -1L, -1L, -1L);
        Assert.assertEquals(2, vertices.size());
        Assert.assertTrue(vertices.contains(personId + ":peter"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutBothAll() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.BOTH, null,
                                            2, false, -1L, -1L, -1L);
        Assert.assertEquals(5, vertices.size());
        Assert.assertTrue(vertices.contains(personId + ":marko"));
        Assert.assertTrue(vertices.contains(personId + ":josh"));
        Assert.assertTrue(vertices.contains(personId + ":peter"));
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutBothAllWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");

        Assert.assertThrows(ServerException.class, () -> {
            koutAPI.get(markoId, Direction.BOTH, null,
                        2, false, -1L, -1L, 1L);
        });
    }

    @Test
    public void testKneighbor() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = kneighborAPI.get(markoId, Direction.OUT,
                                                 null, 2, -1L, -1L);
        Assert.assertEquals(5, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
        Assert.assertTrue(vertices.contains(personId + ":vadas"));
        Assert.assertTrue(vertices.contains(personId + ":josh"));
        Assert.assertTrue(vertices.contains(personId + ":marko"));
    }

    @Test
    public void testRings() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");

        List<Path> paths = ringsAPI.get(markoId, Direction.BOTH, null,
                                        2, -1L, -1L, -1L);
        Assert.assertEquals(3, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, markoId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId, markoId);
        List<Object> path3 = ImmutableList.of(markoId, lopId, markoId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
    }

    @Test
    public void testRingsWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");

        List<Path> paths = ringsAPI.get(markoId, Direction.BOTH, null,
                                        2, -1L, -1L, 2L);
        Assert.assertEquals(2, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, markoId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId, markoId);
        List<Object> path3 = ImmutableList.of(markoId, lopId, markoId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
    }

    @Test
    public void testRingsWithDepth() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = ringsAPI.get(markoId, Direction.BOTH, null,
                                        1, -1L, -1L, -1L);
        Assert.assertEquals(0, paths.size());

        paths = ringsAPI.get(markoId, Direction.BOTH, null,
                             2, -1L, -1L, -1L);
        Assert.assertEquals(3, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, joshId, markoId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId, markoId);
        List<Object> path3 = ImmutableList.of(markoId, lopId, markoId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2,
                                                            path3);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));

        paths = ringsAPI.get(markoId, Direction.BOTH, null,
                             3, -1L, -1L, -1L);

        Assert.assertEquals(9, paths.size());
        List<Object> path4 = ImmutableList.of(markoId, joshId, lopId, joshId);
        List<Object> path5 = ImmutableList.of(markoId, joshId, lopId, markoId);
        List<Object> path6 = ImmutableList.of(markoId, lopId, peterId, lopId);
        List<Object> path7 = ImmutableList.of(markoId, joshId, rippleId,
                                              joshId);
        List<Object> path8 = ImmutableList.of(markoId, lopId, joshId, markoId);
        List<Object> path9 = ImmutableList.of(markoId, lopId, joshId, lopId);
        expectedPaths = ImmutableList.of(path1, path2, path3, path4, path5,
                                         path6, path7, path8, path9);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(4).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(5).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(6).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(7).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(8).objects()));
    }

    @Test
    public void testRingsWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");

        Assert.assertThrows(ServerException.class, () -> {
            ringsAPI.get(markoId, Direction.BOTH, null,
                         2, -1L, 1L, -1L);
        });
    }

    @Test
    public void testRays() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Path> paths = raysAPI.get(markoId, Direction.OUT, null,
                                       2, -1L, -1L, -1L);
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
                                       2, -1L, -1L, 2L);
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
                                       1, -1L, -1L, -1L);
        Assert.assertEquals(2, paths.size());
        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, vadasId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        paths = raysAPI.get(markoId, Direction.OUT, null,
                            2, -1L, -1L, -1L);
        Assert.assertEquals(4, paths.size());
        List<Object> path3 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path4 = ImmutableList.of(markoId, joshId, lopId);
        expectedPaths = ImmutableList.of(path1, path2, path3, path4);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(2).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(3).objects()));

        paths = raysAPI.get(markoId, Direction.OUT, null,
                            3, -1L, -1L, -1L);
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
                        2, -1L, 1L, -1L);
        });
    }

    @Test
    public void testCustomizedPathsSourceLabelProperty() {
        initEdgesWithWeights();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = new PathsRequest.Builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
                       .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1);
        builder.sortBy(PathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        PathsRequest request = builder.build();

        CustomizedPaths customizedPaths = customizedPathsAPI.post(request);
        List<CustomizedPaths.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, rippleId);
        Assert.assertEquals(path1, paths.get(0).objects());
        Assert.assertEquals(path2, paths.get(1).objects());

        List<Double> weights1 = ImmutableList.of(1.0D, 0.4D);
        List<Double> weights2 = ImmutableList.of(1.0D, 1.0D);
        Assert.assertEquals(weights1, paths.get(0).weights());
        Assert.assertEquals(weights2, paths.get(1).weights());

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(4, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId,
                                                  rippleId, joshId);
        Assert.assertEquals(expectedVertices, vertices);
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsSourceIds() {
        initEdgesWithWeights();

        Object markoId = getVertexId("person", "name", "marko");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        PathsRequest.Builder builder = new PathsRequest.Builder();
        builder.sources().ids(markoId, peterId);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1);
        builder.sortBy(PathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        PathsRequest request = builder.build();

        CustomizedPaths customizedPaths = customizedPathsAPI.post(request);
        List<CustomizedPaths.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(peterId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        List<Double> weights1 = ImmutableList.of(0.2D);
        List<Double> weights2 = ImmutableList.of(0.4D);
        List<List<Double>> expectedWeights = ImmutableList.of(weights1,
                                                              weights2);
        Assert.assertTrue(expectedWeights.contains(paths.get(0).weights()));
        Assert.assertTrue(expectedWeights.contains(paths.get(1).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId, peterId);
        Assert.assertEquals(expectedVertices, vertices);
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsSourceLabelPropertyMultiValue() {
        initEdgesWithWeights();

        Object markoId = getVertexId("person", "name", "marko");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        PathsRequest.Builder builder = new PathsRequest.Builder();
        List<String> names = ImmutableList.of("marko", "peter");
        builder.sources().label("person").property("name", names);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1);
        builder.sortBy(PathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        PathsRequest request = builder.build();

        CustomizedPaths customizedPaths = customizedPathsAPI.post(request);
        List<CustomizedPaths.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, lopId);
        List<Object> path2 = ImmutableList.of(peterId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        List<Double> weights1 = ImmutableList.of(0.2D);
        List<Double> weights2 = ImmutableList.of(0.4D);
        List<List<Double>> expectedWeights = ImmutableList.of(weights1,
                                                              weights2);
        Assert.assertTrue(expectedWeights.contains(paths.get(0).weights()));
        Assert.assertTrue(expectedWeights.contains(paths.get(1).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId, peterId);
        Assert.assertEquals(expectedVertices, vertices);
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsWithSample() {
        initEdgesWithWeights();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = new PathsRequest.Builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
                       .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1).sample(1);
        builder.sortBy(PathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(-1);
        PathsRequest request = builder.build();

        CustomizedPaths customizedPaths = customizedPathsAPI.post(request);
        List<CustomizedPaths.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(1, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, lopId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));

        List<Double> weights1 = ImmutableList.of(1D, 0.4D);
        List<Double> weights2 = ImmutableList.of(1D, 1D);

        Assert.assertTrue(weights1.equals(paths.get(0).weights()) ||
                          weights2.equals(paths.get(0).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Assert.assertTrue(path1.containsAll(vertices) ||
                          path2.containsAll(vertices));
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsWithDecr() {
        initEdgesWithWeights();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = new PathsRequest.Builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
                       .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1);
        builder.sortBy(PathsRequest.SortBy.DECR).withVertex(true)
               .capacity(-1).limit(-1);
        PathsRequest request = builder.build();

        CustomizedPaths customizedPaths = customizedPathsAPI.post(request);
        List<CustomizedPaths.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, rippleId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, lopId);
        Assert.assertEquals(path1, paths.get(0).objects());
        Assert.assertEquals(path2, paths.get(1).objects());

        List<Double> weights1 = ImmutableList.of(1.0D, 1.0D);
        List<Double> weights2 = ImmutableList.of(1.0D, 0.4D);
        Assert.assertEquals(weights1, paths.get(0).weights());
        Assert.assertEquals(weights2, paths.get(1).weights());

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(4, vertices.size());
        Set<?> expectedVertices = ImmutableSet.of(markoId, lopId,
                                                  rippleId, joshId);
        Assert.assertEquals(expectedVertices, vertices);
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsWithLimit() {
        initEdgesWithWeights();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = new PathsRequest.Builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
                       .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1);
        builder.sortBy(PathsRequest.SortBy.INCR).withVertex(true)
               .capacity(-1).limit(1);
        PathsRequest request = builder.build();

        CustomizedPaths customizedPaths = customizedPathsAPI.post(request);
        List<CustomizedPaths.Paths> paths = customizedPaths.paths();

        Assert.assertEquals(1, paths.size());

        List<Object> path1 = ImmutableList.of(markoId, joshId, lopId);
        List<Object> path2 = ImmutableList.of(markoId, joshId, rippleId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));

        List<Double> weights1 = ImmutableList.of(1.0D, 0.4D);
        List<Double> weights2 = ImmutableList.of(1.0D, 1.0D);
        List<List<Double>> expectedWeights = ImmutableList.of(weights1,
                                                              weights2);
        Assert.assertTrue(expectedWeights.contains(paths.get(0).weights()));

        Set<?> vertices = customizedPaths.vertices().stream()
                                         .map(Vertex::id)
                                         .collect(Collectors.toSet());
        Assert.assertEquals(3, vertices.size());
        Set<?> vertices1 = ImmutableSet.of(markoId, lopId, joshId);
        Set<?> vertices2 = ImmutableSet.of(markoId, lopId, rippleId);
        Set<Set<?>> expectedVertices = ImmutableSet.of(vertices1, vertices2);
        Assert.assertTrue(expectedVertices.contains(vertices));
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedPathsWithCapacity() {
        initEdgesWithWeights();

        PathsRequest.Builder builder = new PathsRequest.Builder();
        builder.sources().label("person").property("name", "marko");
        builder.steps().direction(Direction.OUT).labels("knows1")
                       .weightBy("weight").degree(-1);
        builder.steps().direction(Direction.OUT).labels("created1")
                       .weightBy("weight").degree(-1);
        builder.sortBy(PathsRequest.SortBy.INCR).withVertex(true)
               .capacity(1).limit(-1);
        PathsRequest request = builder.build();

        Assert.assertThrows(ServerException.class, () -> {
            customizedPathsAPI.post(request);
        });
        removeEdgesWithWeights();
    }

    @Test
    public void testCustomizedCrosspoints() {
        Object lopId = getVertexId("software", "name", "lop");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");

        CrosspointsRequest.Builder builder = new CrosspointsRequest.Builder();
        builder.sources().ids(lopId, rippleId);
        builder.pathPatterns().steps().direction(Direction.IN)
                                      .labels("created").degree(-1);
        builder.withPath(true).withVertex(true).capacity(-1).limit(-1);

        CustomizedCrosspoints customizedCrosspoints =
                              customizedCrosspointsAPI.post(builder.build());
        List<Object> crosspoints = customizedCrosspoints.crosspoints();
        Assert.assertEquals(1, crosspoints.size());
        Assert.assertEquals(joshId, crosspoints.get(0));

        List<Path> paths = customizedCrosspoints.paths();
        Assert.assertEquals(2, paths.size());

        List<Object> path1 = ImmutableList.of(rippleId, joshId);
        List<Object> path2 = ImmutableList.of(lopId, joshId);
        List<List<Object>> expectedPaths = ImmutableList.of(path1, path2);
        Assert.assertTrue(expectedPaths.contains(paths.get(0).objects()));
        Assert.assertTrue(expectedPaths.contains(paths.get(1).objects()));

        Set<?> vertices = customizedCrosspoints.vertices().stream()
                                               .map(Vertex::id)
                                               .collect(Collectors.toSet());
        List<Object> expectedVids = ImmutableList.of(rippleId, joshId, lopId);
        Assert.assertTrue(expectedVids.containsAll(vertices));
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
        List<Vertex> vertices = verticesAPI.list(ids);

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

    @Test
    public void testEdges() {
        String date2012Id = getEdgeId("knows", "date", "20120110");
        String date2013Id = getEdgeId("knows", "date", "20130110");
        String date2014Id = getEdgeId("created", "date", "20140110");
        String date2015Id = getEdgeId("created", "date", "20150110");
        String date2016Id = getEdgeId("created", "date", "20160110");
        String date2017Id = getEdgeId("created", "date", "20170110");

        List<String> ids = ImmutableList.of(date2012Id, date2013Id, date2014Id,
                                            date2015Id, date2016Id, date2017Id);

        List<Edge> edges = edgesAPI.list(ids);

        Assert.assertEquals(6, edges.size());

        Assert.assertEquals(date2012Id, edges.get(0).id());
        Assert.assertEquals(date2013Id, edges.get(1).id());
        Assert.assertEquals(date2014Id, edges.get(2).id());
        Assert.assertEquals(date2015Id, edges.get(3).id());
        Assert.assertEquals(date2016Id, edges.get(4).id());
        Assert.assertEquals(date2017Id, edges.get(5).id());

        Map<String, Object> props = ImmutableMap.of("date", "20140110",
                                                    "city", "Shanghai");
        Assert.assertEquals(props, edges.get(2).properties());
    }

    @Test
    public void testScanVertex() {
        List<Shard> shards = verticesAPI.shards(1 * 1024 * 1024);
        List<Vertex> vertices = new LinkedList<>();
        for (Shard shard : shards) {
            Vertices results = verticesAPI.scan(shard, null, 0L);
            vertices.addAll(ImmutableList.copyOf(results.results()));
            Assert.assertNull(results.page());
        }
        Assert.assertEquals(6, vertices.size());
    }

    @Test
    public void testScanVertexInPaging() {
        List<Shard> shards = verticesAPI.shards(1 * 1024 * 1024);
        List<Vertex> vertices = new LinkedList<>();
        for (Shard shard : shards) {
            String page = "";
            while (page != null) {
                Vertices results = verticesAPI.scan(shard, page, DEFAULT_PAGE_LIMIT);
                vertices.addAll(ImmutableList.copyOf(results.results()));
                page = results.page();
            }
        }
        Assert.assertEquals(6, vertices.size());
    }

    @Test
    public void testScanVertexInPagingWithNegativeLimit() {
        List<Shard> shards = verticesAPI.shards(1 * 1024 * 1024);
        for (Shard shard : shards) {
            String page = "";
            Assert.assertThrows(ServerException.class, () -> {
                verticesAPI.scan(shard, page, -1);
            });
        }
    }

    @Test
    public void testScanVertexWithSplitSizeLt1MB() {
        Assert.assertThrows(ServerException.class, () -> {
            verticesAPI.shards(1 * 1024 * 1024 - 1);
        });
    }

    @Test
    public void testScanEdge() {
        List<Shard> shards = edgesAPI.shards(1 * 1024 * 1024);
        List<Edge> edges = new LinkedList<>();
        for (Shard shard : shards) {
            Edges results = edgesAPI.scan(shard, null, 0L);
            Assert.assertNull(results.page());
            edges.addAll(ImmutableList.copyOf(results.results()));
        }
        Assert.assertEquals(6, edges.size());
    }

    @Test
    public void testScanEdgeInPaging() {
        List<Shard> shards = edgesAPI.shards(1 * 1024 * 1024);
        List<Edge> edges = new LinkedList<>();
        for (Shard shard : shards) {
            String page = "";
            while (page != null) {
                Edges results = edgesAPI.scan(shard, page, DEFAULT_PAGE_LIMIT);
                edges.addAll(ImmutableList.copyOf(results.results()));
                page = results.page();
            }
        }
        Assert.assertEquals(6, edges.size());
    }

    @Test
    public void testScanEdgeInPagingWithNegativeLimit() {
        List<Shard> shards = edgesAPI.shards(1 * 1024 * 1024);
        for (Shard shard : shards) {
            String page = "";
            Assert.assertThrows(ServerException.class, () -> {
                edgesAPI.scan(shard, page, -1);
            });
        }
    }

    @Test
    public void testScanEdgeWithSplitSizeLt1MB() {
        Assert.assertThrows(ServerException.class, () -> {
            edgesAPI.shards(1 * 1024 * 1024 - 1);
        });
    }


    private static void initEdgesWithWeights() {
        SchemaManager schema = schema();
        schema.edgeLabel("knows1")
              .sourceLabel("person")
              .targetLabel("person")
              .properties("date", "weight")
              .nullableKeys("weight")
              .ifNotExist()
              .create();

        schema.edgeLabel("created1")
              .sourceLabel("person")
              .targetLabel("software")
              .properties("date", "weight")
              .nullableKeys("weight")
              .ifNotExist()
              .create();

        Vertex marko = getVertex("person", "name", "marko");
        Vertex vadas = getVertex("person", "name", "vadas");
        Vertex lop = getVertex("software", "name", "lop");
        Vertex josh = getVertex("person", "name", "josh");
        Vertex ripple = getVertex("software", "name", "ripple");
        Vertex peter = getVertex("person", "name", "peter");

        marko.addEdge("knows1", vadas, "date", "20160110", "weight", 0.5);
        marko.addEdge("knows1", josh, "date", "20130220", "weight", 1.0);
        marko.addEdge("created1", lop, "date", "20171210", "weight", 0.4);
        josh.addEdge("created1", lop, "date", "20091111", "weight", 0.4);
        josh.addEdge("created1", ripple, "date", "20171210", "weight", 1.0);
        peter.addEdge("created1", lop, "date", "20170324", "weight", 0.2);
    }

    private static void removeEdgesWithWeights() {
        List<Long> elTaskIds = new ArrayList<>();
        EdgeLabel knows1 = schema().getEdgeLabel("knows1");
        EdgeLabel created1 = schema().getEdgeLabel("created1");
        ImmutableList.of(knows1, created1).forEach(edgeLabel -> {
            elTaskIds.add(edgeLabelAPI.delete(edgeLabel.name()));
        });
        elTaskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
    }
}
