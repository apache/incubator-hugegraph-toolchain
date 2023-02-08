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

import java.util.Arrays;
import java.util.List;

import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.auth.HugeResource;
import org.apache.hugegraph.structure.auth.HugeResourceType;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Whitebox;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TargetApiTest extends AuthApiTest {

    private static TargetAPI api;

    @BeforeClass
    public static void init() {
        api = new TargetAPI(initClient(), GRAPH);
    }

    @AfterClass
    public static void clear() {
        List<Target> targets = api.list(-1);
        for (Target target : targets) {
            api.delete(target.id());
        }
    }

    @Override
    @After
    public void teardown() {
        clear();
    }

    @Test
    public void testCreate() {
        Target target1 = new Target();
        target1.name("gremlin");
        target1.graph("hugegraph");
        target1.url("127.0.0.1:8080");
        HugeResource gremlin = new HugeResource(HugeResourceType.GREMLIN);
        target1.resources(gremlin);

        Target target2 = new Target();
        target2.name("task");
        target2.graph("hugegraph2");
        target2.url("127.0.0.1:8081");
        HugeResource task = new HugeResource(HugeResourceType.TASK);
        target2.resources(task);

        Target result1 = api.create(target1);
        Target result2 = api.create(target2);

        Assert.assertEquals("gremlin", result1.name());
        Assert.assertEquals("hugegraph", result1.graph());
        Assert.assertEquals("127.0.0.1:8080", result1.url());
        Assert.assertEquals(Arrays.asList(gremlin), result1.resources());

        Assert.assertEquals("task", result2.name());
        Assert.assertEquals("hugegraph2", result2.graph());
        Assert.assertEquals("127.0.0.1:8081", result2.url());
        Assert.assertEquals(Arrays.asList(task), result2.resources());

        Assert.assertThrows(ServerException.class, () -> {
            api.create(target1);
        }, e -> {
            Assert.assertContains("Can't save target", e.getMessage());
            Assert.assertContains("that already exists", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Target target3 = new Target();
            api.create(target3);
        }, e -> {
            Assert.assertContains("The name of target can't be null",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Target target3 = new Target();
            target3.name("test");
            api.create(target3);
        }, e -> {
            Assert.assertContains("The graph of target can't be null",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Target target3 = new Target();
            target3.name("test");
            target3.graph("hugegraph3");
            api.create(target3);
        }, e -> {
            Assert.assertContains("The url of target can't be null",
                                  e.getMessage());
        });
    }

    @Test
    public void testGet() {
        Target target1 = createTarget("test1", HugeResourceType.VERTEX);
        Target target2 = createTarget("test2", HugeResourceType.EDGE);

        Assert.assertEquals(HugeResourceType.VERTEX,
                            target1.resource().resourceType());
        Assert.assertEquals(HugeResourceType.EDGE,
                            target2.resource().resourceType());

        target1 = api.get(target1.id());
        target2 = api.get(target2.id());

        Assert.assertEquals("test1", target1.name());
        Assert.assertEquals(HugeResourceType.VERTEX,
                            target1.resource().resourceType());

        Assert.assertEquals("test2", target2.name());
        Assert.assertEquals(HugeResourceType.EDGE,
                            target2.resource().resourceType());
    }

    @Test
    public void testList() {
        createTarget("test1", HugeResourceType.VERTEX);
        createTarget("test2", HugeResourceType.EDGE);
        createTarget("test3", HugeResourceType.ALL);

        List<Target> targets = api.list(-1);
        Assert.assertEquals(3, targets.size());

        targets.sort((t1, t2) -> t1.name().compareTo(t2.name()));
        Assert.assertEquals("test1", targets.get(0).name());
        Assert.assertEquals("test2", targets.get(1).name());
        Assert.assertEquals("test3", targets.get(2).name());
        Assert.assertEquals(HugeResourceType.VERTEX,
                            targets.get(0).resource().resourceType());
        Assert.assertEquals(HugeResourceType.EDGE,
                            targets.get(1).resource().resourceType());
        Assert.assertEquals(HugeResourceType.ALL,
                            targets.get(2).resource().resourceType());

        targets = api.list(1);
        Assert.assertEquals(1, targets.size());

        targets = api.list(2);
        Assert.assertEquals(2, targets.size());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            api.list(0);
        }, e -> {
            Assert.assertContains("Limit must be > 0 or == -1", e.getMessage());
        });
    }

    @Test
    public void testUpdate() {
        Target target1 = createTarget("test1", HugeResourceType.VERTEX);
        Target target2 = createTarget("test2", HugeResourceType.EDGE);

        Assert.assertEquals(HugeResourceType.VERTEX,
                            target1.resource().resourceType());
        Assert.assertEquals(HugeResourceType.EDGE,
                            target2.resource().resourceType());

        target1.resources(new HugeResource(HugeResourceType.ALL));
        Target updated = api.update(target1);
        Assert.assertEquals(HugeResourceType.ALL,
                            updated.resource().resourceType());
        Assert.assertNotEquals(target1.updateTime(), updated.updateTime());

        Assert.assertThrows(ServerException.class, () -> {
            target2.name("test2-updated");
            api.update(target2);
        }, e -> {
            Assert.assertContains("The name of target can't be updated",
                                  e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            Whitebox.setInternalState(target2, "id", "fake-id");
            api.update(target2);
        }, e -> {
            Assert.assertContains("Invalid target id: fake-id",
                                  e.getMessage());
        });
    }

    @Test
    public void testDelete() {
        Target target1 = createTarget("test1", HugeResourceType.VERTEX);
        Target target2 = createTarget("test2", HugeResourceType.EDGE);

        Assert.assertEquals(2, api.list(-1).size());
        api.delete(target1.id());

        Assert.assertEquals(1, api.list(-1).size());
        Assert.assertEquals(target2, api.list(-1).get(0));

        api.delete(target2.id());
        Assert.assertEquals(0, api.list(-1).size());

        Assert.assertThrows(ServerException.class, () -> {
            api.delete(target2.id());
        }, e -> {
            Assert.assertContains("Invalid target id:", e.getMessage());
        });

        Assert.assertThrows(ServerException.class, () -> {
            api.delete("fake-id");
        }, e -> {
            Assert.assertContains("Invalid target id: fake-id",
                                  e.getMessage());
        });
    }

    protected static Target createTarget(String name, HugeResourceType res) {
        Target target = new Target();
        target.name(name);
        target.graph("hugegraph");
        target.url("127.0.0.1:8080");
        target.resources(new HugeResource(res));
        return api.create(target);
    }
}
