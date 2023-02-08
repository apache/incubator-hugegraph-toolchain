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

package org.apache.hugegraph.manager;

import java.util.List;
import java.util.Set;

import org.apache.hugegraph.base.ToolClient;
import org.apache.hugegraph.base.ToolManager;
import org.apache.hugegraph.structure.Task;

import com.google.common.collect.ImmutableSet;

public class TasksManager extends ToolManager {

    public static final Set<String> TASK_STATUSES = ImmutableSet.of(
            "UNKNOWN", "NEW", "QUEUED", "RESTORING", "RUNNING",
            "SUCCESS", "CANCELLED", "FAILED"
    );

    private static final Set<String> UNCOMPLETED_STATUSES = ImmutableSet.of(
            "UNKNOWN", "NEW", "QUEUED", "RESTORING", "RUNNING"
    );

    private static final Set<String> COMPLETED_STATUSES = ImmutableSet.of(
            "SUCCESS", "CANCELLED", "FAILED"
    );

    private static long TASK_LIMIT = 10000;

    public TasksManager(ToolClient.ConnectionInfo info) {
        super(info, "tasks");
    }

    public List<Task> list(String status, long limit) {
        return this.client.tasks().list(status, limit);
    }

    public Task get(long taskId) {
        return this.client.tasks().get(taskId);
    }

    public void delete(long taskId) {
        this.client.tasks().delete(taskId);
    }

    public void cancel(long taskId) {
        this.client.tasks().cancel(taskId);
    }

    public void clear(boolean force) {
        if (force) {
            // Cancel all uncompleted tasks
            for (String status : UNCOMPLETED_STATUSES) {
                do {
                    List<Task> tasks = this.list(status, TASK_LIMIT);
                    tasks.forEach(t -> this.cancel(t.id()));
                    if (tasks.size() < TASK_LIMIT) {
                        break;
                    }
                } while (true);
            }
        }

        // Delete all completed tasks
        for (String status : COMPLETED_STATUSES) {
            do {
                List<Task> tasks = this.list(status, TASK_LIMIT);
                tasks.forEach(t -> this.delete(t.id()));
                if (tasks.size() < TASK_LIMIT) {
                    break;
                }
            } while (true);
        }
    }
}
