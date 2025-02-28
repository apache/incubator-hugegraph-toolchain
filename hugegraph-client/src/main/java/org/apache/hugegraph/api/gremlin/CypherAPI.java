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
import org.apache.hugegraph.structure.gremlin.Response;

public class CypherAPI extends API {

    private static final String PATH = "graphspaces/%s/graphs/%s/cypher";

    public CypherAPI(RestClient client) {
        super(client);
    }

    @Override
    protected String type() {
        return PATH;
    }

    public Response post(String graphSpace, String graph, String cypher) {
        this.path(type(), graphSpace, graph);
        RestResult result = this.client.post(this.path(), cypher);
        return result.readObject(Response.class);
    }
}
