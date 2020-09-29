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

package com.baidu.hugegraph.service.load;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.baidu.hugegraph.entity.enums.JobManagerStatus;
import com.baidu.hugegraph.entity.enums.LoadStatus;
import com.baidu.hugegraph.entity.load.JobManager;
import com.baidu.hugegraph.entity.load.LoadTask;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.mapper.load.JobManagerMapper;
import com.baidu.hugegraph.util.HubbleUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class JobManagerService {

    @Autowired
    private JobManagerMapper mapper;
    @Autowired
    private LoadTaskService taskService;

    public int count() {
        return this.mapper.selectCount(null);
    }

    public JobManager get(int id) {
        return this.mapper.selectById(id);
    }

    public JobManager getTask(String job_name, int connId) {
        QueryWrapper<JobManager> query = Wrappers.query();
        query.eq("job_name", job_name);
        query.eq("conn_id", connId);
        return this.mapper.selectOne(query);
    }

    public List<JobManager> list(int connId, List<Integer> jobIds) {
        return this.mapper.selectBatchIds(jobIds);
    }

    public IPage<JobManager> list(int connId, int pageNo, int pageSize, String content) {
        QueryWrapper<JobManager> query = Wrappers.query();
        query.eq("conn_id", connId);
        if (!content.isEmpty()) {
            query.like("job_name", content);
        }
        query.orderByDesc("create_time");
        Page<JobManager> page = new Page<>(pageNo, pageSize);
        IPage<JobManager> list = this.mapper.selectPage(page, query);
        list.getRecords().forEach((p) -> {
            if (p.getJobStatus() == JobManagerStatus.IMPORTING) {
                List<LoadTask> tasks = this.taskService.taskListByJob(p.getId());
                JobManagerStatus status = JobManagerStatus.SUCCESS;
                Iterator<LoadTask> loadTasks = tasks.iterator();
                while (loadTasks.hasNext()) {
                    LoadTask loadTask = loadTasks.next();
                    if (loadTask.getStatus().inRunning() ||
                        loadTask.getStatus() == LoadStatus.PAUSED ||
                        loadTask.getStatus() == LoadStatus.STOPPED) {
                        status = JobManagerStatus.IMPORTING;
                        break;
                    }
                    if (loadTask.getStatus() == LoadStatus.FAILED) {
                        status = JobManagerStatus.FAILED;
                        break;
                    }
                }

                if (status == JobManagerStatus.SUCCESS ||
                    status == JobManagerStatus.FAILED) {
                    p.setJobStatus(status);
                    if (this.update(p) != 1) {
                        throw new InternalException("job-manager.entity.update.failed",
                                                    p);
                    }
                }
            }
            Date endDate = (p.getJobStatus() == JobManagerStatus.FAILED ||
                           p.getJobStatus() == JobManagerStatus.SUCCESS) ?
                           p.getUpdateTime() : HubbleUtil.nowDate();
            p.setJobDuration(endDate.getTime() - p.getCreateTime().getTime());
        });
        return list;
    }

    public List<JobManager> listAll() {
        return this.mapper.selectList(null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int remove(int id) {
        return this.mapper.deleteById(id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int save(JobManager entity) {
        return this.mapper.insert(entity);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int update(JobManager entity) {
        return this.mapper.updateById(entity);
    }
}
