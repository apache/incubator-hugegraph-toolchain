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
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.testutil.Assert;

public class VertexLabelTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseFuncTest.initPropertyKey();
    }

    @After
    public void teardown() throws Exception {
        BaseFuncTest.clearData();
    }

    @Test
    public void testAddVertexLabelWithUserData() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name")
                                   .userdata("super_vl", "person")
                                   .create();
        Assert.assertEquals(1, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));

        VertexLabel runner = schema.vertexLabel("runner")
                                   .properties("name")
                                   .userdata("super_vl", "person")
                                   .userdata("super_vl", "player")
                                   .create();
        // The same key user data will be overwritten
        Assert.assertEquals(1, runner.userdata().size());
        Assert.assertEquals("player", runner.userdata().get("super_vl"));
    }

    @Test
    public void testAppendVertexLabelWithUserData() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name")
                                   .create();
        Assert.assertEquals(0, player.userdata().size());

        player = schema.vertexLabel("player")
                       .userdata("super_vl", "person")
                       .append();
        Assert.assertEquals(1, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
    }

    @Test
    public void testEliminateVertexLabelWithUserData() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name")
                                   .userdata("super_vl", "person")
                                   .userdata("icon", "picture1")
                                   .create();
        Assert.assertEquals(2, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
        Assert.assertEquals("picture1", player.userdata().get("icon"));

        player = schema.vertexLabel("player")
                       .userdata("icon", "")
                       .eliminate();
        Assert.assertEquals(1, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
    }
}
