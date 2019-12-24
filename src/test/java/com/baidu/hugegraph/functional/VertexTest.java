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

package com.baidu.hugegraph.functional;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.BaseClientTest;
import com.baidu.hugegraph.exception.InvalidOperationException;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class VertexTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseClientTest.initPropertyKey();
        BaseClientTest.initVertexLabel();
        BaseClientTest.initEdgeLabel();
    }

    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testAddVertexProperty() {
        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19);
        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19);
        Assert.assertEquals(props, vadas.properties());

        vadas.property("city", "Beijing");
        props = ImmutableMap.of("name", "vadas", "age", 19, "city", "Beijing");
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testUpdateVertexProperty() {
        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19);
        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19);
        Assert.assertEquals(props, vadas.properties());

        vadas.property("age", 20);
        props = ImmutableMap.of("name", "vadas", "age", 20);
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueList() {
        schema().propertyKey("time")
                .asDate()
                .valueList()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                    Utils.date("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2014-02-14");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of(Utils.date("2012-10-10"),
                                                 Utils.date("2014-02-14")));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueSet() {
        schema().propertyKey("time")
                .asDate()
                .valueSet()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                    Utils.date("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2014-02-14");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of(Utils.date("2012-10-10"),
                                                 Utils.date("2014-02-14")));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueListWithSameValue() {
        schema().propertyKey("time")
                .asDate()
                .valueList()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                    Utils.date("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2012-10-10");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of(Utils.date("2012-10-10"),
                                                 Utils.date("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueSetWithSameValue() {
        schema().propertyKey("time")
                .asDate()
                .valueSet()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                    Utils.date("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2012-10-10");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of(Utils.date("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testRemoveVertexProperty() {
        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "city", "Beijing");
        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "city", "Beijing");
        Assert.assertEquals(props, vadas.properties());

        vadas.removeProperty("city");
        props = ImmutableMap.of("name", "vadas", "age", 19);
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testRemoveVertexPropertyNotExist() {
        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "city", "Beijing");
        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "city", "Beijing");
        Assert.assertEquals(props, vadas.properties());

        Assert.assertThrows(InvalidOperationException.class, () -> {
            vadas.removeProperty("not-exist");
        });
    }

    @Test
    public void testGetAllVertices() {
        BaseClientTest.initVertex();

        List<Vertex> vertices = graph().listVertices();
        Assert.assertEquals(6, vertices.size());
        assertContains(vertices, T.label, "person", "name", "marko",
                                 "age", 29, "city", "Beijing");
        assertContains(vertices, T.label, "person", "name", "vadas",
                                 "age", 27, "city", "Hongkong");
        assertContains(vertices, T.label, "software", "name", "lop",
                                 "lang", "java", "price", 328);
        assertContains(vertices, T.label, "person", "name", "josh",
                                 "age", 32, "city", "Beijing");
        assertContains(vertices, T.label, "software", "name", "ripple",
                                 "lang", "java", "price", 199);
        assertContains(vertices, T.label, "person", "name", "peter",
                                 "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesWithNoLimit() {
        BaseClientTest.initVertex();

        List<Vertex> vertices = graph().listVertices(-1);
        Assert.assertEquals(6, vertices.size());
        assertContains(vertices, T.label, "person", "name", "marko",
                                 "age", 29, "city", "Beijing");
        assertContains(vertices, T.label, "person", "name", "vadas",
                                 "age", 27, "city", "Hongkong");
        assertContains(vertices, T.label, "software", "name", "lop",
                                 "lang", "java", "price", 328);
        assertContains(vertices, T.label, "person", "name", "josh",
                                 "age", 32, "city", "Beijing");
        assertContains(vertices, T.label, "software", "name", "ripple",
                                 "lang", "java", "price", 199);
        assertContains(vertices, T.label, "person", "name", "peter",
                                 "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesByLabel() {
        BaseClientTest.initVertex();

        List<Vertex> vertices = graph().listVertices("person");
        Assert.assertEquals(4, vertices.size());
        assertContains(vertices, T.label, "person", "name", "marko",
                                 "age", 29, "city", "Beijing");
        assertContains(vertices, T.label, "person", "name", "vadas",
                                 "age", 27, "city", "Hongkong");
        assertContains(vertices, T.label, "person", "name", "josh",
                                 "age", 32, "city", "Beijing");
        assertContains(vertices, T.label, "person", "name", "peter",
                                 "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesByLabelWithLimit2() {
        BaseClientTest.initVertex();

        List<Vertex> vertices = graph().listVertices("person", 2);
        Assert.assertEquals(2, vertices.size());
        for (Vertex vertex : vertices) {
            Assert.assertEquals("person", vertex.label());
        }
    }

    @Test
    public void testGetVerticesByLabelAndProperties() {
        schema().indexLabel("PersonByAge").onV("person").by("age")
                .secondary().create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("age", 29);
        List<Vertex> vertices = graph().listVertices("person", properties);
        Assert.assertEquals(2, vertices.size());
        assertContains(vertices, T.label, "person", "name", "marko",
                                 "age", 29, "city", "Beijing");
        assertContains(vertices, T.label, "person", "name", "peter",
                                 "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesByLabelAndPropertiesWithLimit1() {
        schema().indexLabel("PersonByAge").onV("person").by("age")
                .secondary().create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("age", 29);
        List<Vertex> vertices = graph().listVertices("person", properties, 0, 1);
        Assert.assertEquals(1, vertices.size());
        Assert.assertEquals("person", vertices.get(0).label());
    }

    private static void assertContains(List<Vertex> vertices,
                                       Object... keyValues) {
        String label = Utils.getLabelValue(keyValues).get();
        Map<String, Object> properties = Utils.asMap(keyValues);

        Vertex vertex = new Vertex(label);
        for (String key : properties.keySet()) {
            if (key.equals(T.label)) {
                continue;
            }
            vertex.property(key, properties.get(key));
        }

        Assert.assertTrue(Utils.contains(vertices, vertex));
    }
}
