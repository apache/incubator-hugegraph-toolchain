/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.functional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.structure.auth.Access;
import com.baidu.hugegraph.structure.auth.Belong;
import com.baidu.hugegraph.structure.auth.Group;
import com.baidu.hugegraph.structure.auth.HugePermission;
import com.baidu.hugegraph.structure.auth.HugeResource;
import com.baidu.hugegraph.structure.auth.HugeResourceType;
import com.baidu.hugegraph.structure.auth.Login;
import com.baidu.hugegraph.structure.auth.LoginResult;
import com.baidu.hugegraph.structure.auth.Target;
import com.baidu.hugegraph.structure.auth.TokenPayload;
import com.baidu.hugegraph.structure.auth.User;
import com.baidu.hugegraph.structure.auth.User.UserRole;
import com.baidu.hugegraph.testutil.Assert;

public class AuthManagerTest extends BaseFuncTest {

    @Override
    @Before
    public void setup() {
    }

    @Override
    @After
    public void teardown() throws Exception {
        auth().deleteAll();
    }

    @Test
    public void testAuth() {
        User user = new User();
        user.name("bob");
        user.password("123456");
        user = auth().createUser(user);

        Group group = new Group();
        group.name("managers");
        group = auth().createGroup(group);

        Target gremlin = new Target();
        gremlin.name("gremlin");
        gremlin.graph("hugegraph");
        gremlin.url("127.0.0.1:8080");
        gremlin.resources(new HugeResource(HugeResourceType.GREMLIN));
        gremlin = auth().createTarget(gremlin);

        Target task = new Target();
        task.name("task");
        task.graph("hugegraph");
        task.url("127.0.0.1:8080");
        task.resources(new HugeResource(HugeResourceType.TASK));
        task = auth().createTarget(task);

        Belong belong = new Belong();
        belong.user(user);
        belong.group(group);
        belong = auth().createBelong(belong);

        Access access1 = new Access();
        access1.group(group);
        access1.target(gremlin);
        access1.permission(HugePermission.EXECUTE);
        access1 = auth().createAccess(access1);

        Access access2 = new Access();
        access2.group(group);
        access2.target(task);
        access2.permission(HugePermission.READ);
        access2 = auth().createAccess(access2);

        UserRole role = auth().getUserRole(user);
        String r = "{\"roles\":{\"hugegraph\":" +
                   "{\"READ\":[{\"type\":\"TASK\",\"label\":\"*\",\"properties\":null}]," +
                   "\"EXECUTE\":[{\"type\":\"GREMLIN\",\"label\":\"*\",\"properties\":null}]}}}";
        Assert.assertEquals(r, role.toString());

        Login login = new Login();
        login.name("bob");
        login.password("123456");
        LoginResult result = auth().login(login);

        String token = result.token();

        HugeClient client = baseClient();
        client.setAuthContext("Bearer " + token);

        TokenPayload payload = auth().verifyToken();
        Assert.assertEquals("bob", payload.username());
        Assert.assertEquals(user.id(), payload.userId());

        auth().logout();
        client.resetAuthContext();
    }
}
