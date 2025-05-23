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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hugegraph.driver.GremlinManager;
import org.apache.hugegraph.structure.gremlin.ResultSet;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GremlinRequest {

    // See org.apache.tinkerpop.gremlin.server.channel.HttpChannelizer
    public String gremlin;
    public Map<String, Object> bindings;
    public String language;
    public Map<String, String> aliases;

    public GremlinRequest(String gremlin) {
        this.gremlin = gremlin;
        this.bindings = new ConcurrentHashMap<>();
        this.language = "gremlin-groovy";
        this.aliases = new ConcurrentHashMap<>();
    }

    public static class Builder {

        private final GremlinRequest request;
        private final GremlinManager manager;

        public Builder(String gremlin, GremlinManager executor) {
            this.request = new GremlinRequest(gremlin);
            this.manager = executor;
        }

        public ResultSet execute() {
            return this.manager.execute(this.request);
        }

        public long executeAsTask() {
            return this.manager.executeAsTask(this.request);
        }

        public Builder binding(String key, Object value) {
            this.request.bindings.put(key, value);
            return this;
        }

        public Builder language(String language) {
            this.request.language = language;
            return this;
        }

        public Builder alias(String key, String value) {
            this.request.aliases.put(key, value);
            return this;
        }
    }
}
