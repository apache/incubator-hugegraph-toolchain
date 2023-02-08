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

package org.apache.hugegraph.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.api.task.TaskAPI;
import org.apache.hugegraph.structure.Task;

public class TaskCache {

    private static final Task FAKE_TASK = new Task();

    private Map<TaskAPI, Map<Long, Task>> taskTable;
    private ScheduledExecutorService service;

    private static final TaskCache INSTANCE = new TaskCache();

    private TaskCache() {
        this.taskTable = new ConcurrentHashMap<>();
        this.service = null;
    }

    public static TaskCache instance() {
        return INSTANCE;
    }

    public Task get(TaskAPI api, long task) {
        this.add(api, task);
        return this.taskTable.get(api).get(task);
    }

    public void remove(TaskAPI api, long task) {
        Map<Long, Task> tasks = this.tasks(api);
        tasks.remove(task);
        if (tasks.isEmpty()) {
            this.taskTable.remove(api);
        }
        if (this.taskTable.isEmpty()) {
            this.stop();
        }
    }

    private void add(TaskAPI api, long task) {
        Map<Long, Task> tasks = this.tasks(api);
        if (!tasks.containsKey(task)) {
            tasks.putIfAbsent(task, FAKE_TASK);
        }
        if (this.service == null || this.service.isShutdown()) {
            this.start();
        }
    }

    private Map<Long, Task> tasks(TaskAPI api) {
        if (!this.taskTable.containsKey(api)) {
            this.taskTable.putIfAbsent(api, new ConcurrentHashMap<>());
        }
        return this.taskTable.get(api);
    }

    private synchronized void start() {
        if (this.service == null || this.service.isShutdown()) {
            this.service = ExecutorUtil.newScheduledThreadPool("task-worker");
            // Schedule a query task to query task status every 1 second,
            this.service.scheduleAtFixedRate(this::asyncQueryTask, 0L, 1L,
                                             TimeUnit.SECONDS);
        }
    }

    private synchronized void stop() {
        if (this.taskTable.isEmpty() && this.service != null) {
            this.service.shutdown();
        }
    }

    private void asyncQueryTask() {
        for (Map.Entry<TaskAPI, Map<Long, Task>> query :
             this.taskTable.entrySet()) {
            TaskAPI api = query.getKey();
            Map<Long, Task> targets = query.getValue();
            if (targets == null || targets.isEmpty()) {
                this.taskTable.remove(api);
                continue;
            }
            List<Long> taskIds = new ArrayList<>(targets.keySet());
            List<Task> results = api.list(taskIds);
            for (Task task : results) {
                targets.put(task.id(), task);
            }
        }
    }
}
