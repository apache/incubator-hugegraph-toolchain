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

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.controller.BaseController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.hugegraph.service.auth.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.driver.HugeClient;

@RestController
@RequestMapping(Constant.API_VERSION + "auth/groups")
public class GroupController extends BaseController {

    @Autowired
    private GroupService groupService;

    @GetMapping("list")
    public List<Group> list() {
        HugeClient client = this.authClient(null, null);
        return this.groupService.list(client);
    }

    @GetMapping
    public IPage<Group> queryPage(@RequestParam(name = "query", required = false,
            defaultValue = "") String query,
                                  @RequestParam(name = "page_no", required = false,
                                          defaultValue = "1") int pageNo,
                                  @RequestParam(name = "page_size", required = false,
                                          defaultValue = "10") int pageSize) {
        HugeClient client = this.authClient(null, null);
        return this.groupService.queryPage(client, query, pageNo, pageSize);
    }

    @GetMapping("{id}")
    public Group get(@PathVariable("id") String rid) {
        HugeClient client = this.authClient(null, null);
        return this.groupService.get(client, rid);
    }

    @PostMapping
    public Group add(@RequestBody Group group) {
        HugeClient client = this.authClient(null, null);
        return this.groupService.insert(client, group);
    }

    @PutMapping("{id}")
    public Group update(@PathVariable("id") String id,
                        @RequestBody Group group) {
        HugeClient client = this.authClient(null, null);
        Group g = this.groupService.get(client, id);
        g.description(group.description());
        g.nickname(group.nickname());
        this.groupService.update(client, g);

        return g;
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("id") String id) {
        HugeClient client = this.authClient(null, null);
        this.groupService.delete(client, id);
    }

    @PostMapping("batch/{id}")
    public Map<String, Object> batch(@PathVariable("id") String id,
                                     @RequestBody Map<String, Object> action) {
        HugeClient client = this.authClient(null, null);
        return groupService.batch(client, id, action);
    }
}
