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
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GroupApiTest extends AuthApiTest {

    private static GroupAPI api;

    @BeforeClass
    public static void init() {
        api = new GroupAPI(initClient(), GRAPH);
    }

    @AfterClass
    public static void clear() {
        List<Group> groups = api.list(-1);
        for (Group group : groups) {
            api.delete(group.id());
        }
    }

    @Override
    @After
    public void teardown() {
        clear();
    }

    @Test
    public void testCreate() {
        Group group1 = new Group();
        group1.name("group-beijing");
        group1.description("group for users in beijing");

        Group group2 = new Group();
        group2.name("group-shanghai");
        group2.description("group for users in shanghai");

        Group result1 = api.create(group1);
        Group result2 = api.create(group2);

        Assert.assertEquals("group-beijing", result1.name());
        Assert.assertEquals("group for users in beijing",
                            result1.description());
        Assert.assertEquals("group-shanghai", result2.name());
        Assert.assertEquals("group for users in shanghai",
                            result2.description());

        Assert.assertThrows(ServerException.class, () -> {
            api.create(group1);
        }, e -> {
            Assert.assertContains("Can't save group", e.getMessage());
            Assert.assertContains("that already exists", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.create(new Group());
        }, e -> {
            Assert.assertContains("The name of group can't be null",
                                  e.getMessage());
        });
    }

    @Test
    public void testGet() {
        Group group1 = createGroup("test1", "description 1");
        Group group2 = createGroup("test2", "description 2");

        Assert.assertEquals("description 1", group1.description());
        Assert.assertEquals("description 2", group2.description());

        group1 = api.get(group1.id());
        group2 = api.get(group2.id());

        Assert.assertEquals("test1", group1.name());
        Assert.assertEquals("description 1", group1.description());

        Assert.assertEquals("test2", group2.name());
        Assert.assertEquals("description 2", group2.description());
    }

    @Test
    public void testList() {
        createGroup("test1", "description 1");
        createGroup("test2", "description 2");
        createGroup("test3", "description 3");

        List<Group> groups = api.list(-1);
        Assert.assertEquals(3, groups.size());

        groups.sort((t1, t2) -> t1.name().compareTo(t2.name()));
        Assert.assertEquals("test1", groups.get(0).name());
        Assert.assertEquals("test2", groups.get(1).name());
        Assert.assertEquals("test3", groups.get(2).name());
        Assert.assertEquals("description 1", groups.get(0).description());
        Assert.assertEquals("description 2", groups.get(1).description());
        Assert.assertEquals("description 3", groups.get(2).description());

        groups = api.list(1);
        Assert.assertEquals(1, groups.size());

        groups = api.list(2);
        Assert.assertEquals(2, groups.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });
    }

    @Test
    public void testUpdate() {
        Group group1 = createGroup("test1", "description 1");
        Group group2 = createGroup("test2", "description 2");

        Assert.assertEquals("description 1", group1.description());
        Assert.assertEquals("description 2", group2.description());

        group1.description("description updated");
        Group updated = api.update(group1);
        Assert.assertEquals("description updated", updated.description());
        Assert.assertNotEquals(group1.updateTime(), updated.updateTime());

        Assert.assertThrows(ServerException.class, () -> {
            group2.name("test2-updated");
            api.update(group2);
        }, e -> {
            Assert.assertContains("The name of group can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Whitebox.setInternalState(group2, "id", "fake-id");
            api.update(group2);
        }, e -> {
            Assert.assertContains("Invalid group id: fake-id",
                                  e.getMessage());
        });
    }

    @Test
    public void testDelete() {
        Group group1 = createGroup("test1", "description 1");
        Group group2 = createGroup("test2", "description 2");

        Assert.assertEquals(2, api.list(-1).size());
        api.delete(group1.id());

        Assert.assertEquals(1, api.list(-1).size());
        Assert.assertEquals(group2, api.list(-1).get(0));

        api.delete(group2.id());
        Assert.assertEquals(0, api.list(-1).size());

        Assert.assertThrows(ServerException.class, () -> {
            api.delete(group2.id());
        }, e -> {
            Assert.assertContains("Invalid group id:", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete("fake-id");
        }, e -> {
            Assert.assertContains("Invalid group id: fake-id",
                                  e.getMessage());
        });
    }

    protected static Group createGroup(String name, String description) {
        Group group = new Group();
        group.name(name);
        group.description(description);
        return api.create(group);
    }
}
