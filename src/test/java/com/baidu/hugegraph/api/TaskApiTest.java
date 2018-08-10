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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.structure.Task;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.testutil.Utils;

public class TaskApiTest extends BaseApiTest {

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
    public void testList() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        IndexLabel personByAge = schema().getIndexLabel("personByAge");
        IndexLabel knowsByDate = schema().getIndexLabel("knowsByDate");
        IndexLabel createdByDate = schema().getIndexLabel("createdByDate");

        Set<Long> taskIds = new HashSet<>();
        taskIds.add(rebuildAPI.rebuild(personByCity));
        taskIds.add(rebuildAPI.rebuild(personByAge));
        taskIds.add(rebuildAPI.rebuild(knowsByDate));
        taskIds.add(rebuildAPI.rebuild(createdByDate));

        List<Task> tasks = taskAPI.list(null, -1);
        Assert.assertEquals(4, tasks.size());

        Set<Long> listedTaskIds = new HashSet<>();
        for (Task task : tasks) {
            listedTaskIds.add(task.id());
        }
        Assert.assertEquals(taskIds, listedTaskIds);

        taskIds.forEach(BaseApiTest::waitUntilTaskCompleted);
        taskIds.forEach(id -> taskAPI.delete(id));

        tasks = taskAPI.list(null, -1);
        Assert.assertEquals(0, tasks.size());
    }

    @Test
    public void testGet() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        long taskId = rebuildAPI.rebuild(personByCity);

        Task task = taskAPI.get(taskId);

        Assert.assertNotNull(task);
        Assert.assertEquals(taskId, task.id());
    }

    @Test
    public void testDelete() {
        IndexLabel personByCity = schema().getIndexLabel("personByCity");
        long taskId = rebuildAPI.rebuild(personByCity);

        waitUntilTaskCompleted(taskId);
        taskAPI.delete(taskId);

        Utils.assertResponseError(404, () -> {
            taskAPI.get(taskId);
        });
    }
}
