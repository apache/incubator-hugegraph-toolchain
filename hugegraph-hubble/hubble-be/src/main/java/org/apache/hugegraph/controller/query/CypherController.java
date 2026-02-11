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

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.enums.AsyncTaskStatus;
import org.apache.hugegraph.entity.enums.ExecuteStatus;
import org.apache.hugegraph.entity.enums.ExecuteType;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/cypher")
public class CypherController extends GremlinController {
    @Autowired
    private ExecuteHistoryService historyService;

    @Autowired
    private QueryService queryService;

    @GetMapping
    public GremlinResult execute(@PathVariable("graphspace") String graphSpace,
                                 @PathVariable("graph") String graph,
                                 @RequestParam(name = "cypher") String query) {
        this.checkParamsValid(query);

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, graphSpace, graph, 0L,
                ExecuteType.CYPHER, query,
                status, AsyncTaskStatus.UNKNOWN,
                -1L, createTime);
        this.historyService.save(history);

        StopWatch timer = StopWatch.createStarted();
        try {
            HugeClient client = this.authClient(graphSpace, graph);
            GremlinResult result =
                    this.queryService.executeCypherQuery(client, query);
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
    public Map<String, Object> executeAsyncTask(
            @PathVariable("graphspace") String graphSpace,
            @PathVariable("graph") String graph,
            @RequestBody GremlinQuery query) {
        this.checkParamsValid(query.getContent());

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.ASYNC_TASK_RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, graphSpace, graph, 0L,
                ExecuteType.CYPHER_ASYNC,
                query.getContent(), status,
                AsyncTaskStatus.UNKNOWN, -1L, createTime);
        this.historyService.save(history);

        StopWatch timer = StopWatch.createStarted();
        long asyncId = 0L;
        Map<String, Object> result = new HashMap<>(3);
        try {
            HugeClient client = this.authClient(graphSpace, graph);
            asyncId = this.queryService.executeCypherAsyncTask(client, query.getContent());
            status = ExecuteStatus.ASYNC_TASK_SUCCESS;
            result.put("task_id", asyncId);
            result.put("execute_status", status);
            return result;
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

    private void checkParamsValid(String query) {
        Ex.check(!StringUtils.isEmpty(query),
                "common.param.cannot-be-null-or-empty",
                "gremlin-query.content");
        checkContentLength(query);
    }
}
