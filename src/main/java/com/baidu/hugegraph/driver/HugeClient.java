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
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.util.VersionUtil;
import com.baidu.hugegraph.version.ClientVersion;

public class HugeClient implements Closeable {

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
    private VariablesManager variable;
    private JobManager job;
    private TaskManager task;
    private AuthManager auth;
    private MetricsManager metrics;

    public HugeClient(HugeClientBuilder builder) {
        try {
            this.client = new RestClient(builder.url(),
                                         builder.username(),
                                         builder.password(),
                                         builder.timeout(),
                                         builder.maxConns(),
                                         builder.maxConnsPerRoute(),
                                         builder.protocol(),
                                         builder.trustStoreFile(),
                                         builder.trustStorePassword());
        } catch (ProcessingException e) {
            throw new ClientException("Failed to connect url '%s'", builder.url());
        }
        try {
            this.initManagers(this.client, builder.graph());
        } catch (Throwable e) {
            this.client.close();
            throw e;
        }
    }

    public static HugeClientBuilder builder(String url, String graph) {
        return new HugeClientBuilder(url, graph);
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
        this.variable = new VariablesManager(client, graph);
        this.job = new JobManager(client, graph);
        this.task = new TaskManager(client, graph);
        this.auth = new AuthManager(client, graph);
        this.metrics = new MetricsManager(client);
    }

    private void checkServerApiVersion() {
        VersionUtil.Version apiVersion = VersionUtil.Version.of(
                                         this.version.getApiVersion());
        VersionUtil.check(apiVersion, "0.38", "0.59",
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
        return this.variable;
    }

    public JobManager job() {
        return this.job;
    }

    public TaskManager task() {
        return this.task;
    }

    public AuthManager auth() {
        return this.auth;
    }

    public MetricsManager metrics() {
        return this.metrics;
    }
}
