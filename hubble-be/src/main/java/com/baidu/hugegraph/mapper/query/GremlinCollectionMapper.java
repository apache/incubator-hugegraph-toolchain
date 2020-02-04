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

package com.baidu.hugegraph.mapper.query;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import com.baidu.hugegraph.entity.query.GremlinCollection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

@Mapper
@Component
public interface GremlinCollectionMapper extends BaseMapper<GremlinCollection> {

    @Select("SELECT * FROM `gremlin_collection` " +
            "WHERE `name` LIKE '%${content}%' OR `content` LIKE '%${content}%'" +
            "ORDER BY " +
            "   CASE " +
            "       WHEN `name` LIKE '%${content}%' AND " +
            "            `content` LIKE '%${content}%' THEN 0 " +
            "       WHEN `name` LIKE '%${content}%' THEN 1 " +
            "       WHEN `content` LIKE '%${content}%' THEN 2 " +
            "   END ASC, " +
            "   `create_time` DESC")
    IPage<GremlinCollection> selectByContentInPage(IPage<GremlinCollection> page,
                                                   @Param("content")
                                                   String content);

    @SuppressWarnings("unused")
    @Insert("INSERT INTO `gremlin_collection`(name, content, create_time) " +
            "SELECT '${statement.name}', '${statement.content}', sysdate " +
            "WHERE (SELECT COUNT(*) FROM `gremlin_collection`) < ${limit}")
    @Options(useGeneratedKeys = true, keyProperty = "statement.id",
             flushCache = Options.FlushCachePolicy.TRUE)
    int insertIfUnreachLimit(@Param("statement") GremlinCollection collection,
                             @Param("limit") int limit);
}
