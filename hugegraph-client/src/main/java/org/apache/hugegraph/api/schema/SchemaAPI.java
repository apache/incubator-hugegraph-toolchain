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

package org.apache.hugegraph.api.schema;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.exception.NotSupportException;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.SchemaElement;

public class SchemaAPI extends API {

    private static final String PATH = "graphspaces/%s/graphs/%s/%s";

    public SchemaAPI(RestClient client, String graphSpace, String graph) {
        super(client);
        this.path(PATH, graphSpace, graph, this.type());
    }

    public Map<String, List<SchemaElement>> list() {
        return this.listJson();
    }

    public Map<String, String> listGroovy() {
        if (this.client.apiVersionLt("0.66")) {
            throw new NotSupportException("schema get api");
        }
        RestResult result = this.client.get(this.path(), ImmutableMap.of(
                "format", "groovy"));
        return result.readObject(Map.class);
    }

    public Map<String, Object> listGroovy(String format, boolean attachIdFlag) {
        if (this.client.apiVersionLt("0.66")) {
            throw new NotSupportException("schema get api");
        }
        RestResult result = this.client.get(this.path(), ImmutableMap.of(
                "format", format, "attachidflag", attachIdFlag));
        return result.readObject(Map.class);
    }

    public Map<String, List<SchemaElement>> listJson() {
        if (this.client.apiVersionLt("0.66")) {
            throw new NotSupportException("schema get api");
        }
        RestResult result = this.client.get(this.path(), ImmutableMap.of(
                "format", "json"));
        return result.readObject(Map.class);
    }

    @Override
    protected String type() {
        return "schema";
    }
}
