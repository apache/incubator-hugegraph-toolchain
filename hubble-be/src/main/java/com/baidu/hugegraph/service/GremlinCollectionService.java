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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.entity.GremlinCollection;
import com.baidu.hugegraph.mapper.GremlinCollectionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class GremlinCollectionService {

    @Autowired
    private GremlinCollectionMapper mapper;

    public IPage<GremlinCollection> list(String content, Boolean nameOrderAsc,
                                         long current, long pageSize) {
        QueryWrapper<GremlinCollection> query = Wrappers.query();
        if (!StringUtils.isEmpty(content)) {
            String value = content;
            if (Constant.LIKE_WILDCARDS.contains(content)) {
                value = "\\" + content;
            }
            query.like("name", value).or().like("content", value);
        }
        if (nameOrderAsc != null) {
            if (nameOrderAsc) {
                query.orderByAsc("name");
            } else {
                query.orderByDesc("name");
            }
        }
        query.orderByDesc("create_time");
        return this.mapper.selectPage(new Page<>(current, pageSize), query);
    }

    public GremlinCollection get(int id) {
        return this.mapper.selectById(id);
    }

    public GremlinCollection getByName(String name) {
        QueryWrapper<GremlinCollection> query = Wrappers.query();
        query.eq("name", name);
        return this.mapper.selectOne(query);
    }

    public int count() {
        return this.mapper.selectCount(null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int save(GremlinCollection collection) {
        return this.mapper.insert(collection);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int update(GremlinCollection collection) {
        return this.mapper.updateById(collection);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int remove(int id) {
        return this.mapper.deleteById(id);
    }
}
