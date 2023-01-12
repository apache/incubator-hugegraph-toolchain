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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.api.BaseApiTest;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.MultiNodeShortestPathRequest;
import org.apache.hugegraph.structure.traverser.PathsWithVertices;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class MultiNodeShortestPathApiTest extends TraverserApiTest {

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
    public void testMultiNodeShortestPath() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        MultiNodeShortestPathRequest.Builder builder =
                MultiNodeShortestPathRequest.builder();
        builder.vertices().ids(markoId, rippleId, joshId,
                               lopId, vadasId, peterId);
        builder.step().direction(Direction.BOTH);

        MultiNodeShortestPathRequest request = builder.build();
        PathsWithVertices pathsWithVertices =
                          multiNodeShortestPathAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(15, paths.size());

        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, joshId),
                ImmutableList.of(peterId, lopId),
                ImmutableList.of(peterId, lopId, markoId, vadasId),
                ImmutableList.of(peterId, lopId, joshId),
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(vadasId, markoId, lopId),
                ImmutableList.of(vadasId, markoId, joshId),
                ImmutableList.of(peterId, lopId, joshId, rippleId),
                ImmutableList.of(lopId, joshId, rippleId),
                ImmutableList.of(lopId, joshId),
                ImmutableList.of(rippleId, joshId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId),
                ImmutableList.of(vadasId, markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(contains(expected, path));
        }
    }

    @Test
    public void testMultiNodeShortestPathWithMaxDepth() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        MultiNodeShortestPathRequest.Builder builder =
                MultiNodeShortestPathRequest.builder();
        builder.vertices().ids(markoId, rippleId, joshId,
                               lopId, vadasId, peterId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);

        MultiNodeShortestPathRequest request = builder.build();
        PathsWithVertices pathsWithVertices =
                          multiNodeShortestPathAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(12, paths.size());

        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, joshId),
                ImmutableList.of(peterId, lopId),
                ImmutableList.of(peterId, lopId, joshId),
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(vadasId, markoId, lopId),
                ImmutableList.of(vadasId, markoId, joshId),
                ImmutableList.of(lopId, joshId, rippleId),
                ImmutableList.of(lopId, joshId),
                ImmutableList.of(rippleId, joshId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(contains(expected, path));
        }

        builder = MultiNodeShortestPathRequest.builder();
        builder.vertices().ids(markoId, rippleId, joshId,
                               lopId, vadasId, peterId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);

        request = builder.build();
        pathsWithVertices = multiNodeShortestPathAPI.post(request);
        paths = pathsWithVertices.paths();
        Assert.assertEquals(6, paths.size());

        expected = ImmutableList.of(
                ImmutableList.of(markoId, joshId),
                ImmutableList.of(peterId, lopId),
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(lopId, joshId),
                ImmutableList.of(rippleId, joshId),
                ImmutableList.of(markoId, lopId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(contains(expected, path));
        }
    }

    @Test
    public void testMultiNodeShortestPathWithVertex() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        MultiNodeShortestPathRequest.Builder builder =
                MultiNodeShortestPathRequest.builder();
        builder.vertices().ids(markoId, rippleId, joshId,
                               lopId, vadasId, peterId);
        builder.step().direction(Direction.BOTH);
        builder.withVertex(true);

        MultiNodeShortestPathRequest request = builder.build();
        PathsWithVertices pathsWithVertices =
                          multiNodeShortestPathAPI.post(request);
        List<PathsWithVertices.Paths> paths = pathsWithVertices.paths();
        Assert.assertEquals(15, paths.size());

        List<List<Object>> expected = ImmutableList.of(
                ImmutableList.of(markoId, joshId),
                ImmutableList.of(peterId, lopId),
                ImmutableList.of(peterId, lopId, markoId, vadasId),
                ImmutableList.of(peterId, lopId, joshId),
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(vadasId, markoId, lopId),
                ImmutableList.of(vadasId, markoId, joshId),
                ImmutableList.of(peterId, lopId, joshId, rippleId),
                ImmutableList.of(lopId, joshId, rippleId),
                ImmutableList.of(lopId, joshId),
                ImmutableList.of(rippleId, joshId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId),
                ImmutableList.of(vadasId, markoId, joshId, rippleId)
        );
        for (PathsWithVertices.Paths path : paths) {
            Assert.assertTrue(contains(expected, path));
        }

        Set<Vertex> vertices = pathsWithVertices.vertices();
        Assert.assertEquals(6, vertices.size());
        Set<Object> vertexIds = ImmutableSet.of(markoId, rippleId, joshId,
                                                lopId, vadasId, peterId);
        for (Vertex vertex : vertices) {
            Assert.assertTrue(vertexIds.contains(vertex.id()));
        }
    }

    private static boolean contains(List<List<Object>> expected,
                                    PathsWithVertices.Paths path) {
        List<Object> objects = path.objects();
        if (expected.contains(objects)) {
            return true;
        }
        Collections.reverse(objects);
        return expected.contains(objects);
    }
}
