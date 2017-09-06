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

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.baidu.hugegraph.api.graph.EdgeAPI;
import com.baidu.hugegraph.api.graph.VertexAPI;
import com.baidu.hugegraph.api.schema.EdgeLabelAPI;
import com.baidu.hugegraph.api.schema.IndexLabelAPI;
import com.baidu.hugegraph.api.schema.PropertyKeyAPI;
import com.baidu.hugegraph.api.schema.VertexLabelAPI;
import com.baidu.hugegraph.client.BaseClientTest;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;

public class BaseApiTest extends BaseClientTest {

    private static RestClient client;

    protected static PropertyKeyAPI propertyKeyAPI;
    protected static VertexLabelAPI vertexLabelAPI;
    protected static EdgeLabelAPI edgeLabelAPI;
    protected static IndexLabelAPI indexLabelAPI;
    protected static VertexAPI vertexAPI;
    protected static EdgeAPI edgeAPI;

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
    }

    @AfterClass
    public static void clear() throws Exception {
        BaseApiTest.clearData();

        client.close();
        BaseClientTest.clear();
    }

    protected static void clearData() {
        // Clear edge
        edgeAPI.list(-1).forEach(edge -> edgeAPI.delete(edge.id()));
        // Clear vertex
        vertexAPI.list(-1).forEach(vertex -> vertexAPI.delete(vertex.id()));

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

    protected static void initPropertyKey() {
        SchemaManager schema = schema();
        schema.propertyKey("name").asText().ifNotExist().create();
        schema.propertyKey("age").asInt().ifNotExist().create();
        schema.propertyKey("city").asText().ifNotExist().create();
        schema.propertyKey("lang").asText().ifNotExist().create();
        schema.propertyKey("date").asText().ifNotExist().create();
        schema.propertyKey("price").asInt().ifNotExist().create();
    }

    protected static void initVertexLabel() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .ifNotExist()
              .create();

        schema.vertexLabel("software")
              .properties("name", "lang", "price")
              .primaryKeys("name")
              .ifNotExist()
              .create();
    }

    protected static void initEdgeLabel() {
        SchemaManager schema = schema();

        schema.edgeLabel("knows")
              .sourceLabel("person")
              .targetLabel("person")
              .properties("date")
              .ifNotExist()
              .create();

        schema.edgeLabel("created")
              .sourceLabel("person")
              .targetLabel("software")
              .properties("date", "city")
              .ifNotExist()
              .create();
    }

    protected static void initVertex() {
        graph().addVertex(T.label, "person", "name", "marko",
                          "age", 29, "city", "Beijing");
        graph().addVertex(T.label, "person", "name", "vadas",
                          "age", 27, "city", "Hongkong");
        graph().addVertex(T.label, "software", "name", "lop",
                          "lang", "java", "price", 328);
        graph().addVertex(T.label, "person", "name", "josh",
                          "age", 32, "city", "Beijing");
        graph().addVertex(T.label, "software", "name", "ripple",
                          "lang", "java", "price", 199);
        graph().addVertex(T.label, "person", "name", "peter",
                          "age", 29, "city", "Shanghai");
    }

    protected static void initEdge() {
        graph().addEdge("person:marko", "knows", "person:vadas",
                        "date", "20160110");
        graph().addEdge("person:marko", "knows", "person:josh",
                        "date", "20130220");
        graph().addEdge("person:marko", "created", "software:lop",
                        "date", "20171210", "city", "Shanghai");
        graph().addEdge("person:josh", "created", "software:ripple",
                        "date", "20171210", "city", "Beijing");
        graph().addEdge("person:josh", "created", "software:lop",
                        "date", "20091111", "city", "Beijing");
        graph().addEdge("person:peter", "created", "software:lop",
                        "date", "20170324", "city", "Hongkong");
    }

    protected List<Vertex> create100PersonBatch() {
        List<Vertex> vertices = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            Vertex vertex = new Vertex("person");
            vertex.property("name", "Person" + "-" + i);
            vertex.property("city", "Beijing");
            vertex.property("age", 30);
            vertices.add(vertex);
        }
        return vertices;
    }

    protected List<Vertex> create50SoftwareBatch() {
        List<Vertex> vertices = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Vertex vertex = new Vertex("software");
            vertex.property("name", "Software" + "-" + i);
            vertex.property("lang", "java");
            vertex.property("price", 328);
            vertices.add(vertex);
        }
        return vertices;
    }

    protected List<Edge> create50CreatedBatch() {
        List<Edge> edges = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Edge edge = new Edge("created");
            edge.sourceLabel("person");
            edge.targetLabel("software");
            edge.source("person:Person-" + i);
            edge.target("software:Software-" + i);
            edge.property("date", "20170324");
            edge.property("city", "Hongkong");
            edges.add(edge);
        }
        return edges;
    }

    protected List<Edge> create50KnowsBatch() {
        List<Edge> edges = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Edge edge = new Edge("knows");
            edge.sourceLabel("person");
            edge.targetLabel("person");
            edge.source("person:Person-" + i);
            edge.target("person:Person-" + (i + 50));
            edge.property("date", "20170324");
            edges.add(edge);
        }
        return edges;
    }
}
