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
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class EdgeTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseClientTest.initPropertyKey();
        BaseClientTest.initVertexLabel();
        BaseClientTest.initEdgeLabel();
        BaseClientTest.initVertex();
    }

    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testAddEdgeProperty() {
        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324");
        Map<String, Object> props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, created.properties());

        created.property("city", "HongKong");
        props = ImmutableMap.of("date", "20170324", "city", "HongKong");
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testUpdateEdgeProperty() {
        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324");
        Map<String, Object> props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, created.properties());

        created.property("date", "20170808");
        props = ImmutableMap.of("date", "20170808");
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueList() {
        schema().propertyKey("time")
                .asText()
                .valueList()
                .ifNotExist()
                .create();
        schema().edgeLabel("created")
                .properties("time")
                .nullableKeys("time")
                .append();

        Edge created = graph().addEdge("person:peter", "created",
                                        "software:lop",
                                        "date", "20170324",
                                        "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, created.properties());

        created.property("time", "20140214");
        props = ImmutableMap.of("date", "20170324", "time",
                                ImmutableList.of("20121010", "20140214"));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueSet() {
        schema().propertyKey("time")
                .asText()
                .valueSet()
                .ifNotExist()
                .create();
        schema().edgeLabel("created")
                .properties("time")
                .nullableKeys("time")
                .append();

        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324",
                                       "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, created.properties());

        created.property("time", "20140214");
        props = ImmutableMap.of("date", "20170324", "time",
                                ImmutableList.of("20140214", "20121010"));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueListWithSameValue() {
        schema().propertyKey("time")
                .asText()
                .valueList()
                .ifNotExist()
                .create();
        schema().edgeLabel("created")
                .properties("time")
                .nullableKeys("time")
                .append();

        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324",
                                       "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, created.properties());

        created.property("time", "20121010");
        props = ImmutableMap.of("date", "20170324", "time",
                                ImmutableList.of("20121010", "20121010"));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueSetWithSameValue() {
        schema().propertyKey("time")
                .asText()
                .valueSet()
                .ifNotExist()
                .create();
        schema().edgeLabel("created")
                .properties("time")
                .nullableKeys("time")
                .append();

        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324",
                                       "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, created.properties());

        created.property("time", "20121010");
        props = ImmutableMap.of("date", "20170324", "time",
                                ImmutableList.of("20121010"));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testRemoveEdgeProperty() {
        schema().propertyKey("time")
                .asText()
                .valueSet()
                .ifNotExist()
                .create();
        schema().edgeLabel("created")
                .properties("time")
                .nullableKeys("time")
                .append();

        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324",
                                       "time", "20121010");

        Map<String, Object> props = ImmutableMap.of("date", "20170324",
                                                    "time", ImmutableList.of(
                                                    "20121010"));
        Assert.assertEquals(props, created.properties());

        created.removeProperty("time");
        props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testRemoveEdgePropertyNotExist() {
        Edge created = graph().addEdge("person:peter", "created",
                                       "software:lop",
                                       "date", "20170324");
        Map<String, Object> props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, created.properties());

        Assert.assertThrows(InvalidOperationException.class, () -> {
            created.removeProperty("not-exist");
        });
    }
}
