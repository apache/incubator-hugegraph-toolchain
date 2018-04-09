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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.testutil.Assert;
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
                                .userData("min", 0)
                                .userData("max", 100)
                                .create();
        Assert.assertEquals(2, age.userData().size());
        Assert.assertEquals(0, age.userData().get("min"));
        Assert.assertEquals(100, age.userData().get("max"));

        PropertyKey id = schema.propertyKey("id")
                               .userData("length", 15)
                               .userData("length", 18)
                               .create();
        // The same key user data will be overwritten
        Assert.assertEquals(1, id.userData().size());
        Assert.assertEquals(18, id.userData().get("length"));

        PropertyKey sex = schema.propertyKey("sex")
                                .userData("range",
                                          ImmutableList.of("male", "female"))
                                .create();
        Assert.assertEquals(1, sex.userData().size());
        Assert.assertEquals(ImmutableList.of("male", "female"),
                            sex.userData().get("range"));
    }

    @Test
    public void testAppendPropertyKeyWithUserData() {
        SchemaManager schema = schema();
        PropertyKey age = schema.propertyKey("age")
                                .userData("min", 0)
                                .create();
        Assert.assertEquals(1, age.userData().size());
        Assert.assertEquals(0, age.userData().get("min"));

        age = schema.propertyKey("age")
                    .userData("min", 1)
                    .userData("max", 100)
                    .append();
        Assert.assertEquals(2, age.userData().size());
        Assert.assertEquals(1, age.userData().get("min"));
        Assert.assertEquals(100, age.userData().get("max"));
    }

    @Test
    public void testEliminatePropertyKeyWithUserData() {
        SchemaManager schema = schema();
        PropertyKey age = schema.propertyKey("age")
                                .userData("min", 0)
                                .userData("max", 100)
                                .create();
        Assert.assertEquals(2, age.userData().size());
        Assert.assertEquals(0, age.userData().get("min"));
        Assert.assertEquals(100, age.userData().get("max"));

        age = schema.propertyKey("age")
                    .userData("max", "")
                    .eliminate();
        Assert.assertEquals(1, age.userData().size());
        Assert.assertEquals(0, age.userData().get("min"));
    }
}
