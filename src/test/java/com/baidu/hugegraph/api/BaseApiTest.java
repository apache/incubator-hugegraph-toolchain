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

package com.baidu.hugegraph.api;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.baidu.hugegraph.api.graph.EdgeAPI;
import com.baidu.hugegraph.api.graph.VertexAPI;
import com.baidu.hugegraph.api.schema.EdgeLabelAPI;
import com.baidu.hugegraph.api.schema.IndexLabelAPI;
import com.baidu.hugegraph.api.schema.PropertyKeyAPI;
import com.baidu.hugegraph.api.schema.VertexLabelAPI;
import com.baidu.hugegraph.api.variables.VariablesAPI;
import com.baidu.hugegraph.client.BaseClientTest;
import com.baidu.hugegraph.client.RestClient;

public class BaseApiTest extends BaseClientTest {

    private static RestClient client;

    protected static PropertyKeyAPI propertyKeyAPI;
    protected static VertexLabelAPI vertexLabelAPI;
    protected static EdgeLabelAPI edgeLabelAPI;
    protected static IndexLabelAPI indexLabelAPI;
    protected static VertexAPI vertexAPI;
    protected static EdgeAPI edgeAPI;
    protected static VariablesAPI variablesAPI;

    @BeforeClass
    public static void init() {
        BaseClientTest.init();

        client = new RestClient(BASE_URL, 5);
        propertyKeyAPI = new PropertyKeyAPI(client, GRAPH);
        vertexLabelAPI = new VertexLabelAPI(client, GRAPH);
        edgeLabelAPI = new EdgeLabelAPI(client, GRAPH);
        indexLabelAPI = new IndexLabelAPI(client, GRAPH);
        vertexAPI = new VertexAPI(client, GRAPH);
        edgeAPI = new EdgeAPI(client, GRAPH);
        variablesAPI = new VariablesAPI(client, GRAPH);
    }

    @AfterClass
    public static void clear() throws Exception {
        BaseApiTest.clearData();

        client.close();
        BaseClientTest.clear();
    }

    protected static void clearData() {
        // Clear edge
        edgeAPI.list(-1).results().forEach(edge -> {
            edgeAPI.delete(edge.id());
        });
        // Clear vertex
        vertexAPI.list(-1).results().forEach(vertex -> {
            vertexAPI.delete(vertex.id());
        });

        // Clear schema
        indexLabelAPI.list().forEach(indexLabel -> {
            indexLabelAPI.delete(indexLabel.name());
        });
        edgeLabelAPI.list().forEach(edgeLabel -> {
            edgeLabelAPI.delete(edgeLabel.name());
        });
        vertexLabelAPI.list().forEach(vertexLabel -> {
            vertexLabelAPI.delete(vertexLabel.name());
        });
        propertyKeyAPI.list().forEach(propertyKey -> {
            propertyKeyAPI.delete(propertyKey.name());
        });
    }
}
