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

package org.apache.hugegraph.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class EdgeApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initVertex();
    }

    @Override
    @After
    public void teardown() {
        edgeAPI.list(-1).results().forEach(e -> edgeAPI.delete(e.id()));
    }

    @Test
    public void testCreate() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        edge = edgeAPI.create(edge);

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(outVId, edge.sourceId());
        Assert.assertEquals(inVId, edge.targetId());
        String date = Utils.formatDate("2017-03-24");
        Map<String, Object> props = ImmutableMap.of("date", date,
                                                    "city", "Hongkong");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithUndefinedLabel() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("undefined");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithUndefinedPropertyKey() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("not-exist-key", "not-exist-value");
        edge.property("city", "Hongkong");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithoutSourceOrTargetLabel() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("created");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        edge = edgeAPI.create(edge);

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(outVId, edge.sourceId());
        Assert.assertEquals(inVId, edge.targetId());
        String date = Utils.formatDate("2017-03-24");
        Map<String, Object> props = ImmutableMap.of("date", date,
                                                    "city", "Hongkong");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithUndefinedSourceOrTargetLabel() {
        Edge edge = new Edge("created");
        edge.sourceLabel("undefined");
        edge.targetLabel("undefined");
        edge.sourceId("person:peter");
        edge.targetId("software:lop");
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithoutSourceOrTargetId() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithNotExistSourceOrTargetId() {
        Edge edge = new Edge("created");
        edge.sourceId("not-exist-source-id");
        edge.targetId("not-exist-target-id");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateExistVertex() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");
        edgeAPI.create(edge);

        edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("date", Utils.date("2017-03-24"));
        edge.property("city", "Beijing");

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(outVId, edge.sourceId());
        Assert.assertEquals(inVId, edge.targetId());
        Map<String, Object> props = ImmutableMap.of("date", Utils.date("2017-03-24"),
                                                    "city", "Beijing");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithNullableKeysAbsent() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        // Absent prop 'city'
        edge.property("date", Utils.date("2017-03-24"));
        edgeAPI.create(edge);

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(outVId, edge.sourceId());
        Assert.assertEquals(inVId, edge.targetId());
        Map<String, Object> props = ImmutableMap.of("date",
                                                    Utils.date("2017-03-24"));
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithNonNullKeysAbsent() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId("person:peter");
        edge.targetId("software:lop");
        // Absent prop 'date'
        edge.property("city", "Beijing");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testAddEdgeWithTtl() {
        SchemaManager schema = schema();
        schema.propertyKey("place").asText().ifNotExist().create();
        schema.edgeLabel("read").link("person", "book")
              .properties("place", "date")
              .ttl(3000L)
              .enableLabelIndex(true)
              .ifNotExist()
              .create();

        Vertex baby = graph().addVertex(T.LABEL, "person", "name", "Baby",
                                        "age", 3, "city", "Beijing");
        Vertex java = graph().addVertex(T.LABEL, "book", T.ID, "java",
                                        "name", "Java in action");
        Edge edge = baby.addEdge("read", java, "place", "library of school",
                                 "date", "2019-12-23 12:00:00");

        Edge result = graph().getEdge(edge.id());
        Assert.assertEquals("read", result.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("book", edge.targetLabel());
        Assert.assertEquals(baby.id(), edge.sourceId());
        Assert.assertEquals(java.id(), edge.targetId());
        Map<String, Object> props = ImmutableMap.of("place",
                                                    "library of school",
                                                    "date",
                                                    "2019-12-23 12:00:00.000");
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        result = graph().getEdge(edge.id());
        Assert.assertEquals("read", result.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("book", edge.targetLabel());
        Assert.assertEquals(baby.id(), edge.sourceId());
        Assert.assertEquals(java.id(), edge.targetId());
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        result = graph().getEdge(edge.id());
        Assert.assertEquals("read", result.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("book", edge.targetLabel());
        Assert.assertEquals(baby.id(), edge.sourceId());
        Assert.assertEquals(java.id(), edge.targetId());
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        Assert.assertThrows(ServerException.class, () -> {
            graph().getEdge(edge.id());
        }, e -> {
            Assert.assertContains("does not exist", e.getMessage());
        });
    }

    @Test
    public void testAddEdgeWithTtlAndTtlStartTime() {
        SchemaManager schema = schema();
        schema.propertyKey("place").asText().ifNotExist().create();
        schema.edgeLabel("borrow").link("person", "book")
              .properties("place", "date")
              .ttl(3000L)
              .ttlStartTime("date")
              .enableLabelIndex(true)
              .ifNotExist()
              .create();

        Vertex baby = graph().addVertex(T.LABEL, "person", "name", "Baby",
                                        "age", 3, "city", "Beijing");
        Vertex java = graph().addVertex(T.LABEL, "book", T.ID, "java",
                                        "name", "Java in action");
        long date = DateUtil.now().getTime() - 1000L;
        String dateString = Utils.formatDate(new Date(date));
        Edge edge = baby.addEdge("borrow", java, "place", "library of school",
                                 "date", date);

        Edge result = graph().getEdge(edge.id());
        Assert.assertEquals("borrow", result.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("book", edge.targetLabel());
        Assert.assertEquals(baby.id(), edge.sourceId());
        Assert.assertEquals(java.id(), edge.targetId());
        Map<String, Object> props = ImmutableMap.of("place",
                                                    "library of school",
                                                    "date",
                                                    dateString);
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        result = graph().getEdge(edge.id());
        Assert.assertEquals("borrow", result.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("book", edge.targetLabel());
        Assert.assertEquals(baby.id(), edge.sourceId());
        Assert.assertEquals(java.id(), edge.targetId());
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        Assert.assertThrows(ServerException.class, () -> {
            graph().getEdge(edge.id());
        }, e -> {
            Assert.assertContains("does not exist", e.getMessage());
        });
    }

    @Test
    public void testBatchCreateWithValidVertexAndCheck() {
        VertexLabel person = schema().getVertexLabel("person");
        VertexLabel software = schema().getVertexLabel("software");

        List<Vertex> persons = super.create100PersonBatch();
        List<Vertex> softwares = super.create50SoftwareBatch();

        vertexAPI.create(persons);
        vertexAPI.create(softwares);

        List<Edge> createds = super.create50CreatedBatch();
        List<Edge> knows = super.create50KnowsBatch();

        List<String> createdIds = edgeAPI.create(createds, true);
        List<String> knowsIds = edgeAPI.create(knows, true);

        Assert.assertEquals(50, createdIds.size());
        Assert.assertEquals(50, knowsIds.size());

        for (int i = 0; i < 50; i++) {
            Edge created = edgeAPI.get(createdIds.get(i));
            Assert.assertEquals("created", created.label());
            Assert.assertEquals("person", created.sourceLabel());
            Assert.assertEquals("software", created.targetLabel());
            Assert.assertEquals(person.id() + ":Person-" + i, created.sourceId());
            Assert.assertEquals(software.id() + ":Software-" + i,
                                created.targetId());
            String date = Utils.formatDate("2017-03-24");
            Map<String, Object> props = ImmutableMap.of("date", date,
                                                        "city", "Hongkong");
            Assert.assertEquals(props, created.properties());
        }

        for (int i = 0; i < 50; i++) {
            Edge know = edgeAPI.get(knowsIds.get(i));
            Assert.assertEquals("knows", know.label());
            Assert.assertEquals("person", know.sourceLabel());
            Assert.assertEquals("person", know.targetLabel());
            Assert.assertEquals(person.id() + ":Person-" + i, know.sourceId());
            Assert.assertEquals(person.id() + ":Person-" + (i + 50),
                                know.targetId());
            String date = Utils.formatDate("2017-03-24");
            Map<String, Object> props = ImmutableMap.of("date", date);
            Assert.assertEquals(props, know.properties());
        }
    }

    @Test
    public void testBatchCreateContainsInvalidEdge() {
        List<Vertex> persons = super.create100PersonBatch();
        List<Vertex> softwares = super.create50SoftwareBatch();

        vertexAPI.create(persons);
        vertexAPI.create(softwares);

        List<Edge> createds = super.create50CreatedBatch();
        List<Edge> knows = super.create50KnowsBatch();

        createds.get(0).sourceId("not-exist-sourceId-id");
        knows.get(10).property("undefined-key", "undefined-value");

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(createds, true);
        });
        Utils.assertResponseError(400, () -> {
            edgeAPI.create(knows, true);
        });
    }

    @Test
    public void testBatchCreateWithMoreThanBatchSize() {
        List<Vertex> persons = super.create100PersonBatch();
        List<Vertex> softwares = super.create50SoftwareBatch();

        vertexAPI.create(persons);
        vertexAPI.create(softwares);

        List<Edge> edges = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            Edge edge = new Edge("created");
            edge.sourceLabel("person");
            edge.targetLabel("software");
            edge.sourceId("person:Person-" + i);
            edge.targetId("software:Software-" + i);
            edge.property("date", "2017-08-24");
            edge.property("city", "Hongkong");
            edges.add(edge);
        }
        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edges, true);
        });
    }

    @Test
    public void testBatchCreateWithValidVertexAndNotCheck() {
        VertexLabel person = schema().getVertexLabel("person");
        VertexLabel software = schema().getVertexLabel("software");

        List<Vertex> persons = super.create100PersonBatch();
        List<Vertex> softwares = super.create50SoftwareBatch();

        vertexAPI.create(persons);
        vertexAPI.create(softwares);

        List<Edge> createds = super.create50CreatedBatch();
        List<Edge> knows = super.create50KnowsBatch();

        List<String> createdIds = edgeAPI.create(createds, false);
        List<String> knowsIds = edgeAPI.create(knows, false);

        Assert.assertEquals(50, createdIds.size());
        Assert.assertEquals(50, knowsIds.size());

        for (int i = 0; i < 50; i++) {
            Edge created = edgeAPI.get(createdIds.get(i));
            Assert.assertEquals("created", created.label());
            Assert.assertEquals("person", created.sourceLabel());
            Assert.assertEquals("software", created.targetLabel());
            Assert.assertEquals(person.id() + ":Person-" + i, created.sourceId());
            Assert.assertEquals(software.id() + ":Software-" + i,
                                created.targetId());
            String date = Utils.formatDate("2017-03-24");
            Map<String, Object> props = ImmutableMap.of("date", date,
                                                        "city", "Hongkong");
            Assert.assertEquals(props, created.properties());
        }

        for (int i = 0; i < 50; i++) {
            Edge know = edgeAPI.get(knowsIds.get(i));
            Assert.assertEquals("knows", know.label());
            Assert.assertEquals("person", know.sourceLabel());
            Assert.assertEquals("person", know.targetLabel());
            Assert.assertEquals(person.id() + ":Person-" + i, know.sourceId());
            Assert.assertEquals(person.id() + ":Person-" + (i + 50),
                                know.targetId());
            String date = Utils.formatDate("2017-03-24");
            Map<String, Object> props = ImmutableMap.of("date", date);
            Assert.assertEquals(props, know.properties());
        }
    }

    @Test
    public void testBatchCreateWithInvalidVertexIdAndCheck() {
        List<Edge> edges = new ArrayList<>(2);

        Edge edge1 = new Edge("created");
        edge1.sourceLabel("person");
        edge1.targetLabel("software");
        edge1.sourceId("person:invalid");
        edge1.targetId("software:lop");
        edge1.property("date", "2017-03-24");
        edge1.property("city", "Hongkong");
        edges.add(edge1);

        Edge edge2 = new Edge("knows");
        edge2.sourceLabel("person");
        edge2.targetLabel("person");
        edge2.sourceId("person:peter");
        edge2.targetId("person:invalid");
        edge2.property("date", "2017-03-24");
        edges.add(edge2);

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edges, true);
        });
    }

    @Test
    public void testBatchCreateWithInvalidVertexLabelAndCheck() {
        List<Edge> edges = new ArrayList<>(2);

        Edge edge1 = new Edge("created");
        edge1.sourceId("person:peter");
        edge1.targetId("software:lop");
        edge1.property("date", "2017-03-24");
        edge1.property("city", "Hongkong");
        edges.add(edge1);

        Edge edge2 = new Edge("knows");
        edge2.sourceLabel("undefined");
        edge2.targetLabel("undefined");
        edge2.sourceId("person:peter");
        edge2.targetId("person:marko");
        edge2.property("date", "2017-03-24");
        edges.add(edge2);

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edges, true);
        });
    }

    /**
     * Note: When the vertex of an edge is dirty (id), g.E() will
     * construct the vertex, and then throw an illegal exception.
     * That will lead clearData error.
     * (Icafe: HugeGraph-768)
     */
//    @Test
//    public void testBatchCreateWithInvalidVertexIdButNotCheck() {
//        List<Edge> edges = new ArrayList<>(2);
//
//        Edge edge1 = new Edge("created");
//        edge1.sourceLabel("person");
//        edge1.targetLabel("software");
//        edge1.sourceId("person:invalid");
//        edge1.targetId("software:lop");
//        edge1.property("date", "2017-03-24");
//        edge1.property("city", "Hongkong");
//        edges.add(edge1);
//
//        Edge edge2 = new Edge("knows");
//        edge2.sourceLabel("person");
//        edge2.targetLabel("person");
//        edge2.sourceId("person:peter");
//        edge2.targetId("person:invalid");
//        edge2.property("date", "2017-03-24");
//        edges.add(edge2);
//
//        List<String> ids = edgeAPI.create(edges, false);
//        Assert.assertEquals(2, ids.size());
//
//        Utils.assertErrorResponse(404, () -> {
//            edgeAPI.get(ids.get(0));
//        });
//        Utils.assertErrorResponse(404, () -> {
//            edgeAPI.get(ids.get(1));
//        });
//    }

    @Test
    public void testBatchCreateWithInvalidVertexLabelButNotCheck() {
        List<Edge> edges = new ArrayList<>(2);

        Edge edge1 = new Edge("created");
        edge1.sourceId("person:peter");
        edge1.targetId("software:lop");
        edge1.property("date", "2017-03-24");
        edge1.property("city", "Hongkong");
        edges.add(edge1);

        Edge edge2 = new Edge("knows");
        edge2.sourceLabel("undefined");
        edge2.targetLabel("undefined");
        edge2.sourceId("person:peter");
        edge2.targetId("person:marko");
        edge2.property("date", "2017-03-24");
        edges.add(edge2);

        Utils.assertResponseError(400, () -> {
            edgeAPI.create(edges, false);
        });
    }

    @Test
    public void testGet() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge1 = new Edge("created");
        edge1.sourceLabel("person");
        edge1.targetLabel("software");
        edge1.sourceId(outVId);
        edge1.targetId(inVId);
        edge1.property("date", "2017-03-24");
        edge1.property("city", "Hongkong");

        edge1 = edgeAPI.create(edge1);

        Edge edge2 = edgeAPI.get(edge1.id());

        Assert.assertEquals(edge1.label(), edge2.label());
        Assert.assertEquals(edge1.sourceLabel(), edge2.sourceLabel());
        Assert.assertEquals(edge1.targetLabel(), edge2.targetLabel());
        Assert.assertEquals(edge1.sourceId(), edge2.sourceId());
        Assert.assertEquals(edge1.targetId(), edge2.targetId());
        Assert.assertEquals(edge1.properties(), edge2.properties());
    }

    @Test
    public void testGetNotExist() {
        String edgeId = "not-exist-edge-id";
        Assert.assertThrows(ServerException.class, () -> {
            // TODO: id to be modified
            edgeAPI.get(edgeId);
        }, e -> {
            Assert.assertContains("Edge id must be formatted as 4~5 parts, " +
                                  "but got 1 parts: 'not-exist-edge-id'",
                                  e.getMessage());
            Assert.assertInstanceOf(ServerException.class, e);
            Assert.assertContains("NotFoundException",
                                  ((ServerException) e).exception());
        });
    }

    @Test
    public void testList() {
        List<Vertex> persons = super.create100PersonBatch();
        List<Vertex> softwares = super.create50SoftwareBatch();

        vertexAPI.create(persons);
        vertexAPI.create(softwares);

        List<Edge> createds = super.create50CreatedBatch();
        List<Edge> knows = super.create50KnowsBatch();

        edgeAPI.create(createds, true);
        edgeAPI.create(knows, true);

        List<Edge> edges = edgeAPI.list(-1).results();
        Assert.assertEquals(100, edges.size());
    }

    @Test
    public void testListWithLimit() {
        List<Vertex> persons = super.create100PersonBatch();
        List<Vertex> softwares = super.create50SoftwareBatch();

        vertexAPI.create(persons);
        vertexAPI.create(softwares);

        List<Edge> createds = super.create50CreatedBatch();
        List<Edge> knows = super.create50KnowsBatch();

        edgeAPI.create(createds, true);
        edgeAPI.create(knows, true);

        List<Edge> edges = edgeAPI.list(10).results();
        Assert.assertEquals(10, edges.size());
    }

    @Test
    public void testDelete() {
        Object outVId = getVertexId("person", "name", "peter");
        Object inVId = getVertexId("software", "name", "lop");

        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.sourceId(outVId);
        edge.targetId(inVId);
        edge.property("date", "2017-03-24");
        edge.property("city", "Hongkong");

        edge = edgeAPI.create(edge);

        final String id = edge.id();
        edgeAPI.delete(id);

        Assert.assertThrows(ServerException.class, () -> {
            edgeAPI.get(id);
        }, e -> {
            String expect = String.format("Edge '%s' does not exist", id);
            Assert.assertContains(expect, e.getMessage());
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(400, () -> {
            edgeAPI.delete("S364:peter>213>>S365:not-found");
        });
        Utils.assertResponseError(400, () -> {
            edgeAPI.delete("not-exist-e");
        });
    }

    @SuppressWarnings("unused")
    private static void assertContains(List<Edge> edges, Edge edge) {
        Assert.assertTrue(Utils.contains(edges, edge));
    }
}
