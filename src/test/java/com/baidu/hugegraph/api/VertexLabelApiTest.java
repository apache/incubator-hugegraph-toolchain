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
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .useAutomaticId()
                                          .properties("name", "age", "city")
                                          .build();

        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person", vertexLabel.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel.idStrategy());
        Assert.assertEquals(true, vertexLabel.enableLabelIndex());
        Set<String> props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel.properties());
    }

    @Test
    public void testCreateWithEnableLabelIndexFalse() {
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .useAutomaticId()
                                          .properties("name", "age", "city")
                                          .enableLabelIndex(false)
                                          .build();

        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person", vertexLabel.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel.idStrategy());
        Assert.assertEquals(false, vertexLabel.enableLabelIndex());
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
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .usePrimaryKeyId()
                                          .properties("name", "undefined")
                                          .primaryKeys("name")
                                          .build();

        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testCreateWithUndefinedPrimaryKey() {
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .usePrimaryKeyId()
                                          .properties("name", "age", "city")
                                          .primaryKeys("undefined")
                                          .build();

        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testCreateWithUndefinedNullableKeys() {
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .usePrimaryKeyId()
                                          .properties("name", "age", "city")
                                          .primaryKeys("name")
                                          .nullableKeys("undefined")
                                          .build();

        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testCreateWithNonNullKeysIntersectPrimaryKeys() {
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .usePrimaryKeyId()
                                           .properties("name", "age", "city")
                                           .primaryKeys("name")
                                           .nullableKeys("name")
                                           .build();
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel1);
        });

        VertexLabel vertexLabel2 = schema().vertexLabel("person")
                                           .usePrimaryKeyId()
                                           .properties("name", "age", "city")
                                           .primaryKeys("name", "age")
                                           .nullableKeys("name")
                                           .build();
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel2);
        });

        VertexLabel vertexLabel3 = schema().vertexLabel("person")
                                           .usePrimaryKeyId()
                                           .properties("name", "age", "city")
                                           .primaryKeys("name")
                                           .nullableKeys("name", "age")
                                           .build();
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.create(vertexLabel3);
        });
    }

    @Test
    public void testCreateWithUnmatchIdStrategyAndProperties() {
        Assert.assertResponse(400, () -> {
            VertexLabel vertexLabel = schema().vertexLabel("person")
                                              .properties("name", "age", "city")
                                              .useAutomaticId()
                                              .primaryKeys("name")
                                              .build();
            vertexLabelAPI.create(vertexLabel);
        });
        Assert.assertResponse(400, () -> {
            VertexLabel vertexLabel = schema().vertexLabel("person")
                                              .properties("name", "age", "city")
                                              .useCustomizeStringId()
                                              .primaryKeys("name")
                                              .build();
            vertexLabelAPI.create(vertexLabel);
        });
        Assert.assertResponse(400, () -> {
            VertexLabel vertexLabel = schema().vertexLabel("person")
                                              .properties("name", "age", "city")
                                              .usePrimaryKeyId()
                                              .build();
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testAppend() {
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .useAutomaticId()
                                           .properties("name", "age")
                                           .build();
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age");
        Set<String> nullableKeys = ImmutableSet.of();
        Assert.assertEquals(props, vertexLabel1.properties());
        Assert.assertEquals(nullableKeys, vertexLabel1.nullableKeys());

        VertexLabel vertexLabel2 = schema().vertexLabel("person")
                                           .properties("city")
                                           .nullableKeys("city")
                                           .build();
        vertexLabel2 = vertexLabelAPI.append(vertexLabel2);
        Assert.assertEquals("person", vertexLabel2.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel2.idStrategy());
        props = ImmutableSet.of("name", "age", "city");
        nullableKeys = ImmutableSet.of("city");
        Assert.assertEquals(props, vertexLabel2.properties());
        Assert.assertEquals(nullableKeys, vertexLabel2.nullableKeys());
    }

    @Test
    public void testAppendWithUndefinedPropertyKey() {
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .useAutomaticId()
                                           .properties("name", "age")
                                           .build();
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age");
        Assert.assertEquals(props, vertexLabel1.properties());

        VertexLabel vertexLabel2 = schema().vertexLabel("person")
                                           .properties("undefined")
                                           .build();
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.append(vertexLabel2);
        });
    }

    @Test
    public void testAppendWithUndefinedNullabelKeys() {
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .useAutomaticId()
                                           .properties("name", "age")
                                           .build();
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age");
        Assert.assertEquals(props, vertexLabel1.properties());

        VertexLabel vertexLabel2 = schema().vertexLabel("person")
                                           .nullableKeys("undefined")
                                           .build();
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.append(vertexLabel2);
        });
    }

    @Test
    public void testEliminate() {
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .useAutomaticId()
                                           .properties("name", "age", "city")
                                           .build();
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);
        Assert.assertEquals("person", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel1.idStrategy());
        Set<String> props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel1.properties());

        VertexLabel vertexLabel2 = schema().vertexLabel("person")
                                           .properties("city")
                                           .build();
        Assert.assertResponse(400, () -> {
            vertexLabelAPI.eliminate(vertexLabel2);
        });
    }

    @Test
    public void testGet() {
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .useAutomaticId()
                                           .properties("name", "age", "city")
                                           .build();

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
        VertexLabel vertexLabel1 = schema().vertexLabel("person")
                                           .useAutomaticId()
                                           .properties("name", "age", "city")
                                           .build();
        vertexLabel1 = vertexLabelAPI.create(vertexLabel1);

        VertexLabel vertexLabel2 = schema().vertexLabel("software")
                                           .useCustomizeStringId()
                                           .properties("name", "lang", "price")
                                           .build();
        vertexLabel2 = vertexLabelAPI.create(vertexLabel2);

        List<VertexLabel> vertexLabels = vertexLabelAPI.list();
        Assert.assertEquals(2, vertexLabels.size());
        assertContains(vertexLabels, vertexLabel1);
        assertContains(vertexLabels, vertexLabel2);
    }

    @Test
    public void testDelete() {
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .useAutomaticId()
                                          .properties("name", "age", "city")
                                          .build();
        vertexLabelAPI.create(vertexLabel);

        vertexLabelAPI.delete("person");

        Assert.assertResponse(404, () -> {
            vertexLabelAPI.get("person");
        });
    }

    @Test
    public void testDeleteNotExist() {
        Assert.assertResponse(404, () -> {
            vertexLabelAPI.delete("not-exist-vl");
        });
    }

    @Test
    public void testAddVertexLabelWithUserData() {
        VertexLabel player = schema().vertexLabel("player")
                                     .properties("name")
                                     .userdata("super_vl", "person")
                                     .build();
        vertexLabelAPI.create(player);
        Assert.assertEquals(1, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));

        VertexLabel runner = schema().vertexLabel("runner")
                                     .properties("name")
                                     .userdata("super_vl", "person")
                                     .userdata("super_vl", "player")
                                     .build();
        vertexLabelAPI.create(runner);
        // The same key user data will be overwritten
        Assert.assertEquals(1, runner.userdata().size());
        Assert.assertEquals("player", runner.userdata().get("super_vl"));
    }

    private static void assertContains(List<VertexLabel> vertexLabels,
                                       VertexLabel vertexLabel) {
        Assert.assertTrue(Utils.contains(vertexLabels, vertexLabel));
    }
}
