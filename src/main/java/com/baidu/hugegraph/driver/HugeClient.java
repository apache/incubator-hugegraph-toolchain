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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.driver;

import com.baidu.hugegraph.util.VersionUtil;
import com.baidu.hugegraph.version.ClientVersion;

public class HugeClient {

    static {
        ClientVersion.check();
    }

    private VersionManager version;
    private GraphsManager graphs;
    private SchemaManager schema;
    private GraphManager graph;
    private GremlinManager gremlin;

    public HugeClient(String url, String graph) {
        // Check hugegraph-server api version
        this.version = new VersionManager(url);
        this.checkServerApiVersion();

        this.graphs = new GraphsManager(url);
        this.schema = new SchemaManager(url, graph);
        this.graph = new GraphManager(url, graph);
        this.gremlin = new GremlinManager(url, graph);
    }

    public static HugeClient open(String url, String name) {
        return new HugeClient(url, name);
    }

    private void checkServerApiVersion() {
        VersionUtil.Version apiVersion = VersionUtil.Version.of(
                                         this.version.getApiVersion());
        VersionUtil.check(apiVersion, "0.3", "0.4", "hugegraph-api");
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

}
