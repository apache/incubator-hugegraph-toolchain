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
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.apache.hugegraph.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class VertexLabelTest extends BaseFuncTest {

    @Before
    public void setup() {
        BaseFuncTest.initPropertyKey();
    }

    @After
    public void teardown() throws Exception {
        clearData();
    }

    @Test
    public void testAddVertexLabelWithUserData() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name")
                                   .userdata("super_vl", "person")
                                   .create();
        Assert.assertEquals(2, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
        String time = (String) player.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        VertexLabel runner = schema.vertexLabel("runner")
                                   .properties("name")
                                   .userdata("super_vl", "person")
                                   .userdata("super_vl", "player")
                                   .create();
        // The same key user data will be overwritten
        Assert.assertEquals(2, runner.userdata().size());
        Assert.assertEquals("player", runner.userdata().get("super_vl"));
        time = (String) runner.userdata().get("~create_time");
        createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);
    }

    @Test
    public void testAppendVertexLabelWithUserData() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name")
                                   .create();
        Assert.assertEquals(1, player.userdata().size());
        String time = (String) player.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Utils.assertBeforeNow(createTime);

        player = schema.vertexLabel("player")
                       .userdata("super_vl", "person")
                       .append();
        Assert.assertEquals(2, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
        time = (String) player.userdata().get("~create_time");
        Assert.assertEquals(createTime, DateUtil.parse(time));
    }

    @Test
    public void testEliminateVertexLabelWithUserData() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name")
                                   .userdata("super_vl", "person")
                                   .userdata("icon", "picture1")
                                   .create();
        Assert.assertEquals(3, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
        Assert.assertEquals("picture1", player.userdata().get("icon"));
        String time = (String) player.userdata().get("~create_time");
        Date createTime = DateUtil.parse(time);
        Utils.assertBeforeNow(createTime);

        player = schema.vertexLabel("player")
                       .userdata("icon", "")
                       .eliminate();
        Assert.assertEquals(2, player.userdata().size());
        Assert.assertEquals("person", player.userdata().get("super_vl"));
        time = (String) player.userdata().get("~create_time");
        Assert.assertEquals(createTime, DateUtil.parse(time));
    }

    @Test
    public void testRemoveVertexLabelSync() {
        SchemaManager schema = schema();

        schema.vertexLabel("player").properties("name").create();
        // Remove vertex label sync
        schema.removeVertexLabel("player");

        schema.vertexLabel("player").properties("name").create();
        // Remove vertex label sync with timeout
        schema.removeVertexLabel("player", 10);
    }

    @Test
    public void testRemoveVertexLabelASync() {
        SchemaManager schema = schema();
        schema.vertexLabel("player").properties("name").create();

        // Remove vertex label async and wait
        long taskId = schema.removeVertexLabelAsync("player");
        Task task = task().waitUntilTaskCompleted(taskId, 10);
        Assert.assertTrue(task.completed());
    }

    @Test
    public void testListByNames() {
        SchemaManager schema = schema();

        VertexLabel player = schema.vertexLabel("player").create();
        VertexLabel runner = schema.vertexLabel("runner").create();

        List<VertexLabel> vertexLabels;

        vertexLabels = schema.getVertexLabels(ImmutableList.of("player"));
        Assert.assertEquals(1, vertexLabels.size());
        assertContains(vertexLabels, player);

        vertexLabels = schema.getVertexLabels(ImmutableList.of("runner"));
        Assert.assertEquals(1, vertexLabels.size());
        assertContains(vertexLabels, runner);

        vertexLabels = schema.getVertexLabels(ImmutableList.of("player",
                                                               "runner"));
        Assert.assertEquals(2, vertexLabels.size());
        assertContains(vertexLabels, player);
        assertContains(vertexLabels, runner);
    }

    @Test
    public void testResetVertexLabelId() {
        SchemaManager schema = schema();
        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name").create();
        Assert.assertTrue(player.id() > 0);
        player.resetId();
        Assert.assertEquals(0L, player.id());
    }

    @Test
    public void testSetCheckExist() {
        SchemaManager schema = schema();
        VertexLabel player = schema.vertexLabel("player")
                                   .properties("name").build();
        Assert.assertTrue(player.checkExist());
        player.checkExist(false);
        Assert.assertFalse(player.checkExist());
    }
}
