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

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.structure.auth.Role;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Log4j2
@Service
public class RoleService extends AuthService {

    public Role get(HugeClient client, String rid) {
        AuthManager auth = client.auth();
        Role role = auth.getRole(rid);
        if (role == null) {
            throw new ExternalException("auth.role.get.not-exist",
                    rid);
        }

        return role;
    }

    public List<Role> list(HugeClient client) {
        List<Role> roles = client.auth().listRoles();

        return roles;
    }

    public IPage<Role> queryPage(HugeClient client, String query,
                                  int pageNo, int pageSize) {
        ArrayList<Role> results = new ArrayList<>();
        client.auth().listRoles().stream().filter((g) -> g.nickname().contains(query))
              .forEach((g) -> {
                  results.add(g);
              });

        return PageUtil.page(results, pageNo, pageSize);
    }

    public Role update(HugeClient client, Role role) {
        AuthManager auth = client.auth();
        if (auth.getRole(role.id()) == null ) {
            throw new ExternalException("auth.role.not-exist",
                                        role.id(), role.name());
        }

        return auth.updateRole(role);
    }

    public Role insert(HugeClient client, Role role) {
        AuthManager auth = client.auth();

        return auth.createRole(role);
    }

    public void delete(HugeClient client, String rid) {
        AuthManager auth = client.auth();
        Role role = RoleService.getRole(auth, rid);

        auth.deleteRole(rid);

        auth.listAccessesByRole(role, -1).forEach(
            access -> {
                auth.deleteAccess(access.id());
            }
        );

        auth.listBelongsByRole(role, -1).forEach(
            belong -> {
                auth.deleteBelong(belong.id());
            }
        );
    }

    protected static Role getRole(AuthManager auth, String rid) {
        Role role = auth.getRole(rid);
        if (role == null) {
            throw new ExternalException("auth.role.not-exist",
                    rid);
        }
        return role;
    }
}
