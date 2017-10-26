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

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.client.BaseClientTest;
import com.baidu.hugegraph.exception.InvalidOperationException;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;
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
                .asText()
                .valueList()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                            "20121010"));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "20140214");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of("20121010", "20140214"));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueSet() {
        schema().propertyKey("time")
                .asText()
                .valueSet()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                            "20121010"));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "20140214");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of("20140214", "20121010"));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueListWithSameValue() {
        schema().propertyKey("time")
                .asText()
                .valueList()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "20121010");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of("20121010", "20121010"));
        Assert.assertEquals(props, vadas.properties());
    }

    @Test
    public void testAddVertexPropertyValueSetWithSameValue() {
        schema().propertyKey("time")
                .asText()
                .valueSet()
                .ifNotExist()
                .create();
        schema().vertexLabel("person")
                .properties("time")
                .nullableKeys("time")
                .append();

        Vertex vadas = graph().addVertex(T.label, "person", "name", "vadas",
                                         "age", 19, "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("name", "vadas",
                                                    "age", 19,
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, vadas.properties());

        vadas.property("time", "20121010");
        props = ImmutableMap.of("name", "vadas", "age", 19, "time",
                                ImmutableList.of("20121010"));
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
}
