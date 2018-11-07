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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.Task;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.testutil.Assert;

public class JobApiTest extends BaseApiTest {

    @BeforeClass
    public static void prepareSchema() {
        BaseApiTest.initPropertyKey();
        BaseApiTest.initVertexLabel();
        BaseApiTest.initEdgeLabel();
        BaseApiTest.initIndexLabel();
    }

    @After
    public void teardown() throws Exception {
        taskAPI.list(null, -1).forEach(task -> taskAPI.delete(task.id()));
    }

    @Test
    public void testRebuildVertexLabel() {
        VertexLabel person = schema().getVertexLabel("person");
        long taskId = rebuildAPI.rebuild(person);
        Task task = taskAPI.get(taskId);
        Assert.assertNotNull(task);
        Assert.assertEquals(taskId, task.id());
        waitUntilTaskCompleted(taskId);
    }

    @Test
    public void testRebuildEdgeLabel() {
        EdgeLabel created = schema().getEdgeLabel("created");
        long taskId = rebuildAPI.rebuild(created);
        Task task = taskAPI.get(taskId);
        Assert.assertNotNull(task);
        Assert.assertEquals(taskId, task.id());
        waitUntilTaskCompleted(taskId);
    }

    @Test
    public void testRebuildIndexLabel() {
        IndexLabel personByCity = schema().getIndexLabel("personByAge");
        long taskId = rebuildAPI.rebuild(personByCity);
        Task task = taskAPI.get(taskId);
        Assert.assertNotNull(task);
        Assert.assertEquals(taskId, task.id());
        waitUntilTaskCompleted(taskId);
    }
}
