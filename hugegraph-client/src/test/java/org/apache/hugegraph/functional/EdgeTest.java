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

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.BaseClientTest;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.exception.InvalidOperationException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.Result;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;

public class EdgeTest extends BaseFuncTest {

    @Override
    @Before
    public void setup() {
        BaseClientTest.initPropertyKey();
        BaseClientTest.initVertexLabel();
        BaseClientTest.initEdgeLabel();
        BaseClientTest.initVertex();
    }

    @Override
    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testLinkedVertex() {
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
                                       "date", "2017-03-24");
        Assert.assertTrue(created.linkedVertex(peterId));
        Assert.assertTrue(created.linkedVertex(lopId));
    }

    @Test
    public void testAddEdgeProperty() {
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
                                       "date", "2017-03-24");
        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"));
        Assert.assertEquals(props, created.properties());

        created.property("city", "HongKong");
        props = ImmutableMap.of("date", Utils.formatDate("2017-03-24"),
                                "city", "HongKong");
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testUpdateEdgeProperty() {
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
                                       "date", "2017-03-24");
        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"));
        Assert.assertEquals(props, created.properties());

        created.property("date", "2017-08-08");
        props = ImmutableMap.of("date", Utils.formatDate("2017-08-08"));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueList() {
        schema().propertyKey("time")
                .asDate()
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
                                       "date", "2017-03-24",
                                       "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"),
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());

        created.property("time", "2014-02-14");
        props = ImmutableMap.of("date", Utils.formatDate("2017-03-24"),
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10"),
                                        Utils.formatDate("2014-02-14")));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueSet() {
        schema().propertyKey("time")
                .asDate()
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
                                       "date", "2017-03-24",
                                       "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"),
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());

        created.property("time", "2014-02-14");
        props = ImmutableMap.of("date", Utils.formatDate("2017-03-24"),
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10"),
                                        Utils.formatDate("2014-02-14")));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueListWithSameValue() {
        schema().propertyKey("time")
                .asDate()
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
                                       "date", "2017-03-24",
                                       "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"),
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());

        created.property("time", "2012-10-10");
        props = ImmutableMap.of("date", Utils.formatDate("2017-03-24"),
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10"),
                                        Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgePropertyValueSetWithSameValue() {
        schema().propertyKey("time")
                .asDate()
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
                                       "date", "2017-03-24",
                                       "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"),
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());

        created.property("time", "2012-10-10");
        props = ImmutableMap.of("date", Utils.formatDate("2017-03-24"),
                                "time", ImmutableList.of(
                                        Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testAddEdgeWithMapProperties() {
        Vertex peter = getVertex("person", "name", "peter");
        Vertex lop = getVertex("software", "name", "lop");

        Map<String, Object> properties = ImmutableMap.of("date", "2017-03-24",
                                                         "city", "HongKong");
        Edge created = graph().addEdge(peter, "created", lop, properties);
        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"),
                                    "city", "HongKong");
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testRemoveEdgeProperty() {
        schema().propertyKey("time")
                .asDate()
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
                                       "date", "2017-03-24",
                                       "time", "2012-10-10");

        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"),
                                    "time", ImmutableList.of(
                                            Utils.formatDate("2012-10-10")));
        Assert.assertEquals(props, created.properties());

        created.removeProperty("time");
        props = ImmutableMap.of("date", Utils.formatDate("2017-03-24"));
        Assert.assertEquals(props, created.properties());
    }

    @Test
    public void testRemoveEdgePropertyNotExist() {
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");

        Edge created = graph().addEdge(peterId, "created", lopId,
                                       "date", "2017-03-24");
        Map<String, Object> props = ImmutableMap.of(
                                    "date", Utils.formatDate("2017-03-24"));
        Assert.assertEquals(props, created.properties());

        Assert.assertThrows(InvalidOperationException.class, () -> {
            created.removeProperty("not-exist");
        });
    }

    @Test
    public void testName() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");
        List<Edge> edges = graph().getEdges(markoId, Direction.OUT, "knows");
        Assert.assertEquals(2, edges.size());
        Edge edge1 = edges.get(0);
        Edge edge2 = edges.get(1);
        Date date1 = DateUtil.parse((String) edge1.property("date"));
        Date date2 = DateUtil.parse((String) edge2.property("date"));
        String name1 = edge1.name();
        String name2 = edge2.name();
        if (date1.before(date2)) {
            Assert.assertTrue(name1.compareTo(name2) < 0);
        } else {
            Assert.assertTrue(name1.compareTo(name2) >= 0);
        }
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
        assertContains(edges, markoId, "knows", vadasId,
                       "date", Utils.formatDate("2012-01-10"));
        assertContains(edges, markoId, "knows", joshId,
                       "date", Utils.formatDate("2013-01-10"));
        assertContains(edges, markoId, "created", lopId,
                       "date", Utils.formatDate("2014-01-10"),
                       "city", "Shanghai");
        assertContains(edges, joshId, "created", rippleId,
                       "date", Utils.formatDate("2015-01-10"),
                       "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", Utils.formatDate("2016-01-10"),
                       "city", "Beijing");
        assertContains(edges, peterId, "created", lopId,
                       "date", Utils.formatDate("2017-01-10"),
                       "city", "Hongkong");
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
        assertContains(edges, markoId, "knows", vadasId,
                       "date", Utils.formatDate("2012-01-10"));
        assertContains(edges, markoId, "knows", joshId,
                       "date", Utils.formatDate("2013-01-10"));
        assertContains(edges, markoId, "created", lopId,
                       "date", Utils.formatDate("2014-01-10"),
                       "city", "Shanghai");
        assertContains(edges, joshId, "created", rippleId,
                       "date", Utils.formatDate("2015-01-10"),
                       "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", Utils.formatDate("2016-01-10"),
                       "city", "Beijing");
        assertContains(edges, peterId, "created", lopId,
                       "date", Utils.formatDate("2017-01-10"),
                       "city", "Hongkong");
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
        assertContains(edges, markoId, "knows", vadasId,
                       "date", Utils.formatDate("2012-01-10"));
        assertContains(edges, markoId, "knows", joshId,
                       "date", Utils.formatDate("2013-01-10"));
        assertContains(edges, markoId, "created", lopId,
                       "date", Utils.formatDate("2014-01-10"),
                       "city", "Shanghai");
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
                       "date", Utils.formatDate("2015-01-10"),
                       "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", Utils.formatDate("2016-01-10"),
                       "city", "Beijing");

        edges = graph().getEdges(joshId, Direction.IN);
        Assert.assertEquals(1, edges.size());
        assertContains(edges, markoId, "knows", joshId,
                       "date", Utils.formatDate("2013-01-10"));
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
                       "date", Utils.formatDate("2015-01-10"),
                       "city", "Beijing");
        assertContains(edges, joshId, "created", lopId,
                       "date", Utils.formatDate("2016-01-10"),
                       "city", "Beijing");

        edges = graph().getEdges(joshId, Direction.IN, "knows");
        Assert.assertEquals(1, edges.size());
        assertContains(edges, markoId, "knows", joshId,
                       "date", Utils.formatDate("2013-01-10"));
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

        Map<String, Object> properties = ImmutableMap.of(
                                         "date",
                                         Utils.formatDate("2015-01-10"));
        List<Edge> edges = graph().getEdges(joshId, Direction.OUT,
                                            "created", properties);
        Assert.assertEquals(1, edges.size());
        assertContains(edges, joshId, "created", rippleId,
                       "date", Utils.formatDate("2015-01-10"),
                       "city", "Beijing");

        properties = ImmutableMap.of("date", Utils.formatDate("2013-01-10"));
        edges = graph().getEdges(joshId, Direction.IN, "knows", properties);
        Assert.assertEquals(1, edges.size());
        assertContains(edges, markoId, "knows", joshId,
                       "date", Utils.formatDate("2013-01-10"));
    }

    @Test
    public void testGetEdgesByVertexIdDirectionLabelPropertiesWithLimit1() {
        BaseClientTest.initEdge();

        Object joshId = getVertexId("person", "name", "josh");

        Map<String, Object> properties = ImmutableMap.of(
                                         "date",
                                         Utils.formatDate("2015-01-10"));
        List<Edge> edges = graph().getEdges(joshId, Direction.OUT,
                                            "created", properties);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.sourceId());
            Assert.assertEquals("created", edge.label());
        }

        properties = ImmutableMap.of("date", Utils.formatDate("2013-01-10"));
        edges = graph().getEdges(joshId, Direction.IN, "knows", properties);
        Assert.assertEquals(1, edges.size());
        for (Edge edge : edges) {
            Assert.assertEquals(joshId, edge.targetId());
            Assert.assertEquals("knows", edge.label());
        }
    }

    @Test
    public void testGetEdgesByLabelAndPropertiesWithRangeCondition()
                throws ParseException {
        schema().indexLabel("knowsByDate").range()
                .onE("knows").by("date").create();
        schema().indexLabel("createdByDate").range()
                .onE("created").by("date").create();

        BaseClientTest.initEdge();

        Date expected = DateUtil.parse("2014-01-10");
        Date expected2 = DateUtil.parse("2016-01-10");

        Map<String, Object> properties = ImmutableMap.of(
                                         "date", "P.eq(\"2014-1-10\")");
        List<Edge> edges = graph().listEdges("created", properties);

        Date time;
        Assert.assertEquals(1, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("created", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertEquals(expected.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date", "P.gt(\"2014-1-10\")");
        edges = graph().listEdges("created", properties);
        Assert.assertEquals(3, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("created", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertGt(expected.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date", "P.gte(\"2014-1-10\")");
        edges = graph().listEdges("created", properties);
        Assert.assertEquals(4, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("created", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertGte(expected.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date", "P.lt(\"2014-1-10\")");
        edges = graph().listEdges(null, properties);
        Assert.assertEquals(2, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("knows", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertLt(expected.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date", "P.lte(\"2014-1-10\")");
        edges = graph().listEdges(null, properties);
        Assert.assertEquals(3, edges.size());
        for (Edge e : edges) {
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertLte(expected.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date",
                                     "P.between(\"2014-1-10\",\"2016-1-10\")");
        edges = graph().listEdges(null, properties);
        Assert.assertEquals(2, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("created", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertGte(expected.getTime(), time.getTime());
            Assert.assertLt(expected2.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date",
                                     "P.inside(\"2014-1-10\",\"2016-1-10\")");
        edges = graph().listEdges(null, properties);
        Assert.assertEquals(1, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("created", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertGt(expected.getTime(), time.getTime());
            Assert.assertLt(expected2.getTime(), time.getTime());
        }

        properties = ImmutableMap.of("date",
                                     "P.within(\"2014-1-10\",\"2016-1-10\")");
        edges = graph().listEdges(null, properties);
        Assert.assertEquals(2, edges.size());
        for (Edge e : edges) {
            Assert.assertEquals("created", e.label());
            time = DateUtil.parse((String) e.property("date"));
            Assert.assertGte(expected.getTime(), time.getTime());
            Assert.assertLte(expected2.getTime(), time.getTime());
        }
    }

    @Test
    public void testGetEdgesByLabelAndPropertiesWithKeepP()
                throws ParseException {
        schema().indexLabel("createdByCity").secondary()
                .onE("created").by("city").create();
        schema().indexLabel("createdByDate").secondary()
                .onE("created").by("date").create();

        BaseClientTest.initEdge();

        Map<String, Object> properties = ImmutableMap.of(
                                         "date", "P.eq(\"2014-1-10\")");
        List<Edge> edges = graph().listEdges("created", properties, false);
        Assert.assertEquals(1, edges.size());

        Assert.assertThrows(ServerException.class, () -> {
            graph().listEdges("created", properties, true);
        }, e -> {
            Assert.assertContains("Expected date format is:", e.getMessage());
        });

        Map<String, Object> properties2 = ImmutableMap.of("city", "P.gt(1)");
        edges = graph().listEdges("created", properties2, true);
        Assert.assertEquals(0, edges.size());

        edges = graph().listEdges("created", properties2, true, 3);
        Assert.assertEquals(0, edges.size());
    }

    @Test
    public void testIterateEdgesByLabel() {
        BaseClientTest.initEdge();

        Iterator<Edge> edges = graph().iterateEdges("created", 1);
        Assert.assertEquals(4, Iterators.size(edges));

        edges = graph().iterateEdges("knows", 1);
        Assert.assertEquals(2, Iterators.size(edges));
    }

    @Test
    public void testIterateEdgesByVertexId() {
        BaseClientTest.initEdge();

        Object markoId = getVertexId("person", "name", "marko");

        Iterator<Edge> edges = graph().iterateEdges(markoId, 1);
        Assert.assertEquals(3, Iterators.size(edges));

        edges = graph().iterateEdges(markoId, Direction.OUT, 1);
        Assert.assertEquals(3, Iterators.size(edges));

        edges = graph().iterateEdges(markoId, Direction.OUT, "knows", 1);
        Assert.assertEquals(2, Iterators.size(edges));

        edges = graph().iterateEdges(markoId, Direction.OUT, "created", 1);
        Assert.assertEquals(1, Iterators.size(edges));

        Map<String, Object> properties = ImmutableMap.of("date",
                                                         "P.gt(\"2012-1-1\")");
        Iterator<Edge> iter = graph().iterateEdges(markoId, Direction.OUT,
                                                   "knows", properties, 1);
        Assert.assertEquals(2, Iterators.size(iter));
    }

    @Test
    public void testQueryByPagingAndFiltering() {
        SchemaManager schema = schema();
        schema.propertyKey("no").asText().create();
        schema.propertyKey("location").asText().create();
        schema.propertyKey("callType").asText().create();
        schema.propertyKey("calltime").asDate().create();
        schema.propertyKey("duration").asInt().create();
        schema.vertexLabel("phone")
              .properties("no")
              .primaryKeys("no")
              .enableLabelIndex(false)
              .create();
        schema.edgeLabel("call").multiTimes()
              .properties("location", "callType", "duration", "calltime")
              .sourceLabel("phone").targetLabel("phone")
              .sortKeys("location", "callType", "duration", "calltime")
              .create();

        Vertex v1 = graph().addVertex(T.LABEL, "phone", "no", "13812345678");
        Vertex v2 = graph().addVertex(T.LABEL, "phone", "no", "13866668888");
        Vertex v10086 = graph().addVertex(T.LABEL, "phone", "no", "10086");

        v1.addEdge("call", v2, "location", "Beijing", "callType", "work",
                   "duration", 3, "calltime", "2017-5-1 23:00:00");
        v1.addEdge("call", v2, "location", "Beijing", "callType", "work",
                   "duration", 3, "calltime", "2017-5-2 12:00:01");
        v1.addEdge("call", v2, "location", "Beijing", "callType", "work",
                   "duration", 3, "calltime", "2017-5-3 12:08:02");
        v1.addEdge("call", v2, "location", "Beijing", "callType", "work",
                   "duration", 8, "calltime", "2017-5-3 22:22:03");
        v1.addEdge("call", v2, "location", "Beijing", "callType", "fun",
                   "duration", 10, "calltime", "2017-5-4 20:33:04");

        v1.addEdge("call", v10086, "location", "Nanjing", "callType", "work",
                   "duration", 12, "calltime", "2017-5-2 15:30:05");
        v1.addEdge("call", v10086, "location", "Nanjing", "callType", "work",
                   "duration", 14, "calltime", "2017-5-3 14:56:06");
        v2.addEdge("call", v10086, "location", "Nanjing", "callType", "fun",
                   "duration", 15, "calltime", "2017-5-3 17:28:07");

        ResultSet resultSet = gremlin().gremlin("g.V(vid).outE('call')" +
                                                ".has('location', 'Beijing')" +
                                                ".has('callType', 'work')" +
                                                ".has('duration', 3)" +
                                                ".has('calltime', " +
                                                "P.between('2017-5-2', " +
                                                "'2017-5-4'))" +
                                                ".toList()")
                                       .binding("vid", v1.id())
                                       .execute();
        Iterator<Result> results = resultSet.iterator();
        Assert.assertEquals(2, Iterators.size(results));

        Assert.assertThrows(ServerException.class, () -> {
            // no location
            gremlin().gremlin("g.V(vid).outE('call').has('callType', 'work')" +
                              ".has('duration', 3).has('calltime', " +
                              "P.between('2017-5-2', '2017-5-4'))" +
                              ".has('~page', '')")
                     .binding("vid", v1.id())
                     .execute();
        }, e -> {
            Assert.assertContains("Can't query by paging and filtering",
                                  e.getMessage());
        });
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
