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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/execute-histories")
public class ExecuteHistoryController extends GremlinController {

    @Autowired
    private ExecuteHistoryService service;

    @GetMapping
    public IPage<ExecuteHistory> list(@PathVariable("connId") int connId,
                                      @RequestParam(name = "page_no",
                                                    required = false,
                                                    defaultValue = "1")
                                      int pageNo,
                                      @RequestParam(name = "page_size",
                                                    required = false,
                                                    defaultValue = "10")
                                      int pageSize) {
        return this.service.list(connId, pageNo, pageSize);
    }

    @GetMapping("{id}")
    public ExecuteHistory get(@PathVariable("connId") int connId,
                              @PathVariable("id") int id) {
        return this.service.get(connId, id);
    }

    @DeleteMapping("{id}")
    public ExecuteHistory delete(@PathVariable("connId") int connId,
                                 @PathVariable("id") int id) {
        ExecuteHistory oldEntity = this.service.get(connId, id);
        if (oldEntity == null) {
            throw new ExternalException("execute-history.not-exist.id", id);
        }
        this.service.remove(connId, id);
        return oldEntity;
    }
}
