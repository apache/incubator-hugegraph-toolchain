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
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
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
    public void teardown() throws Exception {
        edgeAPI.list(-1).forEach(e -> edgeAPI.delete(e.id()));
    }

    @Test
    public void testCreate() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        edge = edgeAPI.create(edge);

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals("person:peter", edge.source());
        Assert.assertEquals("software:lop", edge.target());
        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "city", "Hongkong");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithUndefinedLabel() {
        Edge edge = new Edge("undefined");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithUndefinedPropertyKey() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("not-exist-key", "not-exist-value");
        edge.property("city", "Hongkong");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithoutSourceOrTargetLabel() {
        Edge edge = new Edge("created");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        edge = edgeAPI.create(edge);

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals("person:peter", edge.source());
        Assert.assertEquals("software:lop", edge.target());
        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "city", "Hongkong");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithUndefinedSourceOrTargetLabel() {
        Edge edge = new Edge("created");
        edge.sourceLabel("undefined");
        edge.targetLabel("undefined");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithoutSourceOrTargetId() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateWithNotExistSourceOrTargetId() {
        Edge edge = new Edge("created");
        edge.source("not-exist-source-id");
        edge.target("not-exist--target-id");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testCreateExistVertex() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");
        edgeAPI.create(edge);

        edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Beijing");

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals("person:peter", edge.source());
        Assert.assertEquals("software:lop", edge.target());
        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "city", "Beijing");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithNullableKeysAbsent() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        // Absent prop 'city'
        edge.property("date", "20170324");
        edgeAPI.create(edge);

        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals("person:peter", edge.source());
        Assert.assertEquals("software:lop", edge.target());
        Map<String, Object> props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, edge.properties());
    }

    @Test
    public void testCreateWithNonNullKeysAbsent() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        // Absent prop 'date'
        edge.property("city", "Beijing");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edge);
        });
    }

    @Test
    public void testBatchCreateWithValidVertexAndCheck() {
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
            Assert.assertEquals("person:Person-" + i, created.source());
            Assert.assertEquals("software:Software-" + i, created.target());
            Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                        "city", "Hongkong");
            Assert.assertEquals(props, created.properties());
        }

        for (int i = 0; i < 50; i++) {
            Edge know = edgeAPI.get(knowsIds.get(i));
            Assert.assertEquals("knows", know.label());
            Assert.assertEquals("person", know.sourceLabel());
            Assert.assertEquals("person", know.targetLabel());
            Assert.assertEquals("person:Person-" + i, know.source());
            Assert.assertEquals("person:Person-" + (i + 50), know.target());
            Map<String, Object> props = ImmutableMap.of("date", "20170324");
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

        createds.get(0).source("not-exist-source-id");
        knows.get(10).property("undefined-key", "undefined-value");

        Assert.assertResponse(400, () -> {
            edgeAPI.create(createds, true);
        });
        Assert.assertResponse(400, () -> {
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
            edge.source("person:Person-" + i);
            edge.target("software:Software-" + i);
            edge.property("date", "20170824");
            edge.property("city", "Hongkong");
            edges.add(edge);
        }
        Assert.assertResponse(400, () -> {
            edgeAPI.create(edges, true);
        });
    }

    @Test
    public void testBatchCreateWithValidVertexAndNotCheck() {
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
            Assert.assertEquals("person:Person-" + i, created.source());
            Assert.assertEquals("software:Software-" + i, created.target());
            Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                        "city", "Hongkong");
            Assert.assertEquals(props, created.properties());
        }

        for (int i = 0; i < 50; i++) {
            Edge know = edgeAPI.get(knowsIds.get(i));
            Assert.assertEquals("knows", know.label());
            Assert.assertEquals("person", know.sourceLabel());
            Assert.assertEquals("person", know.targetLabel());
            Assert.assertEquals("person:Person-" + i, know.source());
            Assert.assertEquals("person:Person-" + (i + 50), know.target());
            Map<String, Object> props = ImmutableMap.of("date", "20170324");
            Assert.assertEquals(props, know.properties());
        }
    }

    @Test
    public void testBatchCreateWithInvalidVertexIdAndCheck() {
        List<Edge> edges = new ArrayList<>(2);

        Edge edge1 = new Edge("created");
        edge1.sourceLabel("person");
        edge1.targetLabel("software");
        edge1.source("person:invalid");
        edge1.target("software:lop");
        edge1.property("date", "20170324");
        edge1.property("city", "Hongkong");
        edges.add(edge1);

        Edge edge2 = new Edge("knows");
        edge2.sourceLabel("person");
        edge2.targetLabel("person");
        edge2.source("person:peter");
        edge2.target("person:invalid");
        edge2.property("date", "20170324");
        edges.add(edge2);

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edges, true);
        });
    }

    @Test
    public void testBatchCreateWithInvalidVertexLabelAndCheck() {
        List<Edge> edges = new ArrayList<>(2);

        Edge edge1 = new Edge("created");
        edge1.source("person:peter");
        edge1.target("software:lop");
        edge1.property("date", "20170324");
        edge1.property("city", "Hongkong");
        edges.add(edge1);

        Edge edge2 = new Edge("knows");
        edge2.sourceLabel("undefined");
        edge2.targetLabel("undefined");
        edge2.source("person:peter");
        edge2.target("person:marko");
        edge2.property("date", "20170324");
        edges.add(edge2);

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edges, true);
        });
    }

    /**
     * Note: When the vertex of the edge is dirty (id), use g.E () will
     * construct the vertex, and then throw the exception illegally.
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
//        edge1.source("person:invalid");
//        edge1.target("software:lop");
//        edge1.property("date", "20170324");
//        edge1.property("city", "Hongkong");
//        edges.add(edge1);
//
//        Edge edge2 = new Edge("knows");
//        edge2.sourceLabel("person");
//        edge2.targetLabel("person");
//        edge2.source("person:peter");
//        edge2.target("person:invalid");
//        edge2.property("date", "20170324");
//        edges.add(edge2);
//
//        List<String> ids = edgeAPI.create(edges, false);
//        Assert.assertEquals(2, ids.size());
//
//        Assert.assertResponse(404, () -> {
//            edgeAPI.get(ids.get(0));
//        });
//        Assert.assertResponse(404, () -> {
//            edgeAPI.get(ids.get(1));
//        });
//    }

    @Test
    public void testBatchCreateWithInvalidVertexLabelButNotCheck() {
        List<Edge> edges = new ArrayList<>(2);

        Edge edge1 = new Edge("created");
        edge1.source("person:peter");
        edge1.target("software:lop");
        edge1.property("date", "20170324");
        edge1.property("city", "Hongkong");
        edges.add(edge1);

        Edge edge2 = new Edge("knows");
        edge2.sourceLabel("undefined");
        edge2.targetLabel("undefined");
        edge2.source("person:peter");
        edge2.target("person:marko");
        edge2.property("date", "20170324");
        edges.add(edge2);

        Assert.assertResponse(400, () -> {
            edgeAPI.create(edges, false);
        });
    }

    @Test
    public void testGet() {
        Edge edge1 = new Edge("created");
        edge1.sourceLabel("person");
        edge1.targetLabel("software");
        edge1.source("person:peter");
        edge1.target("software:lop");
        edge1.property("date", "20170324");
        edge1.property("city", "Hongkong");

        edge1 = edgeAPI.create(edge1);

        Edge edge2 = edgeAPI.get(edge1.id());

        Assert.assertEquals(edge1.label(), edge2.label());
        Assert.assertEquals(edge1.sourceLabel(), edge2.sourceLabel());
        Assert.assertEquals(edge1.targetLabel(), edge2.targetLabel());
        Assert.assertEquals(edge1.source(), edge2.source());
        Assert.assertEquals(edge1.target(), edge2.target());
        Assert.assertEquals(edge1.properties(), edge2.properties());
    }

    @Test
    public void testGetNotExist() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            // TODO: id to be modified
            edgeAPI.get("not-exist-edge-id");
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

        List<Edge> edges = edgeAPI.list(-1);
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

        List<Edge> edges = edgeAPI.list(10);
        Assert.assertEquals(10, edges.size());
    }

    @Test
    public void testDelete() {
        Edge edge = new Edge("created");
        edge.sourceLabel("person");
        edge.targetLabel("software");
        edge.source("person:peter");
        edge.target("software:lop");
        edge.property("date", "20170324");
        edge.property("city", "Hongkong");

        edge = edgeAPI.create(edge);

        final String id = edge.id();
        edgeAPI.delete(id);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            edgeAPI.get(id);
        });
    }

    @Test
    public void testDeleteNotExist() {
        // TODO: Should change to 204 when server end add the method
        // 'removeEdge()'
        Assert.assertResponse(404, () -> {
            edgeAPI.delete("not-exist-e");
        });
    }

    @SuppressWarnings("unused")
    private static void assertContains(List<Edge> edges, Edge edge) {
        Assert.assertTrue(Utils.contains(edges, edge));
    }
}
