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

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.structure.auth.Login;
import org.apache.hugegraph.options.HubbleOptions;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.UserEntity;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.exception.ParameterizedException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.exception.UnauthorizedException;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.PageUtil;

import com.csvreader.CsvReader;

import org.springframework.web.multipart.MultipartFile;

@Log4j2
@Service
public class UserService extends AuthService {

    public static final String CREATE_SUCCESS = "successfully created";

    //@Autowired
    //BelongService belongService;

    @Autowired
    ManagerService managerService;

    @Autowired
    private HugeConfig config;
    @Autowired
    private GraphSpaceUserService graphSpaceUserService;
    @Autowired
    private StandaloneAccountPermissionService standalonePermissions;

    private boolean isPdEnabled() {
        return config.get(HubbleOptions.PD_ENABLED);
    }

    public List<UserEntity> listUsers(HugeClient hugeClient) {
        AuthManager auth = hugeClient.auth();

        List<User> users = auth.listUsers();
        List<UserEntity> ues = new ArrayList<>(users.size());
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<String>> spaceMap = new HashMap<>();
        Set<String> standaloneReadWrite = isPdEnabled() ?
                                          new HashSet<>() :
                                          this.standalonePermissions
                                              .readWriteUsers(hugeClient);
        users.forEach(u -> {
            UserEntity ue = convert(hugeClient, u);
            if (isPdEnabled()) {
                ue.setSuperadmin(isSuperAdmin(hugeClient, ue.getId()));
            } else {
                ue.setSuperadmin(isStandaloneAdmin(ue.getName()));
                this.populateStandalonePermission(ue, standaloneReadWrite);
            }
            ues.add(ue);
        });
        if (isPdEnabled()) {
            List<Object> listMap = getSpaceAndSpacenum(hugeClient);
            spaceMap = HubbleUtil.uncheckedCast(listMap.get(0));
            countMap = HubbleUtil.uncheckedCast(listMap.get(1));
            for (UserEntity user : ues) {
                user.setSpacenum(countMap.get(user.getName()));
                user.setAdminSpaces(spaceMap.get(user.getName()));
            }
            this.populatePermissionPresets(hugeClient, ues);
        }

        return ues;
    }

    public UserEntity getUser(HugeClient client, String name) {
        return convert(client, client.findUserByName(name));
    }

    public Object queryPage(HugeClient hugeClient, String query,
                            int pageNo, int pageSize) {
        AuthManager auth = hugeClient.auth();
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<String>> spaceMap = new HashMap<>();

        List<UserEntity> results =
                hugeClient.auth().listUsers().stream()
                        .filter((u) -> u.name().contains(query) ||
                                u.nickname() != null && u.nickname().contains(query))
                        .sorted(Comparator.comparing(User::name))
                        .map((u) -> {
                            UserEntity ue = convert(hugeClient, u);
                            return ue;
                        }).collect(Collectors.toList());

        if (isPdEnabled()) {
            List<Object> listMap = getSpaceAndSpacenum(hugeClient);
            spaceMap = HubbleUtil.uncheckedCast(listMap.get(0));
            countMap = HubbleUtil.uncheckedCast(listMap.get(1));
            for (UserEntity user : results) {
                user.setSpacenum(countMap.get(user.getName()));
                user.setAdminSpaces(spaceMap.get(user.getName()));
                user.setSuperadmin(isSuperAdmin(hugeClient, user.getId()));
            }
            IPage<UserEntity> page = PageUtil.page(results, pageNo, pageSize);
            this.populatePermissionPresets(hugeClient, page.getRecords());
            return page;
        } else {
            Set<String> standaloneReadWrite =
                    this.standalonePermissions.readWriteUsers(hugeClient);
            for (UserEntity user : results) {
                user.setSuperadmin(isStandaloneAdmin(user.getName()));
                this.populateStandalonePermission(user,
                                                  standaloneReadWrite);
            }
        }
        return PageUtil.page(results, pageNo, pageSize);
    }

    public UserEntity get(HugeClient hugeClient, String userId) {
        AuthManager auth = hugeClient.auth();
        User user = auth.getUser(userId);
        if (user == null) {
            throw new InternalException("auth.user.get.%s Not Exits",
                    userId);
        }
        UserEntity userEntity = convert(hugeClient, user);
        if (isPdEnabled()) {
            userEntity.setSuperadmin(isSuperAdmin(hugeClient, userEntity.getId()));
            List<String> spaces = hugeClient.graphSpace().listGraphSpace();
            List<Object> listMap = getSpaceAndSpacenum(hugeClient);
            Map<String, List<String>> spaceMap =
                    HubbleUtil.uncheckedCast(listMap.get(0));
            List<String> adminSpaces = spaceMap.get(user.name());
            if (adminSpaces == null) {
                adminSpaces = new ArrayList<>();
            }
            List<String> resSpaces = new ArrayList<>();
            for (String space : spaces) {
                if (this.graphSpaceUserService.hasGraphSpaceAccess(
                        hugeClient, space, user.name())) {
                    resSpaces.add(space);
                }
            }
            resSpaces.addAll(adminSpaces);
            userEntity.setAdminSpaces(adminSpaces);
            userEntity.setSpacenum(adminSpaces.size());
            userEntity.setResSpaces(resSpaces);
            this.populatePermissionPresets(hugeClient, userEntity);
        } else {
            userEntity.setSuperadmin(isStandaloneAdmin(user.name()));
            userEntity.setAdminSpaces(new ArrayList<>());
            userEntity.setSpacenum(0);
            userEntity.setResSpaces(new ArrayList<>());
            this.populateStandalonePermission(hugeClient, userEntity);
        }
        return userEntity;
    }

    public UserEntity getpersonal(HugeClient hugeClient, String username) {
        User user = currentUser(hugeClient, username);
        if (user == null) {
            throw new InternalException("auth.user.get.%s Not Exits",
                    username);
        }
        UserEntity userEntity = convert(hugeClient, user);
        if (isPdEnabled()) {
            userEntity.setSuperadmin(isSuperAdmin(hugeClient));
            List<String> adminSpaces = new ArrayList<>();
            List<String> resSpaces = new ArrayList<>();
            List<String> spaces = hugeClient.graphSpace().listGraphSpace();
            for (String space : spaces) {
                if (hugeClient.auth().isSpaceAdmin(space)) {
                    adminSpaces.add(space);
                }
                if (hugeClient.auth().isSpaceAdmin(space) ||
                    hasCurrentUserAccess(hugeClient, space)) {
                    resSpaces.add(space);
                }
            }
            userEntity.setAdminSpaces(adminSpaces);
            userEntity.setSpacenum(adminSpaces.size());
            userEntity.setResSpaces(resSpaces);
            this.populateCurrentUserPermissionPresets(hugeClient, userEntity);
        } else {
            userEntity.setSuperadmin(isStandaloneAdmin(username));
            userEntity.setAdminSpaces(new ArrayList<>());
            userEntity.setSpacenum(0);
            userEntity.setResSpaces(new ArrayList<>());
        }
        return userEntity;
    }

    public UserEntity add(HugeClient client, UserEntity ue) {
        boolean permissionPresets = isPdEnabled() &&
                                    client.supportsDefaultRole();
        this.validatePermissionMutation(client, ue, true,
                                        permissionPresets);
        User user = new User();
        user.name(ue.getName());
        user.password(ue.getPassword());
        user.phone(ue.getPhone());
        user.email(ue.getEmail());
        user.avatar(ue.getAvatar());
        user.description(ue.getDescription());
        if (isPdEnabled()) {
            user.nickname(ue.getNickname());
        }

        User newUser = client.auth().createUser(user);
        boolean superAdminAttempted = false;
        try {
            if (permissionPresets && ue.getPermissionPreset() != null) {
                this.graphSpaceUserService
                    .applyPermissionPresetsForNewAccount(
                        client, ue.getName(),
                        ue.getGraphspacePermissions(),
                        ue.getPermissionPreset());
            }
            if (permissionPresets && newUser != null && ue.isSuperadmin()) {
                superAdminAttempted = true;
                client.auth().addSuperAdmin(ue.getName());
            }
            if (!isPdEnabled()) {
                this.standalonePermissions.assignReadWrite(client, newUser);
            }
        } catch (RuntimeException error) {
            this.rollbackNewAccount(client, newUser, ue.getName(),
                                    superAdminAttempted, error);
            throw error;
        }
        return this.convert(client, newUser);
    }

    private void rollbackNewAccount(HugeClient client, User user,
                                    String username,
                                    boolean superAdminAttempted,
                                    RuntimeException failure) {
        if (superAdminAttempted) {
            this.suppressRollback(
                    () -> client.auth().delSuperAdmin(username), failure);
        }
        if (user != null) {
            this.suppressRollback(
                    () -> client.auth().deleteUser(user.id()), failure);
        }
    }

    private void suppressRollback(Runnable rollback, RuntimeException failure) {
        try {
            rollback.run();
        } catch (RuntimeException rollbackError) {
            failure.addSuppressed(rollbackError);
        }
    }

    private void populateStandalonePermission(HugeClient client,
                                              UserEntity user) {
        if (!user.isSuperadmin() &&
            this.standalonePermissions.hasReadWrite(client, user.getId())) {
            user.setPermissionPreset("GS_READ_WRITE");
        }
    }

    private void populateStandalonePermission(UserEntity user,
                                              Set<String> readWriteUsers) {
        if (!user.isSuperadmin() && readWriteUsers.contains(user.getId())) {
            user.setPermissionPreset("GS_READ_WRITE");
        }
    }

    public String addbatch(HugeClient client, MultipartFile csvFile) {
        File file = multipartFileToFile(csvFile);
        try {
            Map<String, Object> csv = readCsvByCsvReader(file);
            List<Map<String, String>> createBatchBody =
                    HubbleUtil.uncheckedCast(csv.get("data"));
            Map<String, List<Map<String, String>>> result =
                    client.auth().createBatch(createBatchBody);
            List<Map<String, String>> resultList = result.get("result");
            List<String> failedList = new ArrayList<>(createBatchBody.size());
            for (Map<String, String> entry : resultList) {
                if (!CREATE_SUCCESS.equals(entry.get("result"))) {
                    failedList.add(entry.get("user_name"));
                } else if (!isPdEnabled()) {
                    User user = client.findUserByName(entry.get("user_name"));
                    this.standalonePermissions.assignReadWrite(client, user);
                }
            }
            if (!failedList.isEmpty()) {
                throw new InternalException("auth.user.batch-create.failed",
                                            failedList);
            }
            return "success";
        } finally {
            if (!file.delete()) {
                log.warn("Failed to delete temporary user import file '{}'; " +
                         "it will be removed by the operating system", file);
            }
        }
    }

    public File multipartFileToFile(MultipartFile multiFile) {
        File file = null;
        try {
            String originalName = multiFile.getOriginalFilename();
            String suffix = ".tmp";
            if (originalName != null) {
                int dot = originalName.lastIndexOf('.');
                String candidate = dot >= 0 ? originalName.substring(dot) : "";
                if (candidate.matches("\\.[A-Za-z0-9]{1,16}")) {
                    suffix = candidate;
                }
            }
            file = File.createTempFile("hubble-user-import-", suffix);
            multiFile.transferTo(file);
            return file;
        } catch (Exception e) {
            log.error("Failed to create a temporary file for user import", e);
            if (file != null && file.exists() && !file.delete()) {
                log.warn("Failed to delete incomplete user import file '{}'",
                         file);
            }
            throw new InternalException("auth.user.import-file.failed", e);
        }
    }

    public Map<String, Object> readCsvByCsvReader(File file) {
        if (file == null) {
            throw new InternalException("auth.user.import-file.failed");
        }
        Map<String, Object> mapData = new HashMap<>();
        String fileName = file.getName();
        mapData.put("sheetName", fileName);

        List<String> strList = new ArrayList<>();
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<String[]> arrList = new ArrayList<String[]>();
            CsvReader reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
            // 读取表头
            reader.readHeaders();
            String[] headArray = reader.getHeaders();
            while (reader.readRecord()) {
                // 按行读取，并把每一行的数据添加到list集合
                arrList.add(reader.getValues());
            }
            reader.close();
            // 如果要返回 String[] 类型的 list 集合，则直接返回 arrList
            // 以下步骤是把 String[] 类型的 list 集合转化为 String 类型的 list 集合
            for (int i = 0; i < arrList.size(); i++) {
                // 组装String字符串
                // 如果不知道有多少列，则可再加一个循环
                Map<String, Object> map = new HashMap<>();
                for (int j = 0; j < arrList.get(0).length; j++) {
                    map.put("" + headArray[j] + "", arrList.get(i)[j]);
                }
                list.add(map);
            }
        } catch (Exception e) {
            log.error("Failed to parse the user import CSV file", e);
            throw new InternalException("auth.user.import-csv.failed", e);
        }
        mapData.put("data", list);
        return mapData;
    }

    public void delete(HugeClient hugeClient, String userId) {
        hugeClient.auth().deleteUser(userId);
    }

    protected UserEntity convert(HugeClient client, User user) {
        if (user == null) {
            return null;
        }

        UserEntity u = new UserEntity();
        u.setId(user.id().toString());
        u.setName(user.name());
        u.setNickname(user.nickname());
        u.setEmail(user.email());
        u.setPhone(user.phone());
        u.setAvatar(user.avatar());
        u.setDescription(user.description());
        u.setCreate(user.createTime());
        u.setUpdate(user.updateTime());
        u.setCreator(user.creator());

        return u;
    }

    private void populatePermissionPresets(HugeClient client,
                                            UserEntity userEntity) {
        List<Map<String, String>> permissions = new ArrayList<>();
        if (userEntity.isSuperadmin()) {
            userEntity.setGraphspacePermissions(permissions);
            userEntity.setPermissionPreset("SUPER_ADMIN");
            return;
        }
        if (!client.supportsDefaultRole()) {
            userEntity.setGraphspacePermissions(permissions);
            userEntity.setPermissionPreset("LEGACY_CUSTOM");
            return;
        }
        List<String> graphSpaces = client.graphSpace().listGraphSpace();
        boolean legacyCustom = false;
        for (String graphSpace : graphSpaces) {
            legacyCustom |= this.graphSpaceUserService.hasCustomRoles(client, graphSpace, userEntity.getId());
            if (userEntity.getAdminSpaces() != null &&
                userEntity.getAdminSpaces().contains(graphSpace)) {
                permissions.add(permission(graphSpace, "GS_ADMIN"));
                continue;
            }
            if (client.graphSpace().checkDefaultRole(
                    graphSpace, userEntity.getName(), "analyst")) {
                permissions.add(permission(graphSpace, "GS_READ_WRITE"));
            } else if (hasObserverRole(client, graphSpace, userEntity.getName())) {
                permissions.add(permission(graphSpace, "GS_READ_ONLY"));
            }
        }
        userEntity.setGraphspacePermissions(permissions);
        if (legacyCustom) {
            userEntity.setPermissionPreset("LEGACY_CUSTOM");
        }
    }

    private void populateCurrentUserPermissionPresets(
            HugeClient client, UserEntity userEntity) {
        List<Map<String, String>> permissions = new ArrayList<>();
        if (userEntity.isSuperadmin()) {
            userEntity.setGraphspacePermissions(permissions);
            userEntity.setPermissionPreset("SUPER_ADMIN");
            return;
        }
        if (!client.supportsDefaultRole()) {
            userEntity.setGraphspacePermissions(permissions);
            userEntity.setPermissionPreset("LEGACY_CUSTOM");
            return;
        }
        for (String graphSpace : client.graphSpace().listGraphSpace()) {
            if (userEntity.getAdminSpaces() != null &&
                userEntity.getAdminSpaces().contains(graphSpace)) {
                permissions.add(permission(graphSpace, "GS_ADMIN"));
            } else if (client.auth().checkDefaultRole(
                    graphSpace, "analyst")) {
                permissions.add(permission(graphSpace, "GS_READ_WRITE"));
            } else if (hasCurrentUserObserverRole(client, graphSpace)) {
                permissions.add(permission(graphSpace, "GS_READ_ONLY"));
            }
        }
        userEntity.setGraphspacePermissions(permissions);
    }

    private void populatePermissionPresets(HugeClient client,
                                            Collection<UserEntity> users) {
        if (!client.supportsDefaultRole()) {
            for (UserEntity user : users) {
                user.setGraphspacePermissions(new ArrayList<>());
                user.setPermissionPreset(user.isSuperadmin() ? "SUPER_ADMIN" : "LEGACY_CUSTOM");
            }
            return;
        }
        List<String> graphSpaces = client.graphSpace().listGraphSpace();
        for (UserEntity user : users) {
            List<Map<String, String>> values = new ArrayList<>();
            if (user.isSuperadmin()) {
                user.setGraphspacePermissions(values);
                user.setPermissionPreset("SUPER_ADMIN");
                continue;
            }
            boolean legacyCustom = false;
            if (user.getAdminSpaces() != null) {
                for (String graphSpace : user.getAdminSpaces()) {
                    values.add(permission(graphSpace, "GS_ADMIN"));
                }
            }
            for (String graphSpace : graphSpaces) {
                legacyCustom |= this.graphSpaceUserService.hasCustomRoles(client, graphSpace, user.getId());
                if (user.getAdminSpaces() != null &&
                    user.getAdminSpaces().contains(graphSpace)) {
                    continue;
                }
                if (client.graphSpace().checkDefaultRole(
                        graphSpace, user.getName(), "analyst")) {
                    values.add(permission(graphSpace, "GS_READ_WRITE"));
                } else if (hasObserverRole(client, graphSpace, user.getName())) {
                    values.add(permission(graphSpace, "GS_READ_ONLY"));
                }
            }
            user.setGraphspacePermissions(values);
            if (legacyCustom) {
                user.setPermissionPreset("LEGACY_CUSTOM");
            }
        }
    }

    private static Map<String, String> permission(String graphSpace,
                                                  String preset) {
        Map<String, String> permission = new HashMap<>();
        permission.put("graphspace", graphSpace);
        permission.put("permission_preset", preset);
        return permission;
    }

    private static boolean hasObserverRole(HugeClient client,
                                           String graphSpace,
                                           String username) {
        return client.graphSpace().checkDefaultRole(
                graphSpace, username, "observer");
    }

    private static boolean hasCurrentUserAccess(HugeClient client,
                                                String graphSpace) {
        if (!client.supportsDefaultRole()) {
            return client.auth().isSpaceMember(graphSpace);
        }
        if (client.auth().checkDefaultRole(graphSpace, "analyst")) {
            return true;
        }
        if (hasCurrentUserObserverRole(client, graphSpace)) {
            return true;
        }
        return client.auth().isSpaceMember(graphSpace);
    }

    private static boolean hasCurrentUserObserverRole(HugeClient client,
                                                      String graphSpace) {
        try {
            return client.auth().checkDefaultRole(graphSpace, "observer");
        } catch (ServerException e) {
            if (e.status() == 403) {
                return false;
            }
            throw e;
        }
    }

    protected List<Object> getSpaceAndSpacenum(HugeClient hugeClient) {
        AuthManager auth = hugeClient.auth();
        List<Object> listMap = new ArrayList<>();
        if (!isPdEnabled()) {
            // Non-PD mode: no GraphSpace/Manager APIs available
            listMap.add(new HashMap<>());
            listMap.add(new HashMap<>());
            return listMap;
        }
        List<User> users = auth.listUsers();
        List<String> spaces = hugeClient.graphSpace().listGraphSpace();
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<String>> spaceMap = new HashMap<>();
        for (User user : users) {
            countMap.put(user.name(), 0);
            spaceMap.put(user.name(), new ArrayList<>());
        }

        for (String space : spaces) {
            List<String> spaceManagers =
                    hugeClient.auth().listSpaceAdmin(space);
            for (String spaceManager : spaceManagers) {
                countMap.put(spaceManager, countMap.get(spaceManager) + 1);
                List<String> tempspace = spaceMap.get(spaceManager);
                tempspace.add(space);
            }
        }
        listMap.add(spaceMap);
        listMap.add(countMap);
        return listMap;
    }

    public void update(HugeClient hugeClient, UserEntity userEntity) {
        boolean permissionPresets = isPdEnabled() &&
                                    hugeClient.supportsDefaultRole();
        boolean permissionMutation = userEntity.getPermissionPreset() != null;
        this.validatePermissionMutation(hugeClient, userEntity, false,
                                        permissionPresets);
        if (isPdEnabled() && !permissionPresets) {
            this.validateLegacyPermissionUpdate(hugeClient, userEntity);
        }
        User user = new User();
        user.setId(userEntity.getId());
        user.name(userEntity.getName());
        user.password(userEntity.getPassword());
        user.phone(userEntity.getPhone());
        user.email(userEntity.getEmail());
        user.description(userEntity.getDescription());
        if (isPdEnabled()) {
            user.nickname(userEntity.getNickname());
        }
        if (permissionPresets && permissionMutation) {
            this.updateModernPermissionAccount(hugeClient, user, userEntity);
            return;
        }
        hugeClient.auth().updateUser(user);
    }

    private void validateLegacyPermissionUpdate(HugeClient client,
                                                UserEntity user) {
        if (user.getPermissionPreset() != null ||
            user.getGraphspacePermissions() != null &&
            !user.getGraphspacePermissions().isEmpty()) {
            throw new ParameterizedException(
                      "auth.permission-preset.unsupported");
        }
        if (user.getAdminSpaces() != null) {
            List<String> current = this.listAdminSpace(client, user.getName());
            if (!new HashSet<>(current).equals(
                    new HashSet<>(user.getAdminSpaces()))) {
                throw new ParameterizedException(
                          "auth.permission-preset.unsupported");
            }
        }
        if (user.hasSuperadmin() &&
            this.isSuperAdmin(client, user.getId()) != user.isSuperadmin()) {
            throw new ParameterizedException(
                      "auth.permission-preset.unsupported");
        }
    }

    private void updateModernPermissionAccount(HugeClient client, User user,
                                               UserEntity userEntity) {
        String userId = user.id().toString();
        User previous = client.auth().getUser(userId);
        E.checkNotNull(previous, "User");
        String username = previous.name();
        boolean previousSuperAdmin = client.auth().listSuperAdmin().contains(username);
        try {
            client.auth().updateUser(user);
            this.graphSpaceUserService.applyPermissionPresets(
                    client, userEntity.getName(),
                    userEntity.getGraphspacePermissions(),
                    userEntity.getPermissionPreset());
            if (!previousSuperAdmin && userEntity.isSuperadmin()) {
                client.auth().addSuperAdmin(username);
            } else if (previousSuperAdmin && !userEntity.isSuperadmin()) {
                client.auth().delSuperAdmin(username);
            }
        } catch (RuntimeException error) {
            this.restoreAccountProfile(client, previous, error);
            this.restoreSuperAdmin(client, username, previousSuperAdmin, error);
            throw error;
        }
    }

    private void restoreAccountProfile(HugeClient client, User previous,
                                       RuntimeException failure) {
        try {
            client.auth().updateUser(previous);
        } catch (RuntimeException rollbackError) {
            failure.addSuppressed(rollbackError);
            log.warn("Failed to restore profile for account '{}'",
                     previous.id(), rollbackError);
        }
    }

    private void restoreSuperAdmin(HugeClient client, String username,
                                   boolean expected,
                                   RuntimeException failure) {
        try {
            boolean current = client.auth().listSuperAdmin()
                                    .contains(username);
            if (expected && !current) {
                client.auth().addSuperAdmin(username);
            } else if (!expected && current) {
                client.auth().delSuperAdmin(username);
            }
        } catch (RuntimeException rollbackError) {
            failure.addSuppressed(rollbackError);
            log.warn("Failed to restore super administrator state for '{}'",
                     username, rollbackError);
        }
    }

    private void validatePermissionMutation(HugeClient client,
                                            UserEntity user,
                                            boolean create,
                                            boolean supported) {
        if (!supported) {
            if (create && (user.isSuperadmin() ||
                           user.getPermissionPreset() != null ||
                           user.getAdminSpaces() != null &&
                           !user.getAdminSpaces().isEmpty() ||
                           user.getGraphspacePermissions() != null &&
                           !user.getGraphspacePermissions().isEmpty())) {
                throw new ParameterizedException(
                          "auth.permission-preset.unsupported");
            }
            return;
        }
        String preset = user.getPermissionPreset();
        if (preset == null) {
            return;
        }
        if (!create && user.getPassword() != null &&
            !user.getPassword().isEmpty()) {
            throw new ParameterizedException(
                      "Update password and permissions separately");
        }
        boolean superAdminPreset = "SUPER_ADMIN".equals(preset);
        if (superAdminPreset != user.isSuperadmin()) {
            throw new ParameterizedException(
                      "auth.permission-preset.superadmin-mismatch");
        }
        boolean unassignedDemotion = !create &&
                                     "GS_READ_ONLY".equals(preset) &&
                                     (user.getGraphspacePermissions() == null ||
                                      user.getGraphspacePermissions().isEmpty()) &&
                                     user.hasSuperadmin() &&
                                     !user.isSuperadmin() &&
                                     this.isSuperAdmin(client, user.getId());
        if (!unassignedDemotion) {
            this.graphSpaceUserService.validatePermissionPresets(
                    client, user.getGraphspacePermissions(), preset);
        }
    }

    public void updatePersonal(HugeClient hugeClient, String username,
                               String nickname, String description) {
        if (!hugeClient.supportsPersonalProfileUpdate()) {
            throw new ParameterizedException(
                      "auth.profile.update-unsupported");
        }
        User user = currentUser(hugeClient, username);
        if (isPdEnabled()) {
            user.nickname(nickname);
        } else {
            user.nickname(null);
        }
        user.description(description);
        user.password(null);
        hugeClient.auth().updateUser(user);
    }

    public Response updatepwd(HugeClient hugeClient, String username,
                              String oldpwd, String newpwd) {
        Login login = new Login();
        login.name(username);
        login.password(oldpwd);
        try {
            hugeClient.auth().login(login);
        } catch (Exception e) {
            return Response.builder()
                    .status(Constant.STATUS_BAD_REQUEST)
                    .message(e.getMessage())
                    .cause(null)
                    .build();
        }
        // Must fetch user first to get the ID, otherwise updateUser sends
        // PUT to the collection path (no {id}) and gets HTTP 405.
        User user = currentUser(hugeClient, username);
        user.password(newpwd);
        hugeClient.auth().updateUser(user);
        return Response.builder()
                .status(Constant.STATUS_OK)
                .build();
    }

    private static User currentUser(HugeClient client, String username) {
        try {
            return client.findCurrentUser(username);
        } catch (IllegalStateException e) {
            throw new UnauthorizedException();
        } catch (ServerException e) {
            if (e.status() == 401 || e.status() == 403 || e.status() == 404) {
                throw new UnauthorizedException();
            }
            throw e;
        }
    }

    public List<String> listAdminSpace(HugeClient hugeClient, String username) {
        if (!isPdEnabled()) {
            return new ArrayList<>();
        }
        List<String> spaces = hugeClient.graphSpace().listGraphSpace();
        List<String> adminspace = new ArrayList<String>();
        for (String space : spaces) {
            List<String> spaceManagers =
                    hugeClient.auth().listSpaceAdmin(space);
            for (String spaceManager : spaceManagers) {
                if (spaceManager.equals(username)) {
                    adminspace.add(space);
                }
            }
        }
        return adminspace;
    }

    public void updateAdminSpace(HugeClient hugeClient, String username,
                                 List<String> adminspaces) {
        if (adminspaces == null || !isPdEnabled()) {
            return;
        }
        if (!hugeClient.supportsDefaultRole()) {
            throw new ParameterizedException(
                      "auth.permission-preset.unsupported");
        }
        List<String> oldadminspaces = listAdminSpace(hugeClient, username);
        User account = hugeClient.findUserByName(username);
        E.checkNotNull(account, "User");
        for (String adminspace : adminspaces) {
            if (!oldadminspaces.contains(adminspace)) {
                this.graphSpaceUserService.applySpacePreset(hugeClient, adminspace, account.id().toString(),
                        account.name(),
                        "GS_ADMIN");
            }
        }
        for (String oldadminspace : oldadminspaces) {
            if (!adminspaces.contains(oldadminspace)) {
                this.graphSpaceUserService.removeSpacePreset(hugeClient, oldadminspace,
                        account.id().toString());
            }
        }
    }

    public String userLevel(HugeClient client, String username) {
        if (!isPdEnabled()) {
            return standaloneUserLevel(username);
        }

        if (isSuperAdmin(client)) {
            return "ADMIN";
        }

        if (isSpaceAdmin(client)) {
            return "SPACEADMIN";
        }

        // Default: user
        return "USER";
    }

    static String standaloneUserLevel(String username) {
        // StandardAuthenticator reserves the configured administrator account
        // name. Other authenticated users still depend on explicit role grants.
        return "admin".equals(username) ? "ADMIN" : "USER";
    }

    private static boolean isStandaloneAdmin(String username) {
        return "ADMIN".equals(standaloneUserLevel(username));
    }

    public boolean isSuperAdmin(HugeClient client, String uid) {
        if (!isPdEnabled()) {
            return false;
        }
        User account = client.auth().getUser(uid);
        return account != null && client.auth().listSuperAdmin().contains(account.name());
    }

    public boolean isSuperAdmin(HugeClient client) {
        if (!isPdEnabled()) {
            return false;
        }
        // Check: if current user is superadmin
        return client.auth().isSuperAdmin();
    }

    /*
    public boolean isAssignSpaceAdmin(HugeClient client, String uid,
                                      String graphSpace) {
        // Only used by superadmin
        // Check: if user is spaceadmin for one graphSpace
        return client.auth().listSpaceAdmin(graphSpace).contains(uid);
    }
     */

    public boolean isAssignSpaceAdmin(HugeClient client, String graphSpace) {
        if (!isPdEnabled()) {
            return false;
        }
        // Check: if current user is spaceadmin
        return client.auth().isSpaceAdmin(graphSpace);
    }

    public boolean isSpaceAdmin(HugeClient client) {
        if (!isPdEnabled()) {
            return false;
        }
        // Check: if current user is spaceadmin
        List<String> graphSpaces = client.graphSpace().listGraphSpace();
        for (String gs : graphSpaces) {
            if (isAssignSpaceAdmin(client, gs)) {
                return true;
            }
        }

        return false;
    }

    // List graphspace admin
    public List<String> listGraphSpaceAdmin(HugeClient client,
                                          String graphSpace) {
        if (!isPdEnabled()) {
            return new ArrayList<>();
        }
        AuthManager auth = client.auth();

        return auth.listSpaceAdmin(graphSpace);
    }
}
