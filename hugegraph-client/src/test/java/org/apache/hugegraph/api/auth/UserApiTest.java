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
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.structure.auth.User.UserRole;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserApiTest extends AuthApiTest {

    private static UserAPI api;

    @BeforeClass
    public static void init() {
        api = new UserAPI(initClient(), GRAPH);
    }

    @AfterClass
    public static void clear() {
        List<User> users = api.list(-1);
        for (User user : users) {
            if (user.name().equals("admin")) {
                continue;
            }
            api.delete(user.id());
        }
    }

    @Override
    @After
    public void teardown() {
        clear();
    }

    @Test
    public void testCreate() {
        User user1 = new User();
        user1.name("user1");
        user1.password("p1");
        user1.email("user1@hugegraph.com");
        user1.phone("123456789");
        user1.avatar("image1.jpg");

        User user2 = new User();
        user2.name("user2");
        user2.password("p2");
        user2.email("user2@hugegraph.com");
        user2.phone("1357924680");
        user2.avatar("image2.jpg");

        User result1 = api.create(user1);
        User result2 = api.create(user2);

        Assert.assertEquals("user1", result1.name());
        Assert.assertNotEquals("p1", result1.password());
        Assert.assertEquals("user1@hugegraph.com", result1.email());
        Assert.assertEquals("123456789", result1.phone());
        Assert.assertEquals("image1.jpg", result1.avatar());

        Assert.assertEquals("user2", result2.name());
        Assert.assertNotEquals("p2", result2.password());
        Assert.assertEquals("user2@hugegraph.com", result2.email());
        Assert.assertEquals("1357924680", result2.phone());
        Assert.assertEquals("image2.jpg", result2.avatar());

        Assert.assertThrows(ServerException.class, () -> {
            api.create(new User());
        }, e -> {
            Assert.assertContains("The name of user can't be null",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            User user3 = new User();
            user3.name("test");
            api.create(user3);
        }, e -> {
            Assert.assertContains("The password of user can't be null",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.create(user1);
        }, e -> {
            Assert.assertContains("Can't save user", e.getMessage());
            Assert.assertContains("that already exists", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            user1.name("admin");
            api.create(user1);
        }, e -> {
            Assert.assertContains("Invalid user name 'admin'", e.getMessage());
        });
    }

    @Test
    public void testGet() {
        User user1 = createUser("test1", "psw1");
        User user2 = createUser("test2", "psw2");

        Assert.assertEquals("test1", user1.name());
        Assert.assertEquals("test2", user2.name());

        user1 = api.get(user1.id());
        user2 = api.get(user2.id());

        Assert.assertEquals("test1", user1.name());
        Assert.assertEquals("test2", user2.name());
    }

    @Test
    public void testGetUserRole() {
        User user1 = createUser("test1", "psw1");
        User user2 = createUser("test2", "psw2");

        Assert.assertEquals("test1", user1.name());
        Assert.assertEquals("test2", user2.name());

        UserRole role1 = api.getUserRole(user1.id());
        UserRole role2 = api.getUserRole(user2.id());

        Assert.assertEquals("{\"roles\":{}}", role1.toString());
        Assert.assertEquals("{\"roles\":{}}", role2.toString());
    }

    @Test
    public void testList() {
        createUser("test1", "psw1");
        createUser("test2", "psw2");
        createUser("test3", "psw3");

        List<User> users = api.list(-1);
        Assert.assertEquals(4, users.size());

        users.sort((t1, t2) -> t1.name().compareTo(t2.name()));
        Assert.assertEquals("admin", users.get(0).name());
        Assert.assertEquals("test1", users.get(1).name());
        Assert.assertEquals("test2", users.get(2).name());
        Assert.assertEquals("test3", users.get(3).name());

        users = api.list(1);
        Assert.assertEquals(1, users.size());

        users = api.list(2);
        Assert.assertEquals(2, users.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });
    }

    @Test
    public void testUpdate() {
        User user1 = createUser("test1", "psw1");
        User user2 = createUser("test2", "psw2");

        Assert.assertEquals("test@hugegraph.com", user1.email());
        Assert.assertEquals("16812345678", user1.phone());
        Assert.assertEquals("image.jpg", user1.avatar());

        String oldPassw = user1.password();
        Assert.assertNotEquals("psw1", oldPassw);

        user1.password("psw-udated");
        user1.email("test_updated@hugegraph.com");
        user1.phone("1357924680");
        user1.avatar("image-updated.jpg");

        User updated = api.update(user1);
        Assert.assertNotEquals(oldPassw, updated.password());
        Assert.assertEquals("test_updated@hugegraph.com", updated.email());
        Assert.assertEquals("1357924680", updated.phone());
        Assert.assertEquals("image-updated.jpg", updated.avatar());
        Assert.assertNotEquals(user1.updateTime(), updated.updateTime());

        Assert.assertThrows(ServerException.class, () -> {
            user2.name("test2-updated");
            api.update(user2);
        }, e -> {
            Assert.assertContains("The name of user can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Whitebox.setInternalState(user2, "id", "fake-id");
            api.update(user2);
        }, e -> {
            Assert.assertContains("Invalid user id: fake-id",
                                  e.getMessage());
        });
    }

    @Test
    public void testDelete() {
        User user1 = createUser("test1", "psw1");
        User user2 = createUser("test2", "psw2");

        List<User> users = api.list(-1);
        Assert.assertEquals(3, users.size());
        Assert.assertTrue(users.contains(user1));
        Assert.assertTrue(users.contains(user2));
        api.delete(user1.id());

        users = api.list(-1);
        Assert.assertEquals(2, users.size());
        Assert.assertFalse(users.contains(user1));
        Assert.assertTrue(users.contains(user2));

        api.delete(user2.id());
        users = api.list(-1);
        Assert.assertEquals(1, users.size());
        Assert.assertEquals("admin", users.get(0).name());

        User admin = users.get(0);
        Assert.assertThrows(ServerException.class, () -> {
            api.delete(admin.id());
        }, e -> {
            Assert.assertContains("Can't delete user 'admin'", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete(user2.id());
        }, e -> {
            Assert.assertContains("Invalid user id:", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete("fake-id");
        }, e -> {
            Assert.assertContains("Invalid user id: fake-id", e.getMessage());
        });
    }

    protected static User createUser(String name, String password) {
        User user = new User();
        user.name(name);
        user.password(password);
        user.email("test@hugegraph.com");
        user.phone("16812345678");
        user.avatar("image.jpg");
        return api.create(user);
    }
}
