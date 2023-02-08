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

package org.apache.hugegraph.functional;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.BaseClientTest;
import org.apache.hugegraph.exception.InvalidOperationException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;

public class VertexTest extends BaseFuncTest {

    @Override
    @Before
    public void setup() {
        BaseClientTest.initPropertyKey();
        BaseClientTest.initVertexLabel();
        BaseClientTest.initEdgeLabel();
    }

    @Override
    @After
    public void teardown() throws Exception {
        clearData();
    }

    @Test
    public void testAddVertexProperty() {
        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
                                         "age", 19);
        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19);
        Assert.assertEquals(props, vadas.properties());

        vadas.property("city", "Beijing");
        props = ImmutableMap.of("name", "vadas", "age", 19,
                                "city", "Beijing");
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testUpdateVertexProperty() {
        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
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

        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "name", "vadas", "age", 19,
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2014-02-14");
        props = ImmutableMap.of("name", "vadas", "age", 19,
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10"),
                                        Utils.formatDate("2014-02-14")));
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

        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "name", "vadas", "age", 19,
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2014-02-14");
        props = ImmutableMap.of("name", "vadas", "age", 19,
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10"),
                                        Utils.formatDate("2014-02-14")));
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

        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "name", "vadas", "age", 19,
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2012-10-10");
        props = ImmutableMap.of("name", "vadas", "age", 19,
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10"),
                                        Utils.formatDate("2012-10-10")));
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

        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
                                         "age", 19, "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "name", "vadas", "age", 19,
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "2012-10-10");
        props = ImmutableMap.of("name", "vadas", "age", 19,
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexWithMapProperties() {
        Map<String, Object> properties = ImmutableMap.of("name", "vadas",
                                                         "age", 19,
                                                         "city", "Beijing");
        // 1st param is label
        Vertex vadas = graph().addVertex("person", properties);
        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "city", "Beijing");
        Assert.assertEquals(props, vadas.properties());

        properties = ImmutableMap.of("name", "java in action", "price", 88);
        // 1st param is label, 2nd param is id
        Vertex java = graph().addVertex("book", "ISBN-1", properties);
        props = ImmutableMap.of("name", "java in action", "price", 88);
        Assert.assertEquals(props, java.properties());
    }

    @Test
    public void testRemoveVertexProperty() {
        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
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
        Vertex vadas = graph().addVertex(T.LABEL, "person", "name", "vadas",
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
        assertContains(vertices, T.LABEL, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, T.LABEL, "person", "name", "vadas",
                       "age", 27, "city", "Hongkong");
        assertContains(vertices, T.LABEL, "software", "name", "lop",
                       "lang", "java", "price", 328);
        assertContains(vertices, T.LABEL, "person", "name", "josh",
                       "age", 32, "city", "Beijing");
        assertContains(vertices, T.LABEL, "software", "name", "ripple",
                       "lang", "java", "price", 199);
        assertContains(vertices, T.LABEL, "person", "name", "peter",
                       "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesWithNoLimit() {
        BaseClientTest.initVertex();

        List<Vertex> vertices = graph().listVertices(-1);
        Assert.assertEquals(6, vertices.size());
        assertContains(vertices, T.LABEL, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, T.LABEL, "person", "name", "vadas",
                       "age", 27, "city", "Hongkong");
        assertContains(vertices, T.LABEL, "software", "name", "lop",
                       "lang", "java", "price", 328);
        assertContains(vertices, T.LABEL, "person", "name", "josh",
                       "age", 32, "city", "Beijing");
        assertContains(vertices, T.LABEL, "software", "name", "ripple",
                       "lang", "java", "price", 199);
        assertContains(vertices, T.LABEL, "person", "name", "peter",
                       "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesByLabel() {
        BaseClientTest.initVertex();

        List<Vertex> vertices = graph().listVertices("person");
        Assert.assertEquals(4, vertices.size());
        assertContains(vertices, T.LABEL, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, T.LABEL, "person", "name", "vadas",
                       "age", 27, "city", "Hongkong");
        assertContains(vertices, T.LABEL, "person", "name", "josh",
                       "age", 32, "city", "Beijing");
        assertContains(vertices, T.LABEL, "person", "name", "peter",
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
        schema().indexLabel("personByAge").range()
                .onV("person").by("age").create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("age", 29);
        List<Vertex> vertices = graph().listVertices("person", properties);
        Assert.assertEquals(2, vertices.size());
        assertContains(vertices, T.LABEL, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, T.LABEL, "person", "name", "peter",
                       "age", 29, "city", "Shanghai");
    }

    @Test
    public void testGetVerticesByLabelAndPropertiesWithLimit1() {
        schema().indexLabel("personByAge").range()
                .onV("person").by("age").create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("age", 29);
        List<Vertex> vertices = graph().listVertices("person", properties, 1);
        Assert.assertEquals(1, vertices.size());
        Assert.assertEquals("person", vertices.get(0).label());
    }

    @Test
    public void testGetVerticesByLabelAndPropertiesWithRangeCondition() {
        schema().indexLabel("personByAge").range()
                .onV("person").by("age").create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("age", "P.eq(29)");
        List<Vertex> vertices = graph().listVertices("person", properties);
        Assert.assertEquals(2, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertEquals(29, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.gt(29)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(1, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertGt(29, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.gte(29)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(3, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertGte(29, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.lt(29)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(1, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertLt(29, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.lte(29)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(3, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertLte(29, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.between(29,32)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(2, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertGte(29, v.property("age"));
            Assert.assertLt(31, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.inside(27,32)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(2, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertGt(27, v.property("age"));
            Assert.assertLt(32, v.property("age"));
        }

        properties = ImmutableMap.of("age", "P.within(27,32)");
        vertices = graph().listVertices("person", properties);
        Assert.assertEquals(2, vertices.size());
        for (Vertex v : vertices) {
            Assert.assertEquals("person", v.label());
            Assert.assertThat(v.property("age"), CoreMatchers.anyOf(
                              CoreMatchers.is(27), CoreMatchers.is(32)));
        }
    }

    @Test
    public void testGetVerticesByLabelAndPropertiesWithKeepP() {
        schema().indexLabel("personByAge").range()
                .onV("person").by("age").create();
        schema().indexLabel("personByCity").secondary()
                .onV("person").by("city").create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("age", "P.eq(29)");
        List<Vertex> vertices = graph().listVertices("person", properties,
                                                     false);
        Assert.assertEquals(2, vertices.size());

        Assert.assertThrows(ServerException.class, () -> {
            graph().listVertices("person", properties, true);
        }, e -> {
            Assert.assertContains("expect INT for 'age'", e.getMessage());
        });

        Map<String, Object> properties2 = ImmutableMap.of("city", "P.gt(1)");
        vertices = graph().listVertices("person", properties2, true);
        Assert.assertEquals(0, vertices.size());

        vertices = graph().listVertices("person", properties2, true, 3);
        Assert.assertEquals(0, vertices.size());
    }

    @Test
    public void testIterateVerticesByLabel() {
        BaseClientTest.initVertex();

        Iterator<Vertex> vertices = graph().iterateVertices("person", 1);
        Assert.assertEquals(4, Iterators.size(vertices));

        vertices = graph().iterateVertices("software", 1);
        Assert.assertEquals(2, Iterators.size(vertices));
    }

    @Test
    public void testIterateVerticesByLabelAndProperties() {
        schema().indexLabel("personByCity").secondary()
                .onV("person").by("city").create();
        BaseClientTest.initVertex();

        Map<String, Object> properties = ImmutableMap.of("city", "Beijing");
        Iterator<Vertex> vertices = graph().iterateVertices("person",
                                                            properties, 1);
        Assert.assertEquals(2, Iterators.size(vertices));
    }

    private static void assertContains(List<Vertex> vertices,
                                       Object... keyValues) {
        String label = Utils.getLabelValue(keyValues).get();
        Map<String, Object> properties = Utils.asMap(keyValues);

        Vertex vertex = new Vertex(label);
        for (String key : properties.keySet()) {
            if (key.equals(T.LABEL)) {
                continue;
            }
            vertex.property(key, properties.get(key));
        }

        Assert.assertTrue(Utils.contains(vertices, vertex));
    }
}
