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

package org.apache.hugegraph.service.load;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.hugegraph.entity.load.Datasource;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.mapper.load.DatasourceMapper;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatasourceService {

    @Autowired
    private DatasourceMapper mapper;

    public Datasource get(int id) {
        return this.mapper.selectById(id);
    }

    public IPage<Datasource> list(int pageNo, int pageSize, String query) {
        QueryWrapper<Datasource> wrapper = Wrappers.query();
        if (query != null && !query.isEmpty()) {
            wrapper.like("datasource_name", query);
        }
        wrapper.orderByDesc("create_time");
        return this.mapper.selectPage(
                new Page<>(pageNo, PageUtil.boundedSize(pageSize)), wrapper);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(Datasource entity) {
        entity.setCreateTime(HubbleUtil.nowDate());
        if (this.mapper.insert(entity) != 1) {
            throw new InternalException("entity.insert.failed", entity);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(int id) {
        if (this.mapper.deleteById(id) != 1) {
            throw new InternalException("entity.delete.failed", id);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeBatch(java.util.List<Integer> ids) {
        this.mapper.deleteBatchIds(ids);
    }
}
