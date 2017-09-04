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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
import com.google.common.collect.ImmutableMap;

public class VertexApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
    }

    @After
    public void teardown() throws Exception {
        vertexAPI.list(-1).forEach(v -> vertexAPI.delete(v.id()));
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
        Assert.assertEquals(props, prune(vertex.properties()));
    }

    @Test
    public void testCreateWithUndefinedLabel() {
        Vertex vertex = new Vertex("undefined");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        Assert.assertResponse(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateWithUndefinedProperty() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("not-exist-key", "not-exist-value");

        Assert.assertResponse(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateWithoutPrimaryKey() {
        Vertex vertex = new Vertex("person");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        Assert.assertResponse(400, () -> {
            vertexAPI.create(vertex);
        });
    }

    @Test
    public void testCreateWithCustomizeId() {
        Vertex vertex = new Vertex("person");
        vertex.property(T.id, 123456);
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        Assert.assertResponse(400, () -> {
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
        Assert.assertEquals(props, prune(vertex.properties()));
    }

    @Test
    public void testBatchCreate() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertexAPI.create(vertices);

        List<String> ids = vertexAPI.create(vertices);
        Assert.assertEquals(100, ids.size());
        for (int i = 0; i < 100; i++) {
            Vertex person = vertexAPI.get(ids.get(i));
            Assert.assertEquals("person", person.label());
            Map<String, Object> props = ImmutableMap.of("name", "Person-" + i,
                                                        "city", "Beijing",
                                                        "age", 30);
            Assert.assertEquals(props, prune(person.properties()));
        }
    }

    @Test
    public void testBatchCreateContainsInvalidVertex() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertices.get(0).property("invalid-key", "invalid-value");
        vertices.get(10).property("not-exist-key", "not-exist-value");

        Assert.assertResponse(400, () -> {
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
        Assert.assertResponse(400, () -> {
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

        Assert.assertEquals(vertex1.label(), vertex2.label());
        Assert.assertEquals(vertex1.properties(), vertex2.properties());
    }

    @Test
    public void testGetNotExist() {
        Assert.assertResponse(404, () -> {
            vertexAPI.get("not-exist-vertex-id");
        });
    }

    @Test
    public void testList() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertexAPI.create(vertices);

        vertices = vertexAPI.list(-1);
        Assert.assertEquals(100, vertices.size());
    }

    @Test
    public void testListWithLimit() {
        List<Vertex> vertices = super.create100PersonBatch();
        vertexAPI.create(vertices);

        vertices = vertexAPI.list(10);
        Assert.assertEquals(10, vertices.size());
    }

    @Test
    public void testDelete() {
        Vertex vertex = new Vertex("person");
        vertex.property("name", "James");
        vertex.property("city", "Beijing");
        vertex.property("age", 19);

        vertex = vertexAPI.create(vertex);

        String id = vertex.id();
        vertexAPI.delete(id);

        Assert.assertResponse(404, () -> {
            vertexAPI.get(id);
        });
    }

    @Test
    public void testDeleteNotExist() {
        // TODO: Should change to 204 when server end add the method
        // 'removeVertex()'
        Assert.assertResponse(404, () -> {
            vertexAPI.delete("not-exist-v");
        });
    }

    private static void assertContains(List<Vertex> vertices, Vertex vertex) {
        Assert.assertTrue(Utils.contains(vertices, vertex));
    }

    public static Map<String,Object> prune(Map<String, Object> properties) {
        Map<String, Object> props = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            try {
                value = ((List<?>) value).get(0);
                props.put(entry.getKey(), ((Map) value).get("value"));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                          "Failed to cast properties value", e);
            }
        }
        return props;
    }
}
