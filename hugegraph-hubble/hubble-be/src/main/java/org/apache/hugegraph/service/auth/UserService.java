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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.structure.auth.Login;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.UserEntity;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.util.PageUtil;

import com.csvreader.CsvReader;

import org.springframework.web.multipart.MultipartFile;

@Log4j2
@Service
public class UserService extends AuthService{

    public static final String CREATE_SUCCESS = "successfully created";

    @Autowired
    BelongService belongService;

    @Autowired
    ManagerService managerService;

    @Autowired
    private HugeConfig config;

    public List<UserEntity> listUsers(HugeClient hugeClient) {
        AuthManager auth = hugeClient.auth();

        List<User> users = auth.listUsers();
        List<UserEntity> ues= new ArrayList<>(users.size());
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<String>> spaceMap = new HashMap<>();
        users.forEach(u -> {
            UserEntity ue = convert(hugeClient, u);
            ue.setSuperadmin(isSuperAdmin(hugeClient, ue.getId()));
            ues.add(ue);
        });
        List<Map> listMap = getSpaceAndSpacenum(hugeClient);
        spaceMap = listMap.get(0);
        countMap = listMap.get(1);
        for (UserEntity user: ues) {
            user.setSpacenum(countMap.get(user.getName()));
            user.setAdminSpaces(spaceMap.get(user.getName()));
        }

        return ues;
    }

    public UserEntity getUser(HugeClient client, String name) {
        // No Check if user is superadmin;
        return convert(client, client.auth().getUser(name));
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

        List<Map> listMap = getSpaceAndSpacenum(hugeClient);
        spaceMap = listMap.get(0);
        countMap = listMap.get(1);
        for (UserEntity user: results) {
            user.setSpacenum(countMap.get(user.getName()));
            user.setAdminSpaces(spaceMap.get(user.getName()));
            user.setSuperadmin(isSuperAdmin(hugeClient, user.getId()));
        }
        return PageUtil.page(results, pageNo, pageSize);
    }

    public Object superQueryPage(HugeClient hugeClient, String query,
                            int pageNo, int pageSize) {
        AuthManager auth = hugeClient.auth();
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<String>> spaceMap = new HashMap<>();

        List<UserEntity> results =
                hugeClient.auth().listUsers().stream()
                        .filter((u) -> !"admin".equals(u.name())
                                && !"system".equals(u.name())
                                && isSuperAdmin(hugeClient, u.id().toString()))
                        .sorted(Comparator.comparing(User::name))
                        .map((u) -> {
                            UserEntity ue = convert(hugeClient, u);
                            return ue;
                        }).collect(Collectors.toList());

        List<Map> listMap = getSpaceAndSpacenum(hugeClient);
        spaceMap = listMap.get(0);
        countMap = listMap.get(1);
        for (UserEntity user: results) {
            user.setSpacenum(countMap.get(user.getName()));
            user.setAdminSpaces(spaceMap.get(user.getName()));
            user.setSuperadmin(isSuperAdmin(hugeClient, user.getId()));
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
        userEntity.setSuperadmin(isSuperAdmin(hugeClient, userEntity.getId()));
        List<String> spaces = hugeClient.graphSpace().listGraphSpace();
        List<Map> listMap = getSpaceAndSpacenum(hugeClient);
        List<String> adminSpaces = (List<String>) listMap.get(0).get(userId);
        List<String> resSpaces = new ArrayList<>();
        for (String space : spaces) {
            if (hugeClient.graphSpace().checkDefaultRole(space, userId, "analyst")) {
                resSpaces.add(space);
            }
        }
        resSpaces.addAll(adminSpaces);
        userEntity.setAdminSpaces(adminSpaces);
        userEntity.setSpacenum(adminSpaces.size());
        userEntity.setResSpaces(resSpaces);
        return userEntity;
    }

    public UserEntity getpersonal(HugeClient hugeClient, String username) {
        AuthManager auth = hugeClient.auth();
        User user = auth.getUser(username);
        if (user == null) {
            throw new InternalException("auth.user.get.%s Not Exits",
                    username);
        }
        UserEntity userEntity = convert(hugeClient, user);
        userEntity.setSuperadmin(isSuperAdmin(hugeClient));
        List<String> adminSpaces = new ArrayList<>();
        List<String> resSpaces = new ArrayList<>();
        List<String> spaces = hugeClient.graphSpace().listGraphSpace();
        for (String space: spaces) {
            if (hugeClient.auth().isSpaceAdmin(space)) {
                adminSpaces.add(space);
            }
            if (hugeClient.auth().isSpaceAdmin(space) ||
                    hugeClient.auth().checkDefaultRole(space, "analyst") ) {
                resSpaces.add(space);
            }
        }
        userEntity.setAdminSpaces(adminSpaces);
        userEntity.setSpacenum(adminSpaces.size());
        userEntity.setResSpaces(resSpaces);
        return userEntity;
    }

    public void add(HugeClient client, UserEntity ue) {
        User user = new User();
        user.name(ue.getName());
        user.password(ue.getPassword());
        user.phone(ue.getPhone());
        user.email(ue.getEmail());
        user.avatar(ue.getAvatar());
        user.description(ue.getDescription());
        user.nickname(ue.getNickname());

        User newUser = client.auth().createUser(user);
        if (ue.getAdminSpaces() != null) {
            for (String graphspace : ue.getAdminSpaces()) {
                client.auth().addSpaceAdmin(ue.getName(), graphspace);
            }
        }

        if (newUser != null && ue.isSuperadmin()) {
            // add superadmin
            client.auth().addSuperAdmin(newUser.id().toString());
        }
    }

    public String addbatch(HugeClient client, MultipartFile csvFile) {
        Map<String, Object> csv = readCsvByCsvReader(multipartFileToFile(csvFile));
        List<Map<String, String>> createBatchBody = (List<Map<String, String>>) csv.get("data");
        Map<String, List<Map<String, String>>> result =
                client.auth().createBatch(createBatchBody);
        List<Map<String, String>> resultList = result.get("result");
        List<String> failedList = new ArrayList<>(createBatchBody.size());
        for (Map<String, String> entry: resultList) {
            if (!CREATE_SUCCESS.equals(entry.get("result"))) {
                failedList.add(entry.get("user_name"));
            }
        }
        if (!failedList.isEmpty()) {
            throw new RuntimeException("创建失败：" + failedList);
        }
        return "创建成功";
    }

    public File multipartFileToFile(MultipartFile multiFile) {
        long currentTime=System.currentTimeMillis();
        String fileName = multiFile.getOriginalFilename().concat(Long.toString(currentTime));
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        try {
            File file = File.createTempFile(fileName, prefix);
            multiFile.transferTo(file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> readCsvByCsvReader(File file) {
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
                for (int j = 0 ; j < arrList.get(0).length ; j++) {
                    map.put("" + headArray[j] + "", arrList.get(i)[j]);
                }
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        u.setAvatar(user.avatar());;
        u.setDescription(user.description());
        u.setCreate(user.createTime());
        u.setUpdate(user.updateTime());
        u.setCreator(user.creator());

        return u;
    }
    protected List<Map> getSpaceAndSpacenum(HugeClient hugeClient){
        AuthManager auth = hugeClient.auth();
        List<Map> listMap = new ArrayList<Map>();
        List<User> users = auth.listUsers();
        List<String> spaces = hugeClient.graphSpace().listGraphSpace();
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, List<String>> spaceMap = new HashMap<>();
        for (User user: users) {
            countMap.put(user.name(), 0);
            spaceMap.put(user.name(), new ArrayList());
        }

        for (String space: spaces) {
            List<String> spaceManagers =
                    hugeClient.auth().listSpaceAdmin(space);
            for (String spaceManager: spaceManagers) {
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
        User user = new User();
        user.setId(userEntity.getId());
        user.name(userEntity.getName());
        user.password(userEntity.getPassword());
        user.phone(userEntity.getPhone());
        user.email(userEntity.getEmail());
        user.description(userEntity.getDescription());
        user.nickname(userEntity.getNickname());
        updateAdminSpace(hugeClient, userEntity.getName(), userEntity.getAdminSpaces());

        // 设置超级管理员权限
        boolean curSuperAdmin = isSuperAdmin(hugeClient, user.id().toString());
        if (curSuperAdmin && !userEntity.isSuperadmin()) {
            hugeClient.auth().delSuperAdmin(user.id().toString());
        }
        if (!curSuperAdmin && userEntity.isSuperadmin()) {
            hugeClient.auth().addSuperAdmin(user.id().toString());
        }

        hugeClient.auth().updateUser(user);
    }

    public void updatePersonal(HugeClient hugeClient, String username, String nickname, String description){
        AuthManager auth = hugeClient.auth();
        User user = auth.getUser(username);
        user.nickname(nickname);
        user.description(description);
        user.password(null);
        hugeClient.auth().updateUser(user);
    }

    public Response updatepwd(HugeClient hugeClient, String username, String oldpwd, String newpwd) {
        Login login = new Login();
        login.name(username);
        login.password(oldpwd);
        try {
            hugeClient.auth().login(login);
        }
        catch (Exception e) {
            return Response.builder()
                    .status(Constant.STATUS_BAD_REQUEST)
                    .message(e.getMessage())
                    .cause(e.getCause())
                    .build();
        }
        User user = new User();
        user.name(username);
        user.password(newpwd);
        hugeClient.auth().updateUser(user);
        return Response.builder()
                .status(Constant.STATUS_OK)
                .build();
    }

    public List<String> listAdminSpace(HugeClient hugeClient, String username) {
        AuthManager auth = hugeClient.auth();
        List<User> users = auth.listUsers();
        List<String> spaces = hugeClient.graphSpace().listGraphSpace();
        List<String> adminspace = new ArrayList<String>();
        for (String space: spaces) {
            List<String> spaceManagers =
                    hugeClient.auth().listSpaceAdmin(space);
            for (String spaceManager: spaceManagers) {
                if (spaceManager.equals(username)) {
                    adminspace.add(space);
                }
            }
        }
        return adminspace;
    }

    public void updateAdminSpace(HugeClient hugeClient, String username, List<String> adminspaces) {
        if (adminspaces == null) {
            return ;
        }
        List<String> oldadminspaces = listAdminSpace(hugeClient, username);
        for(String adminspace : adminspaces) {
            if (!oldadminspaces.contains(adminspace)) {
                hugeClient.auth().addSpaceAdmin(username, adminspace);
            }
        }
        for(String oldadminspace : oldadminspaces) {
            if (!adminspaces.contains(oldadminspace)) {
                hugeClient.auth().delSpaceAdmin(username , oldadminspace);
            }
        }
    }


    public String userLevel(HugeClient client) {

        if (isSuperAdmin(client)) {
            return "ADMIN";
        }

        if (isSpaceAdmin(client)) {
            return "SPACEADMIN";
        }

        // Default: user
        return "USER";
    }

    public boolean isSuperAdmin(HugeClient client, String uid) {
        // Only used by superadmin
        // Check: if user is spaceadmin for any graphspace
        return client.auth().listSuperAdmin().contains(uid);
    }

    public boolean isSuperAdmin(HugeClient client) {
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
        // Check: if current user is spaceadmin
        return client.auth().isSpaceAdmin(graphSpace);
    }

    public boolean isSpaceAdmin(HugeClient client) {
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
        AuthManager auth = client.auth();

        return auth.listSpaceAdmin(graphSpace);
    }
}
