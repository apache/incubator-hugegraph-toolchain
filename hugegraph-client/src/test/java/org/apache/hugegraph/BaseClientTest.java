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

package org.apache.hugegraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.driver.AuthManager;
import org.apache.hugegraph.driver.CypherManager;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.GraphsManager;
import org.apache.hugegraph.driver.GremlinManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.MetricsManager;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.driver.TaskManager;
import org.apache.hugegraph.driver.TraverserManager;
import org.apache.hugegraph.structure.constant.T;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.collect.ImmutableMap;

public class BaseClientTest {

    protected static final String BASE_URL = "http://127.0.0.1:8080";
    protected static final String GRAPH = "hugegraph";
    protected static final String USERNAME = "admin";
    protected static final String PASSWORD = "pa";
    protected static final int TIMEOUT = 10;

    private static HugeClient client;

    protected static HugeClient open() {
        client = HugeClient.builder(BASE_URL, GRAPH)
                           .configUser(USERNAME, PASSWORD)
                           .build();
        return client;
    }

    @BeforeClass
    public static void init() {
        if (client == null) {
            open();
        }
    }

    @AfterClass
    public static void clear() throws Exception {
        // client.close();
    }

    public static HugeClient baseClient() {
        return client;
    }

    public static SchemaManager schema() {
        Assert.assertNotNull("Not opened client", client);
        return client.schema();
    }

    public static GraphManager graph() {
        Assert.assertNotNull("Not opened client", client);
        return client.graph();
    }

    public static GremlinManager gremlin() {
        Assert.assertNotNull("Not opened client", client);
        return client.gremlin();
    }

    public static CypherManager cypher() {
        Assert.assertNotNull("Not opened client", client);
        return client.cypher();
    }

    public static TraverserManager traverser() {
        Assert.assertNotNull("Not opened client", client);
        return client.traverser();
    }

    public static TaskManager task() {
        Assert.assertNotNull("Not opened client", client);
        return client.task();
    }

    public static AuthManager auth() {
        Assert.assertNotNull("Not opened client", client);
        return client.auth();
    }

    public static GraphsManager graphs() {
        Assert.assertNotNull("Not opened client", client);
        return client.graphs();
    }

    public static MetricsManager metrics() {
        Assert.assertNotNull("Not opened client", client);
        return client.metrics();
    }

    @Before
    public void setup() {
        // this.clearData();
    }

    @After
    public void teardown() throws Exception {
        // pass
    }

    protected static Object getVertexId(String label, String key,
                                        String value) {
        return getVertex(label, key, value).id();
    }

    protected static Vertex getVertex(String label, String key, String value) {
        Map<String, Object> params = ImmutableMap.of(key, value);
        List<Vertex> vertices = graph().listVertices(label, params);
        Assert.assertEquals(1, vertices.size());
        return vertices.get(0);
    }

    protected static String getEdgeId(String label, String key, String value) {
        return getEdge(label, key, value).id();
    }

    protected static Edge getEdge(String label, String key, String value) {
        Map<String, Object> params = ImmutableMap.of(key, value);
        List<Edge> edges = graph().listEdges(label, params);
        Assert.assertEquals(1, edges.size());
        return edges.get(0);
    }

    protected static void assertContains(List<PropertyKey> propertyKeys,
                                         PropertyKey propertyKey) {
        Assert.assertTrue(Utils.contains(propertyKeys, propertyKey));
    }

    protected static void assertContains(List<VertexLabel> vertexLabels,
                                         VertexLabel vertexLabel) {
        Assert.assertTrue(Utils.contains(vertexLabels, vertexLabel));
    }

    protected static void assertContains(List<EdgeLabel> edgeLabels,
                                         EdgeLabel edgeLabel) {
        Assert.assertTrue(Utils.contains(edgeLabels, edgeLabel));
    }

    protected static void assertContains(List<IndexLabel> indexLabels,
                                         IndexLabel indexLabel) {
        Assert.assertTrue(Utils.contains(indexLabels, indexLabel));
    }

    protected static void initPropertyKey() {
        SchemaManager schema = schema();
        schema.propertyKey("name").asText().ifNotExist().create();
        schema.propertyKey("age").asInt().ifNotExist().create();
        schema.propertyKey("city").asText().ifNotExist().create();
        schema.propertyKey("lang").asText().ifNotExist().create();
        schema.propertyKey("date").asDate().ifNotExist().create();
        schema.propertyKey("price").asInt().ifNotExist().create();
        schema.propertyKey("weight").asDouble().ifNotExist().create();
    }

    protected static void initVertexLabel() {
        SchemaManager schema = schema();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .primaryKeys("name")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        schema.vertexLabel("software")
              .properties("name", "lang", "price")
              .primaryKeys("name")
              .nullableKeys("price")
              .ifNotExist()
              .create();

        schema.vertexLabel("book")
              .useCustomizeStringId()
              .properties("name", "price")
              .nullableKeys("price")
              .ifNotExist()
              .create();
    }

    protected static void initEdgeLabel() {
        SchemaManager schema = schema();

        schema.edgeLabel("knows")
              .sourceLabel("person")
              .targetLabel("person")
              .multiTimes()
              .properties("date", "city")
              .sortKeys("date")
              .nullableKeys("city")
              .ifNotExist()
              .create();

        schema.edgeLabel("created")
              .sourceLabel("person")
              .targetLabel("software")
              .properties("date", "city")
              .nullableKeys("city")
              .ifNotExist()
              .create();
    }

    protected static void initIndexLabel() {
        SchemaManager schema = schema();

        schema.indexLabel("personByCity")
              .onV("person")
              .by("city")
              .secondary()
              .ifNotExist()
              .create();

        schema.indexLabel("personByAge")
              .onV("person")
              .by("age")
              .range()
              .ifNotExist()
              .create();

        schema.indexLabel("knowsByDate")
              .onE("knows")
              .by("date")
              .secondary()
              .ifNotExist()
              .create();

        schema.indexLabel("createdByDate")
              .onE("created")
              .by("date")
              .secondary()
              .ifNotExist()
              .create();
    }

    protected static void initVertex() {
        graph().addVertex(T.LABEL, "person", "name", "marko",
                          "age", 29, "city", "Beijing");
        graph().addVertex(T.LABEL, "person", "name", "vadas",
                          "age", 27, "city", "Hongkong");
        graph().addVertex(T.LABEL, "software", "name", "lop",
                          "lang", "java", "price", 328);
        graph().addVertex(T.LABEL, "person", "name", "josh",
                          "age", 32, "city", "Beijing");
        graph().addVertex(T.LABEL, "software", "name", "ripple",
                          "lang", "java", "price", 199);
        graph().addVertex(T.LABEL, "person", "name", "peter",
                          "age", 29, "city", "Shanghai");
    }

    protected static void initEdge() {
        Object markoId = getVertexId("person", "name", "marko");
        Object vadasId = getVertexId("person", "name", "vadas");
        Object joshId = getVertexId("person", "name", "josh");
        Object peterId = getVertexId("person", "name", "peter");
        Object lopId = getVertexId("software", "name", "lop");
        Object rippleId = getVertexId("software", "name", "ripple");

        graph().addEdge(markoId, "knows", vadasId, "date", "2012-01-10");
        graph().addEdge(markoId, "knows", joshId, "date", "2013-01-10");
        graph().addEdge(markoId, "created", lopId,
                        "date", "2014-01-10", "city", "Shanghai");
        graph().addEdge(joshId, "created", rippleId,
                        "date", "2015-01-10", "city", "Beijing");
        graph().addEdge(joshId, "created", lopId,
                        "date", "2016-01-10", "city", "Beijing");
        graph().addEdge(peterId, "created", lopId,
                        "date", "2017-01-10", "city", "Hongkong");
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
        VertexLabel person = schema().getVertexLabel("person");
        VertexLabel software = schema().getVertexLabel("software");

        List<Edge> edges = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Edge edge = new Edge("created");
            edge.sourceLabel("person");
            edge.targetLabel("software");
            edge.sourceId(person.id() + ":Person-" + i);
            edge.targetId(software.id() + ":Software-" + i);
            edge.property("date", "2017-03-24");
            edge.property("city", "Hongkong");
            edges.add(edge);
        }
        return edges;
    }

    protected List<Edge> create50KnowsBatch() {
        VertexLabel person = schema().getVertexLabel("person");

        List<Edge> edges = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            Edge edge = new Edge("knows");
            edge.sourceLabel("person");
            edge.targetLabel("person");
            edge.sourceId(person.id() + ":Person-" + i);
            edge.targetId(person.id() + ":Person-" + (i + 50));
            edge.property("date", "2017-03-24");
            edges.add(edge);
        }
        return edges;
    }
}
