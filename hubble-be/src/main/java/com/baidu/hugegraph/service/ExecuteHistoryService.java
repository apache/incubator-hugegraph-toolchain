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

package com.baidu.hugegraph.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.baidu.hugegraph.entity.ExecuteHistory;
import com.baidu.hugegraph.mapper.ExecuteHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class ExecuteHistoryService {

    @Autowired
    private ExecuteHistoryMapper mapper;

    public List<ExecuteHistory> listAll() {
        return this.mapper.selectList(null);
    }

    public IPage<ExecuteHistory> list(long current, long pageSize) {
        QueryWrapper<ExecuteHistory> query = Wrappers.query();
        query.orderByDesc("create_time");
        return this.mapper.selectPage(new Page<>(current, pageSize), query);
    }

    public List<ExecuteHistory> listBatch(List<Integer> ids) {
        return this.mapper.selectBatchIds(ids);
    }

    public ExecuteHistory get(int id) {
        return this.mapper.selectById(id);
    }

    public int count() {
        return this.mapper.selectCount(null);
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
}
