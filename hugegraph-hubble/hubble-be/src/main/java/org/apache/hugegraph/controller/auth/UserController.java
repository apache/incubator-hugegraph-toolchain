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

package org.apache.hugegraph.controller.auth;

import com.google.common.collect.ImmutableMap;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.PasswordEntity;
import org.apache.hugegraph.entity.auth.UserEntity;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.util.HubbleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(Constant.API_VERSION + "auth/users")
public class UserController extends BaseController {

    @Autowired
    UserService userService;

    Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("list")
    public Object list() {
        List<UserEntity> users = this.userService.listUsers(
                this.authClient(null, null));
        return ImmutableMap.of("users", users);
    }

    @GetMapping
    public Object queryPage(@RequestParam(name = "query", required = false,
            defaultValue = "") String query,
                            @RequestParam(name = "page_no", required = false,
                                    defaultValue = "1") int pageNo,
                            @RequestParam(name = "page_size", required = false,
                                    defaultValue = "10") int pageSize) {
        return userService.queryPage(this.authClient(null, null),
                query, pageNo, pageSize);
    }

    @PostMapping
    public void create(@RequestBody UserEntity userEntity) {
        HugeClient client = this.authClient(null, null);
        userService.add(client, userEntity);
    }

    @PostMapping("batch")
    public void createbatch(@RequestParam("file") MultipartFile csvfile) {
        HugeClient client = this.authClient(null, null);
        userService.addbatch(client, csvfile);
    }


    @GetMapping("{id}")
    public Object get(@PathVariable("id") String id) {
        return userService.get(this.authClient(null, null),
                                    id);
    }

    @PutMapping("{id}")
    public void update(@PathVariable("id") String id,
                       @RequestBody UserEntity userEntity) {
        userEntity.setId(id);
        userService.update(this.authClient(null, null), userEntity);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable("id") String id) {
        userService.delete(this.authClient(null, null), id);
    }

    @PostMapping("updatepwd")
    public Response updatepwd(@RequestBody PasswordEntity pwd) {
        HugeClient client = this.authClient(null, null);
        return userService.updatepwd(client, pwd.getUsername(), pwd.getOldpwd(), pwd.getNewpwd());
    }

    @GetMapping("listadminspace/{username}")
    public List<String> listadminspace(@PathVariable("username") String username) {
        HugeClient client = this.authClient(null, null);
        return userService.listAdminSpace(client, username);
    }

    @PostMapping("updateadminspace/{username}")
    public void updateadminspace(@PathVariable("username") String username,
                                 @RequestBody List<String> adminspaces) {
        HugeClient client = this.authClient(null, null);
        userService.updateAdminSpace(client, username, adminspaces);
    }

    @GetMapping("updatepersonal")
    public void updatepersonal(@RequestParam(name = "nickname") String nickname,
                               @RequestParam(name = "description", required = false,
                                       defaultValue = "") String description) {
        userService.updatePersonal(this.authClient(null, null), getUser() , nickname , description);
    }
    @GetMapping("getpersonal")
    public Object getpersonal() {
        return userService.getpersonal(this.authClient(null, null),
                getUser());
    }

    @GetMapping("super")
    public Object getSuperList(@RequestParam(name = "page_no", required = false,
            defaultValue = "1") int pageNo,
                               @RequestParam(name = "page_size", required = false,
                                       defaultValue = "10") int pageSize) {
        return userService.superQueryPage(this.authClient(null, null), "", pageNo, pageSize);
    }

    @PostMapping("super")
    public void addSuper(@RequestParam(name = "usernames", required = true) String usernames,
                         @RequestParam(name = "nicknames", required = false) String nicknames,
                         @RequestParam(name = "description", required = false) String description) {
        List<String> usernameList = Arrays.asList(usernames.split(","));
        List<String> nicknameList = Arrays.asList(nicknames.split(","));

        for (int i = 0; i < usernameList.size(); i++) {
            String username = usernameList.get(i);
            String nickname = nicknameList.get(i);
            this.updateUserAuth(username, nickname, description, null, true);
        }
    }

    @PostMapping("uuap")
    public void addUuap(@RequestParam(name = "usernames", required = true) String usernames,
                        @RequestParam(name = "nicknames", required = false) String nicknames,
                        @RequestParam(name = "description", required = false) String description,
                        @RequestParam(name = "adminSpaces", required = false) String adminSpaces) {
        List<String> usernameList = Arrays.asList(usernames.split(","));
        List<String> nicknameList = Arrays.asList(nicknames.split(","));
        List<String> adminSpacesList = null;

        if (adminSpaces != null) {
            adminSpacesList = Arrays.asList(adminSpaces.split(","));
        }

        for (int i = 0; i < usernameList.size(); i++) {
            String username = usernameList.get(i);
            String nickname = nicknameList.get(i);
            this.updateUserAuth(username, nickname, description, adminSpacesList, false);
        }
    }

    private void updateUserAuth(String username, String nickname, String description,
                                List<String> adminspaces, boolean isAdmin) {
        UserEntity user = null;
        try {
            user = userService.getUser(this.authClient(null, null), username);
        } catch (Exception e) {
            logger.error(String.format("user '%s' not exist", username), e);
        }
        HugeClient client = this.authClient(null, null);

        // 不存在则先创建, 存在则直接设置权限
        if (user == null) {
            UserEntity userData = new UserEntity();
            userData.setName(username);
            userData.setPassword(HubbleUtil.md5Secret(username));
            userData.setNickname(nickname);
            userData.setSuperadmin(isAdmin);
            userData.setDescription(description);
            if (adminspaces != null && !adminspaces.isEmpty()) {
                userData.setAdminSpaces(adminspaces);
                userService.add(this.authClient(null, null), userData);
            }
            logger.info(String.format(
                    "user not exist, have been created [%s], super admin =[%s]",
                    username, isAdmin));
        } else {
            updateUserAuthIfNeed(client, user, isAdmin, adminspaces);
        }
    }

    @DeleteMapping("super/{id}")
    public void deleteSuper(@PathVariable("id") String id){
        try {
            UserEntity user = userService.getUser(this.authClient(null, null), id);
            user.setSuperadmin(false);
            userService.update(this.authClient(null, null), user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserAuthIfNeed(HugeClient client, UserEntity user,
                                      boolean isAdmin, List<String> adminSpaces) {
        // 判断是否为超级管理员
        if (isAdmin && !userService.isSuperAdmin(client, user.getId())) {
            client.auth().addSuperAdmin(user.getId());
        }
        if (adminSpaces != null && !adminSpaces.isEmpty()) {
            for (String space : adminSpaces) {
                try {
                    client.auth().addSpaceAdmin(user.getId(), space);
                } catch (Exception e) {
                    logger.warn(String.format("user '%s' add space '%s' error",
                                              user.getId(), space), e);
                }
            }
        }
    }

}
