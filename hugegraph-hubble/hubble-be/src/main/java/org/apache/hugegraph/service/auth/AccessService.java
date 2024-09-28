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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hugegraph.structure.auth.HugePermission;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.log4j.Log4j2;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.auth.AccessEntity;
import org.apache.hugegraph.structure.auth.Role;
import org.apache.hugegraph.structure.auth.Target;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.exception.ExternalException;

@Log4j2
@Service
public class AccessService extends AuthService {

    @Autowired
    RoleService roleService;

    @Autowired
    TargetService targetService;

    public AccessEntity get(HugeClient client, String aid) {
        Access access = client.auth().getAccess(aid);
        if (access == null) {
            throw new ExternalException("auth.access.not-exist.id", aid);
        }

        Role role = this.roleService.get(client, access.role().toString());
        Target target = this.targetService
                            .get(client, access.target().toString());

        return convert(access, role, target);
    }

    private List<Access> list0(HugeClient client, String rid, String tid) {
        List<Access> result = new ArrayList<>();
        client.auth().listAccessesByRole(rid, -1).forEach(access -> {
            if (tid == null || access.target().toString().equals(tid)) {
                result.add(access);
            }
        });
        return result;
    }

    public List<AccessEntity> list(HugeClient client, String rid, String tid) {
        List<AccessEntity> result = new ArrayList<>();
        List<Access> accesses = list0(client, rid, tid);

        Multimap<ImmutableList<String>, Access> tmp =
                ArrayListMultimap.create();

        accesses.forEach(access -> {
            tmp.put(ImmutableList.of(access.role().toString(),
                                     access.target().toString()),
                    access);
        });

        for(ImmutableList<String> key: tmp.keySet()) {
            try {
                Role role = this.roleService.get(client, key.get(0));
                Target target = this.targetService.get(client, key.get(1));
                result.add(convert(tmp.get(key), role, target));
            } catch (Exception e) {
                log.warn("list access error", e);
            }
        }

        return result;
    }

    public AccessEntity addOrUpdate(HugeClient client, AccessEntity accessEntity) {
        List<Access> accesses = list0(client, accessEntity.getRoleId(),
                                      accessEntity.getTargetId());
        
        // CurrentPermission
        Set<HugePermission> curPermissions
                = accesses.stream().map(Access::permission)
                          .collect(Collectors.toSet());

        // Delete
        accesses.forEach(access -> {
            if(!accessEntity.getPermissions().contains(access.permission())) {
                client.auth().deleteAccess(access.id());
            }
        });

        // Add
        accessEntity.getPermissions().forEach(permission -> {
            if (!curPermissions.contains(permission)) {
                Access access = new Access();
                access.graphSpace(accessEntity.getGraphSpace());
                access.role(accessEntity.getRoleId());
                access.target(accessEntity.getTargetId());
                access.permission(permission);
                client.auth().createAccess(access);
            }
        });

        List<AccessEntity> results = list(client, accessEntity.getRoleId(),
                                          accessEntity.getTargetId());

        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    public void delete(HugeClient client, String rid, String tid) {
        list0(client, rid, tid).forEach(access -> {
            client.auth().deleteAccess(access.id());
        });
    }

    protected AccessEntity convert(Access access, Role role, Target target) {

        AccessEntity ae = new AccessEntity(target.id().toString(), target.name(),
                                           role.id().toString(), role.name(),
                                           target.graphSpace(), target.graph(),
                                           new HashSet<>(), target.description(),
                                           target.resources());

        ae.addPermission(access.permission());

        return ae;
    }

    protected AccessEntity convert(Collection<Access> accesses, Role role,
                                   Target target) {

        AccessEntity ae = new AccessEntity(target.id().toString(), target.name(),
                                           role.id().toString(), role.name(),
                                           target.graphSpace(), target.graph(),
                                           new HashSet<>(), target.description(),
                                           target.resources());
        accesses.forEach(access -> {
            ae.addPermission(access.permission());
        });

        return ae;
    }
}
