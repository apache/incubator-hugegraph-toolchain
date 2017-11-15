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
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.testutil.Assert;

public class EdgeLabelTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseFuncTest.initPropertyKey();
        BaseFuncTest.initVertexLabel();
    }

    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testAddEdgeLabelWithUserData() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .create();

        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .create();

        EdgeLabel father = schema.edgeLabel("father").link("person", "person")
                                 .properties("weight")
                                 .userData("multiplicity", "one-to-many")
                                 .create();

        Assert.assertEquals(1, father.userData().size());
        Assert.assertEquals("one-to-many",
                            father.userData().get("multiplicity"));

        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("time", "weight")
                                .userData("multiplicity", "one-to-many")
                                .userData("multiplicity", "many-to-many")
                                .create();
        // The same key user data will be overwritten
        Assert.assertEquals(1, write.userData().size());
        Assert.assertEquals("many-to-many",
                            write.userData().get("multiplicity"));
    }
}
