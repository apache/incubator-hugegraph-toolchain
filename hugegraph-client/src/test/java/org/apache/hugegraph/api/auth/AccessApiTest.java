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
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.HugePermission;
import org.apache.hugegraph.structure.auth.HugeResourceType;
import org.apache.hugegraph.structure.auth.Role;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AccessApiTest extends AuthApiTest {

    private static AccessAPI api;

    private static Target gremlin;
    private static Group group;

    @BeforeClass
    public static void init() {
        api = new AccessAPI(initClient(), DEFAULT_GRAPHSPACE);

        TargetApiTest.init();
        RoleApiTest.init();
    }

    @AfterClass
    public static void clear() {
        List<Access> accesss = api.list(null, null, -1);
        for (Access access : accesss) {
            api.delete(access.id());
        }

        TargetApiTest.clear();
        RoleApiTest.clear();
    }

    @Before
    @Override
    public void setup() {
        gremlin = TargetApiTest.createTarget("gremlin", HugeResourceType.GREMLIN);
        group = GroupApiTest.createGroup("group-beijing", "group for beijing");
    }

    @After
    @Override
    public void teardown() {
        clear();
    }

    @Test
    public void testCreate() {
        Target gremlin = TargetApiTest.createTarget("gremlinc",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("rolec", "role for beijing");

        Access access1 = new Access();
        access1.role(role);
        access1.target(gremlin);
        access1.permission(HugePermission.EXECUTE);
        access1.description("role beijing execute gremlin");

        Access access2 = new Access();
        access2.role(role);
        access2.target(gremlin);
        access2.permission(HugePermission.READ);
        access2.description("role beijing read gremlin");

        Access result1 = api.create(access1);
        Access result2 = api.create(access2);

        Assert.assertEquals(role.id(), result1.role());
        Assert.assertEquals(gremlin.id(), result1.target());
        Assert.assertEquals(HugePermission.EXECUTE, result1.permission());
        Assert.assertEquals("role beijing execute gremlin",
                            result1.description());

        Assert.assertEquals(role.id(), result2.role());
        Assert.assertEquals(gremlin.id(), result2.target());
        Assert.assertEquals(HugePermission.READ, result2.permission());
        Assert.assertEquals("role beijing read gremlin",
                            result2.description());

        Assert.assertThrows(ServerException.class, () -> {
            api.create(access1);
        }, e -> {
            Assert.assertContains("The access name", e.getMessage());
            Assert.assertContains("has existed", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Access access3 = new Access();
            access3.role(role);
            access3.target(gremlin);
            access3.permission(HugePermission.READ);
            access3.description("role beijing read gremlin");
            api.create(access3);
        }, e -> {
            Assert.assertContains("The access name", e.getMessage());
            Assert.assertContains("has existed", e.getMessage());
        });
    }

    @Test
    public void testGet() {
        Target gremlin = TargetApiTest.createTarget("gremling",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("roleg", "role for beijing");

        Access access1 = createAccess(role, gremlin, HugePermission.WRITE,
                                      "description 1");
        Access access2 = createAccess(role, gremlin, HugePermission.READ,
                                      "description 2");

        Assert.assertEquals("description 1", access1.description());
        Assert.assertEquals("description 2", access2.description());

        access1 = api.get(access1.id());
        access2 = api.get(access2.id());

        Assert.assertEquals(role.id(), access1.role());
        Assert.assertEquals(gremlin.id(), access1.target());
        Assert.assertEquals(HugePermission.WRITE, access1.permission());
        Assert.assertEquals("description 1", access1.description());

        Assert.assertEquals(role.id(), access2.role());
        Assert.assertEquals(gremlin.id(), access2.target());
        Assert.assertEquals(HugePermission.READ, access2.permission());
        Assert.assertEquals("description 2", access2.description());
    }

    @Test
    public void testList() {
        Target gremlin = TargetApiTest.createTarget("gremlinlist",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("rolelist", "role for beijing");

        createAccess(role, gremlin, HugePermission.READ, "description 1");
        createAccess(role, gremlin, HugePermission.WRITE, "description 2");
        createAccess(role, gremlin, HugePermission.EXECUTE, "description 3");

        List<Access> accesss = api.list(null, null, -1);
        Assert.assertEquals(3, accesss.size());

        accesss.sort((t1, t2) -> t1.permission().compareTo(t2.permission()));
        Assert.assertEquals("description 1", accesss.get(0).description());
        Assert.assertEquals("description 2", accesss.get(1).description());
        Assert.assertEquals("description 3", accesss.get(2).description());

        accesss = api.list(null, null, 1);
        Assert.assertEquals(1, accesss.size());

        accesss = api.list(null, null, 2);
        Assert.assertEquals(2, accesss.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(null, null, 0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });
    }

    @Test
    public void testListByRole() {
        Target gremlin = TargetApiTest.createTarget("gremlinlbg",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("rolelbg",
                                               "role for beijing");

        createAccess(role, gremlin, HugePermission.READ, "description 1");
        createAccess(role, gremlin, HugePermission.WRITE, "description 2");
        createAccess(role, gremlin, HugePermission.EXECUTE, "description 3");

        Role hk = RoleApiTest.createRole("role-hk", "role for hongkong");
        createAccess(hk, gremlin, HugePermission.READ, "description 4");
        createAccess(hk, gremlin, HugePermission.WRITE, "description 5");

        List<Access> accesss = api.list(null, null, -1);
        Assert.assertEquals(5, accesss.size());

        accesss = api.list(hk, null, -1);
        Assert.assertEquals(2, accesss.size());
        accesss.sort((t1, t2) -> t1.permission().compareTo(t2.permission()));
        Assert.assertEquals("description 4", accesss.get(0).description());
        Assert.assertEquals("description 5", accesss.get(1).description());

        accesss = api.list(role, null, -1);
        Assert.assertEquals(3, accesss.size());
        accesss.sort((t1, t2) -> t1.permission().compareTo(t2.permission()));
        Assert.assertEquals("description 1", accesss.get(0).description());
        Assert.assertEquals("description 2", accesss.get(1).description());
        Assert.assertEquals("description 3", accesss.get(2).description());

        accesss = api.list(role, null, 1);
        Assert.assertEquals(1, accesss.size());

        accesss = api.list(role, null, 2);
        Assert.assertEquals(2, accesss.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(role, null, 0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.list(role, gremlin, -1);
        }, e -> {
            Assert.assertContains("Can't pass both role and target " +
                                  "at the same time", e.getMessage());
        });
    }

    @Test
    public void testListByTarget() {
        Target gremlin = TargetApiTest.createTarget("gremlinlbg",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("rolelbg",
                                               "role for beijing");

        createAccess(role, gremlin, HugePermission.READ, "description 1");
        createAccess(role, gremlin, HugePermission.WRITE, "description 2");
        createAccess(role, gremlin, HugePermission.EXECUTE, "description 3");

        Role hk = RoleApiTest.createRole("role-hk", "role for hongkong");
        createAccess(hk, gremlin, HugePermission.READ, "description 4");
        createAccess(hk, gremlin, HugePermission.WRITE, "description 5");

        Target task = TargetApiTest.createTarget("task", HugeResourceType.TASK);
        createAccess(hk, task, HugePermission.READ, "description 6");
        createAccess(hk, task, HugePermission.WRITE, "description 7");

        List<Access> accesss = api.list(null, null, -1);
        Assert.assertEquals(7, accesss.size());

        accesss = api.list(null, task, -1);
        Assert.assertEquals(2, accesss.size());
        accesss.sort((t1, t2) -> t1.permission().compareTo(t2.permission()));
        Assert.assertEquals("description 6", accesss.get(0).description());
        Assert.assertEquals("description 7", accesss.get(1).description());

        accesss = api.list(null, gremlin, -1);
        Assert.assertEquals(5, accesss.size());
        accesss.sort((t1, t2) -> {
            String s1 = "" + t1.role() + t1.permission().ordinal();
            String s2 = "" + t2.role() + t2.permission().ordinal();
            return s1.compareTo(s2);
        });
        Assert.assertEquals("description 1", accesss.get(0).description());
        Assert.assertEquals("description 2", accesss.get(1).description());
        Assert.assertEquals("description 3", accesss.get(2).description());
        Assert.assertEquals("description 4", accesss.get(3).description());
        Assert.assertEquals("description 5", accesss.get(4).description());

        accesss = api.list(null, gremlin, 1);
        Assert.assertEquals(1, accesss.size());

        accesss = api.list(null, gremlin, 2);
        Assert.assertEquals(2, accesss.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(null, gremlin, 0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.list(hk, task, -1);
        }, e -> {
            Assert.assertContains("Can't pass both group and target " +
                                  "at the same time", e.getMessage());
        });
    }

    @Test
    public void testUpdate() {
        Target gremlin = TargetApiTest.createTarget("gremlinup",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("roleup", "role for beijing");

        Access access1 = createAccess(role, gremlin, HugePermission.WRITE,
                                      "description 1");
        Access access2 = createAccess(role, gremlin, HugePermission.READ,
                                      "description 2");

        Assert.assertEquals("description 1", access1.description());
        Assert.assertEquals("description 2", access2.description());

        access1.description("description updated");
        Access updated = api.update(access1);
        Assert.assertEquals("description updated", updated.description());
        Assert.assertNotEquals(access1.updateTime(), updated.updateTime());

        Assert.assertThrows(ServerException.class, () -> {
            Role hk = RoleApiTest.createRole("role-hk", "");
            access2.role(hk);
            api.update(access2);
        }, e -> {
            Assert.assertContains("The role of access can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Target task = TargetApiTest.createTarget("task",
                                                     HugeResourceType.TASK);
            access2.role(role);
            access2.target(task);
            api.update(access2);
        }, e -> {
            Assert.assertContains("The target of access can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Whitebox.setInternalState(access2, "id", "fake-id");
            api.update(access2);
        }, e -> {
            Assert.assertContains("Invalid access id: fake-id",
                                  e.getMessage());
        });
    }

    @Test
    public void testDelete() {
        Target gremlin = TargetApiTest.createTarget("gremlindel",
                                                    HugeResourceType.GREMLIN);
        Role role = RoleApiTest.createRole("roledel",
                                               "role for beijing");

        Access access1 = createAccess(role, gremlin, HugePermission.WRITE,
                                      "description 1");
        Access access2 = createAccess(role, gremlin, HugePermission.READ,
                                      "description 2");

        Assert.assertEquals(2, api.list(null, null, -1).size());
        api.delete(access1.id());

        Assert.assertEquals(1, api.list(null, null, -1).size());
        Assert.assertEquals(access2, api.list(null, null, -1).get(0));

        api.delete(access2.id());
        Assert.assertEquals(0, api.list(null, null, -1).size());

        Assert.assertThrows(ServerException.class, () -> {
            api.delete(access2.id());
        }, e -> {
            Assert.assertContains("Invalid access id:", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete("fake-id");
        }, e -> {
            Assert.assertContains("Invalid access id: fake-id",
                                  e.getMessage());
        });
    }

    private Access createAccess(Role role, Target target,
                                HugePermission perm, String description) {
        Access access = new Access();
        access.role(role);
        access.target(target);
        access.permission(perm);
        access.description(description);
        return api.create(access);
    }
}
