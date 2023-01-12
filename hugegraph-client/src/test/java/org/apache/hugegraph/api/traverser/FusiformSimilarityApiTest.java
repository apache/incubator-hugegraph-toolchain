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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.traverser.FusiformSimilarity;
import org.apache.hugegraph.structure.traverser.FusiformSimilarity.Similar;
import org.apache.hugegraph.structure.traverser.FusiformSimilarityRequest;
import org.apache.hugegraph.testutil.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class FusiformSimilarityApiTest extends TraverserApiTest {

    @BeforeClass
    public static void prepareSchemaAndGraph() {
        schema().propertyKey("name").asText().ifNotExist().create();
        schema().propertyKey("city").asText().ifNotExist().create();
        schema().propertyKey("time").asDate().ifNotExist().create();

        schema().vertexLabel("person")
                .properties("name", "city")
                .primaryKeys("name")
                .ifNotExist()
                .create();
        schema().vertexLabel("book")
                .properties("name")
                .primaryKeys("name")
                .ifNotExist()
                .create();

        schema().edgeLabel("read")
                .sourceLabel("person").targetLabel("book")
                .ifNotExist()
                .create();
        schema().edgeLabel("write")
                .sourceLabel("person").targetLabel("book")
                .properties("time")
                .multiTimes()
                .sortKeys("time")
                .ifNotExist()
                .create();

        Vertex p1 = graph().addVertex(T.LABEL, "person", "name", "p1",
                                      "city", "Beijing");
        Vertex p2 = graph().addVertex(T.LABEL, "person", "name", "p2",
                                      "city", "Shanghai");
        Vertex p3 = graph().addVertex(T.LABEL, "person", "name", "p3",
                                      "city", "Beijing");

        Vertex b1 = graph().addVertex(T.LABEL, "book", "name", "b1");
        Vertex b2 = graph().addVertex(T.LABEL, "book", "name", "b2");
        Vertex b3 = graph().addVertex(T.LABEL, "book", "name", "b3");
        Vertex b4 = graph().addVertex(T.LABEL, "book", "name", "b4");
        Vertex b5 = graph().addVertex(T.LABEL, "book", "name", "b5");
        Vertex b6 = graph().addVertex(T.LABEL, "book", "name", "b6");
        Vertex b7 = graph().addVertex(T.LABEL, "book", "name", "b7");
        Vertex b8 = graph().addVertex(T.LABEL, "book", "name", "b8");
        Vertex b9 = graph().addVertex(T.LABEL, "book", "name", "b9");
        Vertex b10 = graph().addVertex(T.LABEL, "book", "name", "b10");

        // p1 read b1-b9 (9 books)
        p1.addEdge("read", b1);
        p1.addEdge("read", b2);
        p1.addEdge("read", b3);
        p1.addEdge("read", b4);
        p1.addEdge("read", b5);
        p1.addEdge("read", b6);
        p1.addEdge("read", b7);
        p1.addEdge("read", b8);
        p1.addEdge("read", b9);
        // p2 read b2-b10 (9 books)
        p2.addEdge("read", b2);
        p2.addEdge("read", b3);
        p2.addEdge("read", b4);
        p2.addEdge("read", b5);
        p2.addEdge("read", b6);
        p2.addEdge("read", b7);
        p2.addEdge("read", b8);
        p2.addEdge("read", b9);
        p2.addEdge("read", b10);
        // p3 read b3-b9 (7 books)
        p3.addEdge("read", b3);
        p3.addEdge("read", b4);
        p3.addEdge("read", b5);
        p3.addEdge("read", b6);
        p3.addEdge("read", b7);
        p3.addEdge("read", b8);
        p3.addEdge("read", b9);

        p1.addEdge("write", b1, "time", "2019-11-13 00:00:00");
        p1.addEdge("write", b1, "time", "2019-11-13 00:01:00");
        p1.addEdge("write", b1, "time", "2019-11-13 00:02:00");

        p1.addEdge("write", b2, "time", "2019-11-13 00:00:00");
        p1.addEdge("write", b2, "time", "2019-11-13 00:01:00");

        p1.addEdge("write", b3, "time", "2019-11-13 00:00:00");
        p1.addEdge("write", b3, "time", "2019-11-13 00:01:00");

        p2.addEdge("write", b1, "time", "2019-11-13 00:00:00");
        p2.addEdge("write", b1, "time", "2019-11-13 00:01:00");
        p2.addEdge("write", b1, "time", "2019-11-13 00:02:00");

        p3.addEdge("write", b2, "time", "2019-11-13 00:00:00");
        p3.addEdge("write", b2, "time", "2019-11-13 00:01:00");

        p3.addEdge("write", b3, "time", "2019-11-13 00:00:00");
        p3.addEdge("write", b3, "time", "2019-11-13 00:01:00");
        p3.addEdge("write", b3, "time", "2019-11-13 00:02:00");
    }

    @Test
    public void testFusiformSimilarity() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(2, entry.getValue().size());
        Set<Object> expected = ImmutableSet.of(p2, p3);
        Set<Object> actual = entry.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFusiformSimilarityLessThanMinEdgeCount() {
        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(10)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();
        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(0, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(9)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p2");
        builder.label("read").direction(Direction.OUT).minNeighbors(10)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(0, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p2");
        builder.label("read").direction(Direction.OUT).minNeighbors(9)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p3");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(0, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p2");
        builder.label("read").direction(Direction.OUT).minNeighbors(7)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFusiformSimilarityAlpha() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(2, entry.getValue().size());
        Set<Object> expected = ImmutableSet.of(p2, p3);
        Set<Object> actual = entry.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(6)
               .alpha(0.83D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());

        entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(1, entry.getValue().size());
        expected = ImmutableSet.of(p2);
        actual = entry.getValue().stream().map(Similar::id)
                      .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFusiformSimilarityMinSimilars() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).minSimilars(3).groupProperty("city")
               .minGroups(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(0, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).minSimilars(2).groupProperty("city")
               .minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();

        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(2, entry.getValue().size());
        Set<Object> expected = ImmutableSet.of(p2, p3);
        Set<Object> actual = entry.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).minSimilars(1).groupProperty("city")
               .minGroups(2);
        builder.capacity(-1).limit(-1);
        request = builder.build();

        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(2, entry.getValue().size());
        Assert.assertEquals(expected, actual);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            FusiformSimilarityRequest.builder().minSimilars(0);
        });
    }

    @Test
    public void testFusiformSimilarityTop() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).top(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(2, entry.getValue().size());
        Set<Object> expected = ImmutableSet.of(p2, p3);
        Set<Object> actual = entry.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(6)
               .alpha(0.8D).top(1);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());

        entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(1, entry.getValue().size());
        expected = ImmutableSet.of(p2);
        actual = entry.getValue().stream().map(Similar::id)
                      .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFusiformSimilarityMinGroupCount() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.7D).groupProperty("city").minGroups(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1, entry.getKey());
        Assert.assertEquals(2, entry.getValue().size());
        Set<Object> expected = ImmutableSet.of(p2, p3);
        Set<Object> actual = entry.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);

        builder = FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(6)
               .alpha(0.8D).groupProperty("city").minGroups(3);
        builder.capacity(-1).limit(-1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(0, results.size());
    }

    @Test
    public void testFusiformSimilarityCapacity() {
        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.8D).groupProperty("city").minGroups(2);
        builder.capacity(10).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        Assert.assertThrows(ServerException.class, () -> {
            fusiformSimilarityAPI.post(request);
        }, e -> {
            String expect = "Exceed capacity '10' while " +
                            "finding fusiform similarity";
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testFusiformSimilarityLimit() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().ids(p1, p2, p3);
        builder.label("read").direction(Direction.OUT).minNeighbors(5)
               .alpha(0.8D).top(2);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();
        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(3, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().ids(p1, p2, p3);
        builder.label("read").direction(Direction.OUT).minNeighbors(5)
               .alpha(0.8D).top(2);
        builder.capacity(-1).limit(2);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(2, results.size());

        builder = FusiformSimilarityRequest.builder();
        builder.sources().ids(p1, p2, p3);
        builder.label("read").direction(Direction.OUT).minNeighbors(5)
               .alpha(0.8D).top(2);
        builder.capacity(-1).limit(1);
        request = builder.build();
        results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void testFusiformSimilarityWithMultiTimesEdges() {
        Vertex p1 = getVertex("person", "name", "p1");
        Vertex p2 = getVertex("person", "name", "p2");
        Vertex p3 = getVertex("person", "name", "p3");

        Object id1 = p1.id();
        Object id2 = p2.id();
        Object id3 = p3.id();

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().ids(id1, id2, id3);
        builder.label("write").direction(Direction.OUT).minNeighbors(3)
               .alpha(0.666D);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();
        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(id1, entry.getKey());
        Assert.assertEquals(1, entry.getValue().size());
        Set<Object> actual = entry.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
        Assert.assertEquals(ImmutableSet.of(id3), actual);
    }

    @Test
    public void testFusiformSimilarityWithoutEdgeLabel() {
        Object p1 = getVertexId("person", "name", "p1");
        Object p2 = getVertexId("person", "name", "p2");
        Object p3 = getVertexId("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().ids(p1, p2, p3);
        builder.direction(Direction.OUT).minNeighbors(8)
               .alpha(0.875D);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(3, results.size());
        Map<Object, Set<Object>> expected = ImmutableMap.of(
                p1, ImmutableSet.of(p2, p3),
                p2, ImmutableSet.of(p1),
                p3, ImmutableSet.of(p1, p2)
        );

        for (Map.Entry<Object, Set<Similar>> e : results.similarsMap()
                                                        .entrySet()) {
            Object key = e.getKey();
            Set<Object> actual = e.getValue().stream().map(Similar::id)
                                  .collect(Collectors.toSet());
            Assert.assertEquals(expected.get(key), actual);
        }
    }

    @Test
    public void testFusiformSimilarityWithIntermediaryAndVertex() {
        Vertex p1 = getVertex("person", "name", "p1");
        Vertex p2 = getVertex("person", "name", "p2");
        Vertex p3 = getVertex("person", "name", "p3");

        Vertex b2 = getVertex("book", "name", "b2");
        Vertex b3 = getVertex("book", "name", "b3");
        Vertex b4 = getVertex("book", "name", "b4");
        Vertex b5 = getVertex("book", "name", "b5");
        Vertex b6 = getVertex("book", "name", "b6");
        Vertex b7 = getVertex("book", "name", "b7");
        Vertex b8 = getVertex("book", "name", "b8");
        Vertex b9 = getVertex("book", "name", "b9");

        Object p1Id = p1.id();
        Object p2Id = p2.id();
        Object p3Id = p3.id();

        Object b2Id = b2.id();
        Object b3Id = b3.id();
        Object b4Id = b4.id();
        Object b5Id = b5.id();
        Object b6Id = b6.id();
        Object b7Id = b7.id();
        Object b8Id = b8.id();
        Object b9Id = b9.id();

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).groupProperty("city").minGroups(2)
               .withIntermediary(true).withVertex(true);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1Id, entry.getKey());

        Assert.assertEquals(2, entry.getValue().size());
        Set<Similar> similars = entry.getValue();
        Set<Object> p2Inter = ImmutableSet.of(b2Id, b3Id, b4Id, b5Id,
                                              b6Id, b7Id, b8Id, b9Id);
        Set<Object> p3Inter = ImmutableSet.of(b3Id, b4Id, b5Id, b6Id,
                                              b7Id, b8Id, b9Id);
        for (Similar similar : similars) {
            if (similar.id().equals(p2Id)) {
                Assert.assertEquals(p2Inter, similar.intermediaries());
            } else {
                Assert.assertEquals(p3Id, similar.id());
                Assert.assertEquals(p3Inter, similar.intermediaries());
            }
        }
        Set<Vertex> vertices = ImmutableSet.of(p1, p2, p3, b2, b3, b4,
                                               b5, b6, b7, b8, b9);
        Assert.assertEquals(vertices, results.vertices());
    }

    @Test
    public void testFusiformSimilarityWithIntermediaryWithoutVertex() {
        Vertex p1 = getVertex("person", "name", "p1");
        Vertex p2 = getVertex("person", "name", "p2");
        Vertex p3 = getVertex("person", "name", "p3");

        Vertex b2 = getVertex("book", "name", "b2");
        Vertex b3 = getVertex("book", "name", "b3");
        Vertex b4 = getVertex("book", "name", "b4");
        Vertex b5 = getVertex("book", "name", "b5");
        Vertex b6 = getVertex("book", "name", "b6");
        Vertex b7 = getVertex("book", "name", "b7");
        Vertex b8 = getVertex("book", "name", "b8");
        Vertex b9 = getVertex("book", "name", "b9");

        Object p1Id = p1.id();
        Object p2Id = p2.id();
        Object p3Id = p3.id();

        Object b2Id = b2.id();
        Object b3Id = b3.id();
        Object b4Id = b4.id();
        Object b5Id = b5.id();
        Object b6Id = b6.id();
        Object b7Id = b7.id();
        Object b8Id = b8.id();
        Object b9Id = b9.id();

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).groupProperty("city").minGroups(2)
               .withIntermediary(true).withVertex(false);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1Id, entry.getKey());

        Assert.assertEquals(2, entry.getValue().size());
        Set<Similar> similars = entry.getValue();
        Set<Object> p2Inter = ImmutableSet.of(b2Id, b3Id, b4Id, b5Id,
                                              b6Id, b7Id, b8Id, b9Id);
        Set<Object> p3Inter = ImmutableSet.of(b3Id, b4Id, b5Id, b6Id,
                                              b7Id, b8Id, b9Id);
        for (Similar similar : similars) {
            if (similar.id().equals(p2Id)) {
                Assert.assertEquals(p2Inter, similar.intermediaries());
            } else {
                Assert.assertEquals(p3Id, similar.id());
                Assert.assertEquals(p3Inter, similar.intermediaries());
            }
        }
        Set<Vertex> vertices = ImmutableSet.of();
        Assert.assertEquals(vertices, results.vertices());
    }

    @Test
    public void testFusiformSimilarityWithoutIntermediaryWithVertex() {
        Vertex p1 = getVertex("person", "name", "p1");
        Vertex p2 = getVertex("person", "name", "p2");
        Vertex p3 = getVertex("person", "name", "p3");

        FusiformSimilarityRequest.Builder builder =
                FusiformSimilarityRequest.builder();
        builder.sources().label("person").property("name", "p1");
        builder.label("read").direction(Direction.OUT).minNeighbors(8)
               .alpha(0.75D).groupProperty("city").minGroups(2)
               .withIntermediary(false).withVertex(true);
        builder.capacity(-1).limit(-1);
        FusiformSimilarityRequest request = builder.build();

        FusiformSimilarity results = fusiformSimilarityAPI.post(request);
        Assert.assertEquals(1, results.size());
        Map.Entry<Object, Set<Similar>> entry = results.first();
        Assert.assertEquals(p1.id(), entry.getKey());

        Assert.assertEquals(2, entry.getValue().size());
        Set<Similar> similars = entry.getValue();
        for (Similar similar : similars) {
            if (similar.id().equals(p2.id())) {
                Assert.assertEquals(ImmutableSet.of(),
                                    similar.intermediaries());
            } else {
                Assert.assertEquals(p3.id(), similar.id());
                Assert.assertEquals(ImmutableSet.of(),
                                    similar.intermediaries());
            }
        }
        Set<Vertex> vertices = ImmutableSet.of(p1, p2, p3);
        Assert.assertEquals(vertices, results.vertices());
    }
}
