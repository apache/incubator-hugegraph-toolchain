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

package org.apache.hugegraph.functional;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.HugePermission;
import org.apache.hugegraph.structure.auth.HugeResource;
import org.apache.hugegraph.structure.auth.HugeResourceType;
import org.apache.hugegraph.structure.auth.Login;
import org.apache.hugegraph.structure.auth.LoginResult;
import org.apache.hugegraph.structure.auth.Project;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.structure.auth.TokenPayload;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.structure.auth.User.UserRole;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

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

        Project project1 = new Project("test");
        project1 = auth().createProject(project1);
        Assert.assertEquals("test", project1.name());

        Project project2 = new Project("test2");
        project2 = auth().createProject(project2);
        Assert.assertEquals("test2", project2.name());

        Project newProject1 = auth().getProject(project1);
        Assert.assertEquals(newProject1.id(), project1.id());
        Assert.assertTrue(CollectionUtils.isEmpty(newProject1.graphs()));

        List<Project> projects = auth().listProjects();
        Assert.assertNotNull(projects);
        Assert.assertEquals(2, projects.size());

        Set<String> graphs = ImmutableSet.of("graph1", "graph2");
        newProject1 = auth().projectAddGraphs(project1, graphs);
        Assert.assertNotNull(newProject1);
        Assert.assertEquals(graphs, newProject1.graphs());

        graphs = ImmutableSet.of("graph2");
        newProject1 = auth().projectRemoveGraphs(project1,
                                                 ImmutableSet.of("graph1"));
        Assert.assertNotNull(newProject1);
        Assert.assertEquals(graphs, newProject1.graphs());

        Object project1Id = project1.id();
        project1 = new Project(project1Id);
        project1.description("test description");
        newProject1 = auth().updateProject(project1);
        Assert.assertEquals(newProject1.description(), project1.description());

        auth().deleteProject(project2);
        projects.remove(project2);
        List<Project> newProjects = auth().listProjects();
        Assert.assertEquals(newProjects, projects);

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
