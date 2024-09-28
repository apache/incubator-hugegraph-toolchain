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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.entity.query.ElementEditHistory;
import org.apache.hugegraph.mapper.query.EditElementHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EditElementHistoryService {

    @Autowired
    private EditElementHistoryMapper mapper;

    public List<ElementEditHistory> queryByLimit(int limit) {
        return mapper.queryByLimit(limit);
    }

    public int add(ElementEditHistory history) {
        return mapper.insert(history);
    }

    public int add(List<ElementEditHistory> histories) {
        try {
            return mapper.insertBatch(histories);
        } catch (Exception e) {
            log.error("add element edit history [{}] error: ",
                      StringUtils.join(histories, ","), e);
        }
        return 0;
    }


    public int add(String graphspace, String graph,
                   String elementId, String label, int propertyNum,
                   String optionType, Date optionTime,
                   String optionPerson, String content) {
        ElementEditHistory eleEditHis = ElementEditHistory.builder()
                                  .graphspace(graphspace)
                                  .graph(graph)
                                  .elementId(elementId)
                                  .label(label)
                                  .propertyNum(propertyNum)
                                  .optionType(optionType)
                                  .optionTime(optionTime)
                                  .optionPerson(optionPerson)
                                  .content(content)
                                  .build();
        try {
            return mapper.insert(eleEditHis);
        } catch (Exception e) {
            log.error("add element edit history [{}] error: ",
                      eleEditHis.toString(), e);
        }
        return 0;
    }


    public IPage<ElementEditHistory> list(String graphSpace,
                                          String graph,
                                          int current,
                                          int pageSize) {
        QueryWrapper<ElementEditHistory> query = Wrappers.query();
        query.eq("graphspace", graphSpace)
             .eq("graph", graph)
             .orderByDesc("option_time");
        Page<ElementEditHistory> page = new Page<>(current, pageSize);
        return this.mapper.selectPage(page, query);
    }

    public IPage<ElementEditHistory> queryByConditions(String graphSpace,
                                                       String graph,
                                                       List<String> optionPersons,
                                                       List<String> optionTypes,
                                                       String elementId,
                                                       String optionTimeFrom,
                                                       String optionTimeTo,
                                                       int current,
                                                       int pageSize) {
        QueryWrapper<ElementEditHistory> query = Wrappers.query();
        query.eq("graphspace", graphSpace)
             .eq("graph", graph)
             .orderByDesc("option_time");

        if (!elementId.isEmpty()) {
            query.eq("element_id", elementId);
        }

        if (!optionPersons.isEmpty()) {
            query.in("option_person", optionPersons);
        }

        if (!optionTypes.isEmpty()) {
            query.in("option_type", optionTypes);
        }

        if (!(StringUtils.isEmpty(optionTimeFrom) &&
              StringUtils.isEmpty(optionTimeTo))) {
            query.between("option_time", optionTimeFrom, optionTimeTo);
        }

        Page<ElementEditHistory> page = new Page<>(current, pageSize);
        return this.mapper.selectPage(page, query);
    }


    public List<ElementEditHistory> queryByElementId(String graphSpace,
                                                     String graph,
                                                     String elementId) {
        return mapper.queryByElementId(graphSpace, graph, elementId);
    }

    public Map<String, ElementEditHistory> queryByElementIds(String graphSpace,
                                                             String graph,
                                                             List<String> elementIds) {
        List<ElementEditHistory> list =
                mapper.queryByElementIds(graphSpace, graph, elementIds);
        Map<String, ElementEditHistory> map = new HashMap<>();
        list.forEach(ele -> {
            map.merge(ele.getElementId(), ele, (oldValue, newValue) -> {
                // 如果 newValue 的 optionTime 比较晚，则替换原来的元素
                return newValue.getOptionTime().compareTo(oldValue.getOptionTime()) > 0 ? newValue : oldValue;
            });
        });
        return map;
    }

    public IPage<ElementEditHistory> queryByElementId(String graphSpace,
                                                      String graph,
                                                      String elementId,
                                                      int current,
                                                      int pageSize) {
        QueryWrapper<ElementEditHistory> query = Wrappers.query();
        query.eq("graphspace", graphSpace)
             .eq("graph", graph)
             .eq("element_id", elementId)
             .orderByDesc("option_time");
        Page<ElementEditHistory> page = new Page<>(current, pageSize);
        return this.mapper.selectPage(page, query);
    }
}
