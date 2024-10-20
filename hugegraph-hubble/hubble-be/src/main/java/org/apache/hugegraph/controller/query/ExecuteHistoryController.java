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

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/execute-histories")
public class ExecuteHistoryController extends GremlinController {

    @Autowired
    private ExecuteHistoryService service;

    @GetMapping
    public IPage<ExecuteHistory> list(@PathVariable("graphspace") String graphSpace,
                                      @PathVariable("graph") String graph,
                                      @RequestParam(name = "type") int type,
                                      @RequestParam(name = "page_no",
                                                    required = false,
                                                    defaultValue = "1")
                                      int pageNo,
                                      @RequestParam(name = "page_size",
                                                    required = false,
                                                    defaultValue = "10")
                                      int pageSize,
                                      @RequestParam(name = "text2gremlin",
                                                    required = false,
                                                    defaultValue = "false")
                                          boolean text2Gremlin) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.list(client, type, pageNo, pageSize, text2Gremlin);
    }

    @GetMapping("{id}")
    public ExecuteHistory get(@PathVariable("graphspace") String graphSpace,
                              @PathVariable("graph") String graph,
                              @PathVariable("id") int id) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.get(client, id);
    }

    @DeleteMapping("{id}")
    public ExecuteHistory delete(@PathVariable("graphspace") String graphSpace,
                                 @PathVariable("graph") String graph,
                                 @PathVariable("id") int id) {
        HugeClient client = this.authClient(graphSpace, graph);
        ExecuteHistory oldEntity = this.service.get(client, id);
        if (oldEntity == null) {
            throw new ExternalException("execute-history.not-exist.id", id);
        }
        this.service.remove(client, id);
        return oldEntity;
    }
}
