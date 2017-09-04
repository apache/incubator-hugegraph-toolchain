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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;
import com.google.common.collect.ImmutableSet;

public class VertexLabelApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
    }

    @After
    public void teardown() throws Exception {
        vertexLabelAPI.list().forEach(vl -> vertexLabelAPI.delete(vl.name()));
    }

    @Test
    public void testCreate() {
        VertexLabel vertexLabel = new VertexLabel("person");
        vertexLabel.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel.properties("name", "age", "city");

        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person", vertexLabel.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel.properties());
    }

    @Test
    public void testCreateWithInvalidName() {
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(new VertexLabel(""));
        });
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(new VertexLabel(" "));
        });
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(new VertexLabel("    "));
        });
    }

    @Test
    public void testCreateExistedVertexLabel() {
        VertexLabel vertexLabel = new VertexLabel("name");
        vertexLabelAPI.create(vertexLabel);

        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(new VertexLabel("name"));
        });
    }

    @Test
    public void testCreateWithUndefinedPropertyKey() {
        VertexLabel vertexLabel = new VertexLabel("person");
        vertexLabel.idStrategy(IdStrategy.PRIMARY_KEY);
        vertexLabel.properties("idcard", "age", "city");
        vertexLabel.primaryKeys("idcard");

        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testCreateWithUnmatchIdStrategyAndProperties() {
        Assert.assertResponse(400, () -> {
            VertexLabel vertexLabel = new VertexLabel("person");
            vertexLabel.properties("name", "age", "city");
            vertexLabel.idStrategy(IdStrategy.AUTOMATIC);
            vertexLabel.primaryKeys("name");
            vertexLabelAPI.create(vertexLabel);
        });
        Assert.assertResponse(400, () -> {
            VertexLabel vertexLabel = new VertexLabel("person");
            vertexLabel.properties("name", "age", "city");
            vertexLabel.idStrategy(IdStrategy.CUSTOMIZE);
            vertexLabel.primaryKeys("name");
            vertexLabelAPI.create(vertexLabel);
        });
        Assert.assertResponse(400, () -> {
            VertexLabel vertexLabel = new VertexLabel("person");
            vertexLabel.properties("name", "age", "city");
            vertexLabel.idStrategy(IdStrategy.PRIMARY_KEY);
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testAppend() {
        VertexLabel vertexLabel1 = new VertexLabel("person");
        vertexLabel1.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel1.properties("name", "age");
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age");
        Assert.assertEquals(props, vertexLabel1.properties());

        VertexLabel vertexLabel2 = new VertexLabel("person");
        vertexLabel2.properties("city");
        vertexLabel2 = vertexLabelAPI.append(vertexLabel2);
        Assert.assertEquals("person", vertexLabel2.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel2.idStrategy());
        props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel2.properties());
    }

    @Test
    public void testAppendWithUndefinedPropertyKey() {
        VertexLabel vertexLabel1 = new VertexLabel("person");
        vertexLabel1.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel1.properties("name", "age");
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age");
        Assert.assertEquals(props, vertexLabel1.properties());

        VertexLabel vertexLabel2 = new VertexLabel("person");
        vertexLabel2.properties("idcard");
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.append(vertexLabel2);
        });
    }

    @Test
    public void testEliminate() {
        VertexLabel vertexLabel1 = new VertexLabel("person");
        vertexLabel1.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel1.properties("name", "age", "city");
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel1.properties());

        VertexLabel vertexLabel2 = new VertexLabel("person");
        vertexLabel2.properties("city");
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.eliminate(vertexLabel2);
        });
    }

    @Test
    public void testGet() {
        VertexLabel vertexLabel1 = new VertexLabel("person");
        vertexLabel1.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel1.properties("name", "age", "city");

        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);

        VertexLabel vertexLabel2 = vertexLabelAPI.get("person");

        Assert.assertEquals(vertexLabel1.name(), vertexLabel2.name());
        Assert.assertEquals(vertexLabel1.idStrategy(),
                            vertexLabel2.idStrategy());
        Assert.assertEquals(vertexLabel1.properties(),
                            vertexLabel2.properties());
    }

    @Test
    public void testGetNotExist() {
        Assert.assertResponse(404, () -> {
            vertexLabelAPI.get("not-exist-vl");
        });
    }

    @Test
    public void testList() {
        VertexLabel vertexLabel1 = new VertexLabel("person");
        vertexLabel1.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel1.properties("name", "age", "city");
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);

        VertexLabel vertexLabel2 = new VertexLabel("software");
        vertexLabel2.idStrategy(IdStrategy.CUSTOMIZE);
        vertexLabel2.properties("name", "lang", "price");
        vertexLabel2 = vertexLabelAPI.create(vertexLabel2);

        List<VertexLabel> vertexLabels = vertexLabelAPI.list();
        Assert.assertEquals(2, vertexLabels.size());
        assertContains(vertexLabels, vertexLabel1);
        assertContains(vertexLabels, vertexLabel2);
    }

    @Test
    public void testDelete() {
        VertexLabel vertexLabel = new VertexLabel("person");
        vertexLabel.idStrategy(IdStrategy.AUTOMATIC);
        vertexLabel.properties("name", "age", "city");
        vertexLabelAPI.create(vertexLabel);

        vertexLabelAPI.delete("person");

        Assert.assertResponse(404, () -> {
            vertexLabelAPI.get("person");
        });
    }

    @Test
    public void testDeleteNotExist() {
        vertexLabelAPI.delete("not-exist-vl");
    }

    private static void assertContains(List<VertexLabel> vertexLabels,
                                       VertexLabel vertexLabel) {
        Assert.assertTrue(Utils.contains(vertexLabels, vertexLabel));
    }
}
