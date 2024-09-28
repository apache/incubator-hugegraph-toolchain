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

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.hugegraph.entity.query.ElementEditHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface EditElementHistoryMapper extends
                                      BaseMapper<ElementEditHistory> {
    @Select("SELECT * FROM `edit_history` LIMIT #{limit}")
    List<ElementEditHistory> queryByLimit(@Param("limit") int limit);


    @Select("SELECT * FROM `edit_history` WHERE graphspace = #{graphspace} " +
            "AND graph = #{graph} AND element_id = #{elementId}")
    List<ElementEditHistory> queryByElementId(
            @Param("graphspace") String graphspace,
            @Param("graph") String graph,
            @Param("elementId") String elementId);


    @Select("<script>" +
            "SELECT * FROM `edit_history` WHERE graphspace = #{graphspace} AND graph = #{graph} " +
            "AND element_id IN " +
            "<foreach collection='elementIds' item='elementId' separator=',' open='(' close=')' >" +
            "#{elementId}" +
            "</foreach>" +
            "</script>")
    List<ElementEditHistory> queryByElementIds(
            @Param("graphspace") String graphspace,
            @Param("graph") String graph,
            @Param("elementIds") List<String> elementIds);

    // 批量插入方法
    @Insert({
            "<script>",
            "INSERT INTO edit_history (graphspace, graph, element_id, label, " +
            "property_num, option_type, option_time, option_person, content)",
            "VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.graphspace}, #{item.graph}, #{item.elementId}, #{item" +
            ".label}, #{item.propertyNum}, #{item.optionType}, #{item" +
            ".optionTime}, #{item.optionPerson}, #{item.content})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<ElementEditHistory> list);
}
