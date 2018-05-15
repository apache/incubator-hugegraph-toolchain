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

package com.baidu.hugegraph.cmd.manager;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.GremlinManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.driver.TraverserManager;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ToolClient {

    private HugeClient client;
    private ObjectMapper mapper;

    public ToolClient(String url, String graph) {
        this.client = new HugeClient(url, graph);
        this.mapper = new ObjectMapper();
    }

    public ToolClient(String url, String graph,
                      String username, String password) {
        this.client = new HugeClient(url, graph, username, password);
        this.mapper = new ObjectMapper();
    }

    public TraverserManager traverser() {
        return this.client.traverser();
    }

    public GraphManager graph() {
        return this.client.graph();
    }

    public SchemaManager schema() {
        return this.client.schema();
    }

    public com.baidu.hugegraph.driver.GraphsManager graphs() {
        return this.client.graphs();
    }

    public GremlinManager gremlin() {
        return this.client.gremlin();
    }

    public ObjectMapper mapper() {
        return this.mapper;
    }
}
