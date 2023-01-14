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

package org.apache.hugegraph.manager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.base.HdfsDirectory;
import org.apache.hugegraph.base.LocalDirectory;
import org.apache.hugegraph.base.Printer;
import org.apache.hugegraph.base.ToolClient;
import org.apache.hugegraph.cmd.SubCommands;
import org.apache.hugegraph.constant.AuthRestoreConflictStrategy;
import org.apache.hugegraph.exception.ToolsException;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.JsonUtil;
import org.apache.logging.log4j.util.Strings;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AuthBackupRestoreManager extends BackupRestoreBaseManager {

    private static final String AUTH_BACKUP_RESTORE = "auth-backup-restore";
    private static final int NO_CONFLICT = 0;

    private AuthRestoreConflictStrategy strategy;
    private String initPassword;
    /*
     * The collection of id relationships of users, groups and targets
     * is the basic data of belong and accesses。
     */
    private Map<String, String> idsMap;
    private Map<String, User> usersByName;
    private Map<String, Group> groupsByName;
    private Map<String, Target> targetsByName;
    private Map<String, Belong> belongsByName;
    private Map<String, Access> accessesByName;

    public AuthBackupRestoreManager(ToolClient.ConnectionInfo info) {
        super(info, AUTH_BACKUP_RESTORE);
    }

    public void init(SubCommands.AuthBackup authBackup) {
        this.retry(authBackup.retry());
        this.directory(authBackup.directory(), authBackup.hdfsConf());
        this.ensureDirectoryExist(true);
    }

    public void init(SubCommands.AuthRestore authRestore) {
        this.retry(authRestore.retry());
        this.directory(authRestore.directory(), authRestore.hdfsConf());
        this.ensureDirectoryExist(false);
        this.strategy = authRestore.strategy();
        this.initPassword(authRestore.types(), authRestore.initPassword());
        this.idsMap = Maps.newHashMap();
        this.usersByName = Maps.newHashMap();
        this.groupsByName = Maps.newHashMap();
        this.targetsByName = Maps.newHashMap();
        this.belongsByName = Maps.newHashMap();
        this.accessesByName = Maps.newHashMap();
    }

    public void backup(List<HugeType> types) {
        try {
            this.doBackup(this.addAuthManagers(types));
        } catch (Throwable e) {
            throw e;
        } finally {
            this.shutdown(this.type());
        }
    }

    private void doBackup(List<AuthManager> authManagers) {
        E.checkState(CollectionUtils.isNotEmpty(authManagers),
                     "Backup data is empty, please check the type");
        for (AuthManager authManager : authManagers) {
             authManager.backup();
        }
    }

    public void restore(List<HugeType> types) {
        List<HugeType> sortedHugeTypes = this.sortListByCode(types);
        try {
            this.doRestore(this.addAuthManagers(sortedHugeTypes));
        } catch (Throwable e) {
            throw e;
        } finally {
            this.shutdown(this.type());
        }
    }

    private void doRestore(List<AuthManager> authManagers) {
        E.checkState(CollectionUtils.isNotEmpty(authManagers),
                     "Restore data is empty, please check the type");
        List<String> allConflicts = Lists.newArrayList();
        for (AuthManager authManager : authManagers) {
             allConflicts.addAll(authManager.checkConflict());
        }
        E.checkState(CollectionUtils.isEmpty(allConflicts),
                     "Restore conflict with STOP strategy, conflicting " +
                     "data is s%", JsonUtil.toJson(allConflicts));
        for (AuthManager authManager : authManagers) {
             authManager.restore();
        }
    }

    private List<AuthManager> addAuthManagers(List<HugeType> types) {
        List<AuthManager> authManagers = Lists.newArrayList();
        for (HugeType type : types) {
            switch (type) {
                case USER:
                    authManagers.add(new UserManager());
                    break;
                case GROUP:
                    authManagers.add(new GroupManager());
                    break;
                case TARGET:
                    authManagers.add(new TargetManager());
                    break;
                case BELONG:
                    authManagers.add(new BelongManager());
                    break;
                case ACCESS:
                    authManagers.add(new AccessManager());
                    break;
                default:
                    throw new AssertionError(String.format(
                              "Bad auth restore type: %s", type));
            }
        }
        return authManagers;
    }

    private boolean checkAllExistInIdMaps(String oneId, String otherId) {
        if (this.idsMap.containsKey(oneId) &&
            this.idsMap.containsKey(otherId)) {
            return true;
        }
        return false;
    }

    private List<String> readRestoreData(HugeType type) {
        List<String> resultList = Lists.newArrayList();
        InputStream is = this.inputStream(type.string());
        try (InputStreamReader isr = new InputStreamReader(is, API.CHARSET);
             BufferedReader reader = new BufferedReader(isr)) {
             String line;
             while ((line = reader.readLine()) != null) {
                 resultList.add(line);
             }
        } catch (IOException e) {
            throw new ToolsException("Failed to deserialize %s from %s",
                                     e, type.string(), resultList);
        }
        return resultList;
    }

    private long writeBackupData(HugeType type, List<?> list) {
        long count = 0L;
        try {
            OutputStream os = this.outputStream(type.string(), false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(LBUF_SIZE);
            StringBuilder builder = new StringBuilder(LBUF_SIZE);

            for (Object e : list) {
                count++;
                builder.append(JsonUtil.toJson(e)).append("\n");
            }
            baos.write(builder.toString().getBytes(API.CHARSET));
            os.write(baos.toByteArray());
        } catch (Throwable e) {
            throw new ToolsException("Failed to serialize %s to %s",
                                     e, list, type.string());
        }
        return count;
    }

    protected void directory(String dir, Map<String, String> hdfsConf) {
        if (hdfsConf == null || hdfsConf.isEmpty()) {
            // Local FS directory
            super.directory = LocalDirectory.constructDir(dir, AUTH_BACKUP_RESTORE);
        } else {
            // HDFS directory
            super.directory = HdfsDirectory.constructDir(dir, AUTH_BACKUP_RESTORE,
                                                         hdfsConf);
        }
    }

    private List<HugeType> sortListByCode(List<HugeType> hugeTypes) {
        return hugeTypes.stream()
                        .sorted(Comparator.comparing(HugeType::code))
                        .collect(Collectors.toList());
    }

    private void initPassword(List<HugeType> types, String password) {
        E.checkState(!types.contains(HugeType.USER) ||
                     Strings.isNotEmpty(password),
                     "The following option is " +
                     "required: [--init-password]");
        this.initPassword = password;
    }

    private interface AuthManager {

        void backup();

        List<String> checkConflict();

        void restore();
    }

    private class UserManager implements AuthManager {

        @Override
        public void backup() {
            Printer.print("Users backup started...");
            List<User> users = retry(client.authManager()::listUsers,
                                     "querying users of authority");
            long writeLines = writeBackupData(HugeType.USER, users);
            Printer.print("Users backup finished, write lines: %d",
                          writeLines);
        }

        @Override
        public List<String> checkConflict() {
            List<User> users = retry(client.authManager()::listUsers,
                                     "querying users of authority");
            Map<String, User> userMap = Maps.newHashMap();
            for (User user : users) {
                 userMap.put(user.name(), user);
            }
            List<String> userJsons = readRestoreData(HugeType.USER);
            List<String> conflicts = Lists.newArrayList();
            for (String user : userJsons) {
                int conflict = NO_CONFLICT;
                User restoreUser = JsonUtil.fromJson(user, User.class);
                if (!userMap.containsKey(restoreUser.name())) {
                    this.prepareUserForRestore(restoreUser);
                    continue;
                }
                User existUser = userMap.get(restoreUser.name());
                if (!StringUtils.equals(existUser.phone(),
                                        restoreUser.phone())) {
                    conflict++;
                }
                if (!StringUtils.equals(existUser.email(),
                                        restoreUser.email())) {
                    conflict++;
                }
                if (!StringUtils.equals(existUser.avatar(),
                                        restoreUser.avatar())) {
                    conflict++;
                }
                if (conflict > NO_CONFLICT) {
                    E.checkState(strategy.isStopStrategy() ||
                                 strategy.isIgnoreStrategy(),
                                 "Restore users strategy is not found");
                    if (strategy.isStopStrategy()) {
                        conflicts.add(restoreUser.toString());
                    }
                } else {
                    idsMap.put(restoreUser.id().toString(),
                               existUser.id().toString());
                }
            }
            return conflicts;
        }

        @Override
        public void restore() {
            int count = 0;
            for (Map.Entry<String, User> entry : usersByName.entrySet()) {
                User restoreUser = entry.getValue();
                restoreUser.password(initPassword);
                User user = retry(() -> {
                    return client.authManager().createUser(restoreUser);
                }, "restore users of authority");
                idsMap.put(restoreUser.id().toString(), user.id().toString());
                count++;
            }
            Printer.print("Restore users finished, total count is %d",
                          count);
        }

        private void prepareUserForRestore(User restoreUser) {
            idsMap.put(restoreUser.id().toString(), restoreUser.id().toString());
            usersByName.put(restoreUser.name(), restoreUser);
        }
    }

    private class GroupManager implements AuthManager {

        @Override
        public void backup() {
            Printer.print("Groups backup started...");
            List<Group> groups = retry(client.authManager()::listGroups,
                                       "querying groups of authority");
            long writeLines = writeBackupData(HugeType.GROUP, groups);
            Printer.print("Groups backup finished, write lines: %d",
                          writeLines);
        }

        @Override
        public List<String> checkConflict() {
            List<Group> groups = retry(client.authManager()::listGroups,
                                       "querying groups of authority");
            Map<String, Group> groupMap = Maps.newHashMap();
            for (Group group : groups) {
                 groupMap.put(group.name(), group);
            }
            List<String> groupJsons = readRestoreData(HugeType.GROUP);
            List<String> conflicts = Lists.newArrayList();
            for (String group : groupJsons) {
                int conflict = NO_CONFLICT;
                Group restoreGroup = JsonUtil.fromJson(group, Group.class);
                if (!groupMap.containsKey(restoreGroup.name())) {
                    this.prepareGroupForRestore(restoreGroup);
                    continue;
                }
                Group existGroup = groupMap.get(restoreGroup.name());
                if (!StringUtils.equals(existGroup.description(),
                                        restoreGroup.description())) {
                    conflict++;
                }
                if (conflict > NO_CONFLICT) {
                    E.checkState(strategy.isStopStrategy() ||
                                 strategy.isIgnoreStrategy(),
                                 "Restore groups strategy is not found");
                    if (strategy.isStopStrategy()) {
                        conflicts.add(restoreGroup.toString());
                    }
                } else {
                    idsMap.put(restoreGroup.id().toString(),
                               existGroup.id().toString());
                }
            }
            return conflicts;
        }

        @Override
        public void restore() {
            int count = 0;
            for (Map.Entry<String, Group> entry : groupsByName.entrySet()) {
                Group restoreGroup = entry.getValue();
                Group group = retry(() -> {
                    return client.authManager().createGroup(restoreGroup);
                }, "restore groups of authority");
                idsMap.put(restoreGroup.id().toString(), group.id().toString());
                count++;
            }
            Printer.print("Restore groups finished, total count is %d",
                          count);
        }

        private void prepareGroupForRestore(Group restoreGroup) {
            idsMap.put(restoreGroup.id().toString(), restoreGroup.id().toString());
            groupsByName.put(restoreGroup.name(), restoreGroup);
        }
    }

    private class TargetManager implements AuthManager {

        @Override
        public void backup() {
            Printer.print("Targets backup started...");
            List<Target> targets = retry(client.authManager()::listTargets,
                                         "querying targets of authority");
            long writeLines = writeBackupData(HugeType.TARGET, targets);
            Printer.print("Targets backup finished, write lines: %d",
                          writeLines);
        }

        @Override
        public List<String> checkConflict() {
            List<Target> targets = retry(client.authManager()::listTargets,
                                         "querying targets of authority");
            Map<String, Target> targetMap = Maps.newHashMap();
            for (Target target : targets) {
                 targetMap.put(target.name(), target);
            }
            List<String> targetJsons = readRestoreData(HugeType.TARGET);
            List<String> conflicts = Lists.newArrayList();
            for (String target : targetJsons) {
                int conflict = NO_CONFLICT;
                Target restoreTarget = JsonUtil.fromJson(target, Target.class);
                if (!targetMap.containsKey(restoreTarget.name())) {
                    this.prepareTargetForRestore(restoreTarget);
                    continue;
                }
                Target existTarget = targetMap.get(restoreTarget.name());
                if (!StringUtils.equals(existTarget.graph(),
                                        restoreTarget.graph())) {
                    conflict++;
                }
                if (!StringUtils.equals(existTarget.url(),
                                        restoreTarget.url())) {
                    conflict++;
                }
                if (conflict > NO_CONFLICT) {
                    E.checkState(strategy.isStopStrategy() ||
                                 strategy.isIgnoreStrategy(),
                                 "Restore targets strategy is not found");
                    if (strategy.isStopStrategy()) {
                        conflicts.add(restoreTarget.toString());
                    }
                } else {
                    idsMap.put(restoreTarget.id().toString(),
                               existTarget.id().toString());
                }
            }
            return conflicts;
        }

        @Override
        public void restore() {
            int count = 0;
            for (Map.Entry<String, Target> entry : targetsByName.entrySet()) {
                Target restoreTarget = entry.getValue();
                Target target = retry(() -> {
                    return client.authManager().createTarget(restoreTarget);
                }, "restore targets of authority");
                idsMap.put(restoreTarget.id().toString(),
                           target.id().toString());
                count++;
            }
            Printer.print("Restore targets finished, total count is %d",
                          count);
        }

        private void prepareTargetForRestore(Target restoreTarget) {
            idsMap.put(restoreTarget.id().toString(),
                       restoreTarget.id().toString());
            targetsByName.put(restoreTarget.name(), restoreTarget);
        }
    }

    private class BelongManager implements AuthManager {

        @Override
        public void backup() {
            Printer.print("Belongs backup started...");
            List<Belong> belongs = retry(client.authManager()::listBelongs,
                                         "querying belongs of authority");
            long writeLines = writeBackupData(HugeType.BELONG, belongs);
            Printer.print("Belongs backup finished, write lines: %d",
                          writeLines);
        }

        @Override
        public List<String> checkConflict() {
            List<Belong> belongs = retry(client.authManager()::listBelongs,
                                         "querying belongs of authority");
            Map<String, Belong>  belongMap = Maps.newHashMap();
            for (Belong belong : belongs) {
                 String belongKey = belong.user() + ":" + belong.group();
                 belongMap.put(belongKey, belong);
            }
            List<String> belongJsons = readRestoreData(HugeType.BELONG);
            List<String> conflicts = Lists.newArrayList();
            for (String belong : belongJsons) {
                Belong restoreBelong = JsonUtil.fromJson(belong, Belong.class);
                if (!checkAllExistInIdMaps(restoreBelong.user().toString(),
                                           restoreBelong.group().toString())) {
                    continue;
                }
                String ids = idsMap.get(restoreBelong.user()) + ":" +
                             idsMap.get(restoreBelong.group());
                if (belongMap.containsKey(ids)) {
                    E.checkState(strategy.isStopStrategy() ||
                                 strategy.isIgnoreStrategy(),
                                 "Restore belongs strategy is not found");
                    if (strategy.isStopStrategy()) {
                        conflicts.add(restoreBelong.toString());
                    }
                    continue;
                }
                belongsByName.put(restoreBelong.id().toString(), restoreBelong);
            }
            return conflicts;
        }

        @Override
        public void restore() {
            int count = 0;
            for (Map.Entry<String, Belong> entry : belongsByName.entrySet()) {
                Belong restoreBelong = entry.getValue();
                restoreBelong.user(idsMap.get(restoreBelong.user().toString()));
                restoreBelong.group(idsMap.get(restoreBelong.group().toString()));
                retry(() -> {
                     return client.authManager().createBelong(restoreBelong);
                }, "restore belongs of authority");
                count++;
            }
            Printer.print("Restore belongs finished, total count is %d",
                          count);
        }
    }

    private class AccessManager implements AuthManager {

        @Override
        public void backup() {
            Printer.print("Accesses backup started...");
            List<Access> accesses = retry(client.authManager()::listAccesses,
                                          "querying accesses of authority");
            long writeLines = writeBackupData(HugeType.ACCESS, accesses);
            Printer.print("Accesses backup finished, write lines: %d",
                          writeLines);
        }

        @Override
        public List<String> checkConflict() {
            List<Access> accesses = retry(client.authManager()::listAccesses,
                                          "querying accesses of authority");
            Map<String, Access>  accessMap = Maps.newHashMap();
            for (Access access : accesses) {
                 String accessKey = access.group() + ":" + access.target();
                 accessMap.put(accessKey, access);
            }
            List<String> accessJsons = readRestoreData(HugeType.ACCESS);
            List<String> conflicts = Lists.newArrayList();
            for (String access : accessJsons) {
                Access restoreAccess = JsonUtil.fromJson(access, Access.class);
                if (!checkAllExistInIdMaps(restoreAccess.group().toString(),
                                           restoreAccess.target().toString())) {
                    continue;
                }
                String ids = idsMap.get(restoreAccess.group()) + ":" +
                             idsMap.get(restoreAccess.target());
                if (accessMap.containsKey(ids)) {
                    E.checkState(strategy.isStopStrategy() ||
                                 strategy.isIgnoreStrategy(),
                                 "Restore accesses strategy is not found");
                    if (strategy.isStopStrategy()) {
                        conflicts.add(restoreAccess.toString());
                    }
                    continue;
                }
                accessesByName.put(restoreAccess.id().toString(), restoreAccess);
            }
            return conflicts;
        }

        @Override
        public void restore() {
            int count = 0;
            for (Map.Entry<String, Access> entry : accessesByName.entrySet()) {
                Access restoreAccess = entry.getValue();
                restoreAccess.target(idsMap.get(restoreAccess.target().toString()));
                restoreAccess.group(idsMap.get(restoreAccess.group().toString()));
                retry(() -> {
                     return client.authManager().createAccess(restoreAccess);
                }, "restore accesses of authority");
                count++;
            }
            Printer.print("Restore accesses finished, total count is %d",
                          count);
        }
    }
}
