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

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.BelongEntity;
import org.apache.hugegraph.service.auth.BelongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/auth/belongs")
public class BelongController extends AuthController {
    @Autowired
    BelongService belongService;

    public List<BelongEntity> list(
            @PathVariable("graphspace") String graphSpace,
            @RequestParam(value = "role_id", required = false) String rid,
            @RequestParam(value = "user_id", required = false) String uid) {
        HugeClient client = this.authClient(graphSpace, null);
        return belongService.list(client, rid, uid);
    }

    @GetMapping
    public IPage<BelongEntity> listPage(
            @PathVariable("graphspace") String graphSpace,
            @RequestParam(value = "role_id", required = false) String rid,
            @RequestParam(value = "user_id", required =
                    false) String uid,
            @RequestParam(name = "page_no", required = false,
                    defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", required = false,
                    defaultValue = "10") int pageSize) {
        HugeClient client = this.authClient(graphSpace, null);
        return belongService.listPage(client, rid, uid, pageNo, pageSize);
    }

    @GetMapping("{id}")
    public BelongEntity get(@PathVariable("graphspace") String graphSpace,
                            @PathVariable("id") String bid) {
        HugeClient client = this.authClient(graphSpace, null);
        return belongService.get(client, bid);
    }

    @PostMapping()
    public void create(@PathVariable("graphspace") String graphSpace,
                       @RequestBody BelongEntity belongEntity) {
        HugeClient client = this.authClient(graphSpace, null);
        belongService.add(client, belongEntity.getRoleId(),
                          belongEntity.getUserId());
    }

    @PostMapping("ids")
    public void createMany(@PathVariable("graphspace") String graphSpace,
                           @RequestBody BelongService.BelongsReq belongsReq) {
        HugeClient client = this.authClient(graphSpace, null);

        for (String uid : belongsReq.getUserIds()) {
            belongService.add(client, belongsReq.getRoleId(), uid);
        }

    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("id") String bid) {
        HugeClient client = this.authClient(graphSpace, null);
        belongService.delete(client, bid);
    }

    @DeleteMapping
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @RequestParam("role_id") String roleId,
                       @RequestParam("user_id") String userId) {

        HugeClient client = this.authClient(graphSpace, null);
        if (StringUtils.isNotEmpty(roleId) && !StringUtils.isNotEmpty(userId)) {
            belongService.delete(client, roleId, userId);
        }
    }

    @PostMapping("delids")
    public void deleteMany(@PathVariable("graphspace") String graphSpace,
                           @RequestBody DelIdsReq delIdsReq) {

        HugeClient client = this.authClient(graphSpace, null);

        belongService.deleteMany(client, delIdsReq.ids.toArray(new String[0]));
    }

    public static class DelIdsReq {
        public List<String> ids = new ArrayList();
    }
}
