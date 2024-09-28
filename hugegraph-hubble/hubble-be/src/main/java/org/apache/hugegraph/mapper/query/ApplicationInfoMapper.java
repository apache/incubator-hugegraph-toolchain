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

package org.apache.hugegraph.mapper.query;

import java.util.List;

import org.apache.hugegraph.entity.query.ApplicationInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface ApplicationInfoMapper extends BaseMapper<ApplicationInfo> {

    @Select("SELECT COUNT(*) FROM `app_info` WHERE graph_name = #{graphName} AND " +
            "app_name = #{appName} AND app_type = #{appType}")
    int countByAppNameAndAppType(@Param("graphName") String graphName,
                                 @Param("appName") String appName,
                                 @Param("appType") String appType);

    @Insert("INSERT INTO app_info (graph_name, app_name, app_type, count_query, distribution_query) " +
            "VALUES (#{graphName}, #{appName}, #{appType}, #{countQuery}, #{distributionQuery})")
    int insertAppInfo(ApplicationInfo appInfo);

    @Update("UPDATE `app_info` SET graph_name = #{graphName}, " +
            "count_query = #{countQuery}, distribution_query = #{distributionQuery} " +
            "WHERE graph_name = #{graphName} AND app_name = #{appName} AND app_type = #{appType}")
    int updateAppInfo(ApplicationInfo appInfo);

    @Select("SELECT * FROM `app_info` WHERE graph_name = #{graphName}")
    List<ApplicationInfo> queryByGraph(@Param("graphName") String graphName);

    @Select("SELECT * FROM `app_info` WHERE graph_name = #{graphName} AND " +
            "app_name = #{appName} AND app_type = #{appType}")
    List<ApplicationInfo> query(@Param("graphName") String graphName,
                                @Param("appName") String appName,
                                @Param("appType") String appType);

    // 方法用于插入或更新数据
    default int insertOrUpdateAppInfo(ApplicationInfo appInfo) {
        int count = countByAppNameAndAppType(appInfo.getGraphName(),
                                             appInfo.getAppName(),
                                             appInfo.getAppType().string());
        if (count > 0) {
            return updateAppInfo(appInfo);
        } else {
            return insertAppInfo(appInfo);
        }
    }
}
