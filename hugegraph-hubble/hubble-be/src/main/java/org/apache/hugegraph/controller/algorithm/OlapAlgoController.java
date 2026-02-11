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

package org.apache.hugegraph.controller.algorithm;

import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.algorithm.OlapEntity;
import org.apache.hugegraph.entity.query.OlapView;
import org.apache.hugegraph.service.algorithm.OlapAlgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/algorithms/olap")
public class OlapAlgoController extends BaseController {
    @Autowired
    private OlapAlgoService service;

    @PostMapping
    public OlapView olapView(@PathVariable("graphspace") String graphspace,
                             @PathVariable("graph") String graph,
                             @RequestBody OlapEntity body) {
        HugeClient client = this.authClient(graphspace, graph);
        return this.service.olapView(client, graphspace, body);
    }
}
