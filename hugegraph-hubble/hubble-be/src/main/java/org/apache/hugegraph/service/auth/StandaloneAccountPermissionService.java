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

package org.apache.hugegraph.service.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.HugePermission;
import org.apache.hugegraph.structure.auth.HugeResourceType;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.util.E;
import org.springframework.stereotype.Service;

@Service
public class StandaloneAccountPermissionService {

    private static final String ALL_GRAPHS = "*";
    private static final String ROLE_NAME = "hubble_standalone_read_write";
    private static final List<HugePermission> PERMISSIONS = Arrays.asList(
            HugePermission.READ, HugePermission.WRITE,
            HugePermission.DELETE, HugePermission.EXECUTE);

    public void assignReadWrite(HugeClient client, User user) {
        E.checkNotNull(user, "User");
        E.checkNotNull(user.id(), "User id");

        AuthManager auth = client.auth();
        List<Group> groups = auth.listGroups().stream()
                                 .filter(group -> ROLE_NAME.equals(group.name()))
                                 .collect(Collectors.toList());
        List<Target> targets = auth.listTargets().stream()
                                   .filter(target -> ROLE_NAME.equals(
                                           target.name()))
                                   .collect(Collectors.toList());
        E.checkState(groups.size() <= 1 && targets.size() <= 1,
                     "Conflicting standalone read-write role");
        E.checkState(groups.isEmpty() == targets.isEmpty(),
                     "Incomplete standalone read-write role");

        if (!groups.isEmpty()) {
            Group group = groups.get(0);
            Target target = targets.get(0);
            this.validateRole(auth, group, target);
            this.ensureBelong(auth, user, group);
            return;
        }

        Group group = null;
        Target target = null;
        List<Access> accesses = new ArrayList<>();
        try {
            group = this.createGroup(auth);
            target = this.createTarget(auth);
            for (HugePermission permission : PERMISSIONS) {
                Access access = new Access();
                access.group(group);
                access.target(target);
                access.permission(permission);
                accesses.add(auth.createAccess(access));
            }
            this.ensureBelong(auth, user, group);
        } catch (RuntimeException error) {
            this.rollbackRole(auth, accesses, target, group, error);
            throw error;
        }
    }

    private Group createGroup(AuthManager auth) {
        Group group = new Group();
        group.name(ROLE_NAME);
        return auth.createGroup(group);
    }

    public boolean hasReadWrite(HugeClient client, Object userId) {
        return this.readWriteUsers(client).contains(userId.toString());
    }

    public Set<String> readWriteUsers(HugeClient client) {
        AuthManager auth = client.auth();
        List<Group> groups = auth.listGroups().stream()
                                 .filter(group -> ROLE_NAME.equals(group.name()))
                                 .collect(Collectors.toList());
        List<Target> targets = auth.listTargets().stream()
                                   .filter(target -> ROLE_NAME.equals(
                                           target.name()))
                                   .collect(Collectors.toList());
        if (groups.size() != 1 || targets.size() != 1) {
            return Collections.emptySet();
        }
        try {
            this.validateRole(auth, groups.get(0), targets.get(0));
        } catch (IllegalStateException ignored) {
            return Collections.emptySet();
        }
        Object groupId = groups.get(0).id();
        return auth.listBelongsByGroup(groupId, -1).stream()
                   .map(belong -> belong.user().toString())
                   .collect(Collectors.toSet());
    }

    private Target createTarget(AuthManager auth) {
        Target target = new Target();
        target.name(ROLE_NAME);
        target.graph(ALL_GRAPHS);
        target.url("");
        Map<String, Object> resource = new HashMap<>();
        resource.put("type", HugeResourceType.ALL.toString());
        resource.put("label", "*");
        resource.put("properties", null);
        target.resources(Collections.singletonList(resource));
        return auth.createTarget(target);
    }

    private void validateRole(AuthManager auth, Group group, Target target) {
        E.checkState(group.id() != null && target.id() != null &&
                     ALL_GRAPHS.equals(target.graph()),
                     "Conflicting standalone read-write role");
        List<Map<String, Object>> resources = target.resourcesList();
        E.checkState(resources != null && resources.size() == 1,
                     "Conflicting standalone read-write target");
        Map<String, Object> resource = resources.get(0);
        E.checkState(HugeResourceType.ALL.toString().equals(
                             resource.get("type")) &&
                     "*".equals(resource.get("label")) &&
                     resource.get("properties") == null,
                     "Conflicting standalone read-write target");

        List<Access> accesses = auth.listAccessesByGroup(group.id(), -1);
        Set<HugePermission> permissions = new HashSet<>();
        for (Access access : accesses) {
            E.checkState(target.id().equals(access.target()),
                         "Conflicting standalone read-write access");
            permissions.add(access.permission());
        }
        E.checkState(accesses.size() == PERMISSIONS.size() &&
                     permissions.equals(new HashSet<>(PERMISSIONS)),
                     "Conflicting standalone read-write access");
    }

    private void ensureBelong(AuthManager auth, User user, Group group) {
        if (auth.listBelongsByUser(user.id(), -1).stream()
                .anyMatch(belong -> group.id().equals(belong.group()))) {
            return;
        }
        Belong belong = new Belong();
        belong.user(user);
        belong.group(group);
        auth.createBelong(belong);
    }

    private void rollbackRole(AuthManager auth, List<Access> accesses,
                              Target target, Group group,
                              RuntimeException failure) {
        for (Access access : accesses) {
            if (access != null && access.id() != null) {
                this.suppressRollback(() -> auth.deleteAccess(access.id()),
                                      failure);
            }
        }
        if (target != null && target.id() != null) {
            this.suppressRollback(() -> auth.deleteTarget(target.id()),
                                  failure);
        }
        if (group != null && group.id() != null) {
            this.suppressRollback(() -> auth.deleteGroup(group.id()), failure);
        }
    }

    private void suppressRollback(Runnable rollback, RuntimeException failure) {
        try {
            rollback.run();
        } catch (RuntimeException error) {
            failure.addSuppressed(error);
        }
    }
}
