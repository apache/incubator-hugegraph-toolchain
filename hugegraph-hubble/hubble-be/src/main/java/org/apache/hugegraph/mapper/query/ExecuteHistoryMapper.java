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

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import org.apache.hugegraph.entity.query.ExecuteHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
@Component
public interface ExecuteHistoryMapper extends BaseMapper<ExecuteHistory> {

    @Delete("DELETE FROM `execute_history` WHERE `id` IN (" +
            "SELECT `id` FROM `execute_history` ORDER BY `create_time` DESC " +
            "LIMIT #{limit} OFFSET #{limit})")
    void deleteExceedLimit(@Param("limit") int limit);
}
