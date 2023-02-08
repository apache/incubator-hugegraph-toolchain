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

package org.apache.hugegraph.api.gremlin;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.gremlin.Response;

public class GremlinAPI extends API {

    public GremlinAPI(RestClient client) {
        super(client);
        this.path(type());
    }

    @Override
    protected String type() {
        return HugeType.GREMLIN.string();
    }

    public Response post(GremlinRequest request) {
        RestResult result = this.client.post(this.path(), request);
        return result.readObject(Response.class);
    }
}
