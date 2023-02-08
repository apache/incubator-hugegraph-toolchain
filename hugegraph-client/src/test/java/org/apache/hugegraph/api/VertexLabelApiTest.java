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

package org.apache.hugegraph.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class VertexLabelApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
    }

    @Override
    @After
    public void teardown() throws Exception {
        List<Long> taskIds = new ArrayList<>();
        vertexLabelAPI.list().forEach(vl -> {
            taskIds.add(vertexLabelAPI.delete(vl.name()));
        });
        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
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
        Assert.assertTrue(vertexLabel.enableLabelIndex());
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
        Assert.assertFalse(vertexLabel.enableLabelIndex());
        Set<String> props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel.properties());
    }

    @Test
    public void testCreateWithUuidIdStrategy() {
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .useCustomizeUuidId()
                                          .properties("name", "age", "city")
                                          .build();

        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person", vertexLabel.name());
        Assert.assertEquals(IdStrategy.CUSTOMIZE_UUID, vertexLabel.idStrategy());
        Assert.assertTrue(vertexLabel.enableLabelIndex());
        Set<String> props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel.properties());

        vertexLabel = schema().vertexLabel("person1")
                              .idStrategy(IdStrategy.CUSTOMIZE_UUID)
                              .properties("name", "age", "city")
                              .build();

        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person1", vertexLabel.name());
        Assert.assertEquals(IdStrategy.CUSTOMIZE_UUID, vertexLabel.idStrategy());
        Assert.assertTrue(vertexLabel.enableLabelIndex());
        props = ImmutableSet.of("name", "age", "city");
        Assert.assertEquals(props, vertexLabel.properties());
    }

    @Test
    public void testCreateWithInvalidName() {
        Utils.assertResponseError(400, () -> {
            vertexLabelAPI.create(new VertexLabel(""));
        });
        Utils.assertResponseError(400, () -> {
            vertexLabelAPI.create(new VertexLabel(" "));
        });
        Utils.assertResponseError(400, () -> {
            vertexLabelAPI.create(new VertexLabel("    "));
        });
    }

    @Test
    public void testCreateExistedVertexLabel() {
        VertexLabel vertexLabel = new VertexLabel("name");
        vertexLabelAPI.create(vertexLabel);

        Utils.assertResponseError(400, () -> {
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

        Utils.assertResponseError(400, () -> {
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

        Utils.assertResponseError(400, () -> {
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

        Utils.assertResponseError(400, () -> {
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
        Utils.assertResponseError(400, () -> {
            vertexLabelAPI.create(vertexLabel1);
        });

        VertexLabel vertexLabel2 = schema().vertexLabel("person")
                                           .usePrimaryKeyId()
                                           .properties("name", "age", "city")
                                           .primaryKeys("name", "age")
                                           .nullableKeys("name")
                                           .build();
        Utils.assertResponseError(400, () -> {
            vertexLabelAPI.create(vertexLabel2);
        });

        VertexLabel vertexLabel3 = schema().vertexLabel("person")
                                           .usePrimaryKeyId()
                                           .properties("name", "age", "city")
                                           .primaryKeys("name")
                                           .nullableKeys("name", "age")
                                           .build();
        Utils.assertResponseError(400, () -> {
            vertexLabelAPI.create(vertexLabel3);
        });
    }

    @Test
    public void testCreateWithUnmatchIdStrategyAndProperties() {
        Utils.assertResponseError(400, () -> {
            VertexLabel vertexLabel = schema().vertexLabel("person")
                                              .properties("name", "age", "city")
                                              .useAutomaticId()
                                              .primaryKeys("name")
                                              .build();
            vertexLabelAPI.create(vertexLabel);
        });
        Utils.assertResponseError(400, () -> {
            VertexLabel vertexLabel = schema().vertexLabel("person")
                                              .properties("name", "age", "city")
                                              .useCustomizeStringId()
                                              .primaryKeys("name")
                                              .build();
            vertexLabelAPI.create(vertexLabel);
        });
        Utils.assertResponseError(400, () -> {
            VertexLabel vertexLabel = schema().vertexLabel("person")
                                              .properties("name", "age", "city")
                                              .usePrimaryKeyId()
                                              .build();
            vertexLabelAPI.create(vertexLabel);
        });
    }

    @Test
    public void testCreateWithTtl() {
        VertexLabel vertexLabel = schema().vertexLabel("person1")
                                          .useAutomaticId()
                                          .properties("name", "age", "date")
                                          .build();
        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person1", vertexLabel.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel.idStrategy());
        Assert.assertTrue(vertexLabel.enableLabelIndex());
        Set<String> props = ImmutableSet.of("name", "age", "date");
        Assert.assertEquals(props, vertexLabel.properties());
        Assert.assertEquals(0L, vertexLabel.ttl());
        Assert.assertNull(vertexLabel.ttlStartTime());

        vertexLabel = schema().vertexLabel("person2")
                              .useAutomaticId()
                              .properties("name", "age", "date")
                              .ttl(3000L)
                              .build();
        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person2", vertexLabel.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel.idStrategy());
        Assert.assertTrue(vertexLabel.enableLabelIndex());
        Assert.assertEquals(props, vertexLabel.properties());
        Assert.assertEquals(3000L, vertexLabel.ttl());
        Assert.assertNull(vertexLabel.ttlStartTime());

        vertexLabel = schema().vertexLabel("person3")
                              .useAutomaticId()
                              .properties("name", "age", "date")
                              .ttl(3000L)
                              .ttlStartTime("date")
                              .build();
        vertexLabel = vertexLabelAPI.create(vertexLabel);

        Assert.assertEquals("person3", vertexLabel.name());
        Assert.assertEquals(IdStrategy.AUTOMATIC, vertexLabel.idStrategy());
        Assert.assertTrue(vertexLabel.enableLabelIndex());
        Assert.assertEquals(props, vertexLabel.properties());
        Assert.assertEquals(3000L, vertexLabel.ttl());
        Assert.assertEquals("date", vertexLabel.ttlStartTime());
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
        Utils.assertResponseError(400, () -> {
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
        Utils.assertResponseError(400, () -> {
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
        Utils.assertResponseError(400, () -> {
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
        Utils.assertResponseError(404, () -> {
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
    public void testListByNames() {
        VertexLabel person = schema().vertexLabel("person")
                                     .useAutomaticId()
                                     .properties("name", "age", "city")
                                     .build();
        person = vertexLabelAPI.create(person);

        VertexLabel software = schema().vertexLabel("software")
                                       .useCustomizeStringId()
                                       .properties("name", "lang", "price")
                                       .build();
        software = vertexLabelAPI.create(software);

        List<VertexLabel> vertexLabels;

        vertexLabels = vertexLabelAPI.list(ImmutableList.of("person"));
        Assert.assertEquals(1, vertexLabels.size());
        assertContains(vertexLabels, person);

        vertexLabels = vertexLabelAPI.list(ImmutableList.of("software"));
        Assert.assertEquals(1, vertexLabels.size());
        assertContains(vertexLabels, software);

        vertexLabels = vertexLabelAPI.list(ImmutableList.of("person",
                                                            "software"));
        Assert.assertEquals(2, vertexLabels.size());
        assertContains(vertexLabels, person);
        assertContains(vertexLabels, software);
    }

    @Test
    public void testDelete() {
        VertexLabel vertexLabel = schema().vertexLabel("person")
                                          .useAutomaticId()
                                          .properties("name", "age", "city")
                                          .build();
        vertexLabelAPI.create(vertexLabel);

        long taskId = vertexLabelAPI.delete("person");
        waitUntilTaskCompleted(taskId);

        Utils.assertResponseError(404, () -> {
            vertexLabelAPI.get("person");
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(404, () -> {
            vertexLabelAPI.delete("not-exist-vl");
        });
    }

    @Test
    public void testAddVertexLabelWithUserData() {
        VertexLabel player = schema().vertexLabel("player")
                                     .properties("name")
                                     .userdata("super_vl", "person")
                                     .build();
        player = vertexLabelAPI.create(player);
        Assert.assertEquals(2, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
        String time = (String) player.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        VertexLabel runner = schema().vertexLabel("runner")
                                     .properties("name")
                                     .userdata("super_vl", "person")
                                     .userdata("super_vl", "player")
                                     .build();
        runner = vertexLabelAPI.create(runner);
        // The same key user data will be overwritten
        Assert.assertEquals(2, runner.userdata().size());
        Assert.assertEquals("player", runner.userdata().get("super_vl"));
        time = (String) runner.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);
    }
}
