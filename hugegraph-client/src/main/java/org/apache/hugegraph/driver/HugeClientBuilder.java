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

import org.apache.hugegraph.util.E;

public class HugeClientBuilder {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_TIMEOUT = 20;
    private static final int DEFAULT_MAX_CONNS = 4 * CPUS;
    private static final int DEFAULT_MAX_CONNS_PER_ROUTE = 2 * CPUS;
    private static final int DEFAULT_IDLE_TIME = 30;

    private String url;
    private String graph;
    private String username;
    private String password;
    private int timeout;
    private int maxConns;
    private int maxConnsPerRoute;
    private int idleTime;
    private String trustStoreFile;
    private String trustStorePassword;

    public HugeClientBuilder(String url, String graph) {
        E.checkArgument(url != null && !url.isEmpty(),
                        "Expect a string value as the url parameter argument, but got: %s", url);
        E.checkArgument(graph != null && !graph.isEmpty(),
                        "Expect a string value as the graph name parameter argument, but got: %s",
                        graph);
        this.url = url;
        this.graph = graph;
        this.username = "";
        this.password = "";
        this.timeout = DEFAULT_TIMEOUT;
        this.maxConns = DEFAULT_MAX_CONNS;
        this.maxConnsPerRoute = DEFAULT_MAX_CONNS_PER_ROUTE;
        this.trustStoreFile = "";
        this.trustStorePassword = "";
        this.idleTime = DEFAULT_IDLE_TIME;
    }

    public HugeClient build() {
        E.checkArgument(this.url != null, "The url parameter can't be null");
        E.checkArgument(this.graph != null, "The graph parameter can't be null");
        return new HugeClient(this);
    }

    public HugeClientBuilder configGraph(String graph) {
        this.graph = graph;
        return this;
    }

    public HugeClientBuilder configIdleTime(int idleTime) {
        E.checkArgument(idleTime > 0,
                        "The idleTime parameter must be > 0, but got %s", idleTime);
        this.idleTime = idleTime;
        return this;
    }

    public HugeClientBuilder configPool(int maxConns, int maxConnsPerRoute) {
        if (maxConns == 0) {
            maxConns = DEFAULT_MAX_CONNS;
        }
        if (maxConnsPerRoute == 0) {
            maxConnsPerRoute = DEFAULT_MAX_CONNS_PER_ROUTE;
        }
        this.maxConns = maxConns;
        this.maxConnsPerRoute = maxConnsPerRoute;
        return this;
    }

    public HugeClientBuilder configSSL(String trustStoreFile, String trustStorePassword) {
        this.trustStoreFile = trustStoreFile;
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public HugeClientBuilder configTimeout(int timeout) {
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        this.timeout = timeout;
        return this;
    }

    public HugeClientBuilder configUrl(String url) {
        this.url = url;
        return this;
    }

    public HugeClientBuilder configUser(String username, String password) {
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        this.username = username;
        this.password = password;

        return this;
    }

    public String url() {
        return this.url;
    }

    public String graph() {
        return this.graph;
    }

    public String username() {
        return this.username;
    }

    public String password() {
        return this.password;
    }

    public int timeout() {
        return this.timeout;
    }

    public int maxConns() {
        return maxConns;
    }

    public int maxConnsPerRoute() {
        return this.maxConnsPerRoute;
    }

    public int idleTime() {
        return this.idleTime;
    }

    public String trustStoreFile() {
        return this.trustStoreFile;
    }

    public String trustStorePassword() {
        return this.trustStorePassword;
    }
}
