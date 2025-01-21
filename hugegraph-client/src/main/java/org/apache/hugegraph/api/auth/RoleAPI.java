/*
 * Copyright 2017 HugeGraph Authors
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

package org.apache.hugegraph.api.auth;

import com.google.common.collect.ImmutableMap;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.auth.AuthElement;
import org.apache.hugegraph.structure.auth.Role;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.List;
import java.util.Map;


public class RoleAPI extends AuthAPI {


    public RoleAPI(RestClient client, String graphSpace) {
        super(client, graphSpace);
    }

    @Override
    protected String type() {
        return (HugeType.ROLE.string());
    }

    public Role create(Role role){
        Object obj = this.checkCreateOrUpdate(role);
        RestResult result = this.client.post(this.path(), obj);
        return result.readObject(Role.class);
    }

    public Role update(Role role) {
        String id = formatEntityId(role.id());
        Object obj = this.checkCreateOrUpdate(role);
        RestResult result = this.client.put(this.path(), id, obj);
        return result.readObject(Role.class);
    }

    public Role get(Object id) {
        RestResult result = this.client.get(this.path(), formatEntityId(id));
        return result.readObject(Role.class);
    }

    public List<Role> list(int limit) {
        checkLimit(limit, "Limit");
        Map<String, Object> params = ImmutableMap.of("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), Role.class);
    }

    public void delete(Object id) {
        this.client.delete(this.path(), formatEntityId(id));
    }


    @Override
    protected Object checkCreateOrUpdate(AuthElement authElement) {
        Role role = (Role) authElement;
        return role.switchReq();
    }
}
