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

import java.io.Closeable;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.rest.RestClientConfig;
import org.apache.hugegraph.util.VersionUtil;
import org.apache.hugegraph.version.ClientVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HugeClient class is the main entry point for interacting with a HugeGraph server.
 * It provides methods for managing graphs, schemas, jobs, tasks, and other resources.
 * It also implements the Closeable interface, so it can be used in a try-with-resources statement.
 */
public class HugeClient implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    static {
        ClientVersion.check();
    }

    private final RestClient client;
    private final boolean borrowedClient;
    protected String graphSpaceName;
    protected String graphName;
    protected GraphSpaceManager graphSpace;
    private VersionManager version;
    private GraphsManager graphs;
    private SchemaManager schema;
    private GraphManager graph;
    private GremlinManager gremlin;
    private CypherManager cypher;
    private TraverserManager traverser;
    private VariablesManager variable;
    private JobManager job;
    private TaskManager task;
    //protected ComputerManager computer;
    private AuthManager auth;
    private MetricsManager metrics;
    //protected ServiceManager serviceManager;
    //protected SchemaTemplateManager schemaTemplageManager;
    //protected PDManager pdManager;
    //protected HStoreManager hStoreManager;
    //protected WhiteIpListManager whiteIpListManager;
    //protected VermeerManager vermeerManager;

    /**
     * Constructs a new HugeClient using the provided builder.
     *
     * @param builder the HugeClientBuilder to use for configuration
     */
    public HugeClient(HugeClientBuilder builder) {
        this.borrowedClient = false;
        this.graphSpaceName = builder.graphSpace();
        this.graphName = builder.graph();
        RestClientConfig config;
        try {
            config = RestClientConfig.builder()
                                     //.token(builder.token())
                                     .user(builder.username())
                                     .password(builder.password())
                                     .timeout(builder.timeout())
                                     .connectTimeout(builder.connectTimeout())
                                     .readTimeout(builder.readTimeout())
                                     .maxConns(builder.maxConns())
                                     .maxConnsPerRoute(builder.maxConnsPerRoute())
                                     .trustStoreFile(builder.trustStoreFile())
                                     .trustStorePassword(builder.trustStorePassword())
                                     .builderCallback(builder.httpBuilderConsumer())
                                     .build();
            this.client = new RestClient(builder.url(), config);
        } catch (Exception e) {
            LOG.warn("Failed to create RestClient instance", e);
            throw new ClientException("Failed to connect url '%s'", builder.url());
        }

        try {
            this.initManagers(this.client, builder.graphSpace(), builder.graph());
        } catch (Throwable e) {
            // TODO: catch some exception(like IO/Network related) rather than throw Throwable
            this.client.close();
            throw e;
        }
    }

    // QUESTION: add gs to method param?
    public HugeClient(HugeClient client, String graphSpace, String graph) {
        this.borrowedClient = true;
        this.client = client.client;
        this.initManagers(this.client, graphSpace, graph);
    }

    public static HugeClientBuilder builder(String url, String graphSpace, String graph) {
        return new HugeClientBuilder(url, graphSpace, graph);
    }

    public HugeClient assignGraph(String graphSpace, String graph) {
        this.graphSpaceName = graphSpace;
        this.graphName = graph;
        this.initManagers(this.client, this.graphSpaceName, this.graphName);
        return this;
    }

    @Override
    public void close() {
        if (!this.borrowedClient) {
            this.client.close();
        }
    }

    public void initManagers(RestClient client, String graphSpace,
                             String graph) {
        assert client != null;
        // Check hugegraph-server api version
        this.version = new VersionManager(client);
        this.checkServerApiVersion();

        this.graphs = new GraphsManager(client);
        this.schema = new SchemaManager(client, graph);
        this.graph = new GraphManager(client, graph);
        this.gremlin = new GremlinManager(client, graph, this.graph);
        this.cypher = new CypherManager(client, graph, this.graph);
        this.traverser = new TraverserManager(client, this.graph);
        this.variable = new VariablesManager(client, graph);
        this.job = new JobManager(client, graph);
        this.task = new TaskManager(client, graph);
        this.auth = new AuthManager(client, graph);
        this.metrics = new MetricsManager(client);
        this.graphSpace = new GraphSpaceManager(client);
        //this.schemaTemplageManager = new SchemaTemplateManager(client, graphSpace);
        //this.serviceManager = new ServiceManager(client, graphSpace);
        //this.pdManager = new PDManager(client);
        //this.hStoreManager = new HStoreManager(client);
        //this.whiteIpListManager = new WhiteIpListManager(client);
        //this.vermeerManager = new VermeerManager(client);
    }

    private void checkServerApiVersion() {
        VersionUtil.Version apiVersion = VersionUtil.Version.of(this.version.getApiVersion());
        // TODO: find a way to keep the range of api version correct automatically
        //       0.81 equals to the {latest_api_version} +10
        VersionUtil.check(apiVersion, "0.38", "0.81", "hugegraph-api in server");
        this.client.apiVersion(apiVersion);
    }

    public String getGraphSpaceName() {
        return graphSpaceName;
    }

    public String getGraphName() {
        return graphName;
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

    public CypherManager cypher() {
        return this.cypher;
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

    //public ComputerManager computer() {
    //    return this.computer;
    //}

    public TaskManager task() {
        return this.task;
    }

    public AuthManager auth() {
        return this.auth;
    }

    public MetricsManager metrics() {
        return this.metrics;
    }

    public GraphSpaceManager graphSpace() {
        return this.graphSpace;
    }

    //public WhiteIpListManager whiteIpListManager(){
    //    return this.whiteIpListManager;
    //}
    //
    //public VermeerManager vermeer(){
    //    return this.vermeerManager;
    //}
    //
    //public SchemaTemplateManager schemaTemplateManager() {
    //    return this.schemaTemplageManager;
    //}
    //
    //public ServiceManager serviceManager() {
    //    return this.serviceManager;
    //}
    //
    //public PDManager pdManager() {
    //    return pdManager;
    //}
    //
    //public HStoreManager hStoreManager() {
    //    return hStoreManager;
    //}

    public VersionManager versionManager() {
        return version;
    }

    public String getAuthContext() {
        return this.client.getAuthContext();
    }

    public void setAuthContext(String auth) {
        this.client.setAuthContext(auth);
    }

    public void resetAuthContext() {
        this.client.resetAuthContext();
    }
}
