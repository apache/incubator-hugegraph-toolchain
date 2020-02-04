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

package com.baidu.hugegraph.controller.query;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.entity.enums.ExecuteStatus;
import com.baidu.hugegraph.entity.enums.ExecuteType;
import com.baidu.hugegraph.entity.query.AdjacentQuery;
import com.baidu.hugegraph.entity.query.ExecuteHistory;
import com.baidu.hugegraph.entity.query.GremlinQuery;
import com.baidu.hugegraph.entity.query.GremlinResult;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.service.query.ExecuteHistoryService;
import com.baidu.hugegraph.service.query.GremlinQueryService;
import com.baidu.hugegraph.util.Ex;
import com.google.common.collect.ImmutableSet;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "gremlin-query")
public class GremlinQueryController extends GremlinController {

    private static final Set<String> CONDITION_OPERATORS = ImmutableSet.of(
            "eq", "gt", "gte", "lt", "lte"
    );

    @Autowired
    private GremlinQueryService queryService;
    @Autowired
    private ExecuteHistoryService historyService;

    @PostMapping
    public GremlinResult execute(@RequestBody GremlinQuery query) {
        this.checkParamsValid(query);

        Date createTime = new Date();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.RUNNING;
        ExecuteHistory history = ExecuteHistory.builder()
                                               .type(ExecuteType.GREMLIN)
                                               .content(query.getContent())
                                               .status(status)
                                               .duration(-1L)
                                               .createTime(createTime)
                                               .build();
        int rows = this.historyService.save(history);
        if (rows != 1) {
            throw new InternalException("entity.insert.failed", history);
        }

        StopWatch timer = StopWatch.createStarted();
        try {
            int connId = query.getConnectionId();
            GremlinResult result = this.queryService.executeQuery(query, connId);
            status = ExecuteStatus.SUCCESS;
            return result;
        } catch (Exception e) {
            status = ExecuteStatus.FAILED;
            throw e;
        } finally {
            timer.stop();
            long duration = timer.getTime(TimeUnit.MILLISECONDS);
            history.setStatus(status);
            history.setDuration(duration);
            rows = this.historyService.update(history);
            if (rows != 1) {
                log.error("Failed to save execute history entity {}", history);
            }
        }
    }

    @PutMapping
    public GremlinResult expand(@RequestBody AdjacentQuery query) {
        this.checkParamsValid(query);
        int connId = query.getConnectionId();
        return this.queryService.expandVertex(query, connId);
    }

    private void checkParamsValid(GremlinQuery query) {
        Ex.check(!StringUtils.isEmpty(query.getContent()),
                 "common.param.cannot-be-null-or-empty",
                 "gremlin-query.content");
        checkContentLength(query.getContent());
    }

    private void checkParamsValid(AdjacentQuery query) {
        Ex.check(!StringUtils.isEmpty(query.getVertexId()),
                 "common.param.cannot-be-null-or-empty", "vertex_id");
        Ex.check(!StringUtils.isEmpty(query.getVertexLabel()),
                 "common.param.cannot-be-null-or-empty", "vertex_label");
        if (query.getConditions() != null && !query.getConditions().isEmpty()) {
            for (AdjacentQuery.Condition condition : query.getConditions()) {
                Ex.check(!StringUtils.isEmpty(condition.getKey()),
                         "common.param.cannot-be-null-or-empty",
                         "condition.key");
                Ex.check(!StringUtils.isEmpty(condition.getOperator()),
                         "common.param.cannot-be-null-or-empty",
                         "condition.operator");
                Ex.check(CONDITION_OPERATORS.contains(condition.getOperator()),
                         "common.param.should-belong-to", "condition.operator",
                         CONDITION_OPERATORS);
                Ex.check(condition.getValue() != null,
                         "common.param.cannot-be-null", "condition.value");
            }
        }
    }
}
