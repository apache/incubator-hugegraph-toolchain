/*
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

package org.apache.hugegraph.service.algorithm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Log4j2
@Service
public class AsyncTaskService {

    private static final String VERMEER_TASK_TYPE = "vermeer-task";
    private static final String VERMEER_TASK_LOAD = "vermeer-task:load";
    private static final String VERMEER_TASK_COMPUTE = "vermeer-task:compute";

    public Task get(HugeClient client, int id) {
        Task task = client.task().get(id);
        if (isVermeerType(task.type())) {
            task.type(switchVermeerType(task.name()));
        }
        return task;
    }

    private boolean isVermeerType(String type) {
        return VERMEER_TASK_TYPE.equals(type);
    }

    private String switchVermeerType(String name) {
        if (VERMEER_TASK_LOAD.equals(name)) {
            return name;
        }
        return VERMEER_TASK_COMPUTE;
    }

    public List<Task> list(HugeClient client, List<Long> taskIds) {
        List<Task> tasks = client.task().list(taskIds);
        for (Task task: tasks) {
            if (isVermeerType(task.type())) {
                task.type(switchVermeerType(task.name()));
            }
        }
        return tasks;
    }

    public IPage<Task> list(HugeClient client, int pageNo, int pageSize,
                            String content,
                            String type, String status) {
        if (status.isEmpty()) {
            status = null;
        }
        List<Task> tasks = client.task().list(status);
        for (Task task: tasks) {
            if (isVermeerType(task.type())) {
                task.type(switchVermeerType(task.name()));
            }
        }

        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (!type.isEmpty() && !type.equals(task.type())) {
                continue;
            }
            if (!content.isEmpty()) {
                String taskId = String.valueOf(task.id());
                if (!content.equals(taskId) && !task.name().contains(content)) {
                    continue;
                }
            }
            result.add(task);
        }
        result.sort(Comparator.comparing(Task::createTime).reversed());
        return PageUtil.page(result, pageNo, pageSize);
    }

    public void remove(HugeClient client, int id) {
        client.task().delete(id);
    }

    public Task cancel(HugeClient client, int id) {
        return client.task().cancel(id);
    }
}
