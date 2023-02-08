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

package org.apache.hugegraph.manager;

import java.util.Map;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.base.ToolClient;
import org.apache.hugegraph.base.ToolManager;
import org.apache.hugegraph.structure.gremlin.ResultSet;

public class GremlinManager extends ToolManager {

    public GremlinManager(ToolClient.ConnectionInfo info) {
        super(info, "gremlin");
    }

    public ResultSet execute(String gremlin, Map<String, String> bindings,
                             String language, Map<String, String> aliases) {
        GremlinRequest.Builder builder = this.client.gremlin().gremlin(gremlin);
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            builder.alias(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            builder.binding(entry.getKey(), entry.getValue());
        }
        builder.language(language);
        return builder.execute();
    }

    public long executeAsTask(String gremlin, Map<String, String> bindings,
                              String language) {
        GremlinRequest.Builder builder = this.client.gremlin().gremlin(gremlin);
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            builder.binding(entry.getKey(), entry.getValue());
        }
        builder.language(language);
        return builder.executeAsTask();
    }
}
