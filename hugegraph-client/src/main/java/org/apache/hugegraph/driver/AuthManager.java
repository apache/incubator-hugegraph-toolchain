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

package org.apache.hugegraph.driver;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import org.apache.hugegraph.api.auth.AccessAPI;
import org.apache.hugegraph.api.auth.BelongAPI;
import org.apache.hugegraph.api.auth.GroupAPI;
import org.apache.hugegraph.api.auth.LoginAPI;
import org.apache.hugegraph.api.auth.LogoutAPI;
import org.apache.hugegraph.api.auth.ProjectAPI;
import org.apache.hugegraph.api.auth.TargetAPI;
import org.apache.hugegraph.api.auth.TokenAPI;
import org.apache.hugegraph.api.auth.UserAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.Login;
import org.apache.hugegraph.structure.auth.LoginResult;
import org.apache.hugegraph.structure.auth.Project;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.structure.auth.TokenPayload;
import org.apache.hugegraph.structure.auth.User;

public class AuthManager {

    private final TargetAPI targetAPI;
    private final GroupAPI groupAPI;
    private final UserAPI userAPI;
    private final AccessAPI accessAPI;
    private final BelongAPI belongAPI;
    private final ProjectAPI projectAPI;
    private final LoginAPI loginAPI;
    private final LogoutAPI logoutAPI;
    private final TokenAPI tokenAPI;

    public AuthManager(RestClient client, String graph) {
        this.targetAPI = new TargetAPI(client, graph);
        this.groupAPI = new GroupAPI(client, graph);
        this.userAPI = new UserAPI(client, graph);
        this.accessAPI = new AccessAPI(client, graph);
        this.belongAPI = new BelongAPI(client, graph);
        this.projectAPI = new ProjectAPI(client, graph);
        this.loginAPI = new LoginAPI(client, graph);
        this.logoutAPI = new LogoutAPI(client, graph);
        this.tokenAPI = new TokenAPI(client, graph);
    }

    public List<Target> listTargets() {
        return this.listTargets(-1);
    }

    public List<Target> listTargets(int limit) {
        return this.targetAPI.list(limit);
    }

    public Target getTarget(Object id) {
        return this.targetAPI.get(id);
    }

    public Target createTarget(Target target) {
        return this.targetAPI.create(target);
    }

    public Target updateTarget(Target target) {
        return this.targetAPI.update(target);
    }

    public void deleteTarget(Object id) {
        this.targetAPI.delete(id);
    }

    public List<Group> listGroups() {
        return this.listGroups(-1);
    }

    public List<Group> listGroups(int limit) {
        return this.groupAPI.list(limit);
    }

    public Group getGroup(Object id) {
        return this.groupAPI.get(id);
    }

    public Group createGroup(Group group) {
        return this.groupAPI.create(group);
    }

    public Group updateGroup(Group group) {
        return this.groupAPI.update(group);
    }

    public void deleteGroup(Object id) {
        this.groupAPI.delete(id);
    }

    public List<User> listUsers() {
        return this.listUsers(-1);
    }

    public List<User> listUsers(int limit) {
        return this.userAPI.list(limit);
    }

    public User getUser(Object id) {
        return this.userAPI.get(id);
    }

    public User.UserRole getUserRole(Object id) {
        return this.userAPI.getUserRole(id);
    }

    public User createUser(User user) {
        return this.userAPI.create(user);
    }

    public User updateUser(User user) {
        return this.userAPI.update(user);
    }

    public void deleteUser(Object id) {
        this.userAPI.delete(id);
    }

    public List<Access> listAccesses() {
        return this.listAccesses(-1);
    }

    public List<Access> listAccesses(int limit) {
        return this.accessAPI.list(null, null, limit);
    }

    public List<Access> listAccessesByGroup(Object group, int limit) {
        return this.accessAPI.list(group, null, limit);
    }

    public List<Access> listAccessesByTarget(Object target, int limit) {
        return this.accessAPI.list(null, target, limit);
    }

    public Access getAccess(Object id) {
        return this.accessAPI.get(id);
    }

    public Access createAccess(Access access) {
        return this.accessAPI.create(access);
    }

    public Access updateAccess(Access access) {
        return this.accessAPI.update(access);
    }

    public void deleteAccess(Object id) {
        this.accessAPI.delete(id);
    }

    public List<Belong> listBelongs() {
        return this.listBelongs(-1);
    }

    public List<Belong> listBelongs(int limit) {
        return this.belongAPI.list(null, null, limit);
    }

    public List<Belong> listBelongsByUser(Object user, int limit) {
        return this.belongAPI.list(user, null, limit);
    }

    public List<Belong> listBelongsByGroup(Object group, int limit) {
        return this.belongAPI.list(null, group, limit);
    }

    public Belong getBelong(Object id) {
        return this.belongAPI.get(id);
    }

    public Belong createBelong(Belong belong) {
        return this.belongAPI.create(belong);
    }

    public Belong updateBelong(Belong belong) {
        return this.belongAPI.update(belong);
    }

    public void deleteBelong(Object id) {
        this.belongAPI.delete(id);
    }

    public void deleteAll() {
        for (Belong belong : this.listBelongs()) {
            this.deleteBelong(belong.id());
        }
        for (Access access : this.listAccesses()) {
            this.deleteAccess(access.id());
        }

        for (User user : this.listUsers()) {
            if (user.name().equals("admin")) {
                continue;
            }
            this.deleteUser(user.id());
        }
        for (Group group : this.listGroups()) {
            this.deleteGroup(group.id());
        }
        for (Target target : this.listTargets()) {
            this.deleteTarget(target.id());
        }
        for (Project project : this.listProjects()) {
            Set<String> graphs = project.graphs();
            if (CollectionUtils.isNotEmpty(graphs)) {
                this.projectRemoveGraphs(project.id(), graphs);
            }
            this.deleteProject(project.id());
        }
    }

    public Project createProject(Project project) {
        return this.projectAPI.create(project);
    }

    public Project getProject(Object id) {
        return this.projectAPI.get(id);
    }

    public List<Project> listProjects() {
        return this.listProject(-1);
    }

    public List<Project> listProject(int limit) {
        return this.projectAPI.list(limit);
    }

    public Project updateProject(Project project) {
        return this.projectAPI.update(project);
    }

    public Project projectAddGraphs(Object projectId, Set<String> graphs) {
        return this.projectAPI.addGraphs(projectId, graphs);
    }

    public Project projectRemoveGraphs(Object projectId, Set<String> graphs) {
        return this.projectAPI.removeGraphs(projectId, graphs);
    }

    public void deleteProject(Object id) {
        this.projectAPI.delete(id);
    }

    public LoginResult login(Login login) {
        return this.loginAPI.login(login);
    }

    public void logout() {
        this.logoutAPI.logout();
    }

    public TokenPayload verifyToken() {
        return this.tokenAPI.verifyToken();
    }
}
