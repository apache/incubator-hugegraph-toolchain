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
import org.apache.hugegraph.entity.auth.UserView;
import org.apache.hugegraph.service.auth.GraphSpaceUserService;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.structure.auth.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/auth/users")
public class GraphSpaceUserController extends AuthController {
    @Autowired
    private GraphSpaceUserService userService;

    @GetMapping
    public IPage<UserView> querySpaceUserViews(
            @PathVariable("graphspace") String graphSpace,
            @RequestParam(name = "query", required = false,
                          defaultValue = "") String query,
            @RequestParam(name = "page_no", required = false,
                          defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", required = false,
                          defaultValue = "10") int pageSize) {
        HugeClient client = this.authClient(graphSpace, null);
        return userService.queryPage(client, query, pageNo, pageSize);
    }

    @GetMapping("spaceadmin")
    public IPage<User> querySpaceAdmins(
            @PathVariable("graphspace") String graphSpace,
            @RequestParam(name = "query", required = false,
                          defaultValue = "") String query,
            @RequestParam(name = "page_no", required = false,
                          defaultValue = "1") int pageNo,
            @RequestParam(name = "page_size", required = false,
                          defaultValue = "10") int pageSize) {
        HugeClient client = this.authClient(graphSpace, null);
        return userService.querySpaceAdmins(client, graphSpace, query, pageNo,
                                            pageSize);
    }


    @GetMapping("{id}")
    public UserView get(@PathVariable("graphspace") String graphSpace,
                        @PathVariable("id") String userId) {
        HugeClient client = this.authClient(graphSpace, null);
        return userService.getUser(client, userId);
    }

    /**
     * 授权用户为graphspace的管理员
     */
    @GetMapping("spaceadmin/{id}")
    public UserManager setGraphSpaceAdmin(@PathVariable("graphspace") String graphSpace,
                                     @PathVariable("id") String userId) {
        HugeClient client = this.authClient(null, null);
        return client.auth().addSpaceAdmin(userId, graphSpace);
    }

    /**
     * 取消用户在某个graphspace的管理员权限
     */
    @DeleteMapping("spaceadmin/{id}")
    public void removeGraphSpaceAdmin(@PathVariable("graphspace") String graphSpace,
                                     @PathVariable("id") String userId) {
        HugeClient client = this.authClient(null, null);
        client.auth().delSpaceAdmin(userId, graphSpace);
    }


    @PostMapping
    public UserView create(@PathVariable("graphspace") String graphSpace,
                           @RequestBody UserView userView) {
        HugeClient client = this.authClient(graphSpace, null);
        return userService.createOrUpdate(client, userView);
    }

    @PutMapping("{id}")
    public UserView createOrUpdate(@PathVariable("graphspace") String graphSpace,
                                   @PathVariable("id") String uid,
                                   @RequestBody UserView userView) {
        HugeClient client = this.authClient(graphSpace, null);
        userView.setId(uid);
        return userService.createOrUpdate(client, userView);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("id") String uid) {
        HugeClient client = this.authClient(graphSpace, null);
        userService.unauthUser(client, uid);
    }
}
