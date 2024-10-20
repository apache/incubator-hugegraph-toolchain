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

package org.apache.hugegraph.controller.auth;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.WhiteIpListEntity;
import org.apache.hugegraph.service.auth.WhiteIpListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(Constant.API_VERSION + "auth/whiteiplist")
public class WhiteIpListController extends AuthController{
    @Autowired
    private WhiteIpListService whiteListService;

    @GetMapping("list")
    public Map<String, Object> list() {
        HugeClient client = this.authClient(null, null);
        return this.whiteListService.get(client);
    }

    @PostMapping("batch")
    public Map<String, Object> update(@RequestBody WhiteIpListEntity whiteIpListEntity) {
        HugeClient client = this.authClient(null, null);
        return this.whiteListService.batch(client, whiteIpListEntity);
    }

    @PutMapping("updatestatus")
    public Map<String, Object> update(@RequestBody boolean status) {
        HugeClient client = this.authClient(null, null);
        return this.whiteListService.updatestatus(client, status);
    }
}
