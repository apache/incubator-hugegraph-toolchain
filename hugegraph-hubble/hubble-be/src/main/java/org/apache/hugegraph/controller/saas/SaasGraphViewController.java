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

package org.apache.hugegraph.controller.saas;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.query.GremlinController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.enums.AsyncTaskStatus;
import org.apache.hugegraph.entity.enums.ExecuteStatus;
import org.apache.hugegraph.entity.enums.ExecuteType;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.HubbleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION +
                "/graphspaces/{graphspace}/graphs/{graph}/graphview")

/**
 * 此接口为定制Gremlin查询接口:
 * 用途: 提供给前端查询Gremlin的接口,包装返回的数据用于前端画布展示
 */
public class SaasGraphViewController extends GremlinController {

    @Autowired
    private QueryService queryService;
    @Autowired
    private ExecuteHistoryService historyService;

    @PostMapping
    public GremlinResult execute(@PathVariable("graphspace") String graphSpace,
                                 @PathVariable("graph") String graph,
                                 @RequestBody GraphViewQuery query) {
        this.checkParamsValid(query);

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, graphSpace, graph, 0L,
                                     ExecuteType.GREMLIN, query.getGremlin(),
                                     status, AsyncTaskStatus.UNKNOWN,
                                     -1L, createTime);
        this.historyService.save(history);
        StopWatch timer = StopWatch.createStarted();

        //uuapLoginController.tryLogin(graphSpace, graph,
        //                             query.userName, query.password);
        // TODO X Deleted
        try {
            HugeClient client = this.authClient(graphSpace, graph);
            GremlinResult result =
                    this.queryService.executeGremlinQuery(client,
                                                          query.convert2GremlinQuery());
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
            @RequestBody GraphViewQuery query) {
        checkParamsValid(query);

        Date createTime = HubbleUtil.nowDate();
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.ASYNC_TASK_RUNNING;
        ExecuteHistory history;
        history = new ExecuteHistory(null, graphSpace, graph, 0L,
                                     ExecuteType.GREMLIN_ASYNC,
                                     query.getGremlin(), status,
                                     AsyncTaskStatus.UNKNOWN, -1L, createTime);
        this.historyService.save(history);

        StopWatch timer = StopWatch.createStarted();
        long asyncId = 0L;
        Map<String, Object> result = new HashMap<>(3);

        //uuapLoginController.tryLogin(graphSpace, graph,
        //                             query.userName, query.password);
        // TODO C Deleted

        try {
            HugeClient client = this.authClient(graphSpace, graph);
            asyncId = this.queryService.executeGremlinAsyncTask(client,
                                                                query.convert2GremlinQuery());
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

    private void checkParamsValid(GraphViewQuery query) {
        E.checkNotNull(query.getGremlin(), "params [gremlin] must not be null");
        checkContentLength(query.getGremlin());
        E.checkNotNull(query.getUserName(), "params [username] must not be null");
        E.checkNotNull(query.getGremlin(), "params [password] must not be null");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GraphViewQuery {
        @JsonProperty("gremlin")
        private String gremlin;

        @JsonProperty("username")
        private String userName;

        @JsonProperty("password")
        private String password;

        public GremlinQuery convert2GremlinQuery() {
            return new GremlinQuery(this.gremlin);
        }
    }
}

