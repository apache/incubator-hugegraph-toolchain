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

package com.baidu.hugegraph.driver;

import java.util.List;

import com.baidu.hugegraph.api.task.TaskAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.structure.Task;

public class TaskManager {

    private TaskAPI taskAPI;

    public TaskManager(RestClient client, String graph) {
        this.taskAPI = new TaskAPI(client, graph);
    }

    public List<Task> list() {
        return this.list(-1);
    }

    public List<Task> list(long limit) {
        return this.taskAPI.list(null, limit);
    }

    public List<Task> list(String status) {
        return this.list(status, -1L);
    }

    public List<Task> list(String status, long limit) {
        return this.taskAPI.list(status, limit);
    }

    public Task get(long id) {
        return this.taskAPI.get(id);
    }

    public void delete(long id) {
        this.taskAPI.delete(id);
    }

    public boolean cancel(long id) {
        return this.taskAPI.cancel(id);
    }

    public Task waitUntilTaskCompleted(long taskId, long seconds) {
        return this.taskAPI.waitUntilTaskCompleted(taskId, seconds);
    }
}
