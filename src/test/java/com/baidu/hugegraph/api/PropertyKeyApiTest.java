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

import org.junit.After;
import org.junit.Test;

import com.baidu.hugegraph.structure.constant.Cardinality;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Utils;

public class PropertyKeyApiTest extends BaseApiTest {

    @After
    public void teardown() throws Exception {
        propertyKeyAPI.list().forEach(pk -> propertyKeyAPI.delete(pk.name()));
    }

    @Test
    public void testCreate() {
        PropertyKey propertyKey = new PropertyKey("name");
        propertyKey.dataType(DataType.TEXT);
        propertyKey.cardinality(Cardinality.SINGLE);

        propertyKey = propertyKeyAPI.create(propertyKey);

        Assert.assertEquals("name", propertyKey.name());
        Assert.assertEquals(DataType.TEXT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
    }

    @Test
    public void testCreateWithInvalidName() {
        Assert.assertResponse(400, () -> {
            propertyKeyAPI.create(new PropertyKey(""));
        });
        Assert.assertResponse(400, () -> {
            propertyKeyAPI.create(new PropertyKey(" "));
        });
        Assert.assertResponse(400, () -> {
            propertyKeyAPI.create(new PropertyKey("    "));
        });
    }

    @Test
    public void testCreateExistedPropertyKey() {
        PropertyKey propertyKey = new PropertyKey("name");
        propertyKeyAPI.create(propertyKey);

        Assert.assertResponse(400, () -> {
            propertyKeyAPI.create(new PropertyKey("name"));
        });
    }

    @Test
    public void testGet() {
        PropertyKey propertyKey1 = new PropertyKey("name");
        propertyKey1.dataType(DataType.TEXT);
        propertyKey1.cardinality(Cardinality.SINGLE);

        propertyKey1 = propertyKeyAPI.create(propertyKey1);

        PropertyKey propertyKey2 = propertyKeyAPI.get("name");

        Assert.assertEquals(propertyKey1.name(), propertyKey2.name());
        Assert.assertEquals(propertyKey1.dataType(), propertyKey2.dataType());
        Assert.assertEquals(propertyKey1.cardinality(),
                            propertyKey2.cardinality());
    }

    @Test
    public void testGetNotExist() {
        Assert.assertResponse(404, () -> {
            propertyKeyAPI.get("not-exist-pk");
        });
    }

    @Test
    public void testList() {
        PropertyKey propertyKey1 = new PropertyKey("name");
        propertyKey1.dataType(DataType.TEXT);
        propertyKey1.cardinality(Cardinality.SINGLE);
        propertyKey1 = propertyKeyAPI.create(propertyKey1);

        PropertyKey propertyKey2 = new PropertyKey("age");
        propertyKey2.dataType(DataType.INT);
        propertyKey2.cardinality(Cardinality.SINGLE);
        propertyKey2 = propertyKeyAPI.create(propertyKey2);

        List<PropertyKey> propertyKeys = propertyKeyAPI.list();
        Assert.assertEquals(2, propertyKeys.size());
        assertContains(propertyKeys, propertyKey1);
        assertContains(propertyKeys, propertyKey2);
    }

    @Test
    public void testDelete() {
        PropertyKey propertyKey = new PropertyKey("name");
        propertyKey.dataType(DataType.TEXT);
        propertyKey.cardinality(Cardinality.SINGLE);
        propertyKeyAPI.create(propertyKey);
        propertyKeyAPI.delete("name");

        Assert.assertResponse(404, () -> {
            propertyKeyAPI.get("name");
        });
    }

    @Test
    public void testDeleteNotExist() {
        propertyKeyAPI.delete("not-exist-pk");
    }

    private static void assertContains(List<PropertyKey> propertyKeys,
                                       PropertyKey propertyKey) {
        Assert.assertTrue(Utils.contains(propertyKeys, propertyKey));
    }
}
