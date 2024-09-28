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

import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.structure.auth.Group;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class GroupService extends AuthService{
    @Autowired
    UserService userService;
    public Group get(HugeClient client, String gid) {
        AuthManager auth = client.auth();
        Group group = auth.getGroup(gid);
        if (group == null) {
            throw new ExternalException("auth.group.get.not-exist",
                    gid);
        }
        return group;
    }

    public List<Group> list(HugeClient client) {
        List<Group> groups = client.auth().listGroups();
        return groups;
    }

    public IPage<Group> queryPage(HugeClient client, String query,
                                  int pageNo, int pageSize) {
        ArrayList<Group> results = new ArrayList<>();
        client.auth().listGroups().stream().filter((g) -> g.nickname().contains(query))
                .forEach((g) -> {
                    results.add(g);
                });

        return PageUtil.page(results, pageNo, pageSize);
    }

    public Group update(HugeClient client, Group group) {
        AuthManager auth = client.auth();
        if (auth.getGroup(group.id()) == null ) {
            throw new ExternalException("auth.group.not-exist",
                    group.id(), group.name());
        }
        return auth.updateGroup(group);
    }

    public Group insert(HugeClient client, Group group) {
        AuthManager auth = client.auth();
        return auth.createGroup(group);
    }

    public void delete(HugeClient client, String gid) {
        AuthManager auth = client.auth();
        auth.deleteGroup(gid);
    }

    public Map<String, Object> batch(HugeClient client, String gid , Map<String, Object> action) {
        AuthManager auth = client.auth();
        return auth.batchGroup(gid, action);
    }
}
