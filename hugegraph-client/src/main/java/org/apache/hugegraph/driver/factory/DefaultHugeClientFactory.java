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

package org.apache.hugegraph.driver.factory;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.util.E;

public class DefaultHugeClientFactory {

    private final String defaultHugeGraph = "hugegraph";
    private final String[] urls;

    public DefaultHugeClientFactory(String[] urls) {
        this.urls = urls;
    }

    public HugeClient createClient(String graphSpace, String graph) {
        return this.createClient(graphSpace, graph, 60);
    }

    public HugeClient createClient(String graphSpace, String graph,
                                   int timeout) {
        return this.createClient(graphSpace, graph, null, null, null, timeout);
    }

    public HugeClient createClient(String graphSpace, String graph,
                                   String token, String username,
                                   String password) {
        return createClient(graphSpace, graph, token, username, password, 60);
    }

    public HugeClient createClient(String graphSpace, String graph,
                                   String token, String username,
                                   String password, int timeout) {
        E.checkArgument(timeout > 0, "Client timeout must > 0");

        int r = (int) Math.floor(Math.random() * urls.length);
        String url = this.urls[r];

        HugeClient client = HugeClient.builder(url, graphSpace, graph)
                                      .configToken(token)
                                      .configUser(username, password)
                                      .configTimeout(timeout)
                                      .build();

        return client;
    }
}
