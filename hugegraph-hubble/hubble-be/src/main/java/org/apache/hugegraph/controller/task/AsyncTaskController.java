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

package org.apache.hugegraph.controller.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.algorithm.AsyncTaskService;
import org.apache.hugegraph.structure.Task;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/async-tasks")
public class AsyncTaskController extends BaseController {

    private final AsyncTaskService service;

    @Autowired
    public AsyncTaskController(AsyncTaskService service) {
        this.service = service;
    }

    @GetMapping("{id}")
    public Task get(@PathVariable("connId") int connId,
                    @PathVariable("id") int id) {
        Task task = this.service.get(connId, id);
        if (task == null) {
            throw new ExternalException("async.task.not-exist.id", id);
        }
        return task;
    }

    @PostMapping("cancel/{id}")
    public Task cancel(@PathVariable("connId") int connId,
                       @PathVariable("id") int id) {
        Task task = this.service.cancel(connId, id);
        if (task == null) {
            throw new ExternalException("async.task.not-exist.id", id);
        }
        return task;
    }

    @GetMapping("ids")
    public List<Task> list(@PathVariable("connId") int connId,
                           @RequestParam("ids") List<Long> taskIds) {
        return this.service.list(connId, taskIds);
    }

    @GetMapping
    public IPage<Task> list(@PathVariable("connId") int connId,
                            @RequestParam(name = "page_no",
                                          required = false,
                                          defaultValue = "1")
                                          int pageNo,
                            @RequestParam(name = "page_size",
                                          required = false,
                                          defaultValue = "10")
                                          int pageSize,
                            @RequestParam(name = "content",
                                          required = false,
                                          defaultValue = "")
                                          String content,
                            @RequestParam(name = "type",
                                          required = false,
                                          defaultValue = "")
                                          String type,
                            @RequestParam(name = "status",
                                          required = false,
                                          defaultValue = "")
                                          String status) {
        return this.service.list(connId, pageNo, pageSize, content, type, status);
    }

    @DeleteMapping
    public void delete(@PathVariable("connId") int connId,
                       @RequestParam("ids") List<Integer> ids) {
        for (int id : ids) {
            Task task = this.service.get(connId, id);
            if (task == null) {
                throw new ExternalException("async.task.not-exist.id", id);
            }
            this.service.remove(connId, id);
        }
    }
}
