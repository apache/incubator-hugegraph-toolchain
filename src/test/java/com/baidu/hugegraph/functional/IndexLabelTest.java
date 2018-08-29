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
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.testutil.Assert;

public class IndexLabelTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseFuncTest.initPropertyKey();
    }

    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testRemoveIndexLabelSync() {
        SchemaManager schema = schema();

        schema.vertexLabel("player")
              .properties("name", "age")
              .create();
        IndexLabel playerByName = schema.indexLabel("playerByName")
                                        .onV("player")
                                        .by("name")
                                        .create();

        Assert.assertNotNull(playerByName);
        // Remove index label sync
        schema.removeIndexLabel("playerByName");

        playerByName = schema.indexLabel("playerByName")
                             .onV("player")
                             .by("name")
                             .create();

        Assert.assertNotNull(playerByName);
        // Remove index label sync with timeout
        schema.removeIndexLabel("playerByName", 10);
    }

    @Test
    public void testRemoveIndexLabelAsync() {
        SchemaManager schema = schema();

        schema.vertexLabel("player")
              .properties("name", "age")
              .create();
        IndexLabel playerByName = schema.indexLabel("playerByName")
                                        .onV("player")
                                        .by("name")
                                        .create();
        Assert.assertNotNull(playerByName);
        // Remove index label async
        schema.removeIndexLabelAsync("playerByName");
    }
}
