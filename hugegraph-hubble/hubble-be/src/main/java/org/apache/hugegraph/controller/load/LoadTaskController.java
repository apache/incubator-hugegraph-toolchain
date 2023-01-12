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

package org.apache.hugegraph.controller.load;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.GraphConnectionService;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/job-manager/{jobId}/load-tasks")
public class LoadTaskController extends BaseController {

    private static final int LIMIT = 500;

    @Autowired
    private GraphConnectionService connService;
    @Autowired
    private FileMappingService fmService;
    @Autowired
    private JobManagerService jobService;

    private final LoadTaskService service;

    @Autowired
    public LoadTaskController(LoadTaskService service) {
        this.service = service;
    }

    @GetMapping
    public IPage<LoadTask> list(@PathVariable("connId") int connId,
                                @PathVariable("jobId") int jobId,
                                @RequestParam(name = "page_no",
                                              required = false,
                                              defaultValue = "1")
                                int pageNo,
                                @RequestParam(name = "page_size",
                                              required = false,
                                              defaultValue = "10")
                                int pageSize) {
        return this.service.list(connId, jobId, pageNo, pageSize);
    }

    @GetMapping("ids")
    public List<LoadTask> list(@PathVariable("connId") int connId,
                               @RequestParam("task_ids") List<Integer> taskIds) {
        return this.service.list(connId, taskIds);
    }

    @GetMapping("{id}")
    public LoadTask get(@PathVariable("id") int id) {
        LoadTask task = this.service.get(id);
        if (task == null) {
            throw new ExternalException("load.task.not-exist.id", id);
        }
        return task;
    }

    @PostMapping
    public LoadTask create(@PathVariable("connId") int connId,
                           @PathVariable("jobId") int jobId,
                           @RequestBody LoadTask entity) {
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.SETTING,
                 "load.task.create.no-permission");
        synchronized (this.service) {
            Ex.check(this.service.count() < LIMIT,
                     "load.task.reached-limit", LIMIT);
            entity.setConnId(connId);
            this.service.save(entity);
        }
        return entity;
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("id") int id) {
        LoadTask task = this.service.get(id);
        if (task == null) {
            throw new ExternalException("load.task.not-exist.id", id);
        }
        this.service.remove(id);
    }

    @PostMapping("start")
    public List<LoadTask> start(@PathVariable("connId") int connId,
                                @PathVariable("jobId") int jobId,
                                @RequestParam("file_mapping_ids")
                                List<Integer> fileIds) {
        GraphConnection connection = this.connService.get(connId);
        if (connection == null) {
            throw new ExternalException("graph-connection.not-exist.id", connId);
        }
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.SETTING,
                 "load.task.start.no-permission");
        boolean existError = false;
        try {
            List<LoadTask> tasks = new ArrayList<>();
            for (Integer fileId : fileIds) {
                FileMapping fileMapping = this.fmService.get(fileId);
                if (fileMapping == null) {
                    throw new ExternalException("file-mapping.not-exist.id",
                                                fileId);
                }
                tasks.add(this.service.start(connection, fileMapping));
            }
            return tasks;
        } catch (Exception e) {
            existError = true;
            throw e;
        } finally {
            if (existError) {
                jobEntity.setJobStatus(JobStatus.FAILED);
            } else {
                jobEntity.setJobStatus(JobStatus.LOADING);
            }
            jobEntity.setUpdateTime(HubbleUtil.nowDate());
            this.jobService.update(jobEntity);
        }
    }

    @PostMapping("pause")
    public LoadTask pause(@PathVariable("connId") int connId,
                          @PathVariable("jobId") int jobId,
                          @RequestParam("task_id") int taskId) {
        GraphConnection connection = this.connService.get(connId);
        if (connection == null) {
            throw new ExternalException("graph-connection.not-exist.id", connId);
        }
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.LOADING,
                 "load.task.pause.no-permission");
        try {
            return this.service.pause(taskId);
        } finally {
            jobEntity.setJobStatus(JobStatus.LOADING);
            jobEntity.setUpdateTime(HubbleUtil.nowDate());
            this.jobService.update(jobEntity);
        }
    }

    @PostMapping("resume")
    public LoadTask resume(@PathVariable("connId") int connId,
                           @PathVariable("jobId") int jobId,
                           @RequestParam("task_id") int taskId) {
        GraphConnection connection = this.connService.get(connId);
        if (connection == null) {
            throw new ExternalException("graph-connection.not-exist.id", connId);
        }
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.LOADING,
                 "load.task.pause.no-permission");
        try {
            return this.service.resume(taskId);
        } finally {
            jobEntity.setJobStatus(JobStatus.LOADING);
            jobEntity.setUpdateTime(HubbleUtil.nowDate());
            this.jobService.update(jobEntity);
        }
    }

    @PostMapping("stop")
    public LoadTask stop(@PathVariable("connId") int connId,
                         @PathVariable("jobId") int jobId,
                         @RequestParam("task_id") int taskId) {
        GraphConnection connection = this.connService.get(connId);
        if (connection == null) {
            throw new ExternalException("graph-connection.not-exist.id", connId);
        }
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.LOADING,
                 "load.task.pause.no-permission");
        try {
            return this.service.stop(taskId);
        } finally {
            jobEntity.setJobStatus(JobStatus.LOADING);
            jobEntity.setUpdateTime(HubbleUtil.nowDate());
            this.jobService.update(jobEntity);
        }
    }

    @PostMapping("retry")
    public LoadTask retry(@PathVariable("connId") int connId,
                          @PathVariable("jobId") int jobId,
                          @RequestParam("task_id") int taskId) {
        GraphConnection connection = this.connService.get(connId);
        if (connection == null) {
            throw new ExternalException("graph-connection.not-exist.id", connId);
        }
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Ex.check(jobEntity.getJobStatus() == JobStatus.LOADING,
                 "load.task.pause.no-permission");
        try {
            return this.service.retry(taskId);
        } finally {
            jobEntity.setJobStatus(JobStatus.LOADING);
            jobEntity.setUpdateTime( HubbleUtil.nowDate());
            this.jobService.update(jobEntity);
        }
    }

    @GetMapping("{id}/reason")
    public Response reason(@PathVariable("connId") int connId,
                           @PathVariable("jobId") int jobId,
                           @PathVariable("id") int id) {
        LoadTask task = this.service.get(id);
        if (task == null) {
            throw new ExternalException("load.task.not-exist.id", id);
        }
        JobManager jobEntity = this.jobService.get(jobId);
        Ex.check(jobEntity != null, "job-manager.not-exist.id", jobId);
        Integer fileId = task.getFileId();
        FileMapping mapping = this.fmService.get(fileId);
        String reason = this.service.readLoadFailedReason(mapping);
        return Response.builder()
                       .status(Constant.STATUS_OK)
                       .data(reason)
                       .build();
    }
}
