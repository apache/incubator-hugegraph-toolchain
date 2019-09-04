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

package com.baidu.hugegraph.controller;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baidu.hugegraph.entity.AdjacentQuery;
import com.baidu.hugegraph.entity.GremlinQuery;
import com.baidu.hugegraph.entity.GremlinResult;
import com.baidu.hugegraph.service.GremlinQueryService;
import com.baidu.hugegraph.util.Ex;
import com.google.common.collect.ImmutableSet;

@RestController
@RequestMapping("gremlin-query")
public class GremlinQueryController extends BaseController {

    private static final Set<String> TERM_OPERATORS = ImmutableSet.of(
            "eq", "gt", "gte", "lt", "lte"
    );

    @Autowired
    private GremlinQueryService service;

    @PostMapping
    public GremlinResult execute(@RequestBody GremlinQuery query) {
        this.checkParamsValid(query);
        return this.service.executeQuery(query);
    }

    @PutMapping
    public GremlinResult expand(@RequestBody AdjacentQuery query) {
        this.checkParamsValid(query);
        return this.service.expandVertex(query);
    }

    private void checkParamsValid(GremlinQuery query) {
        Ex.check(!StringUtils.isEmpty(query.getContent()),
                 "common.param.cannot-be-null-and-empty",
                 "gremlin-query.content");
    }

    private void checkParamsValid(AdjacentQuery query) {
        Ex.check(query.getConnectionId() != null,
                 "common.param.cannot-be-null", "connection_id");
        Ex.check(query.getVertexId() != null,
                 "common.param.cannot-be-null", "vertex_id");
        if (query.getConditions() != null && !query.getConditions().isEmpty()) {
            for (AdjacentQuery.Condition condition : query.getConditions()) {
                Ex.check(!StringUtils.isEmpty(condition.getKey()),
                         "common.param.cannot-be-null-and-empty",
                         "condition.key");
                Ex.check(!StringUtils.isEmpty(condition.getOperator()),
                         "common.param.cannot-be-null-and-empty",
                         "condition.operator");
                Ex.check(TERM_OPERATORS.contains(condition.getOperator()),
                         "common.param.should-belong-to", "condition.operator",
                         TERM_OPERATORS);
                Ex.check(condition.getValue() != null,
                         "common.param.cannot-be-null", "condition.value");
            }
        }
    }
}
