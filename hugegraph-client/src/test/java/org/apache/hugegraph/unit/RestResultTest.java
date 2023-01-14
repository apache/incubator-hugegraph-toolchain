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

package org.apache.hugegraph.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.serializer.PathDeserializer;
import org.apache.hugegraph.structure.constant.Cardinality;
import org.apache.hugegraph.structure.constant.DataType;
import org.apache.hugegraph.structure.constant.Frequency;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.constant.IndexType;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.Response;
import org.apache.hugegraph.structure.gremlin.Result;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RestResultTest extends BaseUnitTest {

    private jakarta.ws.rs.core.Response mockResponse;
    private static GraphManager graphManager;

    @BeforeClass
    public static void init() {
        graphManager = Mockito.mock(GraphManager.class);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Path.class, new PathDeserializer());
        RestResult.registerModule(module);
    }

    public static GraphManager graph() {
        return graphManager;
    }

    @Before
    public void setup() {
        // Mock caches
        this.mockResponse = Mockito.mock(jakarta.ws.rs.core.Response.class);
    }

    @After
    public void teardown() {
        // pass
    }

    @Test
    public void testReadPropertyKey() {
        String json = "{"
                      + "\"id\": 3,"
                      + "\"data_type\": \"INT\","
                      + "\"name\": \"id\","
                      + "\"cardinality\": \"SINGLE\","
                      + "\"properties\": []"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        PropertyKey propertyKey = result.readObject(PropertyKey.class);

        Assert.assertEquals("id", propertyKey.name());
        Assert.assertEquals(DataType.INT, propertyKey.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey.cardinality());
        Assert.assertEquals(Collections.emptySet(), propertyKey.properties());
    }

    @Test
    public void testReadPropertyKeys() {
        String json = "{\"propertykeys\": ["
                      + "{"
                      + "\"id\": 3,"
                      + "\"data_type\": \"TEXT\","
                      + "\"name\": \"id\","
                      + "\"cardinality\": \"SINGLE\","
                      + "\"properties\": []"
                      + "},"
                      + "{\"id\": 4,"
                      + "\"data_type\": \"FLOAT\","
                      + "\"name\": \"date\","
                      + "\"cardinality\": \"SET\","
                      + "\"properties\": []"
                      + "}"
                      + "]}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        List<PropertyKey> propertyKeys = result.readList("propertykeys",
                                                         PropertyKey.class);
        Assert.assertEquals(2, propertyKeys.size());
        PropertyKey propertyKey1 = propertyKeys.get(0);
        PropertyKey propertyKey2 = propertyKeys.get(1);

        Assert.assertEquals("id", propertyKey1.name());
        Assert.assertEquals(DataType.TEXT, propertyKey1.dataType());
        Assert.assertEquals(Cardinality.SINGLE, propertyKey1.cardinality());
        Assert.assertEquals(Collections.emptySet(), propertyKey1.properties());

        Assert.assertEquals("date", propertyKey2.name());
        Assert.assertEquals(DataType.FLOAT, propertyKey2.dataType());
        Assert.assertEquals(Cardinality.SET, propertyKey2.cardinality());
        Assert.assertEquals(Collections.emptySet(), propertyKey2.properties());
    }

    @Test
    public void testReadVertexLabel() {
        String json = "{"
                      + "\"id\": 1,"
                      + "\"primary_keys\": [\"name\"],"
                      + "\"index_labels\": [],"
                      + "\"name\": \"software\","
                      + "\"id_strategy\": \"PRIMARY_KEY\","
                      + "\"properties\": [\"price\", \"name\", \"lang\"]"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        VertexLabel vertexLabel = result.readObject(VertexLabel.class);

        Assert.assertEquals("software", vertexLabel.name());
        Assert.assertEquals(IdStrategy.PRIMARY_KEY, vertexLabel.idStrategy());
        Assert.assertEquals(ImmutableList.of("name"),
                            vertexLabel.primaryKeys());
        Assert.assertEquals(ImmutableSet.of("price", "name", "lang"),
                            vertexLabel.properties());
    }

    @Test
    public void testReadVertexLabels() {
        String json = "{\"vertexlabels\": ["
                      + "{"
                      + "\"id\": 1,"
                      + "\"primary_keys\": [\"name\"],"
                      + "\"index_labels\": [],"
                      + "\"name\": \"software\","
                      + "\"id_strategy\": \"PRIMARY_KEY\","
                      + "\"properties\": [\"price\", \"name\", \"lang\"]"
                      + "},"
                      + "{"
                      + "\"id\": 2,"
                      + "\"primary_keys\": [],"
                      + "\"index_labels\": [],"
                      + "\"name\": \"person\","
                      + "\"id_strategy\": \"CUSTOMIZE_STRING\","
                      + "\"properties\": [\"city\", \"name\", \"age\"]"
                      + "}"
                      + "]}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        List<VertexLabel> vertexLabels = result.readList("vertexlabels",
                                                         VertexLabel.class);
        Assert.assertEquals(2, vertexLabels.size());
        VertexLabel vertexLabel1 = vertexLabels.get(0);
        VertexLabel vertexLabel2 = vertexLabels.get(1);

        Assert.assertEquals("software", vertexLabel1.name());
        Assert.assertEquals(IdStrategy.PRIMARY_KEY, vertexLabel1.idStrategy());
        Assert.assertEquals(ImmutableList.of("name"),
                            vertexLabel1.primaryKeys());
        Assert.assertEquals(ImmutableSet.of("price", "name", "lang"),
                            vertexLabel1.properties());

        Assert.assertEquals("person", vertexLabel2.name());
        Assert.assertEquals(IdStrategy.CUSTOMIZE_STRING, vertexLabel2.idStrategy());
        Assert.assertEquals(Collections.emptyList(),
                            vertexLabel2.primaryKeys());
        Assert.assertEquals(ImmutableSet.of("city", "name", "age"),
                            vertexLabel2.properties());
    }

    @Test
    public void testReadEdgeLabel() {
        String json = "{"
                      + "\"id\": 2,"
                      + "\"source_label\": \"person\","
                      + "\"index_labels\": [\"createdByDate\"],"
                      + "\"name\": \"created\","
                      + "\"target_label\": \"software\","
                      + "\"sort_keys\": [],"
                      + "\"properties\": [\"date\"],"
                      + "\"frequency\": \"SINGLE\""
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        EdgeLabel edgeLabel = result.readObject(EdgeLabel.class);

        Assert.assertEquals("created", edgeLabel.name());
        Assert.assertEquals("person", edgeLabel.sourceLabel());
        Assert.assertEquals("software", edgeLabel.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel.frequency());
        Assert.assertEquals(Collections.emptyList(), edgeLabel.sortKeys());
        Assert.assertEquals(ImmutableSet.of("date"), edgeLabel.properties());
    }

    @Test
    public void testReadEdgeLabels() {
        String json = "{\"edgelabels\": ["
                      + "{"
                      + "\"id\": 2,"
                      + "\"source_label\": \"person\","
                      + "\"index_labels\": [\"createdByDate\"],"
                      + "\"name\": \"created\","
                      + "\"target_label\": \"software\","
                      + "\"sort_keys\": [],"
                      + "\"properties\": [\"date\"],"
                      + "\"frequency\": \"SINGLE\""
                      + "},"
                      + "{\"id\": 3,"
                      + "\"source_label\": \"person\","
                      + "\"index_labels\": [],"
                      + "\"name\": \"knows\","
                      + "\"target_label\": \"person\","
                      + "\"sort_keys\": [],"
                      + "\"properties\": [\"date\", \"city\"],"
                      + "\"frequency\": \"SINGLE\""
                      + "}"
                      + "]}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        List<EdgeLabel> edgeLabels = result.readList("edgelabels",
                                                     EdgeLabel.class);
        Assert.assertEquals(2, edgeLabels.size());
        EdgeLabel edgeLabel1 = edgeLabels.get(0);
        EdgeLabel edgeLabel2 = edgeLabels.get(1);

        Assert.assertEquals("created", edgeLabel1.name());
        Assert.assertEquals("person", edgeLabel1.sourceLabel());
        Assert.assertEquals("software", edgeLabel1.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel1.frequency());
        Assert.assertEquals(Collections.emptyList(), edgeLabel1.sortKeys());
        Assert.assertEquals(ImmutableSet.of("date"), edgeLabel1.properties());

        Assert.assertEquals("knows", edgeLabel2.name());
        Assert.assertEquals("person", edgeLabel2.sourceLabel());
        Assert.assertEquals("person", edgeLabel2.targetLabel());
        Assert.assertEquals(Frequency.SINGLE, edgeLabel2.frequency());
        Assert.assertEquals(Collections.emptyList(), edgeLabel2.sortKeys());
        Assert.assertEquals(ImmutableSet.of("date", "city"),
                            edgeLabel2.properties());
    }

    @Test
    public void testReadIndexLabel() {
        String json = "{"
                      + "\"id\": \"4\","
                      + "\"index_type\": \"SEARCH\","
                      + "\"base_value\": \"software\","
                      + "\"name\": \"softwareByPrice\","
                      + "\"fields\": [\"price\"],"
                      + "\"base_type\": \"VERTEX_LABEL\""
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        IndexLabel indexLabel = result.readObject(IndexLabel.class);

        Assert.assertEquals("softwareByPrice", indexLabel.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel.baseType());
        Assert.assertEquals("software", indexLabel.baseValue());
        Assert.assertEquals(IndexType.SEARCH, indexLabel.indexType());
        Assert.assertEquals(ImmutableList.of("price"),
                            indexLabel.indexFields());
    }

    @Test
    public void testReadIndexLabels() {
        String json = "{\"indexlabels\": ["
                      + "{"
                      + "\"id\": \"4\","
                      + "\"index_type\": \"SEARCH\","
                      + "\"base_value\": \"software\","
                      + "\"name\": \"softwareByPrice\","
                      + "\"fields\": [\"price\"],"
                      + "\"base_type\": \"VERTEX_LABEL\""
                      + "},"
                      + "{"
                      + "\"id\": \"4\","
                      + "\"index_type\": \"SECONDARY\","
                      + "\"base_value\": \"person\","
                      + "\"name\": \"personByName\","
                      + "\"fields\": [\"name\"],"
                      + "\"base_type\": \"VERTEX_LABEL\""
                      + "}"
                      + "]}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        List<IndexLabel> indexLabels = result.readList("indexlabels",
                                                       IndexLabel.class);
        Assert.assertEquals(2, indexLabels.size());
        IndexLabel indexLabel1 = indexLabels.get(0);
        IndexLabel indexLabel2 = indexLabels.get(1);

        Assert.assertEquals("softwareByPrice", indexLabel1.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel1.baseType());
        Assert.assertEquals("software", indexLabel1.baseValue());
        Assert.assertEquals(IndexType.SEARCH, indexLabel1.indexType());
        Assert.assertEquals(ImmutableList.of("price"),
                            indexLabel1.indexFields());

        Assert.assertEquals("personByName", indexLabel2.name());
        Assert.assertEquals(HugeType.VERTEX_LABEL, indexLabel2.baseType());
        Assert.assertEquals("person", indexLabel2.baseValue());
        Assert.assertEquals(IndexType.SECONDARY, indexLabel2.indexType());
        Assert.assertEquals(ImmutableList.of("name"),
                            indexLabel2.indexFields());
    }

    @Test
    public void testReadVertex() {
        String json = "{"
                      + "\"id\": \"person:marko\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"name\": \"marko\""
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        Vertex vertex = result.readObject(Vertex.class);

        Assert.assertEquals("person:marko", vertex.id());
        Assert.assertEquals("person", vertex.label());
        Assert.assertEquals(ImmutableMap.of("name", "marko"),
                            vertex.properties());
    }

    @Test
    public void testReadVertices() {
        String json = "{\"vertices\": ["
                      + "{"
                      + "\"id\": \"person:marko\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"city\": [\"Beijing\",\"Wuhan\",\"Beijing\"],"
                      + "\"name\": \"marko\","
                      + "\"age\": 29"
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"software:lop\","
                      + "\"label\": \"software\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"price\": 328,"
                      + "\"name\": \"lop\","
                      + "\"lang\": [\"java\",\"python\",\"c++\"]"
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"person:peter\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"city\": [\"Shanghai\"],"
                      + "\"name\": \"peter\","
                      + "\"age\": 29"
                      + "}"
                      + "}"
                      + "]}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        List<Vertex> vertices = result.readList("vertices", Vertex.class);
        Assert.assertEquals(3, vertices.size());
        Vertex vertex1 = vertices.get(0);
        Vertex vertex2 = vertices.get(1);
        Vertex vertex3 = vertices.get(2);

        Assert.assertEquals("person:marko", vertex1.id());
        Assert.assertEquals("person", vertex1.label());
        Assert.assertEquals(ImmutableMap.of(
                                    "name", "marko",
                                    "age", 29,
                                    "city", ImmutableList.of("Beijing", "Wuhan",
                                                             "Beijing")
                            ),
                            vertex1.properties());

        Assert.assertEquals("software:lop", vertex2.id());
        Assert.assertEquals("software", vertex2.label());
        Assert.assertEquals(ImmutableMap.of(
                                    "name", "lop",
                                    "lang", ImmutableList.of("java", "python", "c++"),
                                    "price", 328),
                            vertex2.properties());

        Assert.assertEquals("person:peter", vertex3.id());
        Assert.assertEquals("person", vertex3.label());
        Assert.assertEquals(ImmutableMap.of(
                                    "name", "peter",
                                    "age", 29,
                                    "city", ImmutableList.of("Shanghai")),
                            vertex3.properties());
    }

    @Test
    public void testReadEdge() {
        String json = "{"
                      + "\"id\": \"person:peter>created>>software:lop\","
                      + "\"label\": \"created\","
                      + "\"type\": \"edge\","
                      + "\"outV\": \"person:peter\","
                      + "\"inV\": \"software:lop\","
                      + "\"outVLabel\": \"person\","
                      + "\"inVLabel\": \"software\","
                      + "\"properties\": {"
                      + "\"city\": \"Hongkong\","
                      + "\"date\": 1495036800000"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        Edge edge = result.readObject(Edge.class);

        Assert.assertEquals("person:peter>created>>software:lop", edge.id());
        Assert.assertEquals("created", edge.label());
        Assert.assertEquals("person:peter", edge.sourceId());
        Assert.assertEquals("software:lop", edge.targetId());
        Assert.assertEquals("person", edge.sourceLabel());
        Assert.assertEquals("software", edge.targetLabel());
        Assert.assertEquals(ImmutableMap.of("city", "Hongkong",
                                            "date", 1495036800000L),
                            edge.properties());
    }

    @Test
    public void testReadEdges() {
        String json = "{\"edges\": ["
                      + "{"
                      + "\"id\": \"person:peter>created>>software:lop\","
                      + "\"label\": \"created\","
                      + "\"type\": \"edge\","
                      + "\"inVLabel\": \"software\","
                      + "\"outVLabel\": \"person\","
                      + "\"inV\": \"software:lop\","
                      + "\"outV\": \"person:peter\","
                      + "\"properties\": {"
                      + "\"date\": 1495036800000,"
                      + "\"city\": \"Hongkong\""
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"person:peter>knows>>person:marko\","
                      + "\"label\": \"knows\","
                      + "\"type\": \"edge\","
                      + "\"inVLabel\": \"person\","
                      + "\"outVLabel\": \"person\","
                      + "\"inV\": \"person:marko\","
                      + "\"outV\": \"person:peter\","
                      + "\"properties\": {"
                      + "\"date\": 1476720000000"
                      + "}"
                      + "}"
                      + "]}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult result = new RestResult(this.mockResponse);
        Assert.assertEquals(200, result.status());
        Assert.assertNull(result.headers());

        List<Edge> edges = result.readList("edges", Edge.class);
        Assert.assertEquals(2, edges.size());
        Edge edge1 = edges.get(0);
        Edge edge2 = edges.get(1);

        Assert.assertEquals("person:peter>created>>software:lop", edge1.id());
        Assert.assertEquals("created", edge1.label());
        Assert.assertEquals("person:peter", edge1.sourceId());
        Assert.assertEquals("software:lop", edge1.targetId());
        Assert.assertEquals("person", edge1.sourceLabel());
        Assert.assertEquals("software", edge1.targetLabel());
        Assert.assertEquals(ImmutableMap.of("city", "Hongkong",
                                            "date", 1495036800000L),
                            edge1.properties());

        Assert.assertEquals("person:peter>knows>>person:marko", edge2.id());
        Assert.assertEquals("knows", edge2.label());
        Assert.assertEquals("person:peter", edge2.sourceId());
        Assert.assertEquals("person:marko", edge2.targetId());
        Assert.assertEquals("person", edge2.sourceLabel());
        Assert.assertEquals("person", edge2.targetLabel());
        Assert.assertEquals(ImmutableMap.of("date", 1476720000000L),
                            edge2.properties());
    }

    @Test
    public void testReadGremlinVertices() {
        String json = "{"
                      + "\"requestId\": \"b0fd8ead-333f-43ac-97b0-4d78784726ae\","
                      + "\"status\": {"
                      + "\"message\": \"\","
                      + "\"code\": 200,"
                      + "\"attributes\": {}"
                      + "},"
                      + "\"result\": {"
                      + "\"data\": ["
                      + "{"
                      + "\"id\": \"person:marko\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"city\": [\"Beijing\",\"Wuhan\",\"Beijing\"],"
                      + "\"name\": \"marko\","
                      + "\"age\": 29"
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"software:lop\","
                      + "\"label\": \"software\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"price\": 328,"
                      + "\"name\": \"lop\","
                      + "\"lang\": [\"java\",\"python\",\"c++\"]"
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"person:peter\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"city\": [\"Shanghai\"],"
                      + "\"name\": \"peter\","
                      + "\"age\": 35"
                      + "}"
                      + "}"
                      + "],"
                      + "\"meta\": {}"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult restResult = new RestResult(this.mockResponse);
        Assert.assertEquals(200, restResult.status());
        Assert.assertNull(restResult.headers());

        Response response = restResult.readObject(Response.class);
        response.graphManager(graph());
        Assert.assertEquals("b0fd8ead-333f-43ac-97b0-4d78784726ae",
                            response.requestId());
        Assert.assertEquals(200, response.status().code());

        Vertex marko = new Vertex("person");
        marko.id("person:marko");
        marko.property("name", "marko");
        marko.property("city", ImmutableList.of("Beijing", "Wuhan", "Beijing"));
        marko.property("age", 29);

        Vertex lop = new Vertex("software");
        lop.id("software:lop");
        lop.property("name", "lop");
        lop.property("lang", ImmutableList.of("java", "python", "c++"));
        lop.property("price", 328);

        Vertex peter = new Vertex("person");
        peter.id("person:peter");
        peter.property("name", "peter");
        peter.property("city", ImmutableList.of("Shanghai"));
        peter.property("age", 35);

        List<Vertex> vertices = new ArrayList<>(3);
        vertices.add(peter);
        vertices.add(marko);
        vertices.add(lop);

        Iterator<Result> results = response.result().iterator();
        while (results.hasNext()) {
            Result result = results.next();
            Assert.assertEquals(Vertex.class, result.getObject().getClass());
            Vertex vertex = result.getVertex();
            Assert.assertTrue(Utils.contains(vertices, vertex));
        }
    }

    @Test
    public void testReadGremlinEdges() {
        String json = "{"
                      + "\"requestId\": \"cd4cfc17-1ee4-4e9e-af40-cb18b115a8dc\","
                      + "\"status\": {"
                      + "\"message\": \"\","
                      + "\"code\": 200,"
                      + "\"attributes\": {}"
                      + "},"
                      + "\"result\": {"
                      + "\"data\": ["
                      + "{"
                      + "\"id\": \"person:peter>created>>software:lop\","
                      + "\"label\": \"created\","
                      + "\"type\": \"edge\","
                      + "\"inVLabel\": \"software\","
                      + "\"outVLabel\": \"person\","
                      + "\"inV\": \"software:lop\","
                      + "\"outV\": \"person:peter\","
                      + "\"properties\": {"
                      + "\"date\": 1490284800000,"
                      + "\"weight\": 0.2"
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"person:peter>knows>>person:marko\","
                      + "\"label\": \"knows\","
                      + "\"type\": \"edge\","
                      + "\"inVLabel\": \"person\","
                      + "\"outVLabel\": \"person\","
                      + "\"inV\": \"person:marko\","
                      + "\"outV\": \"person:peter\","
                      + "\"properties\": {"
                      + "\"date\": 1452355200000,"
                      + "\"weight\": 0.5"
                      + "}"
                      + "}"
                      + "],"
                      + "\"meta\": {}"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult restResult = new RestResult(this.mockResponse);
        Assert.assertEquals(200, restResult.status());
        Assert.assertNull(restResult.headers());

        Response response = restResult.readObject(Response.class);
        response.graphManager(graph());
        Assert.assertEquals(200, response.status().code());

        Edge created = new Edge("created");
        created.id("person:peter>created>>software:lop");
        created.sourceId("person:peter");
        created.targetId("software:lop");
        created.sourceLabel("person");
        created.targetLabel("software");
        created.property("date", 1490284800000L);
        created.property("weight", 0.2);

        Edge knows = new Edge("knows");
        knows.id("person:peter>knows>>person:marko");
        knows.sourceId("person:peter");
        knows.targetId("person:marko");
        knows.sourceLabel("person");
        knows.targetLabel("person");
        knows.property("date", 1452355200000L);
        knows.property("weight", 0.5);

        List<Edge> edges = new ArrayList<>(2);
        edges.add(created);
        edges.add(knows);

        Iterator<Result> results = response.result().iterator();
        while (results.hasNext()) {
            Result result = results.next();
            Assert.assertEquals(Edge.class, result.getObject().getClass());
            Edge edge = result.getEdge();
            Assert.assertTrue(Utils.contains(edges, edge));
        }
    }

    @Test
    public void testReadGremlinPathWithVertexAndEdge() {
        String json = "{"
                      + "\"requestId\": \"238c74ca-18f7-4377-b8e1-2bb3b165e5d6\","
                      + "\"status\":{"
                      + "\"message\": \"\","
                      + "\"code\": 200,"
                      + "\"attributes\":{}"
                      + "},"
                      + "\"result\":{"
                      + "\"data\":["
                      + "{"
                      + "\"labels\":[[], []],"
                      + "\"objects\":["
                      + "{"
                      + "\"id\": \"person:marko\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\":{"
                      + "\"city\":\"Beijing\","
                      + "\"name\":\"marko\","
                      + "\"age\":29"
                      + "}"
                      + "},"
                      + "{"
                      + "\"id\": \"person:marko>knows>>person:vadas\","
                      + "\"label\": \"knows\","
                      + "\"type\": \"edge\","
                      + "\"inVLabel\": \"person\","
                      + "\"outVLabel\": \"person\","
                      + "\"inV\": \"person:vadas\","
                      + "\"outV\": \"person:marko\","
                      + "\"properties\":{"
                      + "\"date\": 1452355200000,"
                      + "\"weight\": 0.5"
                      + "}"
                      + "}"
                      + "]"
                      + "}"
                      + "],"
                      + "\"meta\":{"
                      + "}"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult restResult = new RestResult(this.mockResponse);
        Assert.assertEquals(200, restResult.status());
        Assert.assertNull(restResult.headers());

        Response response = restResult.readObject(Response.class);
        response.graphManager(graph());
        Assert.assertEquals(200, response.status().code());

        Iterator<Result> results = response.result().iterator();
        Assert.assertTrue(results.hasNext());
        Result result = results.next();
        Object object = result.getObject();
        Assert.assertEquals(Path.class, object.getClass());
        Path path = (Path) object;
        Assert.assertEquals(2, path.labels().size());
        Assert.assertEquals(ImmutableList.of(), path.labels().get(0));
        Assert.assertEquals(ImmutableList.of(), path.labels().get(1));

        Vertex vertex = new Vertex("person");
        vertex.id("person:marko");
        vertex.property("name", "marko");
        vertex.property("age", 29);
        vertex.property("city", "Beijing");

        Edge edge = new Edge("knows");
        edge.id("person:marko>knows>>person:vadas");
        edge.sourceId("person:marko");
        edge.sourceLabel("person");
        edge.targetId("person:vadas");
        edge.targetLabel("person");
        edge.property("date", 1452355200000L);
        edge.property("weight", 0.5);

        Assert.assertEquals(2, path.objects().size());
        Utils.assertGraphEqual(ImmutableList.of(vertex),
                               ImmutableList.of(edge),
                               path.objects());
    }

    @Test
    public void testReadGremlinNullData() {
        String json = "{"
                      + "\"requestId\": \"d95ac131-24b5-4140-a3ff-91b0c020764a\","
                      + "\"status\": {"
                      + "\"message\": \"\","
                      + "\"code\": 200,"
                      + "\"attributes\": {}"
                      + "},"
                      + "\"result\": {"
                      + "\"data\": [null],"
                      + "\"meta\": {}"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult restResult = new RestResult(this.mockResponse);
        Assert.assertEquals(200, restResult.status());
        Assert.assertNull(restResult.headers());

        Response response = restResult.readObject(Response.class);
        response.graphManager(graph());
        Assert.assertEquals(200, response.status().code());

        Iterator<Result> results = response.result().iterator();
        Assert.assertTrue(results.hasNext());
        Object object = results.next();
        Assert.assertNull(object);
    }

    @Test
    public void testReadGremlinNullAndVertex() {
        String json = "{"
                      + "\"requestId\": \"d95ac131-24b5-4140-a3ff-91b0c020764a\","
                      + "\"status\": {"
                      + "\"message\": \"\","
                      + "\"code\": 200,"
                      + "\"attributes\": {}"
                      + "},"
                      + "\"result\": {"
                      + "\"data\": ["
                      + "null,"
                      + "{"
                      + "\"id\": \"person:marko\","
                      + "\"label\": \"person\","
                      + "\"type\": \"vertex\","
                      + "\"properties\": {"
                      + "\"city\": \"Beijing\","
                      + "\"name\": \"marko\","
                      + "\"age\": 29"
                      + "}"
                      + "}"
                      + "],"
                      + "\"meta\": {}"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult restResult = new RestResult(this.mockResponse);
        Assert.assertEquals(200, restResult.status());
        Assert.assertNull(restResult.headers());

        Response response = restResult.readObject(Response.class);
        response.graphManager(graph());
        Assert.assertEquals(200, response.status().code());

        Iterator<Result> results = response.result().iterator();
        Assert.assertTrue(results.hasNext());
        Result result = results.next();
        Assert.assertNull(result);

        Assert.assertTrue(results.hasNext());
        result = results.next();
        Assert.assertEquals(Vertex.class, result.getObject().getClass());

        Vertex marko = new Vertex("person");
        marko.id("person:marko");
        marko.property("name", "marko");
        marko.property("city", "Beijing");
        marko.property("age", 29);
        Vertex vertex = result.getVertex();
        Assert.assertTrue(Utils.contains(ImmutableList.of(marko), vertex));
    }

    @Test
    public void testReadGremlinEdgeAndNull() {
        String json = "{"
                      + "\"requestId\": \"d95ac131-24b5-4140-a3ff-91b0c020764a\","
                      + "\"status\": {"
                      + "\"message\": \"\","
                      + "\"code\": 200,"
                      + "\"attributes\": {}"
                      + "},"
                      + "\"result\": {"
                      + "\"data\": ["
                      + "{"
                      + "\"id\": \"person:peter>created>>software:lop\","
                      + "\"label\": \"created\","
                      + "\"type\": \"edge\","
                      + "\"inVLabel\": \"software\","
                      + "\"outVLabel\": \"person\","
                      + "\"inV\": \"software:lop\","
                      + "\"outV\": \"person:peter\","
                      + "\"properties\": {"
                      + "\"date\": 1490284800000,"
                      + "\"weight\": 0.2"
                      + "}"
                      + "},"
                      + "null"
                      + "],"
                      + "\"meta\": {}"
                      + "}"
                      + "}";

        Mockito.when(this.mockResponse.getStatus()).thenReturn(200);
        Mockito.when(this.mockResponse.getHeaders()).thenReturn(null);
        Mockito.when(this.mockResponse.readEntity(String.class))
               .thenReturn(json);
        RestResult restResult = new RestResult(this.mockResponse);
        Assert.assertEquals(200, restResult.status());
        Assert.assertNull(restResult.headers());

        Response response = restResult.readObject(Response.class);
        response.graphManager(graph());
        Assert.assertEquals(200, response.status().code());

        Iterator<Result> results = response.result().iterator();

        Assert.assertTrue(results.hasNext());
        Result result = results.next();
        Assert.assertEquals(Edge.class, result.getObject().getClass());

        Edge created = new Edge("created");
        created.id("person:peter>created>>software:lop");
        created.sourceId("person:peter");
        created.targetId("software:lop");
        created.sourceLabel("person");
        created.targetLabel("software");
        created.property("date", 1490284800000L);
        created.property("weight", 0.2);
        Assert.assertTrue(Utils.contains(ImmutableList.of(created),
                                         result.getEdge()));

        Assert.assertTrue(results.hasNext());
        result = results.next();
        Assert.assertNull(result);
    }
}
