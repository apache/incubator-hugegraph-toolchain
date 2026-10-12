/*
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

package org.apache.hugegraph.service.auth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.BelongEntity;
import org.apache.hugegraph.entity.auth.RoleEntity;
import org.apache.hugegraph.entity.auth.UserView;
import org.apache.hugegraph.exception.ParameterizedException;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class GraphSpaceUserService extends AuthService {

    @Autowired
    private BelongService belongService;

    public List<UserView> listUsers(HugeClient client, String graphSpace) {
        Map<String, UserView> users = new java.util.LinkedHashMap<>();
        List<BelongEntity> belongs = this.belongService.list(
                client, graphSpace, null, null);

        Multimap<String, BelongEntity> grouped = ArrayListMultimap.create();
        belongs.forEach(belong -> {
            grouped.put(belong.getUserId(), belong);
        });

        grouped.keySet().forEach(userId -> {
            UserView user = new UserView(null, null, new ArrayList<>());
            grouped.get(userId).forEach(belong -> {
                user.setId(belong.getUserId());
                user.setName(belong.getUserName());
                user.addRole(new RoleEntity(belong.getRoleId(),
                                            belong.getRoleName()));
            });
            users.put(userId, user);
        });
        client.auth().listSpaceMember(graphSpace).forEach(username -> {
            User account = client.findUserByName(username);
            if (account == null) {
                return;
            }
            String userId = account.id().toString();
            UserView user = users.computeIfAbsent(userId,
                    id -> new UserView(id, username, new ArrayList<>()));
            if (client.supportsDefaultRole()) {
                this.addDefaultRole(client, graphSpace, user, username, "observer");
                this.addDefaultRole(client, graphSpace, user, username, "analyst");
            }
        });
        return new ArrayList<>(users.values());
    }

    public UserView getUser(HugeClient client, String graphSpace,
                            String userId) {
        User account = client.auth().getUser(userId);
        return this.buildUserView(client, graphSpace, userId, account);
    }

    public UserView getUserByAccountId(HugeClient client, String graphSpace,
                                       String accountId) {
        User account = this.resolveAccountId(client, accountId);
        String userId = account.id().toString();
        return this.buildUserView(client, graphSpace, userId, account);
    }

    private UserView buildUserView(HugeClient client, String graphSpace,
                                   String userId, User account) {
        List<BelongEntity> belongs = this.belongService.list(
                client, graphSpace, null, userId);
        UserView user = new UserView(null, null,
                                     new ArrayList<>(belongs.size()));
        belongs.forEach(belong -> {
            user.setId(belong.getUserId());
            user.setName(belong.getUserName());
            user.addRole(new RoleEntity(belong.getRoleId(),
                                        belong.getRoleName()));
        });
        if (account != null) {
            if (user.getId() == null) {
                user.setId(userId);
                user.setName(account.name());
            }
            if (client.supportsDefaultRole()) {
                this.addDefaultRole(client, graphSpace, user,
                                    account.name(), "observer");
                this.addDefaultRole(client, graphSpace, user,
                                    account.name(), "analyst");
            }
        }
        return user;
    }

    private User resolveAccountId(HugeClient client, String accountId) {
        try {
            // HugeGraph's "name" is the unique account ID; nickname may repeat.
            User account = client.findUserByName(accountId);
            if (account != null) {
                return account;
            }
        } catch (RuntimeException e) {
            if (!missingAccount(e)) {
                throw e;
            }
        }
        throw new ParameterizedException("auth.account.not-exist", accountId);
    }

    private static boolean missingAccount(RuntimeException error) {
        String detail = error.getMessage();
        return detail != null &&
               detail.toLowerCase().contains("user") &&
               detail.toLowerCase().contains("not exist");
    }

    public IPage<UserView> queryPage(HugeClient client, String graphSpace,
                                     String query, int pageNo, int pageSize) {
        List<UserView> results =
                this.listUsers(client, graphSpace).stream()
                    .filter(user -> user.getName().contains(query))
                    .sorted(Comparator.comparing(UserView::getName))
                    .collect(Collectors.toList());
        return PageUtil.page(results, pageNo, pageSize);
    }

    public UserView createOrUpdate(HugeClient client, String graphSpace,
                                   UserView userView) {
        requireLegacyRoleAssignments(client);
        E.checkNotNull(userView.getId(), "User Id Not Null");
        E.checkArgument(userView.getRoles() != null &&
                        !userView.getRoles().isEmpty(),
                        "The role info is empty");

        Set<String> newRoles =
                userView.getRoles().stream()
                        .map(RoleEntity::getId)
                        .collect(Collectors.toSet());
        newRoles.forEach(roleId -> RoleService.requireScopedGroup(
                client.auth(), graphSpace, roleId));
        User account = client.auth().getUser(userView.getId());
        E.checkNotNull(account, "User");
        String username = account.name();
        E.checkArgument(username != null && !username.isEmpty(),
                        "The user name is empty");
        List<BelongEntity> current = this.belongService.list(
                client, graphSpace, null, userView.getId());
        if (!client.auth().listSpaceMember(graphSpace).contains(username)) {
            client.auth().addSpaceMember(username, graphSpace);
        }
        current.forEach(belong -> {
            if (!newRoles.contains(belong.getRoleId())) {
                this.belongService.deleteById(client, graphSpace,
                                              belong.getId());
            }
        });
        Set<String> currentRoles = current.stream()
                                          .map(BelongEntity::getRoleId)
                                          .collect(Collectors.toSet());

        userView.getRoles().forEach(role -> {
            if (!currentRoles.contains(role.getId())) {
                this.belongService.add(client, graphSpace, role.getId(),
                                       userView.getId());
            }
        });
        return this.getUser(client, graphSpace, userView.getId());
    }

    public void applyPermissionPresets(HugeClient client, String username,
                                       List<Map<String, String>> permissions,
                                       String preset) {
        this.applyPermissionPresets(client, username, permissions, preset,
                                    true);
    }

    public void applyPermissionPresetsForNewAccount(
            HugeClient client, String username,
            List<Map<String, String>> permissions, String preset) {
        this.applyPermissionPresets(client, username, permissions, preset,
                                    false);
    }

    private void applyPermissionPresets(HugeClient client, String username,
                                        List<Map<String, String>> permissions,
                                        String preset,
                                        boolean reconcileUnrequestedSpaces) {
        if (preset == null || "SUPER_ADMIN".equals(preset)) {
            return;
        }
        requirePermissionPresets(client);
        User account = client.findUserByName(username);
        if (account == null) {
            return;
        }
        Map<String, String> desired = new java.util.LinkedHashMap<>();
        List<Map<String, String>> requested = permissions == null ? new ArrayList<>() : permissions;
        for (Map<String, String> permission : requested) {
            String graphSpace = permission.get("graphspace");
            String permissionPreset = permission.get("permission_preset");
            if (graphSpace != null) {
                desired.put(graphSpace, permissionPreset);
            }
        }
        if (!reconcileUnrequestedSpaces) {
            desired.forEach((graphSpace, desiredPreset) ->
                    this.applySpacePreset(client, graphSpace,
                                          account.id().toString(),
                                          account.name(),
                                          desiredPreset));
            return;
        }
        List<String> graphSpaces = client.graphSpace().listGraphSpace();
        Map<String, SpacePresetState> previous =
                new java.util.LinkedHashMap<>();
        String userId = account.id().toString();
        String accountName = account.name();
        for (String graphSpace : graphSpaces) {
            boolean member = client.auth().listSpaceMember(graphSpace)
                                   .contains(accountName);
            previous.put(graphSpace, this.capturePresetState(
                    client, graphSpace, userId, accountName, member));
        }
        try {
            for (String graphSpace : graphSpaces) {
                String desiredPreset = desired.get(graphSpace);
                if (desiredPreset == null) {
                    this.unauthUser(client, graphSpace, userId);
                } else {
                    this.applySpacePreset(client, graphSpace, userId,
                                          accountName,
                                          desiredPreset);
                }
            }
        } catch (RuntimeException error) {
            for (int i = graphSpaces.size() - 1; i >= 0; i--) {
                String graphSpace = graphSpaces.get(i);
                this.restorePresetState(client, graphSpace, userId,
                                        accountName, previous.get(graphSpace),
                                        error);
            }
            throw error;
        }
    }

    public void validatePermissionPresets(
            HugeClient client, List<Map<String, String>> permissions,
            String preset) {
        if (preset == null || "SUPER_ADMIN".equals(preset)) {
            return;
        }
        requirePermissionPresets(client);
        requirePermissionPreset(preset);
        if (permissions == null || permissions.isEmpty()) {
            throw new ParameterizedException(
                      "auth.permission-preset.graphspace-required");
        }
        Set<String> graphSpaces =
                new java.util.HashSet<>(client.graphSpace().listGraphSpace());
        for (Map<String, String> permission :
                permissions == null ? new ArrayList<Map<String, String>>() : permissions) {
            if (permission == null) {
                throw new ParameterizedException(
                          "auth.permission-preset.entry-invalid");
            }
            String graphSpace = permission.get("graphspace");
            String permissionPreset = permission.get("permission_preset");
            if (graphSpace == null || !graphSpaces.contains(graphSpace)) {
                throw new ParameterizedException(
                          "auth.permission-preset.graphspace-not-found",
                          graphSpace);
            }
            requirePermissionPreset(permissionPreset);
            if (!preset.equals(permissionPreset)) {
                throw new ParameterizedException(
                          "auth.permission-preset.mismatch",
                          permissionPreset, preset);
            }
        }
    }

    public void applySpacePreset(HugeClient client, String graphSpace, String userId, String username, String preset) {
        requirePermissionPreset(preset);
        requirePermissionPresets(client);
        E.checkArgument(username != null && !username.isEmpty(), "The account name can't be empty");
        E.checkArgument(!client.auth().listSuperAdmin().contains(username),
                        "Can't assign GraphSpace preset to super " +
                        "administrator '%s'", username);
        boolean wasMember =
                client.auth().listSpaceMember(graphSpace).contains(username);
        String resolvedUserId = userId;
        SpacePresetState previous = null;
        try {
            if (!wasMember) {
                try {
                    client.auth().addSpaceMember(username, graphSpace);
                } catch (RuntimeException e) {
                    String detail = e.getMessage();
                    if (detail != null &&
                        detail.toLowerCase()
                              .contains("user or group is not exist")) {
                        throw new ParameterizedException(
                                  "auth.account.not-exist", username);
                    }
                    throw e;
                }
            }
            User account = userId == null ?
                           client.findUserByName(username) :
                           client.auth().getUser(userId);
            E.checkNotNull(account, "User");
            E.checkArgument(username.equals(account.name()),
                            "Account id '%s' belongs to '%s', not '%s'",
                            userId, account.name(), username);
            resolvedUserId = account.id().toString();
            previous = this.capturePresetState(client, graphSpace, resolvedUserId, username, wasMember);
            previous.customRoles.forEach(
                    belong -> this.belongService.deleteById(
                            client, graphSpace, belong.getId()));
            if (previous.analyst) {
                client.graphSpace().deleteDefaultRole(
                        graphSpace, username, "analyst");
            }
            if (previous.observer) {
                client.graphSpace().deleteDefaultRole(
                        graphSpace, username, "observer");
            }
            if ("GS_ADMIN".equals(preset)) {
                if (!previous.admin) {
                    client.auth().addSpaceAdmin(username, graphSpace);
                }
                this.setDefaultRole(client, graphSpace, username, "analyst");
                return;
            }
            if (previous.admin) {
                client.auth().delSpaceAdmin(username, graphSpace);
            }
            String role = "GS_READ_ONLY".equals(preset) ?
                          "observer" : "analyst";
            this.setDefaultRole(client, graphSpace, username, role);
        } catch (RuntimeException e) {
            if (previous == null) {
                this.rollbackNewMember(client, graphSpace, username,
                                       wasMember, e);
            } else {
                this.restorePresetState(client, graphSpace, resolvedUserId, username, previous, e);
            }
            throw e;
        }
    }

    public void removeSpacePreset(HugeClient client, String graphSpace,
                                  String userId) {
        this.unauthUser(client, graphSpace, userId);
    }

    private static void requirePermissionPresets(HugeClient client) {
        if (!client.supportsDefaultRole()) {
            throw new ParameterizedException(
                      "auth.permission-preset.unsupported");
        }
    }

    private SpacePresetState capturePresetState(HugeClient client,
                                                String graphSpace,
                                                String userId,
                                                String username,
                                                boolean member) {
        List<BelongEntity> customRoles = this.belongService.list(
                client, graphSpace, null, userId);
        boolean analyst = client.graphSpace().checkDefaultRole(
                graphSpace, username, "analyst");
        boolean observer = client.graphSpace().checkDefaultRole(
                graphSpace, username, "observer");
        boolean admin = client.auth().listSpaceAdmin(graphSpace)
                              .contains(username);
        return new SpacePresetState(customRoles, member, admin,
                                    analyst, observer);
    }

    private void restorePresetState(HugeClient client, String graphSpace,
                                    String userId, String username,
                                    SpacePresetState previous,
                                    RuntimeException failure) {
        if (previous.member) {
            this.rollbackNewMember(client, graphSpace, username, true, failure);
        }
        previous.customRoles.forEach(belong -> {
            this.tryRestore(() -> {
                Set<String> currentRoles = this.belongService.list(
                        client, graphSpace, null, userId).stream()
                        .map(BelongEntity::getRoleId)
                        .collect(Collectors.toSet());
                if (!currentRoles.contains(belong.getRoleId())) {
                    this.belongService.add(client, graphSpace,
                                           belong.getRoleId(), userId);
                }
            }, graphSpace, userId,
                            "custom role " + belong.getRoleId(), failure);
        });
        this.tryRestore(() -> this.restoreDefaultRole(
                client, graphSpace, username, "analyst", previous.analyst),
                        graphSpace, userId, "analyst role", failure);
        this.tryRestore(() -> this.restoreDefaultRole(
                client, graphSpace, username, "observer", previous.observer),
                        graphSpace, userId, "observer role", failure);
        this.tryRestore(() -> {
            boolean current = client.auth().listSpaceAdmin(graphSpace)
                                    .contains(username);
            if (previous.admin && !current) {
                client.auth().addSpaceAdmin(username, graphSpace);
            } else if (!previous.admin && current) {
                client.auth().delSpaceAdmin(username, graphSpace);
            }
        }, graphSpace, userId, "administrator", failure);
        if (!previous.member) {
            this.rollbackNewMember(client, graphSpace, username, false, failure);
        }
    }

    private void restoreDefaultRole(HugeClient client, String graphSpace,
                                    String username, String role,
                                    boolean expected) {
        boolean current = client.graphSpace().checkDefaultRole(
                graphSpace, username, role);
        if (expected && !current) {
            client.graphSpace().setDefaultRole(graphSpace, username, role);
        } else if (!expected && current) {
            client.graphSpace().deleteDefaultRole(graphSpace, username, role);
        }
    }

    private void rollbackNewMember(HugeClient client, String graphSpace,
                                   String username, boolean expected,
                                   RuntimeException failure) {
        this.tryRestore(() -> {
            boolean current = client.auth().listSpaceMember(graphSpace)
                                    .contains(username);
            if (expected && !current) {
                client.auth().addSpaceMember(username, graphSpace);
            } else if (!expected && current) {
                client.auth().delSpaceMember(username, graphSpace);
            }
        }, graphSpace, username, "membership", failure);
    }

    private void tryRestore(Runnable action, String graphSpace,
                            String userId, String state,
                            RuntimeException failure) {
        try {
            action.run();
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
            log.warn("Failed to restore GraphSpace {} for '{}' in '{}'",
                     state, userId, graphSpace, rollbackFailure);
        }
    }

    private static void requireLegacyRoleAssignments(HugeClient client) {
        if (client.supportsDefaultRole()) {
            throw new ParameterizedException(
                      "auth.permission-preset.required");
        }
    }

    private static void requirePermissionPreset(String preset) {
        if (!isGraphSpacePreset(preset)) {
            throw new ParameterizedException(
                      "auth.permission-preset.invalid", preset);
        }
    }

    private static boolean isGraphSpacePreset(String preset) {
        return "GS_READ_ONLY".equals(preset) ||
               "GS_READ_WRITE".equals(preset) ||
               "GS_ADMIN".equals(preset);
    }

    public boolean hasCustomRoles(HugeClient client, String graphSpace,
                                  String userId) {
        return !this.belongService.list(client, graphSpace, null, userId).isEmpty();
    }

    public boolean hasGraphSpaceAccess(HugeClient client, String graphSpace,
                                       String username) {
        if (!client.supportsDefaultRole()) {
            return client.auth().listSpaceMember(graphSpace)
                         .contains(username);
        }
        if (client.graphSpace().checkDefaultRole(
                graphSpace, username, "analyst") ||
            client.graphSpace().checkDefaultRole(
                graphSpace, username, "observer")) {
            return true;
        }
        return client.auth().listSpaceMember(graphSpace).contains(username);
    }

    private void clearDefaultRoles(HugeClient client, String graphSpace,
                                   String username) {
        if (client.graphSpace().checkDefaultRole(
                graphSpace, username, "analyst")) {
            client.graphSpace().deleteDefaultRole(graphSpace, username, "analyst");
        }
        if (client.graphSpace().checkDefaultRole(
                graphSpace, username, "observer")) {
            client.graphSpace().deleteDefaultRole(
                    graphSpace, username, "observer");
        }
    }

    private void clearCustomRoles(HugeClient client, String graphSpace,
                                  String userId) {
        this.belongService.list(client, graphSpace, null, userId)
            .forEach(belong -> this.belongService.deleteById(client, graphSpace, belong.getId()));
    }

    private void setDefaultRole(HugeClient client, String graphSpace,
                                String username, String role) {
        client.graphSpace().setDefaultRole(graphSpace, username, role);
    }

    private void addDefaultRole(HugeClient client, String graphSpace,
                                UserView user, String username, String role) {
        boolean assigned = client.graphSpace().checkDefaultRole(
                graphSpace, username, role);
        if (assigned) {
            String preset = "observer".equals(role) ?
                            "GS_READ_ONLY" : "GS_READ_WRITE";
            user.addRole(new RoleEntity(role, role, preset));
        }
    }

    public void unauthUser(HugeClient client, String graphSpace,
                           String userId) {
        User account = client.auth().getUser(userId);
        E.checkNotNull(account, "User");
        List<BelongEntity> belongs = this.belongService.list(
                client, graphSpace, null, userId);
        belongs.forEach(belong -> {
            this.belongService.deleteById(client, graphSpace, belong.getId());
        });
        if (client.supportsDefaultRole()) {
            this.clearDefaultRoles(client, graphSpace, account.name());
        }
        if (client.auth().listSpaceAdmin(graphSpace).contains(account.name())) {
            client.auth().delSpaceAdmin(account.name(), graphSpace);
        }
        if (client.auth().listSpaceMember(graphSpace).contains(account.name())) {
            client.auth().delSpaceMember(account.name(), graphSpace);
        }
    }

    public IPage<User> querySpaceAdmins(HugeClient client, String graphSpace,
                                        String query, int pageNo,
                                        int pageSize) {
        List<User> spaceAdmins =
                this.getSpaceAdmins(client, graphSpace).stream()
                    .filter(user -> user.name().contains(query))
                    .sorted(Comparator.comparing(User::name))
                    .collect(Collectors.toList());
        return PageUtil.page(spaceAdmins, pageNo, pageSize);
    }

    private List<User> getSpaceAdmins(HugeClient client, String graphSpace) {
        List<String> spaceAdmins = client.auth().listSpaceAdmin(graphSpace);
        ArrayList<User> users = new ArrayList<>();
        for (String spaceAdmin : spaceAdmins) {
            User user = client.findUserByName(spaceAdmin);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    private static class SpacePresetState {

        private final List<BelongEntity> customRoles;
        private final boolean member;
        private final boolean admin;
        private final boolean analyst;
        private final boolean observer;

        private SpacePresetState(List<BelongEntity> customRoles,
                                 boolean member, boolean admin,
                                 boolean analyst, boolean observer) {
            this.customRoles = customRoles;
            this.member = member;
            this.admin = admin;
            this.analyst = analyst;
            this.observer = observer;
        }
    }
}
