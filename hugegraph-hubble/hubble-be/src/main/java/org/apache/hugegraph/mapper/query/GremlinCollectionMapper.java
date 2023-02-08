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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import org.apache.hugegraph.entity.query.GremlinCollection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

@Mapper
@Component
public interface GremlinCollectionMapper extends BaseMapper<GremlinCollection> {

    @Select("SELECT *, " +
            "(CASE WHEN `name` LIKE concat('%', #{content}, '%') AND " +
            "           `content` LIKE concat('%', #{content}, '%') THEN 0 " +
            "      WHEN `name` LIKE concat('%', #{content}, '%') THEN 1 " +
            "      WHEN `content` LIKE concat('%', #{content}, '%') THEN 2 " +
            "END) as relation_sort " +
            "FROM `gremlin_collection` " +
            "WHERE `conn_id` = #{conn_id} AND " +
            "(`name` LIKE concat('%', #{content}, '%') OR " +
            "`content` LIKE concat('%', #{content}, '%')) " +
            "ORDER BY relation_sort ASC, `create_time` DESC")
    IPage<GremlinCollection> selectByContentInPage(IPage<GremlinCollection> page,
                                                   @Param("conn_id") int connId,
                                                   @Param("content")
                                                   String content);
}
