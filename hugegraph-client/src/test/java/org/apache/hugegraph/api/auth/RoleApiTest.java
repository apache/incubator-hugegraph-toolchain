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

package org.apache.hugegraph.api.auth;

import java.util.List;

import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.auth.Role;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RoleApiTest extends AuthApiTest {

    private static RoleAPI api;

    @BeforeClass
    public static void init() {
        api = new RoleAPI(initClient(), GRAPHSPACE);
    }

    @AfterClass
    public static void clear() {
        List<Role> roles = api.list(-1);
        for (Role role : roles) {
            api.delete(role.id());
        }
    }

    @Override
    @After
    public void teardown() {
        clear();
    }

    @Test
    public void testCreate() {
        Role role1 = new Role();
        role1.name("role-beijing");
        role1.description("role for users in beijing");

        Role role2 = new Role();
        role2.name("role-shanghai");
        role2.description("role for users in shanghai");

        Role result1 = api.create(role1);
        Role result2 = api.create(role2);

        Assert.assertEquals("role-beijing", result1.name());
        Assert.assertEquals("role for users in beijing",
                result1.description());
        Assert.assertEquals("role-shanghai", result2.name());
        Assert.assertEquals("role for users in shanghai",
                result2.description());

        Assert.assertThrows(ServerException.class, () -> {
            api.create(role1);
        }, e -> {
            Assert.assertContains("The role name", e.getMessage());
            Assert.assertContains("has existed", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.create(new Role());
        }, e -> {
            Assert.assertContains("The name of role can't be null",
                    e.getMessage());
        });
    }

    @Test
    public void testGet() {
        Role role1 = createRole("test1", "description 1");
        Role role2 = createRole("test2", "description 2");

        Assert.assertEquals("description 1", role1.description());
        Assert.assertEquals("description 2", role2.description());

        role1 = api.get(role1.id());
        role2 = api.get(role2.id());

        Assert.assertEquals("test1", role1.name());
        Assert.assertEquals("description 1", role1.description());

        Assert.assertEquals("test2", role2.name());
        Assert.assertEquals("description 2", role2.description());
    }

    @Test
    public void testList() {
        createRole("test1", "description 1");
        createRole("test2", "description 2");
        createRole("test3", "description 3");

        List<Role> roles = api.list(-1);
        Assert.assertEquals(3, roles.size());

        roles.sort((t1, t2) -> t1.name().compareTo(t2.name()));
        Assert.assertEquals("test1", roles.get(0).name());
        Assert.assertEquals("test2", roles.get(1).name());
        Assert.assertEquals("test3", roles.get(2).name());
        Assert.assertEquals("description 1", roles.get(0).description());
        Assert.assertEquals("description 2", roles.get(1).description());
        Assert.assertEquals("description 3", roles.get(2).description());

        roles = api.list(1);
        Assert.assertEquals(1, roles.size());

        roles = api.list(2);
        Assert.assertEquals(2, roles.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });
    }

    @Test
    public void testUpdate() {
        Role role1 = createRole("test1", "description 1");
        Role role2 = createRole("test2", "description 2");

        Assert.assertEquals("description 1", role1.description());
        Assert.assertEquals("description 2", role2.description());

        role1.description("description updated");
        Role updated = api.update(role1);
        Assert.assertEquals("description updated", updated.description());

        Assert.assertThrows(ServerException.class, () -> {
            role2.name("test2-updated");
            api.update(role2);
        }, e -> {
            Assert.assertContains("is not existed",
                    e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Whitebox.setInternalState(role2, "id", "fake-id");
            Whitebox.setInternalState(role2, "name", "fake-name");
            api.update(role2);
        }, e -> {
            Assert.assertContains("role name 'fake-name' is not existed",
                    e.getMessage());
        });
    }

    @Test
    public void testDelete() {
        Role role1 = createRole("test1", "description 1");
        Role role2 = createRole("test2", "description 2");

        Assert.assertEquals(2, api.list(-1).size());
        api.delete(role1.id());

        Assert.assertEquals(1, api.list(-1).size());
        Assert.assertEquals(role2, api.list(-1).get(0));

        api.delete(role2.id());
        Assert.assertEquals(0, api.list(-1).size());

        Assert.assertThrows(ServerException.class, () -> {
            api.delete(role2.id());
        }, e -> {
            Assert.assertContains("not existed", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete("fake-id");
        }, e -> {
            Assert.assertContains("not existed",
                    e.getMessage());
        });
    }

    protected static Role createRole(String name, String description) {
        Role role = new Role();
        role.name(name);
        role.description(description);
        return api.create(role);
    }
}
