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
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.Kneighbor;
import org.apache.hugegraph.structure.traverser.KneighborRequest;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class KneighborApiTest extends TraverserApiTest {

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
    public void testKneighborGet() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = kneighborAPI.get(markoId, Direction.OUT,
                                                 null, 2, -1L, -1);
        Assert.assertEquals(4, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
        Assert.assertTrue(vertices.contains(personId + ":vadas"));
        Assert.assertTrue(vertices.contains(personId + ":josh"));
    }

    @Test
    public void testKneighborPost() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(5, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId, peterId, rippleId);
        Assert.assertEquals(expected, kneighborResult.ids());
    }

    @Test
    public void testKneighborPostWithPath() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.withPath(true);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());
        Assert.assertEquals(3, kneighborResult.paths().size());
        List<Object> expectedPaths = ImmutableList.of(
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, joshId)
        );
        for (Path path : kneighborResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.withPath(true);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(5, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, peterId, joshId, lopId, rippleId);
        Assert.assertEquals(expected, kneighborResult.ids());
        Assert.assertEquals(5, kneighborResult.paths().size());
        expectedPaths = ImmutableList.of(
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, joshId),
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (Path path : kneighborResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }
    }

    @Test
    public void testKneighborPostWithVertex() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.withPath(false);
        builder.withVertex(true);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());
        Assert.assertEquals(3, kneighborResult.vertices().size());
        Set<Object> expectedVids = ImmutableSet.of(vadasId, lopId, joshId);
        for (Vertex vertex : kneighborResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.withPath(false);
        builder.withVertex(true);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(5, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId, peterId, rippleId);
        Assert.assertEquals(expected, kneighborResult.ids());
        Assert.assertEquals(5, kneighborResult.vertices().size());
        expectedVids = ImmutableSet.of(vadasId, lopId, joshId,
                                       peterId, rippleId);
        for (Vertex vertex : kneighborResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.withPath(true);
        builder.withVertex(true);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());
        Assert.assertEquals(3, kneighborResult.paths().size());
        Set<List<Object>> expectedPaths = ImmutableSet.of(
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, joshId)
        );
        for (Path path : kneighborResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }
        Assert.assertEquals(4, kneighborResult.vertices().size());
        expectedVids = ImmutableSet.of(markoId, vadasId, lopId, joshId);
        for (Vertex vertex : kneighborResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.withPath(true);
        builder.withVertex(true);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(5, kneighborResult.size());
        expected = ImmutableSet.of(peterId, lopId, joshId, rippleId, vadasId);
        Assert.assertEquals(expected, kneighborResult.ids());
        Assert.assertEquals(5, kneighborResult.paths().size());
        expectedPaths = ImmutableSet.of(
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, joshId),
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (Path path : kneighborResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }
        Assert.assertEquals(6, kneighborResult.vertices().size());
        expectedVids = ImmutableSet.of(markoId, peterId, lopId,
                                       joshId, rippleId, vadasId);
        for (Vertex vertex : kneighborResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }
    }

    @Test
    public void testKneighborPostWithLabel() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("created");
        builder.maxDepth(1);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(1, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(lopId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("created");
        builder.maxDepth(2);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        expected = ImmutableSet.of(lopId, peterId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("knows");
        builder.maxDepth(1);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(2, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("knows");
        builder.maxDepth(2);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(2, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());
    }

    @Test
    public void testKneighborPostWithDirection() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.OUT);
        builder.maxDepth(1);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.OUT);
        builder.maxDepth(2);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(4, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId, rippleId);
        Assert.assertEquals(expected, kneighborResult.ids());
    }

    @Test
    public void testKneighborPostWithProperties() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(1);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(1, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(lopId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(2);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        expected = ImmutableSet.of(lopId, peterId, joshId);
        Assert.assertEquals(expected, kneighborResult.ids());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(3);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(4, kneighborResult.size());
        expected = ImmutableSet.of(lopId, peterId, joshId, rippleId);
        Assert.assertEquals(expected, kneighborResult.ids());
    }

    @Test
    public void testKneighborPostWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        // 1 depth with in&out edges
        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.limit(3);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertTrue(expected.containsAll(kneighborResult.ids()));

        // 2 depth with out edges
        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.OUT);
        builder.maxDepth(2);
        builder.limit(5);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(4, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId, rippleId);
        Assert.assertTrue(expected.containsAll(kneighborResult.ids()));

        // 2 depth with in&out edges
        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.limit(5);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(5, kneighborResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId, peterId, rippleId);
        Assert.assertTrue(expected.containsAll(kneighborResult.ids()));
    }

    @Test
    public void testKneighborPostWithCountOnly() {
        Object markoId = getVertexId("person", "name", "marko");

        KneighborRequest.Builder builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.countOnly(true);
        KneighborRequest request = builder.build();

        Kneighbor kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(3, kneighborResult.size());
        Assert.assertTrue(kneighborResult.ids().isEmpty());
        Assert.assertTrue(kneighborResult.paths().isEmpty());
        Assert.assertTrue(kneighborResult.vertices().isEmpty());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.countOnly(true);
        request = builder.build();

        kneighborResult = kneighborAPI.post(request);

        Assert.assertEquals(5, kneighborResult.size());
        Assert.assertTrue(kneighborResult.ids().isEmpty());
        Assert.assertTrue(kneighborResult.paths().isEmpty());
        Assert.assertTrue(kneighborResult.vertices().isEmpty());

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.countOnly(true);
        builder.withPath(true);

        KneighborRequest.Builder finalBuilder = builder;
        Assert.assertThrows(IllegalArgumentException.class, ()-> {
            finalBuilder.build();
        });

        builder = KneighborRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.countOnly(true);
        builder.withVertex(true);

        KneighborRequest.Builder finalBuilder1 = builder;
        Assert.assertThrows(IllegalArgumentException.class, ()-> {
            finalBuilder1.build();
        });
    }
}
