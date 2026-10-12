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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import org.apache.hugegraph.entity.query.ExecuteHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
@Component
public interface ExecuteHistoryMapper extends BaseMapper<ExecuteHistory> {

    // 单条 DELETE ... IN (...) 允许携带的最大 id 数, 避免超出数据库
    // 的报文大小与参数个数上限
    int DELETE_BATCH_SIZE = 500;

    // 删除超出限制的记录, 按 (graphspace, graph) 分别保留最新的 limit 条
    default void deleteExceedLimit(int limit) {
        for (ExecuteHistory scope : findScopes()) {
            while (true) {
                int excess = countScope(scope.getGraphspace(),
                                        scope.getGraph()) - limit;
                if (excess <= 0) {
                    break;
                }
                int batchSize = Math.min(excess, DELETE_BATCH_SIZE);
                List<Long> ids = findIdsOldestFirst(scope.getGraphspace(),
                                                    scope.getGraph(),
                                                    batchSize);
                if (ids.isEmpty()) {
                    break;
                }
                // Another scheduler may have deleted the same candidates.
                // Recheck after selection to avoid consuming retained rows.
                excess = countScope(scope.getGraphspace(), scope.getGraph()) -
                         limit;
                if (excess <= 0) {
                    break;
                }
                deleteBatchIds(ids.subList(0, Math.min(excess, ids.size())));
            }
        }
    }

    // 查询所有出现过的 (graphspace, graph) 组合
    @Select("SELECT `graphspace`, `graph` FROM `execute_history` " +
            "GROUP BY `graphspace`, `graph`")
    List<ExecuteHistory> findScopes();

    @Select("SELECT COUNT(*) FROM `execute_history` " +
            "WHERE `graphspace` = #{graphspace} AND `graph` = #{graph}")
    int countScope(@Param("graphspace") String graphspace,
                   @Param("graph") String graph);

    @Select("SELECT `id` FROM `execute_history` " +
            "WHERE `graphspace` = #{graphspace} AND `graph` = #{graph} " +
            "ORDER BY `create_time` ASC, `id` ASC " +
            "LIMIT #{batchSize}")
    List<Long> findIdsOldestFirst(@Param("graphspace") String graphspace,
                                  @Param("graph") String graph,
                                  @Param("batchSize") int batchSize);
}
