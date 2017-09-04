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

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.Frequency;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
import com.google.common.collect.ImmutableSet;

public class EdgeLabelApiTest extends BaseApiTest {

    private static Function<EdgeLabel, EdgeLabel> fillEdgeLabel =
            (edgeLabel) -> {
                edgeLabel.sourceLabel("person");
                edgeLabel.targetLabel("software");
                edgeLabel.frequency(Frequency.SINGLE);
                edgeLabel.properties("date", "city");
                return edgeLabel;
            };

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
    }

    @After
    public void teardown() throws Exception {
        edgeLabelAPI.list().forEach(el -> edgeLabelAPI.delete(el.name()));
    }

    @Test
    public void testCreate() {
        EdgeLabel edgeLabel = new EdgeLabel("created");
        edgeLabel = edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel));

        Assert.assertEquals("created", edgeLabel.name());
        Assert.assertEquals("person", edgeLabel.sourceLabel());
        Assert.assertEquals("software", edgeLabel.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel.frequency());
        Set<String> props = ImmutableSet.of("date", "city");
        Assert.assertTrue(props.size() == edgeLabel.properties().size());
        Assert.assertTrue(props.containsAll(edgeLabel.properties()));
    }

    @Test
    public void testCreateWithInvalidName() {
        Assert.assertResponse(400, () -> {
            EdgeLabel edgeLabel = new EdgeLabel("");
            edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel));
        });
        Assert.assertResponse(400, () -> {
            EdgeLabel edgeLabel = new EdgeLabel(" ");
            edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel));
        });
        Assert.assertResponse(400, () -> {
            EdgeLabel edgeLabel = new EdgeLabel("    ");
            edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel));
        });
    }

    @Test
    public void testCreateExistedVertexLabel() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel1));

        Assert.assertResponse(400, () -> {
            EdgeLabel edgeLabel2 = new EdgeLabel("created");
            edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel2));
        });
    }

    @Test
    public void testCreateWithUndefinedPropertyKey() {
        EdgeLabel edgeLabel = new EdgeLabel("created");
        edgeLabel.sourceLabel("person");
        edgeLabel.targetLabel("software");
        edgeLabel.frequency(Frequency.SINGLE);
        edgeLabel.properties("time", "city");

        Assert.assertResponse(400, () -> {
            edgeLabelAPI.create(edgeLabel);
        });
    }

    @Test
    public void testCreateWithUndefinedVertexLabel() {
        EdgeLabel edgeLabel = new EdgeLabel("created");
        edgeLabel.sourceLabel("programmer");
        edgeLabel.targetLabel("software");
        edgeLabel.frequency(Frequency.SINGLE);
        edgeLabel.properties("date", "city");

        Assert.assertResponse(400, () -> {
            edgeLabelAPI.create(edgeLabel);
        });
    }

    @Test
    public void testAppend() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabel1.sourceLabel("person");
        edgeLabel1.targetLabel("software");
        edgeLabel1.frequency(Frequency.SINGLE);
        edgeLabel1.properties("date");
        edgeLabel1 = edgeLabelAPI.create(edgeLabel1);

        Assert.assertEquals("created", edgeLabel1.name());
        Assert.assertEquals("person", edgeLabel1.sourceLabel());
        Assert.assertEquals("software", edgeLabel1.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel1.frequency());
        Set<String> props = ImmutableSet.of("date");
        Assert.assertEquals(props, edgeLabel1.properties());

        EdgeLabel edgeLabel2 = new EdgeLabel("created");
        edgeLabel2.properties("city");
        edgeLabel2 = edgeLabelAPI.append(edgeLabel2);

        Assert.assertEquals("created", edgeLabel2.name());
        Assert.assertEquals("person", edgeLabel2.sourceLabel());
        Assert.assertEquals("software", edgeLabel2.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel2.frequency());
        props = ImmutableSet.of("date", "city");
        Assert.assertEquals(props, edgeLabel2.properties());
    }

    @Test
    public void testAppendWithUndefinedPropertyKey() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabel1.sourceLabel("person");
        edgeLabel1.targetLabel("software");
        edgeLabel1.frequency(Frequency.SINGLE);
        edgeLabel1.properties("date");
        edgeLabel1 = edgeLabelAPI.create(edgeLabel1);

        Assert.assertEquals("created", edgeLabel1.name());
        Assert.assertEquals("person", edgeLabel1.sourceLabel());
        Assert.assertEquals("software", edgeLabel1.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel1.frequency());
        Set<String> props = ImmutableSet.of("date");
        Assert.assertEquals(props, edgeLabel1.properties());

        EdgeLabel edgeLabel2 = new EdgeLabel("created");
        edgeLabel2.properties("time");

        Assert.assertResponse(400, () -> {
            edgeLabelAPI.append(edgeLabel2);
        });
    }

    @Test
    public void testAppendWithSourceOrTaregtLabel() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabel1.sourceLabel("person");
        edgeLabel1.targetLabel("software");
        edgeLabel1.frequency(Frequency.SINGLE);
        edgeLabel1.properties("date");
        edgeLabel1 = edgeLabelAPI.create(edgeLabel1);

        Assert.assertEquals("created", edgeLabel1.name());
        Assert.assertEquals("person", edgeLabel1.sourceLabel());
        Assert.assertEquals("software", edgeLabel1.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel1.frequency());
        Set<String> props = ImmutableSet.of("date");
        Assert.assertEquals(props, edgeLabel1.properties());

        EdgeLabel edgeLabel2 = new EdgeLabel("created");
        edgeLabel2.sourceLabel("person");
        edgeLabel2.targetLabel("person");
        edgeLabel2.properties("city");

        Assert.assertResponse(400, () -> {
            edgeLabelAPI.append(edgeLabel2);
        });
    }

    @Test
    public void testEliminate() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabel1.sourceLabel("person");
        edgeLabel1.targetLabel("software");
        edgeLabel1.frequency(Frequency.SINGLE);
        edgeLabel1.properties("date");
        edgeLabel1 = edgeLabelAPI.create(edgeLabel1);
        Assert.assertEquals("created", edgeLabel1.name());
        Assert.assertEquals("person", edgeLabel1.sourceLabel());
        Assert.assertEquals("software", edgeLabel1.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel1.frequency());
        Set<String> props = ImmutableSet.of("date");
        Assert.assertEquals(props, edgeLabel1.properties());

        EdgeLabel edgeLabel2 = new EdgeLabel("created");
        edgeLabel2.properties("city");
        Assert.assertResponse(400, () -> {
            edgeLabelAPI.eliminate(edgeLabel2);
        });
    }

    @Test
    public void testGet() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabel1 = edgeLabelAPI.create(fillEdgeLabel.apply(edgeLabel1));

        EdgeLabel edgeLabel2 = edgeLabelAPI.get("created");

        Assert.assertEquals(edgeLabel1.name(), edgeLabel2.name());
        Assert.assertEquals(edgeLabel1.sourceLabel(), edgeLabel2.sourceLabel());
        Assert.assertEquals(edgeLabel1.targetLabel(), edgeLabel2.targetLabel());
        Assert.assertEquals(edgeLabel1.frequency(), edgeLabel2.frequency());
        Assert.assertEquals(edgeLabel1.properties(), edgeLabel2.properties());
    }

    @Test
    public void testGetNotExist() {
        Assert.assertResponse(404, () -> {
            edgeLabelAPI.get("not-exist-el");
        });
    }

    @Test
    public void testList() {
        EdgeLabel edgeLabel1 = new EdgeLabel("created");
        edgeLabel1.sourceLabel("person");
        edgeLabel1.targetLabel("software");
        edgeLabel1.frequency(Frequency.SINGLE);
        edgeLabel1.properties("date", "city");
        edgeLabel1 = edgeLabelAPI.create(edgeLabel1);

        EdgeLabel edgeLabel2 = new EdgeLabel("knows");
        edgeLabel2.sourceLabel("person");
        edgeLabel2.targetLabel("person");
        edgeLabel2.frequency(Frequency.SINGLE);
        edgeLabel2.properties("date");
        edgeLabel2 = edgeLabelAPI.create(edgeLabel2);

        List<EdgeLabel> edgeLabels = edgeLabelAPI.list();
        Assert.assertEquals(2, edgeLabels.size());
        assertContains(edgeLabels, edgeLabel1);
        assertContains(edgeLabels, edgeLabel2);
    }

    @Test
    public void testDelete() {
        EdgeLabel edgeLabel = new EdgeLabel("created");
        edgeLabel.sourceLabel("person");
        edgeLabel.targetLabel("software");
        edgeLabel.frequency(Frequency.SINGLE);
        edgeLabel.properties("date", "city");
        edgeLabelAPI.create(edgeLabel);

        edgeLabelAPI.delete("created");

        Assert.assertResponse(404, () -> {
            edgeLabelAPI.get("created");
        });
    }

    @Test
    public void testDeleteNotExist() {
        edgeLabelAPI.delete("not-exist-el");
    }

    private static void assertContains(List<EdgeLabel> edgeLabels,
                                       EdgeLabel edgeLabel) {
        Assert.assertTrue(Utils.contains(edgeLabels, edgeLabel));
    }
}
