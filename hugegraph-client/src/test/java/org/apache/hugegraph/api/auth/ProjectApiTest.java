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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.auth.Project;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class ProjectApiTest extends AuthApiTest {

    private static ProjectAPI api;

    @BeforeClass
    public static void init() {
        api = new ProjectAPI(initClient(), GRAPH);
    }

    @AfterClass
    public static void clear() {
        List<Project> projects = api.list(-1);
        for (Project project : projects) {
            Set<String> graphs = project.graphs();
            if (CollectionUtils.isNotEmpty(graphs)) {
                api.removeGraphs(project, graphs);
            }
            api.delete(project.id());
        }
    }

    @Override
    @After
    public void teardown() throws Exception {
        super.teardown();
        clear();
    }

    @Test
    public void testGet() {
        Project createdProject = createProject("project_test");
        Project fetchedProject = api.get(createdProject);
        Assert.assertEquals(createdProject, fetchedProject);
    }

    @Test
    public void testCreate() {
        Project paramsProject = new Project("project_test",
                                            "project_description");
        Project createdProject = api.create(paramsProject);
        Assert.assertEquals(paramsProject.name(), createdProject.name());
        Assert.assertEquals(paramsProject.description(),
                            createdProject.description());
    }

    @Test
    public void testList() {
        Project project1 = createProject("project_test1");
        Project project2 = createProject("project_test2");
        Project project3 = createProject("project_test3");
        List<Project> allProject = api.list(API.NO_LIMIT);
        Assert.assertTrue(allProject.contains(project1));
        Assert.assertTrue(allProject.contains(project2));
        Assert.assertTrue(allProject.contains(project3));
        List<Project> projects = api.list(1);
        Assert.assertEquals(1, projects.size());
        Project project = projects.get(0);
        Assert.assertTrue(StringUtils.isNotEmpty(project.adminGroup()));
        Assert.assertTrue(StringUtils.isNotEmpty(project.opGroup()));
        Assert.assertTrue(StringUtils.isNotEmpty(project.target()));
        Assert.assertTrue(StringUtils.isNotEmpty(project.creator()));
        Assert.assertNotNull(project.createTime());
        Assert.assertNotNull(project.updateTime());
    }

    @Test
    public void testDelete() {
        Project project = createProject("project_test");
        Assert.assertNotNull(project);
        Assert.assertEquals(project, api.get(project));
        api.delete(project);
        Assert.assertThrows(ServerException.class, () -> {
            api.get(project);
        }, e -> {
            Assert.assertTrue(e.getMessage().contains("Invalid project id"));
        });
    }

    @Test
    public void testAddGraph() {
        Project project = createProject("project_test");
        api.addGraphs(project, ImmutableSet.of("test_graph"));
        project = getProject(project);
        Assert.assertEquals(1, project.graphs().size());
        Assert.assertTrue(project.graphs().contains("test_graph"));
        api.addGraphs(project, ImmutableSet.of("test_graph1"));
        project = getProject(project);
        Assert.assertEquals(2, project.graphs().size());
        Assert.assertTrue(project.graphs().contains("test_graph1"));
    }

    @Test
    public void testRemoveGraph() {
        Set<String> graphs = ImmutableSet.of("test_graph1",
                                             "test_graph2",
                                             "test_graph3");
        Project project = createProject("project_test", graphs);
        graphs = new HashSet<>(graphs);
        Assert.assertTrue(graphs.containsAll(project.graphs()));
        project = api.removeGraphs(project, ImmutableSet.of("test_graph1"));
        graphs.remove("test_graph1");
        Assert.assertTrue(graphs.containsAll(project.graphs()));
        project = api.removeGraphs(project, ImmutableSet.of("test_graph2"));
        graphs.remove("test_graph2");
        Assert.assertTrue(graphs.containsAll(project.graphs()));
        project = api.removeGraphs(project, ImmutableSet.of("test_graph3"));
        graphs.remove("test_graph3");
        Assert.assertEquals(0, graphs.size());
        Assert.assertNull(project.graphs());
    }

    private static Project createProject(String name) {
        Project project = new Project(name);
        return api.create(project);
    }

    private static Project createProject(String name, Set<String> graphs) {
        Project project = new Project(name);
        project = api.create(project);
        project = api.addGraphs(project.id(), graphs);
        return project;
    }

    private static Project getProject(Object id) {
        return api.get(id);
    }
}
