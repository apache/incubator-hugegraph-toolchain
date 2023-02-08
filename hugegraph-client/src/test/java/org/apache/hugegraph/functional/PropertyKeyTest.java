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

import java.util.Date;
import java.util.List;

import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.structure.constant.WriteType;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class PropertyKeyTest extends BaseFuncTest {

    @Before
    public void setup() {

    }

    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testAddPropertyKeyWithUserData() {
        SchemaManager schema = schema();

        PropertyKey age = schema.propertyKey("age")
                                .userdata("min", 0)
                                .userdata("max", 100)
                                .create();
        Assert.assertEquals(3, age.userdata().size());
        Assert.assertEquals(0, age.userdata().get("min"));
        Assert.assertEquals(100, age.userdata().get("max"));
        String time = (String) age.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        PropertyKey id = schema.propertyKey("id")
                               .userdata("length", 15)
                               .userdata("length", 18)
                               .create();
        // The same key user data will be overwritten
        Assert.assertEquals(2, id.userdata().size());
        Assert.assertEquals(18, id.userdata().get("length"));
        time = (String) id.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        PropertyKey sex = schema.propertyKey("sex")
                                .userdata("range",
                                          ImmutableList.of("male", "female"))
                                .create();
        Assert.assertEquals(2, sex.userdata().size());
        Assert.assertEquals(ImmutableList.of("male", "female"),
                            sex.userdata().get("range"));
        time = (String) sex.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);
    }

    @Test
    public void testAppendPropertyKeyWithUserData() {
        SchemaManager schema = schema();
        PropertyKey age = schema.propertyKey("age")
                                .userdata("min", 0)
                                .create();
        Assert.assertEquals(2, age.userdata().size());
        Assert.assertEquals(0, age.userdata().get("min"));
        String time = (String) age.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        age = schema.propertyKey("age")
                    .userdata("min", 1)
                    .userdata("max", 100)
                    .append();
        Assert.assertEquals(3, age.userdata().size());
        Assert.assertEquals(1, age.userdata().get("min"));
        Assert.assertEquals(100, age.userdata().get("max"));
        time = (String) age.userdata().get("~create_time");
        Assert.assertEquals(createTime, DateUtil.parse(time));
    }

    @Test
    public void testEliminatePropertyKeyWithUserData() {
        SchemaManager schema = schema();
        PropertyKey age = schema.propertyKey("age")
                                .userdata("min", 0)
                                .userdata("max", 100)
                                .create();
        Assert.assertEquals(3, age.userdata().size());
        Assert.assertEquals(0, age.userdata().get("min"));
        Assert.assertEquals(100, age.userdata().get("max"));
        String time = (String) age.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        age = schema.propertyKey("age")
                    .userdata("max", "")
                    .eliminate();
        Assert.assertEquals(2, age.userdata().size());
        Assert.assertEquals(0, age.userdata().get("min"));
        time = (String) age.userdata().get("~create_time");
        Assert.assertEquals(createTime, DateUtil.parse(time));
    }

    @Test
    public void testListByNames() {
        SchemaManager schema = schema();

        PropertyKey age = schema.propertyKey("age").create();
        PropertyKey id = schema.propertyKey("id").create();

        List<PropertyKey> propertyKeys;

        propertyKeys = schema.getPropertyKeys(ImmutableList.of("age"));
        Assert.assertEquals(1, propertyKeys.size());
        assertContains(propertyKeys, age);

        propertyKeys = schema.getPropertyKeys(ImmutableList.of("id"));
        Assert.assertEquals(1, propertyKeys.size());
        assertContains(propertyKeys, id);

        propertyKeys = schema.getPropertyKeys(ImmutableList.of("age", "id"));
        Assert.assertEquals(2, propertyKeys.size());
        assertContains(propertyKeys, age);
        assertContains(propertyKeys, id);
    }

    @Test
    public void testResetPropertyKeyId() {
        SchemaManager schema = schema();
        PropertyKey age = schema.propertyKey("age")
                                .userdata("min", 0)
                                .userdata("max", 100)
                                .create();
        Assert.assertTrue(age.id() > 0);
        age.resetId();
        Assert.assertEquals(0L, age.id());
    }

    @Test
    public void testSetCheckExist() {
        SchemaManager schema = schema();
        PropertyKey age = schema.propertyKey("age")
                                .userdata("min", 0)
                                .userdata("max", 100)
                                .create();
        Assert.assertTrue(age.checkExist());
        age.checkExist(false);
        Assert.assertFalse(age.checkExist());
    }

    @Test
    public void testOlapPropertyKey() {
        SchemaManager schema = schema();
        PropertyKey pagerank = schema.propertyKey("pagerank")
                                     .asDouble()
                                     .writeType(WriteType.OLAP_RANGE)
                                     .build();
        schema.addPropertyKey(pagerank);
        schema.getPropertyKey(pagerank.name());
        schema.clearPropertyKey(pagerank);
        schema.removePropertyKey(pagerank.name());
        Utils.assertResponseError(404, () -> {
            schema.getPropertyKey(pagerank.name());
        });

        long task = schema.addPropertyKeyAsync(pagerank);
        waitUntilTaskCompleted(task);

        schema.getPropertyKey(pagerank.name());

        task = schema.clearPropertyKeyAsync(pagerank);
        waitUntilTaskCompleted(task);

        task = schema.removePropertyKeyAsync(pagerank.name());
        waitUntilTaskCompleted(task);

        Utils.assertResponseError(404, () -> {
            schema.getPropertyKey(pagerank.name());
        });
    }
}
