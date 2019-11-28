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

import java.io.Closeable;

import javax.ws.rs.ProcessingException;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.util.VersionUtil;
import com.baidu.hugegraph.version.ClientVersion;

public class HugeClient implements Closeable {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    private static final int DEFAULT_TIMEOUT = 20;
    private static final int DEFAULT_MAX_CONNS_PER_ROUTE = 2 * CPUS;
    private static final int DEFAULT_MAX_CONNS = 4 * CPUS;

    static {
        ClientVersion.check();
    }

    private final RestClient client;

    private VersionManager version;
    private GraphsManager graphs;
    private SchemaManager schema;
    private GraphManager graph;
    private GremlinManager gremlin;
    private TraverserManager traverser;
    private VariablesManager variables;
    private JobManager job;
    private TaskManager task;

    public HugeClient(String url, String graph) {
        this(url, graph, DEFAULT_TIMEOUT);
    }

    public HugeClient(String url, String graph, int timeout) {
        this(url, graph, timeout, DEFAULT_MAX_CONNS,
             DEFAULT_MAX_CONNS_PER_ROUTE);
    }

    public HugeClient(String url, String graph, int timeout,
                      int maxConns, int maxConnsPerRoute) {
        try {
            this.client = new RestClient(url, timeout, maxConns,
                                         maxConnsPerRoute);
        } catch (ProcessingException e) {
            throw new ServerException("Failed to connect url '%s'", url);
        }
        this.initManagers(this.client, graph);
    }

    public HugeClient(String url, String graph,
                      String username, String password) {
        this(url, graph, username, password, DEFAULT_TIMEOUT);
    }

    public HugeClient(String url, String graph,
                      String username, String password,
                      int timeout) {
        this(url, graph, username, password, timeout, DEFAULT_MAX_CONNS,
             DEFAULT_MAX_CONNS_PER_ROUTE);
    }

    public HugeClient(String url, String graph,
                      String username, String password,
                      int timeout, int maxConns, int maxConnsPerRoute) {
        try {
            this.client = new RestClient(url, username, password, timeout,
                                         maxConns, maxConnsPerRoute);
        } catch (ProcessingException e) {
            throw new ServerException("Failed to connect url '%s'", url);
        }
        this.initManagers(this.client, graph);
    }

    @Override
    public void close() {
        this.client.close();
    }

    private void initManagers(RestClient client, String graph) {
        assert client != null;
        // Check hugegraph-server api version
        this.version = new VersionManager(client);
        this.checkServerApiVersion();

        this.graphs = new GraphsManager(client);
        this.schema = new SchemaManager(client, graph);
        this.graph = new GraphManager(client, graph);
        this.gremlin = new GremlinManager(client, graph, this.graph);
        this.traverser = new TraverserManager(client, this.graph);
        this.variables = new VariablesManager(client, graph);
        this.job = new JobManager(client, graph);
        this.task = new TaskManager(client, graph);
    }

    private void checkServerApiVersion() {
        VersionUtil.Version apiVersion = VersionUtil.Version.of(
                                         this.version.getApiVersion());
        VersionUtil.check(apiVersion, "0.38", "0.50",
                          "hugegraph-api in server");
        this.client.apiVersion(apiVersion);
    }

    public GraphsManager graphs() {
        return this.graphs;
    }

    public SchemaManager schema() {
        return this.schema;
    }

    public GraphManager graph() {
        return this.graph;
    }

    public GremlinManager gremlin() {
        return this.gremlin;
    }

    public TraverserManager traverser() {
        return this.traverser;
    }

    public VariablesManager variables() {
        return this.variables;
    }

    public JobManager job() {
        return this.job;
    }

    public TaskManager task() {
        return this.task;
    }
}
