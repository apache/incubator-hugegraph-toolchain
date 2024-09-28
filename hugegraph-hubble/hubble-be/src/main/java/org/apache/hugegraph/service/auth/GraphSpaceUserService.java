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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.entity.auth.RoleEntity;
import org.apache.hugegraph.structure.auth.User;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.BelongEntity;
import org.apache.hugegraph.entity.auth.UserView;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.PageUtil;

@Log4j2
@Service
public class GraphSpaceUserService extends AuthService{

    @Autowired
    private BelongService belongService;
    @Autowired
    private RoleService roleService;

    public List<UserView> listUsers(HugeClient client) {

        List<UserView> uvs = new ArrayList<>();

        List<BelongEntity> belongs = new ArrayList<>();
        roleService.list(client).forEach((g) -> {
            belongs.addAll(belongService.listByRole(client, g.id().toString()));
        });

        Multimap<String, BelongEntity> tmp = ArrayListMultimap.create();
        belongs.forEach(belong -> {
            tmp.put(belong.getUserId(), belong);
        });

        tmp.keySet().forEach((k) -> {
            UserView uv = new UserView(null, null, new ArrayList<RoleEntity>());
            tmp.get(k).forEach((b) -> {
                uv.setId(b.getUserId());
                uv.setName(b.getUserName());
                uv.addRole(new RoleEntity(b.getRoleId(), b.getRoleName()));
            });
            uvs.add(uv);
        });

        return uvs;
    }

    public UserView getUser(HugeClient client, String uid) {

        List<UserView> uvs = new ArrayList<>();

        List<BelongEntity> belongs = belongService.listByUser(client, uid);

        UserView uv = new UserView(null, null,
                                   new ArrayList<RoleEntity>(belongs.size()));
        belongs.forEach((b) -> {
            uv.setId(b.getUserId());
            uv.setName(b.getUserName());
            uv.addRole(new RoleEntity(b.getRoleId(), b.getRoleName()));
        });

        return uv;
    }

    public IPage<UserView> queryPage(HugeClient client, String query,
                                     int pageNo, int pageSize) {
        List<UserView> users = listUsers(client);
        List<UserView> results =
                users.stream()
                     .filter((u) -> u.getName().contains(query))
                     .sorted(Comparator.comparing(UserView::getName))
                     .collect(Collectors.toList());

        return PageUtil.page(results, pageNo, pageSize);
    }

    public UserView createOrUpdate(HugeClient client, UserView userView) {
        E.checkNotNull(userView.getId(), "User Id Not Null");
        E.checkArgument(userView.getRoles() != null
                                && userView.getRoles().size() > 0
                , "THe role info is empty");

        // Delete
        Set<String> newRoles = userView.getRoles().stream()
                                     .map(RoleEntity::getId)
                                     .collect(Collectors.toSet());
        belongService.listByUser(client, userView.getId())
                     .forEach(belongEntity -> {
                         if (!newRoles.contains(belongEntity.getRoleId())) {
                             client.auth().deleteBelong(belongEntity.getId());
                         }
                     });

        // Create
        userView.getRoles().forEach((g -> {
            if(!belongService.exists(client, g.getId(), userView.getId())) {
                Belong belong = new Belong();
                belong.user(userView.getId());
                belong.role(g.getId());
                belongService.add(client, belong);
            }
        }));

        return getUser(client, userView.getId());
    }

    public void unauthUser(HugeClient client, String uid) {
        List<BelongEntity> belongs = belongService.listByUser(client, uid);
        E.checkState(!belongs.isEmpty(), "The user: (%s) not exists", uid);
        belongs.forEach((b) -> {
            belongService.delete(client, b.getId().toString());
        });
    }


    /**
     * Page to get the list of space administrators
     */
    public IPage<User> querySpaceAdmins(HugeClient client, String graphSpace,
                                        String query, int pageNo,
                                        int pageSize) {
        List<User> spaceAdminUsers = getSpaceAdmins(client, graphSpace);

        List<User> spaceAdmins =
                spaceAdminUsers.stream()
                               .filter((u) -> u.name().contains(query))
                               .sorted(Comparator.comparing((u) -> u.name()))
                               .collect(Collectors.toList());
        return PageUtil.page(spaceAdmins, pageNo, pageSize);
    }

    /**
     *  Get space admins from server, server return users do not contain department  and image url info
     */
    private List<User> getSpaceAdmins(HugeClient client, String graphSpace) {
        List<String> spaceAdmins = client.auth().listSpaceAdmin(graphSpace);
        ArrayList<User> spaceAdminUser = new ArrayList<>();
        for (String spaceAdmin : spaceAdmins) {
            spaceAdminUser.add(client.auth().getUser(spaceAdmin));
        }
        return spaceAdminUser;
    }
}
