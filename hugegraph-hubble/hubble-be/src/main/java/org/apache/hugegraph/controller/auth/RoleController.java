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
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.service.auth.RoleService;
import org.apache.hugegraph.structure.auth.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/auth/roles")
public class RoleController<role> extends AuthController {

    @Autowired
    private RoleService roleService;

    @GetMapping("list")
    public List<Role> listName(@PathVariable("graphspace") String graphSpace) {
        HugeClient client = this.authClient(graphSpace, null);
        return this.roleService.list(client);
    }

    public List<Role> list(@PathVariable("graphspace") String graphSpace) {
        HugeClient client = this.authClient(graphSpace, null);
        return this.roleService.list(client);
    }

    @GetMapping
    public IPage<Role> queryPage(@PathVariable("graphspace") String graphSpace,
                                  @RequestParam(name = "query", required = false,
                                    defaultValue = "") String query,
                                  @RequestParam(name = "page_no", required = false,
                                    defaultValue = "1") int pageNo,
                                  @RequestParam(name = "page_size", required = false,
                                    defaultValue = "10") int pageSize) {
        HugeClient client = this.authClient(graphSpace, null);
        return this.roleService.queryPage(client, query, pageNo, pageSize);
    }

    @GetMapping("{id}")
    public Role get(@PathVariable("graphspace") String graphSpace,
                     @PathVariable("id") String rid) {
        HugeClient client = this.authClient(graphSpace, null);
        return this.roleService.get(client, rid);
    }

    @PostMapping
    public Role add(@PathVariable("graphspace") String graphSpace,
                    @RequestBody Role role) {
        HugeClient client = this.authClient(graphSpace, null);
        return this.roleService.insert(client, role);
    }

    @PutMapping("{id}")
    public Role update(@PathVariable("graphspace") String graphSpace,
                        @PathVariable("id") String id,
                        @RequestBody Role role) {
        HugeClient client = this.authClient(graphSpace, null);
        Role r = this.roleService.get(client, id);
        r.description(role.description());
        r.nickname(role.nickname());

        this.roleService.update(client, r);

        return r;
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("id") String id) {
        HugeClient client = this.authClient(graphSpace, null);
        this.roleService.delete(client, id);
    }

    @GetMapping("setdefaultrole")
    public Map<String, String> setDefaultRole(@PathVariable("graphspace") String graphSpace,
                                              @RequestParam("user") String user,
                                              @RequestParam("role") String role,
                                              @RequestParam(name = "graph", required = false,
                                                      defaultValue = "")  String graph) {
        HugeClient client = this.authClient(graphSpace, graph);
        return client.graphSpace().setDefaultRole(graphSpace, user, role, graph);
    }

    @DeleteMapping("deldefaultrole")
    public Map<String, String> delDefaultRole(@PathVariable("graphspace") String graphSpace,
                                              @RequestParam("user") String user,
                                              @RequestParam("role") String role,
                                              @RequestParam(name = "graph", required = false,
                                                      defaultValue = "")  String graph) {
        HugeClient client = this.authClient(graphSpace, graph);
        return client.graphSpace().deleteDefaultRole(graphSpace, user, role, graph);
    }
}
