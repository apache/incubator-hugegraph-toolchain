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

package org.apache.hugegraph.api.auth;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.auth.User;
import org.apache.hugegraph.structure.auth.User.UserRole;
import org.apache.hugegraph.structure.constant.HugeType;

import com.google.common.collect.ImmutableMap;

public class UserAPI extends AuthAPI {

    public UserAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.USER.string();
    }

    public User create(User user) {
        RestResult result = this.client.post(this.path(), user);
        return result.readObject(User.class);
    }

    public User get(Object id) {
        RestResult result = this.client.get(this.path(), formatEntityId(id));
        return result.readObject(User.class);
    }

    public UserRole getUserRole(Object id) {
        String idEncoded = RestClient.encode(formatEntityId(id));
        String path = String.join("/", this.path(), idEncoded, "role");
        RestResult result = this.client.get(path);
        return result.readObject(UserRole.class);
    }

    public List<User> list(int limit) {
        checkLimit(limit, "Limit");
        Map<String, Object> params = ImmutableMap.of("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), User.class);
    }

    public User update(User user) {
        String id = formatEntityId(user.id());
        RestResult result = this.client.put(this.path(), id, user);
        return result.readObject(User.class);
    }

    public void delete(Object id) {
        this.client.delete(this.path(), formatEntityId(id));
    }
}
