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
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BelongApiTest extends AuthApiTest {

    private static BelongAPI api;

    private static User user1;
    private static User user2;
    private static Group group1;
    private static Group group2;

    @BeforeClass
    public static void init() {
        api = new BelongAPI(initClient(), GRAPH);

        UserApiTest.init();
        GroupApiTest.init();
    }

    @AfterClass
    public static void clear() {
        List<Belong> belongs = api.list(null, null, -1);
        for (Belong belong : belongs) {
            api.delete(belong.id());
        }

        UserApiTest.clear();
        GroupApiTest.clear();
    }

    @Before
    @Override
    public void setup() {
        user1 = UserApiTest.createUser("user-1", "p1");
        user2 = UserApiTest.createUser("user-2", "p2");
        group1 = GroupApiTest.createGroup("group-1", "group 1");
        group2 = GroupApiTest.createGroup("group-2", "group 2");
    }

    @After
    @Override
    public void teardown() {
        clear();
    }

    @Test
    public void testCreate() {
        Belong belong1 = new Belong();
        belong1.user(user1);
        belong1.group(group1);
        belong1.description("user 1 => group 1");

        Belong belong2 = new Belong();
        belong2.user(user1);
        belong2.group(group2);
        belong2.description("user 1 => group 2");

        Belong belong3 = new Belong();
        belong3.user(user2);
        belong3.group(group2);
        belong3.description("user 2 => group 2");

        Belong result1 = api.create(belong1);
        Belong result2 = api.create(belong2);
        Belong result3 = api.create(belong3);

        Assert.assertEquals(user1.id(), result1.user());
        Assert.assertEquals(group1.id(), result1.group());
        Assert.assertEquals("user 1 => group 1", result1.description());

        Assert.assertEquals(user1.id(), result2.user());
        Assert.assertEquals(group2.id(), result2.group());
        Assert.assertEquals("user 1 => group 2", result2.description());

        Assert.assertEquals(user2.id(), result3.user());
        Assert.assertEquals(group2.id(), result3.group());
        Assert.assertEquals("user 2 => group 2", result3.description());

        Assert.assertThrows(ServerException.class, () -> {
            api.create(belong1);
        }, e -> {
            Assert.assertContains("Can't save belong", e.getMessage());
            Assert.assertContains("that already exists", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Belong belong4 = new Belong();
            belong4.user(user1);
            belong4.group(group1);
            api.create(belong3);
        }, e -> {
            Assert.assertContains("Can't save belong", e.getMessage());
            Assert.assertContains("that already exists", e.getMessage());
        });
    }

    @Test
    public void testGet() {
        Belong belong1 = createBelong(user1, group1, "user1 => group1");
        Belong belong2 = createBelong(user2, group2, "user2 => group2");

        Assert.assertEquals("user1 => group1", belong1.description());
        Assert.assertEquals("user2 => group2", belong2.description());

        belong1 = api.get(belong1.id());
        belong2 = api.get(belong2.id());

        Assert.assertEquals(user1.id(), belong1.user());
        Assert.assertEquals(group1.id(), belong1.group());
        Assert.assertEquals("user1 => group1", belong1.description());

        Assert.assertEquals(user2.id(), belong2.user());
        Assert.assertEquals(group2.id(), belong2.group());
        Assert.assertEquals("user2 => group2", belong2.description());
    }

    @Test
    public void testList() {
        createBelong(user1, group1, "user1 => group1");
        createBelong(user1, group2, "user1 => group2");
        createBelong(user2, group2, "user2 => group2");

        List<Belong> belongs = api.list(null, null, -1);
        Assert.assertEquals(3, belongs.size());

        belongs.sort((t1, t2) -> t1.description().compareTo(t2.description()));
        Assert.assertEquals("user1 => group1", belongs.get(0).description());
        Assert.assertEquals("user1 => group2", belongs.get(1).description());
        Assert.assertEquals("user2 => group2", belongs.get(2).description());

        belongs = api.list(null, null, 1);
        Assert.assertEquals(1, belongs.size());

        belongs = api.list(null, null, 2);
        Assert.assertEquals(2, belongs.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(null, null, 0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });
    }

    @Test
    public void testListByUser() {
        createBelong(user1, group1, "user1 => group1");
        createBelong(user1, group2, "user1 => group2");
        createBelong(user2, group2, "user2 => group2");

        List<Belong> belongs = api.list(null, null, -1);
        Assert.assertEquals(3, belongs.size());

        belongs = api.list(user2, null, -1);
        Assert.assertEquals(1, belongs.size());
        Assert.assertEquals("user2 => group2", belongs.get(0).description());

        belongs = api.list(user1, null, -1);
        Assert.assertEquals(2, belongs.size());

        belongs.sort((t1, t2) -> t1.description().compareTo(t2.description()));
        Assert.assertEquals("user1 => group1", belongs.get(0).description());
        Assert.assertEquals("user1 => group2", belongs.get(1).description());

        belongs = api.list(user1, null, 1);
        Assert.assertEquals(1, belongs.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(user1, null, 0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.list(user1, group1, -1);
        }, e -> {
            Assert.assertContains("Can't pass both user and group " +
                                  "at the same time", e.getMessage());
        });
    }

    @Test
    public void testListByGroup() {
        createBelong(user1, group1, "user1 => group1");
        createBelong(user1, group2, "user1 => group2");
        createBelong(user2, group2, "user2 => group2");

        List<Belong> belongs = api.list(null, null, -1);
        Assert.assertEquals(3, belongs.size());

        belongs = api.list(null, group1, -1);
        Assert.assertEquals(1, belongs.size());
        Assert.assertEquals("user1 => group1", belongs.get(0).description());

        belongs = api.list(null, group2, -1);
        Assert.assertEquals(2, belongs.size());

        belongs.sort((t1, t2) -> t1.description().compareTo(t2.description()));
        Assert.assertEquals("user1 => group2", belongs.get(0).description());
        Assert.assertEquals("user2 => group2", belongs.get(1).description());

        belongs = api.list(null, group2, 1);
        Assert.assertEquals(1, belongs.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(null, group2, 0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.list(user2, group2, -1);
        }, e -> {
            Assert.assertContains("Can't pass both user and group " +
                                  "at the same time", e.getMessage());
        });
    }

    @Test
    public void testUpdate() {
        Belong belong1 = createBelong(user1, group1, "user1 => group1");
        Belong belong2 = createBelong(user2, group2, "user2 => group2");

        Assert.assertEquals("user1 => group1", belong1.description());
        Assert.assertEquals("user2 => group2", belong2.description());

        belong1.description("description updated");
        Belong updated = api.update(belong1);
        Assert.assertEquals("description updated", updated.description());
        Assert.assertNotEquals(belong1.updateTime(), updated.updateTime());

        Assert.assertThrows(ServerException.class, () -> {
            belong2.user(user1);
            api.update(belong2);
        }, e -> {
            Assert.assertContains("The user of belong can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            belong2.user(user2);
            belong2.group(group1);
            api.update(belong2);
        }, e -> {
            Assert.assertContains("The group of belong can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Whitebox.setInternalState(belong2, "id", "fake-id");
            api.update(belong2);
        }, e -> {
            Assert.assertContains("Invalid belong id: fake-id",
                                  e.getMessage());
        });
    }

    @Test
    public void testDelete() {
        Belong belong1 = createBelong(user1, group1, "user1 => group1");
        Belong belong2 = createBelong(user2, group2, "user2 => group2");

        Assert.assertEquals(2, api.list(null, null, -1).size());
        api.delete(belong1.id());

        Assert.assertEquals(1, api.list(null, null, -1).size());
        Assert.assertEquals(belong2, api.list(null, null, -1).get(0));

        api.delete(belong2.id());
        Assert.assertEquals(0, api.list(null, null, -1).size());

        Assert.assertThrows(ServerException.class, () -> {
            api.delete(belong2.id());
        }, e -> {
            Assert.assertContains("Invalid belong id:", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete("fake-id");
        }, e -> {
            Assert.assertContains("Invalid belong id: fake-id",
                                  e.getMessage());
        });
    }

    private Belong createBelong(User user, Group group, String description) {
        Belong belong = new Belong();
        belong.user(user);
        belong.group(group);
        belong.description(description);
        return api.create(belong);
    }
}
