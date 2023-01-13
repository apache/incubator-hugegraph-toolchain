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

package org.apache.hugegraph.service;

import org.apache.hugegraph.exception.InternalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.entity.UserInfo;
import org.apache.hugegraph.mapper.UserInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

@Service
public class UserInfoService {

    @Autowired
    private UserInfoMapper mapper;

    public UserInfo getByName(String name) {
        QueryWrapper<UserInfo> query = Wrappers.query();
        query.eq("username", name);
        return this.mapper.selectOne(query);
    }

    public void save(UserInfo userInfo) {
        if (this.mapper.insert(userInfo) != 1) {
            throw new InternalException("entity.insert.failed", userInfo);
        }
    }

    public void update(UserInfo userInfo) {
        if (this.mapper.updateById(userInfo) != 1) {
            throw new InternalException("entity.update.failed", userInfo);
        }
    }
}
