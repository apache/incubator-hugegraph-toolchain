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

package org.apache.hugegraph.driver;

import org.apache.hugegraph.api.variables.VariablesAPI;
import org.apache.hugegraph.client.RestClient;

import java.util.Map;

public class VariablesManager {

    private VariablesAPI variablesAPI;

    public VariablesManager(RestClient client, String graph) {
        this.variablesAPI = new VariablesAPI(client, graph);
    }

    public Map<String, Object> get(String key) {
        return this.variablesAPI.get(key);
    }

    public Map<String, Object> set(String key, Object value) {
        return this.variablesAPI.set(key, value);
    }

    public void remove(String key) {
        this.variablesAPI.remove(key);
    }

    public Map<String, Object> all() {
        return this.variablesAPI.all();
    }
}
