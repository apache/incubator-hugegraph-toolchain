/*
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

package org.apache.hugegraph.api.auth;

import java.util.List;

import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.auth.Login;
import org.apache.hugegraph.structure.auth.LoginResult;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.testutil.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LoginApiTest extends AuthApiTest {

    private static LoginAPI loginAPI;
    private static UserAPI userAPI;

    @BeforeClass
    public static void init() {
        loginAPI = new LoginAPI(initClient(), GRAPH);
        userAPI = new UserAPI(initClient(), GRAPH);
    }

    @AfterClass
    public static void clear() {
        List<User> users = userAPI.list(-1);
        for (User user : users) {
            if (user.name().equals("admin")) {
                continue;
            }
            userAPI.delete(user.id());
        }
    }

    @Test
    public void testLogin() {
        Login login = new Login();
        login.name("user1");
        login.password("p1");
        Assert.assertThrows(ServerException.class, () -> {
            loginAPI.login(login);
        }, e -> {
            Assert.assertContains("Incorrect username or password",
                                  e.getMessage());
        });

        User user1 = new User();
        user1.name("user1");
        user1.password("p1");
        userAPI.create(user1);

        LoginResult result = loginAPI.login(login);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.token());
    }
}
