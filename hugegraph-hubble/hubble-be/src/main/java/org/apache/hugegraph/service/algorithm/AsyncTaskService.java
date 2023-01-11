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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.structure.Task;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class AsyncTaskService {

    @Autowired
    private HugeClientPoolService poolService;

    private HugeClient getClient(int connId) {
        return this.poolService.getOrCreate(connId);
    }

    public Task get(int connId, int id) {
        HugeClient client = this.getClient(connId);
        return client.task().get(id);
    }

    public List<Task> list(int connId, List<Long> taskIds) {
        HugeClient client = this.getClient(connId);
        return client.task().list(taskIds);
    }

    public IPage<Task> list(int connId, int pageNo, int pageSize, String content,
                            String type, String status) {
        HugeClient client = this.getClient(connId);
        if (status.isEmpty()) {
            status = null;
        }
        List<Task> tasks = client.task().list(status);
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

    public void remove(int connId, int id) {
        HugeClient client = this.getClient(connId);
        client.task().delete(id);
    }

    public Task cancel(int connId, int id) {
        HugeClient client = this.getClient(connId);
        return client.task().cancel(id);
    }
}
