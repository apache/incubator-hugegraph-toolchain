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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.Task;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;

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
    public void testLinkedVertexLabel() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();

        EdgeLabel father = schema.edgeLabel("father").link("person", "person")
                                 .properties("weight")
                                 .userdata("multiplicity", "one-to-many")
                                 .create();
        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("multiplicity", "many-to-many")
                                .create();

        Assert.assertTrue(father.linkedVertexLabel("person"));
        Assert.assertFalse(father.linkedVertexLabel("book"));
        Assert.assertTrue(write.linkedVertexLabel("person"));
        Assert.assertTrue(write.linkedVertexLabel("book"));
    }

    @Test
    public void testAddEdgeLabelWithUserData() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();

        EdgeLabel father = schema.edgeLabel("father").link("person", "person")
                                 .properties("weight")
                                 .userdata("multiplicity", "one-to-many")
                                 .create();

        Assert.assertEquals(1, father.userdata().size());
        Assert.assertEquals("one-to-many",
                            father.userdata().get("multiplicity"));

        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("multiplicity", "many-to-many")
                                .create();
        // The same key user data will be overwritten
        Assert.assertEquals(1, write.userdata().size());
        Assert.assertEquals("many-to-many",
                            write.userdata().get("multiplicity"));
    }

    @Test
    public void testAppendEdgeLabelWithUserData() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        EdgeLabel father = schema.edgeLabel("father").link("person", "person")
                                 .properties("weight")
                                 .create();

        Assert.assertEquals(0, father.userdata().size());

        father = schema.edgeLabel("father")
                       .userdata("multiplicity", "one-to-many")
                       .append();
        Assert.assertEquals(1, father.userdata().size());
        Assert.assertEquals("one-to-many",
                            father.userdata().get("multiplicity"));
    }

    @Test
    public void testEliminateEdgeLabelWithUserData() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();
        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();
        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("icon", "picture2")
                                .create();
        Assert.assertEquals(2, write.userdata().size());
        Assert.assertEquals("one-to-many",
                            write.userdata().get("multiplicity"));
        Assert.assertEquals("picture2", write.userdata().get("icon"));

        write = schema.edgeLabel("write")
                      .userdata("icon", "")
                      .eliminate();
        Assert.assertEquals(1, write.userdata().size());
        Assert.assertEquals("one-to-many",
                            write.userdata().get("multiplicity"));
    }

    @Test
    public void testRemoveEdgeLabelSync() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();
        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();
        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("icon", "picture2")
                                .create();

        Assert.assertNotNull(write);
        // Remove edge label sync
        schema.removeEdgeLabel("write");

        write = schema.edgeLabel("write").link("person", "book")
                      .properties("date", "weight")
                      .userdata("multiplicity", "one-to-many")
                      .userdata("icon", "picture2")
                      .create();

        Assert.assertNotNull(write);
        // Remove edge label sync with timeout
        schema.removeEdgeLabel("write", 10);
    }

    @Test
    public void testRemoveEdgeLabelAsync() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();
        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();
        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("icon", "picture2")
                                .create();
        Assert.assertNotNull(write);
        // Remove edge label async and wait
        long taskId = schema.removeEdgeLabelAsync("write");
        Task task = task().waitUntilTaskCompleted(taskId, 10);
        Assert.assertTrue(task.completed());
    }

    @Test
    public void testListByNames() {
        SchemaManager schema = schema();

        schema.vertexLabel("person").ifNotExist().create();
        schema.vertexLabel("book").ifNotExist().create();

        EdgeLabel father = schema.edgeLabel("father").link("person", "person")
                                 .create();

        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .create();

        List<EdgeLabel> edgeLabels;

        edgeLabels = schema.getEdgeLabels(ImmutableList.of("father"));
        Assert.assertEquals(1, edgeLabels.size());
        assertContains(edgeLabels, father);

        edgeLabels = schema.getEdgeLabels(ImmutableList.of("write"));
        Assert.assertEquals(1, edgeLabels.size());
        assertContains(edgeLabels, write);

        edgeLabels = schema.getEdgeLabels(ImmutableList.of("father", "write"));
        Assert.assertEquals(2, edgeLabels.size());
        assertContains(edgeLabels, father);
        assertContains(edgeLabels, write);
    }

    @Test
    public void testResetEdgeLabelId() {
        SchemaManager schema = schema();
        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();
        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();
        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("icon", "picture2")
                                .create();
        Assert.assertTrue(write.id() > 0);
        write.resetId();
        Assert.assertEquals(0L, write.id());
    }

    @Test
    public void testSetCheckExist() {
        SchemaManager schema = schema();
        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();
        schema.vertexLabel("book")
              .properties("name")
              .primaryKeys("name")
              .ifNotExist()
              .create();
        EdgeLabel write = schema.edgeLabel("write").link("person", "book")
                                .properties("date", "weight")
                                .userdata("multiplicity", "one-to-many")
                                .userdata("icon", "picture2")
                                .create();
        Assert.assertTrue(write.checkExist());
        write.checkExist(false);
        Assert.assertFalse(write.checkExist());
    }
}
