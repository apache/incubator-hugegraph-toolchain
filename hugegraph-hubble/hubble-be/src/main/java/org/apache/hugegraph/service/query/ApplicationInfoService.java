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

package org.apache.hugegraph.service.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.hugegraph.entity.query.ApplicationInfo;
import org.apache.hugegraph.mapper.query.ApplicationInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ApplicationInfoService {

    @Autowired
    public ApplicationInfoMapper mapper;

    public void insertOrUpdateAppInfo(ApplicationInfo appInfo) {
        mapper.insertOrUpdateAppInfo(appInfo);
    }

    public List<ApplicationInfo> query(String graphName) {
        return mapper.queryByGraph(graphName);
    }

    public List<ApplicationInfo> query(String graphName, String appName, String appType) {
        return mapper.query(graphName, appName, appType);
    }

    public void delete(String graphName, String appName, String appType) {
        QueryWrapper<ApplicationInfo> query =
                new QueryWrapper<>();
        query.eq("graph_name", graphName)
             .eq("app_name", appName)
             .eq("app_type", appType);
        mapper.delete(query);
    }
}
