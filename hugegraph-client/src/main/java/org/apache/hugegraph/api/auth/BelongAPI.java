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

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.auth.AuthElement;
import org.apache.hugegraph.structure.auth.Belong;
import org.apache.hugegraph.structure.constant.HugeType;

public class BelongAPI extends AuthAPI {

    public BelongAPI(RestClient client, String graphSpace) {
        super(client, graphSpace);
    }

    @Override
    protected String type() {
        return HugeType.BELONG.string();
    }

    public Belong create(Belong belong) {
        Object obj = this.checkCreateOrUpdate(belong);
        RestResult result = this.client.post(this.path(), obj);
        return result.readObject(Belong.class);
    }

    public Belong get(Object id) {
        RestResult result = this.client.get(this.path(), formatRelationId(id));
        return result.readObject(Belong.class);
    }

    public List<Belong> list(Object user, Object role, int limit) {
        checkLimit(limit, "Limit");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", limit);
        params.put("user", formatEntityId(user));
        params.put("role", formatEntityId(role));
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), Belong.class);
    }

    public Belong update(Belong belong) {
        String id = formatRelationId(belong.id());
        Object obj = this.checkCreateOrUpdate(belong);
        RestResult result = this.client.put(this.path(), id, obj);
        return result.readObject(Belong.class);
    }

    public void delete(Object id) {
        this.client.delete(this.path(), formatRelationId(id));
    }

    @Override
    protected Object checkCreateOrUpdate(AuthElement authElement) {
        Belong belong = (Belong) authElement;
        return belong.switchReq();
    }
}
