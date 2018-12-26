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
import com.baidu.hugegraph.structure.constant.Direction;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
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
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
                                       "date", "20170324");
        Map<String, Object> props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, created.properties());

        created.property("city", "HongKong");
        props = ImmutableMap.of("date", "20170324", "city", "HongKong");
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testUpdateEdgeProperty() {
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
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

        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
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

        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
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

        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
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

        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
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

        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
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
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
                                       "date", "20170324");
        Map<String, Object> props = ImmutableMap.of("date", "20170324");
        Assert.assertEquals(props, created.properties());

        Assert.assertThrows(InvalidOperationException.class, () -> {
            created.removeProperty("not-exist");
        });
    }

    @Test
    public void testGetAllEdges() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");
        
        List<Edge> edges = graph().listEdges();
        Assert.assertEquals(6, edges.size());
        assertContains(edges, markoId, "knows", vadasId, "date", "20120110");
        assertContains(edges, markoId, "knows", joshId, "date", "20130110");
        assertContains(edges, markoId, "created", lopId,
                       "date", "20140110", "city", "Shanghai");
        assertContains(edges, joshId, "created", rippleId,
                       "date", "20150110", "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", "20160110", "city", "Beijing");
        assertContains(edges, peterId, "created", lopId,
                       "date", "20170110", "city", "Hongkong");
    }

    @Test
    public void testGetAllEdgesWithNoLimit() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Edge> edges = graph().listEdges(-1);
        Assert.assertEquals(6, edges.size());
        assertContains(edges, markoId, "knows", vadasId, "date", "20120110");
        assertContains(edges, markoId, "knows", joshId, "date", "20130110");
        assertContains(edges, markoId, "created", lopId,
                       "date", "20140110", "city", "Shanghai");
        assertContains(edges, joshId, "created", rippleId,
                       "date", "20150110", "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", "20160110", "city", "Beijing");
        assertContains(edges, peterId, "created", lopId,
                       "date", "20170110", "city", "Hongkong");
    }

    @Test
    public void testGetEdgesByVertexId() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");

        List<Edge> edges = graph().getEdges(markoId);
        Assert.assertEquals(3, edges.size());
        assertContains(edges, markoId, "knows", vadasId, "date", "20120110");
        assertContains(edges, markoId, "knows", joshId, "date", "20130110");
        assertContains(edges, markoId, "created", lopId,
                       "date", "20140110", "city", "Shanghai");
    }

    @Test
    public void testGetEdgesByVertexIdWithLimit2() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");

        List<Edge> edges = graph().getEdges(markoId, 2);
        Assert.assertEquals(2, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(markoId, edge.sourceId());
        }
    }

    @Test
    public void testGetEdgesByVertexIdDirection() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Edge> edges = graph().getEdges(joshId, Direction.OUT);
        Assert.assertEquals(2, edges.size());
        assertContains(edges, joshId, "created", rippleId,
                       "date", "20150110", "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", "20160110", "city", "Beijing");

        edges = graph().getEdges(joshId, Direction.IN);
        Assert.assertEquals(1, edges.size());
        assertContains(edges, markoId, "knows", joshId, "date", "20130110");
    }

    @Test
    public void testGetEdgesByVertexIdDirectionWithLimit1() {
        BaseClientTest.initEdge();

        Object joshId = getVertexId("person", "name", "josh");

        List<Edge> edges = graph().getEdges(joshId, Direction.OUT, 1);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            // TODO: Whether need to add direction property in Edge?
            Assert.assertEquals(joshId, edge.sourceId());
        }

        edges = graph().getEdges(joshId, Direction.IN, 1);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.targetId());
        }
    }

    @Test
    public void testGetEdgesByVertexIdDirectionLabel() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        List<Edge> edges = graph().getEdges(joshId, Direction.OUT,
                                            "created");
        Assert.assertEquals(2, edges.size());
        assertContains(edges, joshId, "created", rippleId,
                       "date", "20150110", "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", "20160110", "city", "Beijing");

        edges = graph().getEdges(joshId, Direction.IN, "knows");
        Assert.assertEquals(1, edges.size());
        assertContains(edges, markoId, "knows", joshId, "date", "20130110");
    }

    @Test
    public void testGetEdgesByVertexIdDirectionLabelWithLimit1() {
        BaseClientTest.initEdge();

        Object joshId = getVertexId("person", "name", "josh");

        List<Edge> edges = graph().getEdges(joshId, Direction.OUT,
                                            "created", 1);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.sourceId());
            Assert.assertEquals("created", edge.label());
        }

        edges = graph().getEdges(joshId, Direction.IN, "knows", 1);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.targetId());
            Assert.assertEquals("knows", edge.label());
        }
    }

    @Test
    public void testGetEdgesByVertexIdDirectionLabelProperties() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        Object joshId = getVertexId("person", "name", "josh");
        Object rippleId = getVertexId("software", "name", "ripple");

        Map<String, Object> properties = ImmutableMap.of("date", "20150110");
        List<Edge> edges = graph().getEdges(joshId, Direction.OUT,
                                            "created", properties);
        Assert.assertEquals(1, edges.size());
        assertContains(edges, joshId, "created", rippleId,
                       "date", "20150110", "city", "Beijing");

        properties = ImmutableMap.of("date", "20130110");
        edges = graph().getEdges(joshId, Direction.IN, "knows", properties);
        Assert.assertEquals(1, edges.size());
        assertContains(edges, markoId, "knows", joshId, "date", "20130110");
    }

    @Test
    public void testGetEdgesByVertexIdDirectionLabelPropertiesWithLimit1() {
        BaseClientTest.initEdge();

        Object joshId = getVertexId("person", "name", "josh");

        Map<String, Object> properties = ImmutableMap.of("date", "20150110");
        List<Edge> edges = graph().getEdges(joshId, Direction.OUT,
                                            "created", properties);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.sourceId());
            Assert.assertEquals("created", edge.label());
        }

        properties = ImmutableMap.of("date", "20130110");
        edges = graph().getEdges(joshId, Direction.IN, "knows", properties);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.targetId());
            Assert.assertEquals("knows", edge.label());
        }
    }
    
    private static void assertContains(List<Edge> edges, Object source,
                                       String label, Object target,
                                       Object... keyValues) {
        Map<String, Object> properties = Utils.asMap(keyValues);

        Edge edge = new Edge(label);
        edge.sourceId(source);
        edge.targetId(target);
        for (String key : properties.keySet()) {
            edge.property(key, properties.get(key));
        }

        Assert.assertTrue(Utils.contains(edges, edge));
    }
}
