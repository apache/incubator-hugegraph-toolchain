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
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.Kout;
import org.apache.hugegraph.structure.traverser.KoutRequest;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class KoutApiTest extends TraverserApiTest {

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
    public void testKoutGetNearest() {
        Object markoId = getVertexId("person", "name", "marko");

        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.OUT,
                                            null, 2, true, -1L, -1L, -1);
        Assert.assertEquals(1, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutGetAll() {
        Object markoId = getVertexId("person", "name", "marko");

        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.OUT, null,
                                            2, false, -1L, -1L, -1);
        Assert.assertEquals(2, vertices.size());
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutGetBothNearest() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.BOTH,
                                            null, 2, true, -1L, -1L, -1);
        Assert.assertEquals(2, vertices.size());
        Assert.assertTrue(vertices.contains(personId + ":peter"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutGetBothAll() {
        Object markoId = getVertexId("person", "name", "marko");

        long personId = vertexLabelAPI.get("person").id();
        long softwareId = vertexLabelAPI.get("software").id();

        List<Object> vertices = koutAPI.get(markoId, Direction.BOTH, null,
                                            2, false, -1L, -1L, -1);
        Assert.assertEquals(4, vertices.size());
        Assert.assertTrue(vertices.contains(personId + ":josh"));
        Assert.assertTrue(vertices.contains(personId + ":peter"));
        Assert.assertTrue(vertices.contains(softwareId + ":lop"));
        Assert.assertTrue(vertices.contains(softwareId + ":ripple"));
    }

    @Test
    public void testKoutGetBothAllWithCapacity() {
        Object markoId = getVertexId("person", "name", "marko");

        Assert.assertThrows(ServerException.class, () -> {
            koutAPI.get(markoId, Direction.BOTH, null,
                        2, false, -1L, 1L, 2);
        }, e -> {
            String expect = "Capacity can't be less than limit, " +
                            "but got capacity '1' and limit '2'";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testKoutGetBothAllWithCapacityNoLimit() {
        Object markoId = getVertexId("person", "name", "marko");

        Assert.assertThrows(ServerException.class, () -> {
            koutAPI.get(markoId, Direction.BOTH, null,
                    2, false, -1L, 1L, -1);
        }, e -> {
            String expect = "Capacity can't be less than limit, " +
                            "but got capacity '1' and limit '-1'";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testKoutPost() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId);
        Assert.assertEquals(expected, koutResult.ids());
    }

    @Test
    public void testKoutPostWithNearest() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.nearest(false);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.nearest(false);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(4, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());
    }

    @Test
    public void testKoutPostWithPath() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.withPath(true);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());
        Assert.assertEquals(3, koutResult.paths().size());
        List<Object> expectedPaths = ImmutableList.of(
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, joshId)
        );
        for (Path path : koutResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.withPath(true);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId);
        Assert.assertEquals(expected, koutResult.ids());
        Assert.assertEquals(2, koutResult.paths().size());
        expectedPaths = ImmutableList.of(
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (Path path : koutResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }
    }

    @Test
    public void testKoutPostWithVertex() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.withPath(false);
        builder.withVertex(true);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());
        Assert.assertEquals(3, koutResult.vertices().size());
        Set<Object> expectedVids = ImmutableSet.of(vadasId, lopId, joshId);
        for (Vertex vertex : koutResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.withPath(false);
        builder.withVertex(true);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId);
        Assert.assertEquals(expected, koutResult.ids());
        Assert.assertEquals(2, koutResult.vertices().size());
        expectedVids = ImmutableSet.of(peterId, rippleId);
        for (Vertex vertex : koutResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.withPath(true);
        builder.withVertex(true);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());
        Assert.assertEquals(3, koutResult.paths().size());
        Set<List<Object>> expectedPaths = ImmutableSet.of(
                ImmutableList.of(markoId, vadasId),
                ImmutableList.of(markoId, lopId),
                ImmutableList.of(markoId, joshId)
        );
        for (Path path : koutResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }
        Assert.assertEquals(4, koutResult.vertices().size());
        expectedVids = ImmutableSet.of(markoId, vadasId, lopId, joshId);
        for (Vertex vertex : koutResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.withPath(true);
        builder.withVertex(true);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId);
        Assert.assertEquals(expected, koutResult.ids());
        Assert.assertEquals(2, koutResult.paths().size());
        expectedPaths = ImmutableSet.of(
                ImmutableList.of(markoId, lopId, peterId),
                ImmutableList.of(markoId, joshId, rippleId)
        );
        for (Path path : koutResult.paths()) {
            Assert.assertTrue(expectedPaths.contains(path.objects()));
        }
        Assert.assertEquals(5, koutResult.vertices().size());
        expectedVids = ImmutableSet.of(markoId, peterId, lopId,
                                       joshId, rippleId);
        for (Vertex vertex : koutResult.vertices()) {
            Assert.assertTrue(expectedVids.contains(vertex.id()));
        }
    }

    @Test
    public void testKoutPostWithSingleLabel() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("created");
        builder.maxDepth(1);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(1, koutResult.size());
        Set<Object> expected = ImmutableSet.of(lopId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("created");
        builder.maxDepth(2);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, joshId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("knows");
        builder.maxDepth(1);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(vadasId, joshId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH).labels("knows");
        builder.maxDepth(2);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(0, koutResult.size());
    }

    @Test
    public void testKoutPostWithMultiLabels() {
        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");
        Object rippleId = getVertexId("software", "name", "ripple");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .labels("knows").labels("created");
        builder.maxDepth(1);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, joshId, lopId);
        Assert.assertEquals(expected, koutResult.ids());
        
        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .labels("knows").labels("created");
        builder.maxDepth(2);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId);
        Assert.assertEquals(expected, koutResult.ids());
    }

    @Test
    public void testKoutPostWithDirection() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.OUT);
        builder.maxDepth(1);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.OUT);
        builder.maxDepth(2);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(1, koutResult.size());
        expected = ImmutableSet.of(rippleId);
        Assert.assertEquals(expected, koutResult.ids());
    }

    @Test
    public void testKoutPostWithProperties() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(1);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(1, koutResult.size());
        Set<Object> expected = ImmutableSet.of(lopId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(2);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        expected = ImmutableSet.of(peterId, joshId);
        Assert.assertEquals(expected, koutResult.ids());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH)
               .properties("date", "P.gt(\"2014-01-01 00:00:00\")");
        builder.maxDepth(3);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(1, koutResult.size());
        expected = ImmutableSet.of(rippleId);
        Assert.assertEquals(expected, koutResult.ids());
    }

    @Test
    public void testKoutPostWithLimit() {
        Object markoId = getVertexId("person", "name", "marko");
        Object rippleId = getVertexId("software", "name", "ripple");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object peterId = getVertexId("person", "name", "peter");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.limit(2);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        Set<Object> expected = ImmutableSet.of(vadasId, lopId, joshId);
        Assert.assertTrue(expected.containsAll(koutResult.ids()));

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.limit(1);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(1, koutResult.size());
        expected = ImmutableSet.of(peterId, rippleId);
        Assert.assertTrue(expected.containsAll(koutResult.ids()));
    }

    @Test
    public void testKoutPostWithCountOnly() {
        Object markoId = getVertexId("person", "name", "marko");

        KoutRequest.Builder builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.countOnly(true);
        KoutRequest request = builder.build();

        Kout koutResult = koutAPI.post(request);

        Assert.assertEquals(3, koutResult.size());
        Assert.assertTrue(koutResult.ids().isEmpty());
        Assert.assertTrue(koutResult.paths().isEmpty());
        Assert.assertTrue(koutResult.vertices().isEmpty());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(2);
        builder.countOnly(true);
        request = builder.build();

        koutResult = koutAPI.post(request);

        Assert.assertEquals(2, koutResult.size());
        Assert.assertTrue(koutResult.ids().isEmpty());
        Assert.assertTrue(koutResult.paths().isEmpty());
        Assert.assertTrue(koutResult.vertices().isEmpty());

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.countOnly(true);
        builder.withPath(true);

        KoutRequest.Builder finalBuilder = builder;
        Assert.assertThrows(IllegalArgumentException.class, ()-> {
            finalBuilder.build();
        });

        builder = KoutRequest.builder();
        builder.source(markoId);
        builder.step().direction(Direction.BOTH);
        builder.maxDepth(1);
        builder.countOnly(true);
        builder.withVertex(true);

        KoutRequest.Builder finalBuilder1 = builder;
        Assert.assertThrows(IllegalArgumentException.class, ()-> {
            finalBuilder1.build();
        });
    }
}
