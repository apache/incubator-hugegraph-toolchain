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

package com.baidu.hugegraph.service.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.entity.query.ExecuteHistory;
import com.baidu.hugegraph.mapper.query.ExecuteHistoryMapper;
import com.baidu.hugegraph.options.HubbleOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ExecuteHistoryService {

    @Autowired
    private HugeConfig config;
    @Autowired
    private ExecuteHistoryMapper mapper;

    public IPage<ExecuteHistory> list(int connId, long current, long pageSize) {
        QueryWrapper<ExecuteHistory> query = Wrappers.query();
        query.eq("conn_id", connId).orderByDesc("create_time");
        Page<ExecuteHistory> page = new Page<>(current, pageSize);
        IPage<ExecuteHistory> results = this.mapper.selectPage(page, query);

        int limit = this.config.get(HubbleOptions.EXECUTE_HISTORY_SHOW_LIMIT);
        if (results.getTotal() > limit) {
            log.debug("Execute history total records: {}", results.getTotal());
            results.setTotal(limit);
        }
        return results;
    }

    public ExecuteHistory get(int id) {
        return this.mapper.selectById(id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int save(ExecuteHistory history) {
        return this.mapper.insert(history);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int update(ExecuteHistory history) {
        return this.mapper.updateById(history);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int remove(int id) {
        return this.mapper.deleteById(id);
    }

    @Async
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeExceedLimit() {
        int limit = this.config.get(HubbleOptions.EXECUTE_HISTORY_SHOW_LIMIT);
        this.mapper.deleteExceedLimit(limit);
    }
}
