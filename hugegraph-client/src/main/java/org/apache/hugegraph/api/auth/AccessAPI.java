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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.auth.Access;
import org.apache.hugegraph.structure.constant.HugeType;

public class AccessAPI extends AuthAPI {

    public AccessAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.ACCESS.string();
    }

    public Access create(Access access) {
        RestResult result = this.client.post(this.path(), access);
        return result.readObject(Access.class);
    }

    public Access get(Object id) {
        RestResult result = this.client.get(this.path(), formatRelationId(id));
        return result.readObject(Access.class);
    }

    public List<Access> list(Object group, Object target, int limit) {
        checkLimit(limit, "Limit");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", limit);
        params.put("group", formatEntityId(group));
        params.put("target", formatEntityId(target));
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), Access.class);
    }

    public Access update(Access access) {
        String id = formatRelationId(access.id());
        RestResult result = this.client.put(this.path(), id, access);
        return result.readObject(Access.class);
    }

    public void delete(Object id) {
        this.client.delete(this.path(), formatRelationId(id));
    }
}
