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

import org.apache.hugegraph.entity.auth.UserEntity;
import com.google.common.collect.ImmutableMap;
import org.apache.hugegraph.service.auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.auth.Login;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.structure.auth.LoginResult;

@RestController
@RequestMapping(Constant.API_VERSION + "auth")
public class LoginController extends BaseController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public Object login(@RequestBody Login login) {
        // Set Expire: 1 Month
        login.expire(60 * 60 * 24 * 30);

        HugeClient client = unauthClient();
        LoginResult result = client.auth().login(login);
        this.setUser(login.name());
        this.setSession("password", login.password());
        this.setToken(result.token());
        clearRequestHugeClient();

        // Get Current User Info
        client = this.authClient(null, null);
        UserEntity u = userService.getUser(client, login.name());
        u.setSuperadmin(userService.isSuperAdmin(client));
        client.close();

        return u;
    }

    @GetMapping("/status")
    public Object status() {

        HugeClient client = authClient(null, null);

        String level = userService.userLevel(client);

        return ImmutableMap.of("level", level);
    }

    @GetMapping("/logout")
    public void logout() {
        this.delToken();
    }
}
