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

package org.apache.hugegraph.controller.query;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.entity.enums.AsyncTaskStatus;
import org.apache.hugegraph.entity.enums.ExecuteStatus;
import org.apache.hugegraph.entity.enums.ExecuteType;
import org.apache.hugegraph.entity.query.AdjacentQuery;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.GremlinQueryService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import com.google.common.collect.ImmutableSet;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/gremlin-query")
public class GremlinQueryController extends GremlinController {

    private static final Set<String> CONDITION_OPERATORS = ImmutableSet.of(
            "eq", "gt", "gte", "lt", "lte"
    );

    @Autowired
    private GremlinQueryService queryService;
    @Autowired
    private ExecuteHistoryService historyService;

    @PostMapping
    public GremlinResult execute(@PathVariable("connId") int connId,
                                 @RequestBody GremlinQuery query) {
        this.checkParamsValid(query);

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, connId, 0L, ExecuteType.GREMLIN,
                                     query.getContent(), status, AsyncTaskStatus.UNKNOWN,
                                     -1L, createTime);
        this.historyService.save(history);

        StopWatch timer = StopWatch.createStarted();
        try {
            GremlinResult result = this.queryService.executeQuery(connId, query);
            status = ExecuteStatus.SUCCESS;
            return result;
        } catch (Throwable e) {
            status = ExecuteStatus.FAILED;
            throw e;
        } finally {
            timer.stop();
            long duration = timer.getTime(TimeUnit.MILLISECONDS);
            history.setStatus(status);
            history.setDuration(duration);
            this.historyService.update(history);
        }
    }

    @PostMapping("async-task")
    public ExecuteStatus executeAsyncTask(@PathVariable("connId") int connId,
                                          @RequestBody GremlinQuery query) {
        this.checkParamsValid(query);

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.ASYNC_TASK_RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, connId, 0L, ExecuteType.GREMLIN_ASYNC,
                                     query.getContent(), status,
                                     AsyncTaskStatus.UNKNOWN, -1L, createTime);
        this.historyService.save(history);

        StopWatch timer = StopWatch.createStarted();
        long asyncId = 0L;
        try {
            asyncId = this.queryService.executeAsyncTask(connId, query);
            status = ExecuteStatus.ASYNC_TASK_SUCCESS;
            return status;
        } catch (Throwable e) {
            status = ExecuteStatus.ASYNC_TASK_FAILED;
            throw e;
        } finally {
            timer.stop();
            long duration = timer.getTime(TimeUnit.MILLISECONDS);
            history.setStatus(status);
            history.setDuration(duration);
            history.setAsyncId(asyncId);
            this.historyService.update(history);
        }
    }

    @PutMapping
    public GremlinResult expand(@PathVariable("connId") int connId,
                                @RequestBody AdjacentQuery query) {
        this.checkParamsValid(query);
        try {
            return this.queryService.expandVertex(connId, query);
        } catch (Exception e) {
            Throwable rootCause = Ex.rootCause(e);
            throw new ExternalException("gremlin.expand.failed", rootCause,
                                        rootCause.getMessage());
        }
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
