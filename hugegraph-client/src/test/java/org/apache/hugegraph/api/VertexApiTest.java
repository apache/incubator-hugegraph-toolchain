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
import java.util.UUID;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.GraphReadMode;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.constant.WriteType;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class VertexApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
    }

    @Override
    @After
    public void teardown() {
        vertexAPI.list(-1).results().forEach(v -> vertexAPI.delete(v.id()));
    }

    @Test
    public void testCreate() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        vertex = vertexAPI.create(vertex);

        Assert.assertEquals("person", vertex.label());
        Map<String, Object> props = ImmutableMap.of("name", "James",
                                                    "city", "Beijing",
                                                    "age", 19);
        Assert.assertEquals(props, vertex.properties());
    }

    @Test
    public void testCreateWithUndefinedLabel() {
        Vertex vertex = new Vertex("undefined");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateWithUndefinedProperty() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("not-exist-key", "not-exist-value");

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateWithoutPrimaryKey() {
        Vertex vertex = new Vertex("person");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateWithCustomizeStringId() {
        Vertex person = new Vertex("person");
        person.property(T.ID, "123456");
        person.property("name", "James");
        person.property("city", "Beijing");
        person.property("age", 19);

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(person);
        });

        Vertex book = new Vertex("book");
        book.id("ISBN-123456");
        book.property("name", "spark graphx");

        Vertex vertex = vertexAPI.create(book);
        Assert.assertEquals("book", vertex.label());
        Assert.assertEquals("ISBN-123456", vertex.id());
        Map<String, Object> props = ImmutableMap.of("name", "spark graphx");
        Assert.assertEquals(props, vertex.properties());
    }

    @Test
    public void testCreateWithCustomizeNumberId() {
        Vertex person = new Vertex("person");
        person.property(T.ID, 123456);
        person.property("name", "James");
        person.property("city", "Beijing");
        person.property("age", 19);

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(person);
        });

        schema().vertexLabel("log")
                .useCustomizeNumberId()
                .properties("date")
                .ifNotExist()
                .create();

        Vertex log = new Vertex("log");
        log.id(123456);
        log.property("date", "2018-01-01");

        Vertex vertex = vertexAPI.create(log);
        Assert.assertEquals("log", vertex.label());
        Assert.assertEquals(123456, vertex.id());
        String date = Utils.formatDate("2018-01-01");
        Map<String, Object> props = ImmutableMap.of("date", date);
        Assert.assertEquals(props, vertex.properties());
    }

    @Test
    public void testCreateWithCustomizeUuidId() {
        schema().vertexLabel("user")
                .useCustomizeUuidId()
                .properties("date")
                .ifNotExist()
                .create();

        Vertex log = new Vertex("user");
        log.id("835e1153-9281-4957-8691-cf79258e90eb");
        log.property("date", "2018-01-01");

        Vertex vertex = vertexAPI.create(log);
        Assert.assertEquals("user", vertex.label());
        Assert.assertEquals("835e1153-9281-4957-8691-cf79258e90eb",
                            vertex.id());
        String date = Utils.formatDate("2018-01-01");
        Map<String, Object> props = ImmutableMap.of("date", date);
        Assert.assertEquals(props, vertex.properties());

        // NOTE: must clean here due to type info
        UUID id = UUID.fromString("835e1153-9281-4957-8691-cf79258e90eb");
        vertexAPI.delete(id);
    }

    @Test
    public void testCreateWithNullableKeysAbsent() {
        Vertex vertex = new Vertex("person");
        // Absent prop city
        vertex.property("name", "James");
        vertex.property("age", 19);

        vertex = vertexAPI.create(vertex);

        Assert.assertEquals("person", vertex.label());
        Map<String, Object> props = ImmutableMap.of("name", "James",
                                                    "age", 19);
        Assert.assertEquals(props, vertex.properties());
    }

    @Test
    public void testCreateWithNonNullKeysAbsent() {
        Vertex vertex = new Vertex("person");
        // Absent prop 'age'
        vertex.property("name", "James");
        vertex.property("city", "Beijing");

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateExistVertex() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);
        vertexAPI.create(vertex);

        vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("city", "Shanghai");
        vertex.property("age", 20);
        vertex = vertexAPI.create(vertex);

        Assert.assertEquals("person", vertex.label());
        Map<String, Object> props = ImmutableMap.of("name", "James",
                                                    "city", "Shanghai",
                                                    "age", 20);
        Assert.assertEquals(props, vertex.properties());
    }

    @Test
    public void testCreateVertexWithTtl() {
        SchemaManager schema = schema();
        schema.vertexLabel("fan")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .ttl(3000L)
              .ifNotExist()
              .create();

        Vertex vertex = graph().addVertex(T.LABEL, "fan", "name", "Baby",
                                          "age", 3, "city", "Beijing");

        Vertex result = graph().getVertex(vertex.id());
        Assert.assertEquals("fan", result.label());
        Map<String, Object> props = ImmutableMap.of("name", "Baby",
                                                    "city", "Beijing",
                                                    "age", 3);
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        result = graph().getVertex(vertex.id());
        Assert.assertEquals("fan", result.label());
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        result = graph().getVertex(vertex.id());
        Assert.assertEquals("fan", result.label());
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        Assert.assertThrows(ServerException.class, () -> {
            graph().getVertex(vertex.id());
        }, e -> {
            Assert.assertContains("does not exist", e.getMessage());
        });
    }

    @Test
    public void testCreateVertexWithTtlAndTtlStartTime() {
        SchemaManager schema = schema();
        schema.vertexLabel("follower")
              .properties("name", "age", "city", "date")
              .primaryKeys("name")
              .ttl(3000L)
              .ttlStartTime("date")
              .ifNotExist()
              .create();
        long date = DateUtil.now().getTime() - 1000L;
        String dateString = Utils.formatDate(new Date(date));
        Vertex vertex = graph().addVertex(T.LABEL, "follower", "name", "Baby",
                                          "age", 3, "city", "Beijing",
                                          "date", date);

        Vertex result = graph().getVertex(vertex.id());
        Assert.assertEquals("follower", result.label());
        Map<String, Object> props = ImmutableMap.of("name", "Baby",
                                                    "city", "Beijing",
                                                    "age", 3,
                                                    "date", dateString);
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        result = graph().getVertex(vertex.id());
        Assert.assertEquals("follower", result.label());
        Assert.assertEquals(props, result.properties());

        try {
            Thread.sleep(1100L);
        } catch (InterruptedException e) {
            // Ignore
        }

        Assert.assertThrows(ServerException.class, () -> {
            graph().getVertex(vertex.id());
        }, e -> {
            Assert.assertContains("does not exist", e.getMessage());
        });
    }

    @Test
    public void testOlapPropertyWrite() {
        List<Vertex> vertices = super.create100PersonBatch();
        List<Object> ids = vertexAPI.create(vertices);

        // Create olap property key
        PropertyKey pagerank = schema().propertyKey("pagerank")
                                       .asDouble()
                                       .writeType(WriteType.OLAP_RANGE)
                                       .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(pagerank);
        long taskId1 = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId1);

        PropertyKey wcc = schema().propertyKey("wcc")
                                  .asText()
                                  .writeType(WriteType.OLAP_SECONDARY)
                                  .build();

        propertyKeyWithTask = propertyKeyAPI.create(wcc);
        long taskId2 = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId2);

        PropertyKey none = schema().propertyKey("none")
                                   .asText()
                                   .writeType(WriteType.OLAP_COMMON)
                                   .build();

        propertyKeyWithTask = propertyKeyAPI.create(none);
        long taskId3 = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId3);

        waitUntilTaskCompleted(taskId1);
        waitUntilTaskCompleted(taskId2);
        waitUntilTaskCompleted(taskId3);

        // Add olap properties
        vertices = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            Vertex vertex = new Vertex(null);
            vertex.id(ids.get(i));
            vertex.property("pagerank", 0.1D * i);
            vertices.add(vertex);
        }

        ids = vertexAPI.create(vertices);
        Assert.assertEquals(100, ids.size());

        vertices = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            Vertex vertex = new Vertex(null);
            vertex.id(ids.get(i));
            vertex.property("wcc", "wcc" + i);
            vertices.add(vertex);
        }

        ids = vertexAPI.create(vertices);
        Assert.assertEquals(100, ids.size());

        vertices = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            Vertex vertex = new Vertex(null);
            vertex.id(ids.get(i));
            vertex.property("none", "none" + i);
            vertices.add(vertex);
        }

        ids = vertexAPI.create(vertices);
        Assert.assertEquals(100, ids.size());

        // Query vertices by id before set graph read mode to 'ALL'
        for (int i = 0; i < 100; i++) {
            Vertex person = vertexAPI.get(ids.get(i));
            Assert.assertEquals("person", person.label());
            Map<String, Object> props = ImmutableMap.of("name", "Person-" + i,
                                                        "city", "Beijing",
                                                        "age", 30);
            Assert.assertEquals(props, person.properties());
        }

        // Set graph read mode to 'ALL'
        graphsAPI.readMode("hugegraph", GraphReadMode.ALL);

        // Query vertices by id after set graph read mode to 'ALL'
        for (int i = 0; i < 100; i++) {
            Vertex person = vertexAPI.get(ids.get(i));
            Assert.assertEquals("person", person.label());
            Map<String, Object> props = ImmutableMap.<String, Object>builder()
                                                    .put("name", "Person-" + i)
                                                    .put("city", "Beijing")
                                                    .put("age", 30)
                                                    .put("pagerank", 0.1D * i)
                                                    .put("wcc", "wcc" + i)
                                                    .put("none", "none" + i)
                                                    .build();
            Assert.assertEquals(props, person.properties());
        }

        // Query vertices by olap properties
        GremlinRequest request = new GremlinRequest("g.V().has(\"pagerank\", P.gte(5))");
        ResultSet resultSet = gremlin().execute(request);
        Assert.assertEquals(50, resultSet.size());

        request = new GremlinRequest("g.V().has(\"wcc\", P.within(\"wcc10\", \"wcc20\"))");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(2, resultSet.size());

        // Clear olap property key
        propertyKeyWithTask = propertyKeyAPI.clear(pagerank);
        taskId1 = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId1);

        propertyKeyWithTask = propertyKeyAPI.clear(wcc);
        taskId2 = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId2);

        propertyKeyWithTask = propertyKeyAPI.clear(none);
        taskId3 = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId3);

        waitUntilTaskCompleted(taskId1);
        waitUntilTaskCompleted(taskId2);
        waitUntilTaskCompleted(taskId3);

        // Query after clear olap property key
        request = new GremlinRequest("g.V().has(\"pagerank\", P.gte(5))");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(0, resultSet.size());

        request = new GremlinRequest("g.V().has(\"wcc\", P.within(\"wcc10\", \"wcc20\"))");
        resultSet = gremlin().execute(request);
        Assert.assertEquals(0, resultSet.size());

        // Delete olap property key
        taskId1 = propertyKeyAPI.delete(pagerank.name());
        Assert.assertNotEquals(0L, taskId1);

        taskId2 = propertyKeyAPI.delete(wcc.name());
        Assert.assertNotEquals(0L, taskId2);

        taskId3 = propertyKeyAPI.delete(none.name());
        Assert.assertNotEquals(0L, taskId3);

        waitUntilTaskCompleted(taskId1);
        waitUntilTaskCompleted(taskId2);
        waitUntilTaskCompleted(taskId3);

        // Query after delete olap property key
        Assert.assertThrows(ServerException.class, () -> {
            gremlin().execute(new GremlinRequest("g.V().has(\"pagerank\", P.gte(5))"));
        }, e -> {
            Assert.assertContains("Undefined property key: 'pagerank'",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            gremlin().execute(new GremlinRequest("g.V().has(\"wcc\", " +
                                                 "P.within(\"wcc10\", \"wcc20\"))"));
        }, e -> {
            Assert.assertContains("Undefined property key: 'wcc'",
                                  e.getMessage());
        });

        // Resume graph read mode to 'OLTP_ONLY'
        graphsAPI.readMode("hugegraph", GraphReadMode.OLTP_ONLY);
    }

    @Test
    public void testBatchCreate() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertexAPI.create(vertices);

        List<Object> ids = vertexAPI.create(vertices);
        Assert.assertEquals(100, ids.size());
        for (int i = 0; i < 100; i++) {
            Vertex person = vertexAPI.get(ids.get(i));
            Assert.assertEquals("person", person.label());
            Map<String, Object> props = ImmutableMap.of("name", "Person-" + i,
                                                        "city", "Beijing",
                                                        "age", 30);
            Assert.assertEquals(props, person.properties());
        }
    }

    @Test
    public void testBatchCreateContainsInvalidVertex() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertices.get(0).property("invalid-key", "invalid-value");
        vertices.get(10).property("not-exist-key", "not-exist-value");

        Utils.assertResponseError(400, () -> {
            vertexAPI.create(vertices);
        });
    }

    @Test
    public void testBatchCreateWithMoreThanBatchSize() {
        List<Vertex> vertices = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            Vertex vertex = new Vertex("person");
            vertex.property("name", "Person" + "-" + i);
            vertex.property("city", "Beijing");
            vertex.property("age", 20);
            vertices.add(vertex);
        }
        Utils.assertResponseError(400, () -> {
            vertexAPI.create(vertices);
        });
    }

    @Test
    public void testGet() {
        Vertex vertex1 = new Vertex("person");
        vertex1.property("name", "James");
        vertex1.property("city", "Beijing");
        vertex1.property("age", 19);

        vertex1 = vertexAPI.create(vertex1);

        Vertex vertex2 = vertexAPI.get(vertex1.id());
        Assert.assertEquals(vertex1.id(), vertex2.id());
        Assert.assertEquals(vertex1.label(), vertex2.label());
        Assert.assertEquals(vertex1.properties(), vertex2.properties());
    }

    @Test
    public void testGetWithCustomizeStringId() {
        Vertex vertex1 = new Vertex("book");
        vertex1.id("ISBN-123456");
        vertex1.property("name", "spark graphx");

        vertex1 = vertexAPI.create(vertex1);

        Vertex vertex2 = vertexAPI.get("ISBN-123456");
        Assert.assertEquals(vertex1.id(), vertex2.id());
        Assert.assertEquals(vertex1.label(), vertex2.label());
        Assert.assertEquals(vertex1.properties(), vertex2.properties());
    }

    @Test
    public void testGetWithCustomizeNumberId() {
        schema().vertexLabel("log")
                .useCustomizeNumberId()
                .properties("date")
                .ifNotExist()
                .create();

        Vertex vertex1 = new Vertex("log");
        vertex1.id(123456);
        vertex1.property("date", "2018-01-01");

        vertex1 = vertexAPI.create(vertex1);

        Vertex vertex2 = vertexAPI.get(123456);
        Assert.assertEquals(vertex1.id(), vertex2.id());
        Assert.assertEquals(vertex1.label(), vertex2.label());
        Assert.assertEquals(vertex1.properties(), vertex2.properties());
    }

    @Test
    public void testGetWithCustomizeUuidId() {
        schema().vertexLabel("user")
                .useCustomizeUuidId()
                .properties("date")
                .ifNotExist()
                .create();

        Vertex vertex1 = new Vertex("user");
        vertex1.id("835e1153-9281-4957-8691-cf79258e90eb");
        vertex1.property("date", "2018-01-01");

        vertex1 = vertexAPI.create(vertex1);

        UUID id = UUID.fromString("835e1153-9281-4957-8691-cf79258e90eb");
        Vertex vertex2 = vertexAPI.get(id);
        Assert.assertEquals(vertex1.id(), vertex2.id());
        Assert.assertEquals(vertex1.label(), vertex2.label());
        Assert.assertEquals(vertex1.properties(), vertex2.properties());

        // NOTE: must clean here due to type info
        vertexAPI.delete(id);
    }

    @Test
    public void testGetNotExist() {
        Utils.assertResponseError(404, () -> {
            vertexAPI.get("not-exist-vertex-id");
        });
    }

    @Test
    public void testList() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertexAPI.create(vertices);

        vertices = vertexAPI.list(-1).results();
        Assert.assertEquals(100, vertices.size());
    }

    @Test
    public void testListWithLimit() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertexAPI.create(vertices);

        vertices = vertexAPI.list(10).results();
        Assert.assertEquals(10, vertices.size());
    }

    @Test
    public void testDelete() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        vertex = vertexAPI.create(vertex);

        Object id = vertex.id();
        vertexAPI.delete(id);

        Utils.assertResponseError(404, () -> {
            vertexAPI.get(id);
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(400, () -> {
            vertexAPI.delete("not-exist-v");
        });
    }

    @SuppressWarnings("unused")
    private static void assertContains(List<Vertex> vertices, Vertex vertex) {
        Assert.assertTrue(Utils.contains(vertices, vertex));
    }
}
