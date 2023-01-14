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
import java.util.Set;

import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.PathsRequest;
import org.apache.hugegraph.structure.traverser.PathsWithVertices;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class PathsApiTest extends TraverserApiTest {

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
    public void testPathsGet() {
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
    public void testPathsGetWithLimit() {
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
    public void testPathsGetWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");

        Assert.assertThrows(ServerException.class, () -> {
            pathsAPI.get(markoId, rippleId, Direction.BOTH,
                         null, 3, -1L, 2L, 1);
        }, e -> {
            String expect = "Exceed capacity '2' while finding paths";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testPathsPost() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(3);
        PathsRequest request = builder.build();

        PathsWithVertices pathsWithVertices = pathsAPI.post(request);

        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId, rippleId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testPathsPostWithVertex() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(3);
        builder.withVertex(true);
        PathsRequest request = builder.build();

        PathsWithVertices pathsWithVertices = pathsAPI.post(request);

        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        List<Object> expectedIds = ImmutableList.of(markoId, lopId,
                                                    joshId, rippleId);
        List<List<Object>> expected = ImmutableList.of(
                expectedIds,
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        Set<Vertex> vertices = pathsWithVertices.vertices();
        Assert.assertEquals(4, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertTrue(expectedIds.contains(v.id()));
        }
    }

    @Test
    public void testPathsPostWithLabel() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(3);
        PathsRequest request = builder.build();

        PathsWithVertices pathsWithVertices = pathsAPI.post(request);

        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId, rippleId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH).labels("created");
        builder.maxDepth(3);
        request = builder.build();
        pathsWithVertices = pathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testPathsPostWithNearest() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");


        Vertex tom = graph().addVertex(T.LABEL, "person", "name", "Tom",
                                       "age", 29, "city", "Shanghai");
        Vertex jim = graph().addVertex(T.LABEL, "person", "name", "Jim",
                                       "age", 29, "city", "Shanghai");
        Vertex java = graph().addVertex(T.LABEL, "software", "name", "java",
                                        "lang", "java", "price", 199);
        Object tomId = tom.id();
        Object jimId = jim.id();
        Object javaId = java.id();
        graph().addEdge(tomId, "created", rippleId,
                        "date", "2016-01-10", "city", "Beijing");
        graph().addEdge(tomId, "created", javaId,
                        "date", "2017-01-10", "city", "Hongkong");
        graph().addEdge(jimId, "created", javaId,
                        "date", "2017-01-10", "city", "Hongkong");

        PathsRequest.Builder builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(jimId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(6);
        PathsRequest request = builder.build();

        PathsWithVertices pathsWithVertices = pathsAPI.post(request);

        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId,
                                 rippleId, tomId, javaId, jimId),
                ImmutableList.of(markoId, joshId, rippleId,
                                 tomId, javaId, jimId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(jimId);
        builder.step().direction(Direction.BOTH);
        builder.nearest(true);
        builder.maxDepth(6);
        request = builder.build();
        pathsWithVertices = pathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(markoId, joshId, rippleId,
                                 tomId, javaId, jimId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testPathsPostWithProperties() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(3);
        PathsRequest request = builder.build();

        PathsWithVertices pathsWithVertices = pathsAPI.post(request);

        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId, rippleId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(3);
        request = builder.build();
        pathsWithVertices = pathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }

    @Test
    public void testPathsWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        PathsRequest.Builder builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(3);
        PathsRequest request = builder.build();

        PathsWithVertices pathsWithVertices = pathsAPI.post(request);

        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(2, paths.size());
        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, lopId, joshId, rippleId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }

        builder = PathsRequest.builder();
        builder.sources().ids(markoId);
        builder.targets().ids(rippleId);
        builder.step().direction(Direction.BOTH);
        builder.limit(1);
        builder.maxDepth(3);
        request = builder.build();
        pathsWithVertices = pathsAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(1, paths.size());
        expected = ImmutableList.of(
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(expected.contains(path.objects()));
        }
    }
}
