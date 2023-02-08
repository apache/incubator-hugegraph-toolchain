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

import org.apache.hugegraph.structure.constant.AggregateType;
import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.WriteType;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class PropertyKeyApiTest extends BaseApiTest {

    @After
    public void teardown() throws Exception {
        List<Long> pkTaskIds = new ArrayList<>();
        propertyKeyAPI.list().forEach(propertyKey -> {
            pkTaskIds.add(propertyKeyAPI.delete(propertyKey.name()));
        });
        pkTaskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
    }

    @Test
    public void testCreate() {
        PropertyKey propertyKey = schema().propertyKey("name")
                                          .asText()
                                          .valueSingle()
                                          .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("name", propertyKey.name());
        Assert.assertEquals(DataType.TEXT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
    }

    @Test
    public void testCreateWithDataType() {
        PropertyKey propertyKey = schema().propertyKey("name")
                                          .dataType(DataType.LONG)
                                          .valueSingle()
                                          .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("name", propertyKey.name());
        Assert.assertEquals(DataType.LONG, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
    }

    @Test
    public void testCreateWithCardinality() {
        PropertyKey propertyKey = schema().propertyKey("name")
                                          .asText()
                                          .cardinality(Cardinality.SET)
                                          .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("name", propertyKey.name());
        Assert.assertEquals(DataType.TEXT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SET, propertyKey.cardinality());
    }

    @Test
    public void testCreateWithAggregateType() {
        PropertyKey propertyKey = schema().propertyKey("name")
                                          .asText().valueSingle()
                                          .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("name", propertyKey.name());
        Assert.assertEquals(DataType.TEXT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(AggregateType.NONE, propertyKey.aggregateType());
        Assert.assertTrue(propertyKey.aggregateType().isNone());
        Assert.assertFalse(propertyKey.aggregateType().isNumber());
        Assert.assertTrue(propertyKey.aggregateType().isIndexable());

        propertyKey = schema().propertyKey("no")
                              .asText().valueSingle()
                              .calcOld()
                              .build();

        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("no", propertyKey.name());
        Assert.assertEquals(DataType.TEXT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(AggregateType.OLD, propertyKey.aggregateType());
        Assert.assertTrue(propertyKey.aggregateType().isOld());
        Assert.assertTrue(propertyKey.aggregateType().isIndexable());
        Assert.assertFalse(propertyKey.aggregateType().isNumber());

        propertyKey = schema().propertyKey("max")
                              .asInt().valueSingle()
                              .calcMax()
                              .build();

        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("max", propertyKey.name());
        Assert.assertEquals(DataType.INT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(AggregateType.MAX, propertyKey.aggregateType());
        Assert.assertTrue(propertyKey.aggregateType().isMax());
        Assert.assertTrue(propertyKey.aggregateType().isIndexable());
        Assert.assertTrue(propertyKey.aggregateType().isNumber());

        propertyKey = schema().propertyKey("min")
                              .asInt().valueSingle()
                              .calcMin()
                              .build();

        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("min", propertyKey.name());
        Assert.assertEquals(DataType.INT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(AggregateType.MIN, propertyKey.aggregateType());
        Assert.assertTrue(propertyKey.aggregateType().isMin());
        Assert.assertTrue(propertyKey.aggregateType().isIndexable());
        Assert.assertTrue(propertyKey.aggregateType().isNumber());

        propertyKey = schema().propertyKey("sum")
                              .asInt().valueSingle()
                              .calcSum()
                              .build();

        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("sum", propertyKey.name());
        Assert.assertEquals(DataType.INT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(AggregateType.SUM, propertyKey.aggregateType());
        Assert.assertTrue(propertyKey.aggregateType().isSum());
        Assert.assertFalse(propertyKey.aggregateType().isIndexable());
        Assert.assertTrue(propertyKey.aggregateType().isNumber());

        propertyKey = schema().propertyKey("total")
                              .asInt().valueSingle()
                              .aggregateType(AggregateType.SUM)
                              .build();

        propertyKeyWithTask = propertyKeyAPI.create(propertyKey);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("total", propertyKey.name());
        Assert.assertEquals(DataType.INT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(AggregateType.SUM, propertyKey.aggregateType());
        Assert.assertTrue(propertyKey.aggregateType().isSum());
        Assert.assertFalse(propertyKey.aggregateType().isIndexable());
        Assert.assertTrue(propertyKey.aggregateType().isNumber());

        propertyKey = schema().propertyKey("nameV46")
                              .asText().valueSingle()
                              .build();
        PropertyKey.PropertyKeyV46 pk = propertyKey.switchV46();
        Assert.assertEquals("nameV46", pk.name());
        Assert.assertEquals(DataType.TEXT, pk.dataType());
        Assert.assertEquals(Cardinality.SINGLE, pk.cardinality());

        propertyKey = schema().propertyKey("nameV58")
                              .asText().valueSingle()
                              .calcOld()
                              .build();
        PropertyKey.PropertyKeyV58 pk1 = propertyKey.switchV58();
        Assert.assertEquals("nameV58", pk1.name());
        Assert.assertEquals(DataType.TEXT, pk1.dataType());
        Assert.assertEquals(Cardinality.SINGLE, pk1.cardinality());
        Assert.assertEquals(AggregateType.OLD, pk1.aggregateType());
    }

    @Test
    public void testCreateWithInvalidName() {
        Utils.assertResponseError(400, () -> {
            propertyKeyAPI.create(new PropertyKey(""));
        });
        Utils.assertResponseError(400, () -> {
            propertyKeyAPI.create(new PropertyKey(" "));
        });
        Utils.assertResponseError(400, () -> {
            propertyKeyAPI.create(new PropertyKey("    "));
        });
    }

    @Test
    public void testCreateExistedPropertyKey() {
        PropertyKey propertyKey = new PropertyKey("name");
        propertyKeyAPI.create(propertyKey);

        Utils.assertResponseError(400, () -> {
            propertyKeyAPI.create(new PropertyKey("name"));
        });
    }

    @Test
    public void testGet() {
        PropertyKey propertyKey1 = schema().propertyKey("name")
                                           .asText()
                                           .valueSingle()
                                           .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey1);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey1 = propertyKeyWithTask.propertyKey();

        PropertyKey propertyKey2 = propertyKeyAPI.get("name");

        Assert.assertEquals(propertyKey1.name(), propertyKey2.name());
        Assert.assertEquals(propertyKey1.dataType(), propertyKey2.dataType());
        Assert.assertEquals(propertyKey1.cardinality(),
                            propertyKey2.cardinality());
    }

    @Test
    public void testGetNotExist() {
        Utils.assertResponseError(404, () -> {
            propertyKeyAPI.get("not-exist-pk");
        });
    }

    @Test
    public void testList() {
        PropertyKey propertyKey1 = schema().propertyKey("name")
                                           .asText()
                                           .valueSingle()
                                           .build();
        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey1);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey1 = propertyKeyWithTask.propertyKey();

        PropertyKey propertyKey2 = schema().propertyKey("age")
                                           .asInt()
                                           .valueSingle()
                                           .build();
        propertyKeyWithTask = propertyKeyAPI.create(propertyKey2);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        propertyKey2 = propertyKeyWithTask.propertyKey();

        List<PropertyKey> propertyKeys = propertyKeyAPI.list();
        Assert.assertEquals(2, propertyKeys.size());
        assertContains(propertyKeys, propertyKey1);
        assertContains(propertyKeys, propertyKey2);
    }

    @Test
    public void testListByNames() {
        PropertyKey name = schema().propertyKey("name").asText().build();
        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(name);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        name = propertyKeyWithTask.propertyKey();

        PropertyKey age = schema().propertyKey("age").asInt().build();
        propertyKeyWithTask = propertyKeyAPI.create(age);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        age = propertyKeyWithTask.propertyKey();

        List<PropertyKey> propertyKeys;

        propertyKeys = propertyKeyAPI.list(ImmutableList.of("name"));
        Assert.assertEquals(1, propertyKeys.size());
        assertContains(propertyKeys, name);

        propertyKeys = propertyKeyAPI.list(ImmutableList.of("age"));
        Assert.assertEquals(1, propertyKeys.size());
        assertContains(propertyKeys, age);

        propertyKeys = propertyKeyAPI.list(ImmutableList.of("name", "age"));
        Assert.assertEquals(2, propertyKeys.size());
        assertContains(propertyKeys, name);
        assertContains(propertyKeys, age);
    }

    @Test
    public void testDelete() {
        PropertyKey propertyKey = schema().propertyKey("name")
                                          .asText()
                                          .valueSingle()
                                          .build();
        propertyKeyAPI.create(propertyKey);
        propertyKeyAPI.delete("name");

        Utils.assertResponseError(404, () -> {
            propertyKeyAPI.get("name");
        });
    }

    @Test
    public void testDeleteNotExist() {
        Utils.assertResponseError(404, () -> {
            propertyKeyAPI.delete("not-exist-pk");
        });
    }

    @Test
    public void testAddPropertyKeyWithUserData() {
        PropertyKey age = schema().propertyKey("age")
                                  .userdata("min", 0)
                                  .userdata("max", 100)
                                  .build();
        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(age);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        age = propertyKeyWithTask.propertyKey();
        Assert.assertEquals(3, age.userdata().size());
        Assert.assertEquals(0, age.userdata().get("min"));
        Assert.assertEquals(100, age.userdata().get("max"));
        String time = (String) age.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        PropertyKey id = schema().propertyKey("id")
                                 .userdata("length", 15)
                                 .userdata("length", 18)
                                 .build();
        propertyKeyWithTask = propertyKeyAPI.create(id);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        id = propertyKeyWithTask.propertyKey();
        // The same key user data will be overwritten
        Assert.assertEquals(2, id.userdata().size());
        Assert.assertEquals(18, id.userdata().get("length"));
        time = (String) id.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        PropertyKey sex = schema().propertyKey("sex")
                                  .userdata("range",
                                            ImmutableList.of("male", "female"))
                                  .build();
        propertyKeyWithTask = propertyKeyAPI.create(sex);
        Assert.assertEquals(0L, propertyKeyWithTask.taskId());
        sex = propertyKeyWithTask.propertyKey();
        Assert.assertEquals(2, sex.userdata().size());
        Assert.assertEquals(ImmutableList.of("male", "female"),
                            sex.userdata().get("range"));
        time = (String) sex.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);
    }

    @Test
    public void testAddOlapPropertyKey() {
        PropertyKey pagerank = schema().propertyKey("pagerank")
                                       .asDouble()
                                       .writeType(WriteType.OLAP_RANGE)
                                       .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(pagerank);
        long taskId = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        pagerank = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("pagerank", pagerank.name());
        Assert.assertEquals(WriteType.OLAP_RANGE, pagerank.writeType());
        Assert.assertEquals(DataType.DOUBLE, pagerank.dataType());

        PropertyKey wcc = schema().propertyKey("wcc")
                                  .asText()
                                  .writeType(WriteType.OLAP_SECONDARY)
                                  .build();

        propertyKeyWithTask = propertyKeyAPI.create(wcc);
        taskId = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        wcc = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("wcc", wcc.name());
        Assert.assertEquals(WriteType.OLAP_SECONDARY, wcc.writeType());
        Assert.assertEquals(DataType.TEXT, wcc.dataType());

        PropertyKey none = schema().propertyKey("none")
                                   .asText()
                                   .writeType(WriteType.OLAP_COMMON)
                                   .build();

        propertyKeyWithTask = propertyKeyAPI.create(none);
        taskId = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        none = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("none", none.name());
        Assert.assertEquals(WriteType.OLAP_COMMON, none.writeType());
        Assert.assertEquals(DataType.TEXT, none.dataType());
    }

    @Test
    public void testClearOlapPropertyKey() {
        PropertyKey pagerank = schema().propertyKey("pagerank")
                                       .asDouble()
                                       .writeType(WriteType.OLAP_RANGE)
                                       .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(pagerank);
        long taskId = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        pagerank = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("pagerank", pagerank.name());
        Assert.assertEquals(WriteType.OLAP_RANGE, pagerank.writeType());
        Assert.assertEquals(DataType.DOUBLE, pagerank.dataType());

        propertyKeyWithTask = propertyKeyAPI.clear(pagerank);
        taskId = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        pagerank = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("pagerank", pagerank.name());
        Assert.assertEquals(WriteType.OLAP_RANGE, pagerank.writeType());
        Assert.assertEquals(DataType.DOUBLE, pagerank.dataType());

        pagerank = propertyKeyAPI.get("pagerank");

        Assert.assertEquals("pagerank", pagerank.name());
        Assert.assertEquals(WriteType.OLAP_RANGE, pagerank.writeType());
        Assert.assertEquals(DataType.DOUBLE, pagerank.dataType());
    }

    @Test
    public void testDeleteOlapPropertyKey() {
        PropertyKey pagerank = schema().propertyKey("pagerank")
                                       .asDouble()
                                       .writeType(WriteType.OLAP_RANGE)
                                       .build();

        PropertyKey.PropertyKeyWithTask propertyKeyWithTask;
        propertyKeyWithTask = propertyKeyAPI.create(pagerank);
        long taskId = propertyKeyWithTask.taskId();
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        pagerank = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("pagerank", pagerank.name());
        Assert.assertEquals(WriteType.OLAP_RANGE, pagerank.writeType());
        Assert.assertEquals(DataType.DOUBLE, pagerank.dataType());

        taskId = propertyKeyAPI.delete(pagerank.name());
        Assert.assertNotEquals(0L, taskId);
        waitUntilTaskCompleted(taskId);
        pagerank = propertyKeyWithTask.propertyKey();

        Assert.assertEquals("pagerank", pagerank.name());
        Assert.assertEquals(WriteType.OLAP_RANGE, pagerank.writeType());
        Assert.assertEquals(DataType.DOUBLE, pagerank.dataType());

        Utils.assertResponseError(404, () -> {
            propertyKeyAPI.get("pagerank");
        });
    }
}
