/*
 * Copyright 2017 HugeGraph Authors
 *
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

package com.baidu.hugegraph.driver;

import javax.ws.rs.ProcessingException;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.util.VersionUtil;
import com.baidu.hugegraph.version.ClientVersion;

public class HugeClient {

    private static final int DEFAULT_TIMEOUT = 20;

    static {
        ClientVersion.check();
    }

    private RestClient restClient;

    private VersionManager version;
    @SuppressWarnings("unused")
    private GraphsManager graphs;
    private SchemaManager schema;
    private GraphManager graph;
    private GremlinManager gremlin;
    private VariablesManager variables;

    public HugeClient(String url, String graph) {
        this(url, graph, DEFAULT_TIMEOUT);
    }

    public HugeClient(String url, String graph, int timeout) {
        try {
            this.restClient = new RestClient(url, timeout);
        } catch (ProcessingException e) {
            throw new ServerException("Failed to connect url '%s'", url);
        }

        // Check hugegraph-server api version
        this.version = new VersionManager(this.restClient);
        this.checkServerApiVersion();

        this.graphs = new GraphsManager(this.restClient);
        this.schema = new SchemaManager(this.restClient, graph);
        this.graph = new GraphManager(this.restClient, graph);
        this.gremlin = new GremlinManager(this.restClient, graph);
        this.variables = new VariablesManager(this.restClient, graph);
    }

    /**
     * TODO: Need to add some unit test
     */
    private void checkServerApiVersion() {
        VersionUtil.Version apiVersion = VersionUtil.Version.of(
                                         this.version.getApiVersion());
        VersionUtil.check(apiVersion, "0.14", "0.15",
                          "hugegraph-api in server");
    }

    public SchemaManager schema() {
        return this.schema;
    }

    public GraphManager graph() {
        return this.graph;
    }

    public VariablesManager variables() {
        return this.variables;
    }

    public GremlinManager gremlin() {
        return this.gremlin;
    }
}
