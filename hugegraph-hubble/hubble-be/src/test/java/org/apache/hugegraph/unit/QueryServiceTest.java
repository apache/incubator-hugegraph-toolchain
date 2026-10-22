/*
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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.config.JacksonConfig;
import org.apache.hugegraph.driver.CypherManager;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.GremlinManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.AdjacentQuery;
import org.apache.hugegraph.entity.query.GremlinQuery;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.gremlin.Result;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.testutil.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class QueryServiceTest {

    @Test
    public void testDefaultReadLimitIsReported() throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V()"));
        Mockito.verify(gremlin).gremlin("g.V().limit(300)");
        Assert.assertTrue(result.isDefaultLimitApplied());
        Assert.assertEquals(300, result.getDefaultLimit());
    }

    @Test
    public void testConfiguredGremlinDefaultReportsEffectivePreviewLimit() throws Exception {
        for (int configured : new int[]{300, 5000}) {
            QueryService service = this.serviceWithConfig(configured);
            Object[] rows = Collections.nCopies(Math.min(configured, 1001), "value").toArray();
            GremlinManager gremlin = this.mockGremlin(this.resultSet(rows));
            GremlinQuery query = new GremlinQuery("g.V()");
            query.setPreview(true);
            GremlinResult preview = service.executeGremlinQuery(this.mockClient(gremlin), query);

            Mockito.verify(gremlin).gremlin("g.V().limit(" + configured + ").limit(1001)");
            Assert.assertTrue(preview.isDefaultLimitApplied());
            Assert.assertEquals(Math.min(configured, 1000), preview.getDefaultLimit());
            Assert.assertEquals(Math.min(configured, 1000), preview.getJsonView().getData().size());
            Assert.assertEquals(configured > 1000, preview.isResultTruncated());

            GremlinManager rawGremlin = this.mockGremlin();
            query.setPreview(false);
            GremlinResult raw = service.executeGremlinQuery(this.mockClient(rawGremlin), query);
            Mockito.verify(rawGremlin).gremlin("g.V().limit(" + configured + ")");
            Assert.assertTrue(raw.isDefaultLimitApplied());
            Assert.assertEquals(configured, raw.getDefaultLimit());
        }
    }

    @Test
    public void testConfiguredCypherDefaultReportsEffectivePreviewLimit() throws Exception {
        for (int configured : new int[]{300, 5000}) {
            QueryService service = this.serviceWithConfig(configured);
            String query = "MATCH (n) RETURN n";
            String dispatched = query + " LIMIT " + Math.min(configured, 1001);
            Object[] rows = Collections.nCopies(Math.min(configured, 1001), "value").toArray();
            CypherManager cypher = Mockito.mock(CypherManager.class);
            Mockito.when(cypher.cypher(dispatched)).thenReturn(this.resultSet(rows));
            Mockito.when(cypher.cypher(query)).thenReturn(this.resultSet());
            HugeClient client = this.mockClient(this.mockGremlin());
            Mockito.when(client.cypher()).thenReturn(cypher);
            GremlinResult preview = service.executeCypherQuery(client, query, true);

            Mockito.verify(cypher).cypher(dispatched);
            Assert.assertTrue(preview.isDefaultLimitApplied());
            Assert.assertEquals(Math.min(configured, 1000), preview.getDefaultLimit());
            Assert.assertEquals(Math.min(configured, 1000), preview.getJsonView().getData().size());
            Assert.assertEquals(configured > 1000, preview.isResultTruncated());

            GremlinResult raw = service.executeCypherQuery(client, query, false);
            Mockito.verify(cypher).cypher(query);
            Assert.assertFalse(raw.isDefaultLimitApplied());
            Assert.assertEquals(0, raw.getDefaultLimit());
        }
    }

    @Test
    public void testPropertyPreviewCoversAllViewsWithoutChangingLegacyResults() throws Exception {
        Vertex vertex = this.vertices(1).get(0);
        String payload = "x".repeat(1024 * 1024);
        vertex.property("payload", payload);
        byte[] binary = new byte[1024 * 1024];
        vertex.property("binary", binary);
        String expected = "x".repeat(256) + "… [preview 256/1048576 chars]";
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertex), this.resultSet());
        GremlinQuery query = new GremlinQuery("g.V().limit(1)");
        query.setPreview(true);
        GremlinResult preview = this.serviceWithConfig().executeGremlinQuery(
                                this.mockClient(gremlin), query);
        Assert.assertEquals(expected, ((Vertex) preview.getJsonView().getData().get(0)).property("payload"));
        Assert.assertEquals(expected, ((Vertex) preview.getTableView().getRows().get(0)).property("payload"));
        Assert.assertEquals(expected, preview.getGraphView().getVertices().iterator().next().property("payload"));
        Assert.assertTrue(preview.isPropertiesTruncated());
        Assert.assertEquals("[binary preview: 1048576 bytes omitted]",
                            ((Vertex) preview.getJsonView().getData().get(0)).property("binary"));
        Assert.assertEquals(256, preview.getPropertyPreviewLimit());
        Assert.assertEquals(List.of("0"), preview.getPropertyPreviewVertexIds());
        Assert.assertEquals(List.of(), preview.getPropertyPreviewEdgeIds());
        Assert.assertEquals(payload, vertex.property("payload"));

        GremlinManager rawGremlin = this.mockGremlin(this.resultSet(vertex), this.resultSet());
        query.setPreview(false);
        GremlinResult raw = this.serviceWithConfig().executeGremlinQuery(this.mockClient(rawGremlin), query);
        Assert.assertEquals(payload, ((Vertex) raw.getJsonView().getData().get(0)).property("payload"));
        Assert.assertEquals(payload, raw.getGraphView().getVertices().iterator().next().property("payload"));
        Assert.assertSame(binary, ((Vertex) raw.getJsonView().getData().get(0)).property("binary"));
        Assert.assertFalse(raw.isPropertiesTruncated());
    }

    @Test
    public void testOneLargePropertyDoesNotMarkOtherPreviewVertices() throws Exception {
        List<Vertex> vertices = this.vertices(300);
        for (Vertex vertex : vertices) {
            vertex.property("name", "person" + vertex.id());
        }
        vertices.get(150).property("description", "x".repeat(257));
        long partialId = 9007199254741013L;
        vertices.get(150).id(partialId);
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertices.toArray()), this.resultSet());
        GremlinQuery query = new GremlinQuery("g.V().limit(300)");
        query.setPreview(true);
        GremlinResult preview = this.serviceWithConfig().executeGremlinQuery(this.mockClient(gremlin), query);

        Assert.assertEquals(300, preview.getGraphView().getVertices().size());
        Assert.assertTrue(preview.isPropertiesTruncated());
        Assert.assertEquals(List.of(String.valueOf(partialId)), preview.getPropertyPreviewVertexIds());
        Assert.assertEquals(List.of(), preview.getPropertyPreviewEdgeIds());
        Assert.assertEquals("person0", preview.getGraphView().getVertices().iterator().next().property("name"));
        Assert.assertEquals(257, ((String) vertices.get(150).property("description")).length());
        JsonNode json = this.serializedQuery(preview);
        JsonNode graphId = json.get("graph_view").get("vertices").get(150).get("id");
        JsonNode previewId = json.get("property_preview_vertex_ids").get(0);
        Assert.assertTrue(graphId.isTextual());
        Assert.assertTrue(previewId.isTextual());
        Assert.assertEquals(String.valueOf(partialId), graphId.textValue());
        Assert.assertEquals(graphId, previewId);
        Assert.assertTrue(json.get("property_preview_edge_ids").isArray());
        Assert.assertEquals(0, json.get("property_preview_edge_ids").size());
    }

    @Test
    public void testEnrichedEdgePropertiesAreAlsoPreviewed() throws Exception {
        List<Vertex> vertices = this.vertices(2);
        vertices.get(0).property("description", "x".repeat(257));
        Edge edge = this.edge("edge", 0, 1);
        edge.property("payload", "x".repeat(100000));
        Edge complete = this.edge("complete", 0, 1);
        complete.property("name", "complete");
        Edge later = this.edge("later", 1, 0);
        later.property("payload", "y".repeat(257));
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertices.toArray()),
                                                  this.resultSet(edge, complete, later));
        GremlinQuery query = new GremlinQuery("g.V().limit(2)");
        query.setPreview(true);
        GremlinResult preview = this.serviceWithConfig().executeGremlinQuery(this.mockClient(gremlin), query);
        String property = (String) preview.getGraphView().getEdges().iterator().next().property("payload");
        Assert.assertTrue(property.endsWith("/100000 chars]"));
        Assert.assertTrue(preview.isPropertiesTruncated());
        Assert.assertEquals(100000, ((String) edge.property("payload")).length());
        Assert.assertEquals(List.of("0"), preview.getPropertyPreviewVertexIds());
        Assert.assertEquals(List.of("edge", "later"), preview.getPropertyPreviewEdgeIds());
        Assert.assertEquals(3, preview.getGraphView().getEdges().size());
        JsonNode json = this.serializedQuery(preview);
        JsonNode graphEdges = json.get("graph_view").get("edges");
        JsonNode previewIds = json.get("property_preview_edge_ids");
        Assert.assertTrue(previewIds.get(0).isTextual());
        Assert.assertEquals(graphEdges.get(0).get("id"), previewIds.get(0));
        Assert.assertEquals(graphEdges.get(2).get("id"), previewIds.get(1));
    }

    @Test
    public void testScalarPropertyValuesArePreviewed() throws Exception {
        GremlinManager gremlin = this.mockGremlin(this.resultSet("x".repeat(1024 * 1024)));
        GremlinQuery query = new GremlinQuery("g.V().values('payload').limit(1)");
        query.setPreview(true);
        GremlinResult preview = this.serviceWithConfig().executeGremlinQuery(this.mockClient(gremlin), query);
        Assert.assertTrue(((String) preview.getJsonView().getData().get(0)).endsWith("/1048576 chars]"));
        Assert.assertTrue(preview.isPropertiesTruncated());
    }

    @Test
    public void testCypherPreviewBoundsGraphAndScalarPropertiesAcrossViews() throws Exception {
        String payload = "x".repeat(1024 * 1024);
        String expected = "x".repeat(256) + "… [preview 256/1048576 chars]";
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            rows.add(Map.of("n", Map.of("_type", "node", "_label", "person", "_id", i, "payload", payload)));
        }
        String content = "MATCH (n) RETURN n LIMIT 10000";
        String limited = "MATCH (n) RETURN n LIMIT 1001";
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(limited)).thenReturn(this.resultSet(rows.toArray()));
        GremlinManager gremlin = this.mockGremlin();
        HugeClient client = this.mockClient(gremlin);
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();
        GremlinResult preview = service.executeCypherQuery(client, content, true);

        Assert.assertEquals(1000, preview.getJsonView().getData().size());
        Assert.assertEquals(1000, preview.getTableView().getRows().size());
        Assert.assertEquals(1000, preview.getGraphView().getVertices().size());
        Vertex jsonVertex = (Vertex) preview.getJsonView().getData().get(0);
        Vertex graphVertex = preview.getGraphView().getVertices().iterator().next();
        Assert.assertEquals(expected, jsonVertex.property("payload"));
        Assert.assertSame(jsonVertex, preview.getTableView().getRows().get(0));
        Assert.assertSame(jsonVertex.properties(), graphVertex.properties());
        Assert.assertTrue(preview.isResultTruncated());
        Assert.assertTrue(preview.isPropertiesTruncated());
        Assert.assertEquals(1000, preview.getPropertyPreviewVertexIds().size());
        Assert.assertEquals(List.of(), preview.getPropertyPreviewEdgeIds());
        Assert.assertTrue(this.serializedQuery(preview).toString().length() < 2 * 1024 * 1024);
        Assert.assertFalse(preview.isDefaultLimitApplied());
        Mockito.verify(cypher).cypher(limited);
        Mockito.verify(gremlin, Mockito.never()).gremlin(Mockito.anyString());

        String scalarQuery = "MATCH (n) RETURN n.payload AS payload";
        Map<String, Object> scalarRow = Map.of("payload", payload);
        Mockito.when(cypher.cypher(scalarQuery + " LIMIT 300")).thenReturn(this.resultSet(scalarRow));
        GremlinResult scalar = service.executeCypherQuery(client, scalarQuery, true);
        Map<?, ?> jsonRow = (Map<?, ?>) scalar.getJsonView().getData().get(0);
        Map<?, ?> tableRow = (Map<?, ?>) scalar.getTableView().getRows().get(0);
        Assert.assertEquals(expected, jsonRow.get("payload"));
        Assert.assertSame(jsonRow, tableRow.get("result"));
        Assert.assertEquals(1024 * 1024, ((String) scalarRow.get("payload")).length());
        Assert.assertTrue(scalar.isDefaultLimitApplied());
        Assert.assertEquals(300, scalar.getDefaultLimit());
    }

    @Test
    public void testCypherPreviewAddsOnlyFinalOutputLimitsBeforeDispatch() throws Exception {
        this.assertCypherPreview("MATCH (n) RETURN n", "MATCH (n) RETURN n LIMIT 300", true);
        this.assertCypherPreview("MATCH (n) WITH n LIMIT 5 RETURN n;",
                                 "MATCH (n) WITH n LIMIT 5 RETURN n LIMIT 300;", true);
        this.assertCypherPreview("UNWIND range(1, 1000) AS value RETURN value;",
                                 "UNWIND range(1, 1000) AS value RETURN value LIMIT 300;", true);
        this.assertCypherPreview("MATCH (n) RETURN n LIMIT 10000;",
                                 "MATCH (n) RETURN n LIMIT 1001;", false);
        this.assertCypherPreview("MATCH (n) RETURN n.city, count(*) AS total",
                                 "MATCH (n) RETURN n.city, count(*) AS total LIMIT 300", true);
        this.assertCypherPreview("MATCH (n) RETURN count(n) AS total LIMIT 12000",
                                 "MATCH (n) RETURN count(n) AS total LIMIT 1001", false);
        this.assertCypherPreview("MATCH (n) SET n.name = 'Alice' RETURN n",
                                 "MATCH (n) SET n.name = 'Alice' RETURN n", false);
        this.assertCypherPreview("CALL db.labels() YIELD label RETURN label",
                                 "CALL db.labels() YIELD label RETURN label", false);
    }

    @Test
    public void testCypherCollectionPreviewsRequireAsyncBeforeDispatch() throws Exception {
        CypherManager cypher = Mockito.mock(CypherManager.class);
        HugeClient client = this.mockClient(this.mockGremlin());
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();
        for (String content : new String[]{"MATCH (n) RETURN collect(n)",
                                           "MATCH (n) RETURN collect(n) LIMIT 1",
                                           "MATCH (n) WITH collect(n) AS nodes RETURN nodes",
                                           "MATCH (n) WITH n LIMIT 10 RETURN collect(n)",
                                           "UNWIND [1, 2] AS n RETURN collect(n)"}) {
            ExternalException error = (ExternalException) Assert.assertThrows(ExternalException.class,
                    () -> service.executeCypherQuery(client, content, true));
            Assert.assertEquals("gremlin.preview.collection_requires_async", error.getMessage());
        }
        Mockito.verifyNoInteractions(cypher);
    }

    @Test
    public void testRawAndAsyncCypherCollectionsKeepTheirOriginalQuery() throws Exception {
        for (String content : new String[]{"MATCH (n) RETURN collect(n) AS nodes",
                                           "RETURN range(1, 1000000000) AS nodes"}) {
            Map<String, Object> row = Map.of("nodes", List.of("Alice", "Bob"));
            CypherManager cypher = Mockito.mock(CypherManager.class);
            Mockito.when(cypher.cypher(content)).thenReturn(this.resultSet(row));
            Mockito.when(cypher.executeAsTask(content)).thenReturn(7L);
            HugeClient client = this.mockClient(this.mockGremlin());
            Mockito.when(client.cypher()).thenReturn(cypher);
            QueryService service = this.serviceWithConfig();

            GremlinResult raw = service.executeCypherQuery(client, content);
            Assert.assertEquals(row, raw.getJsonView().getData().get(0));
            Assert.assertFalse(raw.isPropertiesTruncated());
            Assert.assertEquals(Long.valueOf(7L), service.executeCypherAsyncTask(client, content));
            Mockito.verify(cypher).cypher(content);
            Mockito.verify(cypher).executeAsTask(content);
        }
    }

    @Test
    public void testCypherWithoutPreviewAndAsyncKeepFullResultsAndQuery() throws Exception {
        String content = "MATCH (n) RETURN n";
        String payload = "x".repeat(1024 * 1024);
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            rows.add(Map.of("n", Map.of("_type", "node", "_label", "person", "_id", i, "payload", payload)));
        }
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(content)).thenReturn(this.resultSet(rows.toArray()));
        Mockito.when(cypher.executeAsTask(content)).thenReturn(7L);
        HugeClient client = this.mockClient(this.mockGremlin());
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();
        GremlinResult raw = service.executeCypherQuery(client, content);

        Assert.assertEquals(1001, raw.getJsonView().getData().size());
        Assert.assertEquals(1001, raw.getTableView().getRows().size());
        Assert.assertSame(payload, ((Vertex) raw.getJsonView().getData().get(0)).property("payload"));
        Assert.assertSame(payload, raw.getGraphView().getVertices().iterator().next().property("payload"));
        Assert.assertFalse(raw.isResultTruncated());
        Assert.assertFalse(raw.isPropertiesTruncated());
        Assert.assertNull(raw.getPropertyPreviewVertexIds());
        Assert.assertEquals(Long.valueOf(7L), service.executeCypherAsyncTask(client, content));
        Mockito.verify(cypher).cypher(content);
        Mockito.verify(cypher).executeAsTask(content);
    }

    @Test
    public void testPreviewCapsLargeReadUpstreamWithoutDefaultLimitNotice()
           throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        GremlinQuery query = new GremlinQuery("g.V().outE().inV().path().limit(12000)");
        query.setPreview(true);
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), query);
        Mockito.verify(gremlin).gremlin("g.V().outE().inV().path().limit(12000).limit(1001)");
        Assert.assertFalse(result.isDefaultLimitApplied());
    }

    @Test
    public void testPreviewRetainsDefaultLimitNotice() throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        GremlinQuery query = new GremlinQuery("g.V()");
        query.setPreview(true);
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), query);
        Mockito.verify(gremlin).gremlin("g.V().limit(300).limit(1001)");
        Assert.assertTrue(result.isDefaultLimitApplied());
        Assert.assertEquals(300, result.getDefaultLimit());
    }

    @Test
    public void testPreviewDoesNotRewriteMutations() throws Exception {
        for (String content : new String[]{"g.V().property('x', 1).out()", "g.V().drop()"}) {
            GremlinManager gremlin = this.mockGremlin();
            GremlinQuery query = new GremlinQuery(content);
            query.setPreview(true);
            GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                                   this.mockClient(gremlin), query);
            Mockito.verify(gremlin).gremlin(content);
            Assert.assertFalse(result.isDefaultLimitApplied());
        }
    }

    @Test
    public void testCollectionPreviewsFailBeforeExecutionAndAsyncKeepsTheOriginalQuery() throws Exception {
        for (String content : new String[]{"g.V().fold()", "g.V().group().by(label)",
                                           "g.V().groupCount().by(label)", "g.V().limit(10).fold()",
                                           "g.V().limit(10).groupCount().by(label)"}) {
            GremlinManager gremlin = this.mockGremlin();
            QueryService service = this.serviceWithConfig();
            GremlinQuery query = new GremlinQuery(content);
            query.setPreview(true);
            ExternalException error = (ExternalException) Assert.assertThrows(ExternalException.class,
                    () -> service.executeGremlinQuery(this.mockClient(gremlin), query));
            Assert.assertEquals("gremlin.preview.collection_requires_async", error.getMessage());
            Mockito.verify(gremlin, Mockito.never()).gremlin(Mockito.anyString());

            query.setPreview(false);
            service.executeGremlinQuery(this.mockClient(gremlin), query);
            Mockito.verify(gremlin).gremlin(content);
            query.setPreview(true);
            Mockito.when(gremlin.executeAsTask(Mockito.any())).thenReturn(7L);
            service.executeGremlinAsyncTask(this.mockClient(gremlin), query);
            Mockito.verify(gremlin).executeAsTask(Mockito.argThat(request -> content.equals(request.gremlin)));
        }
    }

    @Test
    public void testScalarOutputLimitsFollowTheReductionBeforeDispatch() throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        GremlinQuery query = new GremlinQuery("g.V().count()");
        query.setPreview(true);
        this.serviceWithConfig().executeGremlinQuery(this.mockClient(gremlin), query);
        Mockito.verify(gremlin).gremlin("g.V().count().limit(300).limit(1001)");
    }

    @Test
    public void testOversizedVertexResultDoesNotEnrich() throws Exception {
        List<Vertex> vertices = this.vertices(12000);
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertices.toArray()));
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V().limit(12000)"));
        Assert.assertEquals(1000, result.getGraphView().getVertices().size());
        Assert.assertTrue(result.isGraphTruncated());
        Assert.assertFalse(result.isDefaultLimitApplied());
        Mockito.verify(gremlin, Mockito.times(1)).gremlin(Mockito.anyString());
    }

    @Test
    public void testInteractivePreviewIsOptInAndBounded() throws Exception {
        List<Vertex> vertices = this.vertices(12000);
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertices.toArray()));
        GremlinQuery query = new GremlinQuery("g.V().limit(12000)");
        query.setPreview(true);
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), query);
        Assert.assertEquals(1000, result.getJsonView().getData().size());
        Assert.assertEquals(1000, result.getTableView().getRows().size());
        Assert.assertTrue(result.isResultTruncated());
        Assert.assertTrue(result.isGraphTruncated());
        Assert.assertEquals(1000, result.getResultLimit());
        Mockito.verify(gremlin, Mockito.times(1)).gremlin(Mockito.anyString());
    }

    @Test
    public void testPreviewReportsTruncatedFlattenedGraphRows() throws Exception {
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            rows.add(Map.of("a", Map.of("_type", "node", "_label", "person", "_id", i * 2),
                            "b", Map.of("_type", "node", "_label", "person", "_id", i * 2 + 1)));
        }
        GremlinManager gremlin = this.mockGremlin(this.resultSet(rows.toArray()));
        GremlinQuery query = new GremlinQuery("g.V().limit(600)");
        query.setPreview(true);
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), query);
        Assert.assertEquals(1000, result.getJsonView().getData().size());
        Assert.assertTrue(result.isResultTruncated());
        Assert.assertTrue(result.isGraphTruncated());
        Mockito.verify(gremlin, Mockito.times(1)).gremlin(Mockito.anyString());
    }

    @Test
    public void testPathReusesExistingEndpoints() throws Exception {
        List<Vertex> vertices = this.vertices(2);
        Edge edge = this.edge("e", 0, 1);
        Path path = Mockito.mock(Path.class);
        Mockito.when(path.objects()).thenReturn(Arrays.asList(vertices.get(0), edge, vertices.get(1)));
        GremlinManager gremlin = this.mockGremlin(this.resultSet(path));
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V().outE().inV().path()"));
        Assert.assertEquals(2, result.getGraphView().getVertices().size());
        Assert.assertEquals(1, result.getGraphView().getEdges().size());
        Assert.assertFalse(result.isGraphTruncated());
        Mockito.verify(gremlin, Mockito.times(1)).gremlin(Mockito.anyString());
    }

    @Test
    public void testPreviewBoundsPathBeforeBuildingAnyViewOrEnrichingIt() throws Exception {
        List<Object> objects = new AbstractList<>() {
            @Override
            public Object get(int index) {
                Assert.assertTrue("Omitted path objects must never be read", index < 1000);
                Vertex vertex = new Vertex("person");
                vertex.id(index);
                vertex.property("payload", "x".repeat(100000));
                return vertex;
            }

            @Override
            public int size() {
                return 100000;
            }
        };
        Path original = Mockito.mock(Path.class);
        Mockito.when(original.objects()).thenReturn(objects);
        Mockito.when(original.labels()).thenReturn(List.of());
        // Mock the ResultSet boundary: SDK attachment itself traverses a Path,
        // while this regression targets work performed by the Hubble service.
        ResultSet results = Mockito.mock(ResultSet.class);
        Result row = Mockito.mock(Result.class);
        Mockito.when(row.getObject()).thenReturn(original);
        Mockito.when(results.iterator()).thenAnswer(invocation -> List.of(row).iterator());
        Mockito.when(results.size()).thenReturn(1);
        GremlinManager gremlin = this.mockGremlin(results);
        GremlinQuery query = new GremlinQuery("g.V().path().limit(1)");
        query.setPreview(true);
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(this.mockClient(gremlin), query);

        Path json = (Path) result.getJsonView().getData().get(0);
        Assert.assertEquals(1000, json.objects().size());
        List<?> tableIds = (List<?>) ((Map<?, ?>) result.getTableView().getRows().get(0)).get("path");
        Assert.assertEquals(1000, tableIds.size());
        Assert.assertEquals(999, tableIds.get(999));
        Assert.assertEquals(1000, result.getGraphView().getVertices().size());
        Vertex graphVertex = result.getGraphView().getVertices().iterator().next();
        Vertex jsonVertex = (Vertex) json.objects().get(0);
        Assert.assertSame(jsonVertex.properties(), graphVertex.properties());
        Assert.assertEquals("x".repeat(256) + "… [preview 256/100000 chars]",
                            graphVertex.property("payload"));
        Assert.assertTrue(result.isResultTruncated());
        Assert.assertTrue(result.isGraphTruncated());
        Assert.assertTrue(result.isPropertiesTruncated());
        Mockito.verify(gremlin, Mockito.times(1)).gremlin(Mockito.anyString());
    }

    @Test
    public void testCompletePathAtEdgeBudgetIsNotTruncated() throws Exception {
        List<Vertex> vertices = this.vertices(1000);
        List<Object> elements = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            elements.add(vertices.get(i * 2));
            elements.add(this.edge("e" + i, i * 2, i * 2 + 1));
            elements.add(vertices.get(i * 2 + 1));
        }
        Path path = Mockito.mock(Path.class);
        Mockito.when(path.objects()).thenReturn(elements);
        GremlinManager gremlin = this.mockGremlin(this.resultSet(path));
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V().path().limit(1)"));
        Assert.assertEquals(500, result.getGraphView().getEdges().size());
        Assert.assertFalse(result.isGraphTruncated());
        Mockito.verify(gremlin, Mockito.times(1)).gremlin(Mockito.anyString());
    }

    @Test
    public void testEdgeBudgetStopsRemainingBatches() throws Exception {
        List<Vertex> vertices = this.vertices(300);
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            edges.add(this.edge("e" + i, i % 100, (i + 1) % 100));
        }
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertices.toArray()),
                                                  this.resultSet(edges.toArray()));
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V().limit(300)"));
        Assert.assertEquals(500, result.getGraphView().getEdges().size());
        Assert.assertTrue(result.isGraphTruncated());
        Mockito.verify(gremlin, Mockito.times(2)).gremlin(Mockito.anyString());
    }

    @Test
    public void testExtraEnrichmentCandidateReportsPartialEvenWhenEdgesAreFiltered()
           throws Exception {
        List<Edge> externalEdges = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            externalEdges.add(this.edge("e" + i, 0, 2000 + i));
        }
        GremlinManager gremlin = this.mockGremlin(this.resultSet(this.vertices(1).toArray()),
                                                  this.resultSet(externalEdges.toArray()));
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V().limit(1)"));
        Assert.assertEquals(0, result.getGraphView().getEdges().size());
        Assert.assertTrue(result.isGraphTruncated());
        Mockito.verify(gremlin, Mockito.times(2)).gremlin(Mockito.anyString());
    }

    @Test
    public void testExactlyFullSingleEnrichmentBatchIsNotTruncated() throws Exception {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            edges.add(this.edge("e" + i, i % 100, (i + 1) % 100));
        }
        GremlinManager gremlin = this.mockGremlin(this.resultSet(this.vertices(100).toArray()),
                                                  this.resultSet(edges.toArray()));
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(
                               this.mockClient(gremlin), new GremlinQuery("g.V().limit(100)"));
        Assert.assertEquals(500, result.getGraphView().getEdges().size());
        Assert.assertFalse(result.isGraphTruncated());
        Mockito.verify(gremlin).gremlin(Mockito.argThat(query ->
                query.contains(".bothE()") && query.endsWith(".limit(501)")));
        Mockito.verify(gremlin, Mockito.times(2)).gremlin(Mockito.anyString());
    }

    @Test
    public void testOversizedMapKeysAreOmittedWithoutMergingOtherEntries() throws Exception {
        String prefix = "x".repeat(1024 * 1024 - 1);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(prefix + "a", 1L);
        map.put(prefix + "b", 2L);
        map.put("ordinary", 3L);
        GremlinManager gremlin = this.mockGremlin(this.resultSet(map));
        GremlinQuery query = new GremlinQuery("g.V().valueMap().limit(1000)");
        query.setPreview(true);
        GremlinResult result = this.serviceWithConfig().executeGremlinQuery(this.mockClient(gremlin), query);
        Assert.assertEquals(Map.of("ordinary", 3L), result.getJsonView().getData().get(0));
        Assert.assertTrue(result.isPropertiesTruncated());
        Assert.assertEquals(3, map.size());
        Mockito.verify(gremlin).gremlin(query.getContent() + ".limit(1001)");
    }

    @Test
    public void testDegreesPersistAcrossBatchesAndDoNotStopOtherVertices()
           throws Exception {
        List<Vertex> vertices = this.vertices(101);
        Edge first = this.edge("first", 0, 1);
        Edge blocked = this.edge("blocked", 0, 2);
        Edge allowed = this.edge("allowed", 2, 3);
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertices.toArray()),
                                                  this.resultSet(first, first),
                                                  this.resultSet(blocked, allowed));
        QueryService service = this.serviceWithConfig();
        Field field = QueryService.class.getDeclaredField("config");
        field.setAccessible(true);
        HugeConfig config = (HugeConfig) field.get(service);
        Mockito.when(config.get(HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT)).thenReturn(1);
        GremlinResult result = service.executeGremlinQuery(this.mockClient(gremlin),
                               new GremlinQuery("g.V().limit(101)"));
        Assert.assertEquals(2, result.getGraphView().getEdges().size());
        Assert.assertTrue(result.isGraphTruncated());
        Assert.assertTrue(result.getGraphView().getEdges().contains(first));
        Assert.assertTrue(result.getGraphView().getEdges().contains(allowed));
    }

    @Test
    public void testAsyncKeepsOriginalQuery() throws Exception {
        GremlinManager gremlin = Mockito.mock(GremlinManager.class);
        Mockito.when(gremlin.executeAsTask(Mockito.any())).thenReturn(7L);
        GremlinQuery query = new GremlinQuery("g.V()");
        query.setPreview(true);
        this.serviceWithConfig().executeGremlinAsyncTask(this.mockClient(gremlin), query);
        Mockito.verify(gremlin).executeAsTask(Mockito.argThat(request ->
                "g.V()".equals(request.gremlin)));
        Mockito.verify(gremlin, Mockito.never()).gremlin(Mockito.anyString());
    }

    private List<Vertex> vertices(int size) {
        List<Vertex> vertices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Vertex vertex = new Vertex("person");
            vertex.id(i);
            vertices.add(vertex);
        }
        return vertices;
    }

    private Edge edge(String id, int source, int target) {
        Edge edge = new Edge("knows");
        edge.id(id);
        edge.sourceId(source);
        edge.targetId(target);
        return edge;
    }

    @Test
    public void testQueryIgnoresNonEdgeAdjacentResults() throws Exception {
        Vertex vertex = new Vertex("person");
        vertex.id("isolated");
        GremlinManager gremlin = this.mockGremlin(this.resultSet(vertex),
                                                  this.resultSet(vertex));
        HugeClient client = this.mockClient(gremlin);
        QueryService service = this.serviceWithConfig();

        GremlinResult result = service.executeGremlinQuery(
                               client, new GremlinQuery("g.V('isolated')"));

        Assert.assertEquals(1, result.getGraphView().getVertices().size());
        Assert.assertEquals(0, result.getGraphView().getEdges().size());
    }

    @Test
    public void testExpandVertexEscapesGremlinLiterals() throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        HugeClient client = this.mockClient(gremlin);
        QueryService service = this.serviceWithConfig();

        AdjacentQuery query = AdjacentQuery.builder()
                                           .vertexId("marko'id")
                                           .vertexLabel("person")
                                           .edgeLabel("edge'label\\x\r\n")
                                           .direction(Direction.BOTH)
                                           .conditions(Collections.singletonList(
                                                   AdjacentQuery.Condition
                                                   .builder()
                                                   .key("name'key\r")
                                                   .operator("eq")
                                                   .value("value'quote\\slash\r\n")
                                                   .build()))
                                           .build();

        service.expandVertex(client, query);

        Mockito.verify(gremlin).gremlin(
                "g.V('marko\\'id').toE(BOTH, 'edge\\'label\\\\x\\r\\n')" +
                ".has('name\\'key\\r', eq('value\\'quote\\\\slash\\r\\n'))" +
                ".limit(100).otherV().path()");
    }

    @Test
    public void testExpandVertexRejectsInvalidOperatorInService()
           throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        HugeClient client = this.mockClient(gremlin);
        QueryService service = this.serviceWithConfig();

        AdjacentQuery query = AdjacentQuery.builder()
                                           .vertexId("marko")
                                           .vertexLabel("person")
                                           .conditions(Collections.singletonList(
                                                   AdjacentQuery.Condition
                                                   .builder()
                                                   .key("name")
                                                   .operator("drop")
                                                   .value("value")
                                                   .build()))
                                           .build();

        Assert.assertThrows(ExternalException.class, () -> {
            service.expandVertex(client, query);
        });
        Mockito.verify(gremlin, Mockito.never()).gremlin(Mockito.anyString());
    }

    @Test
    public void testExpandVertexRejectsStructuredConditionValue()
           throws Exception {
        GremlinManager gremlin = this.mockGremlin();
        HugeClient client = this.mockClient(gremlin);
        QueryService service = this.serviceWithConfig();

        AdjacentQuery query = AdjacentQuery.builder()
                                           .vertexId("marko")
                                           .vertexLabel("person")
                                           .conditions(Collections.singletonList(
                                                   AdjacentQuery.Condition
                                                   .builder()
                                                   .key("name")
                                                   .operator("eq")
                                                   .value(Collections.singletonMap(
                                                           "payload",
                                                           "x'));g.V().drop();//"))
                                                   .build()))
                                           .build();

        Assert.assertThrows(ExternalException.class, () -> {
            service.expandVertex(client, query);
        });
        Mockito.verify(gremlin, Mockito.never()).gremlin(Mockito.anyString());
    }

    @Test
    public void testGremlinPreservesServerUnavailableStatus() throws Exception {
        ServerException server = new ServerException((String) null);
        server.status(503);
        GremlinManager gremlin = this.mockGremlinFailure(server);
        QueryService service = this.serviceWithConfig();

        ExternalException error = (ExternalException)
                                  Assert.assertThrows(ExternalException.class,
                                                      () -> {
            service.executeGremlinQuery(this.mockClient(gremlin),
                                        new GremlinQuery("g.V()"));
        });

        Assert.assertEquals(503, error.status());
        Assert.assertEquals("gremlin.server.unavailable", error.getMessage());
    }

    @Test
    public void testGremlinHidesServerUnavailableDetail() throws Exception {
        String detail = "The server is too busy to process the request";
        ServerException server = new ServerException(detail);
        server.status(503);
        GremlinManager gremlin = this.mockGremlinFailure(server);
        QueryService service = this.serviceWithConfig();

        ExternalException error = (ExternalException)
                                  Assert.assertThrows(ExternalException.class,
                                                      () -> {
            service.executeGremlinQuery(this.mockClient(gremlin),
                                        new GremlinQuery("g.V()"));
        });

        Assert.assertEquals(503, error.status());
        Assert.assertEquals("gremlin.server.unavailable", error.getMessage());
        Assert.assertEquals(0, error.args().length);
    }

    @Test
    public void testGremlinSeparatesUpstreamUnauthorizedFromHubbleSession()
           throws Exception {
        ServerException server = new ServerException("Unauthorized");
        server.status(401);
        GremlinManager gremlin = this.mockGremlinFailure(server);
        QueryService service = this.serviceWithConfig();

        ExternalException error = (ExternalException)
                                  Assert.assertThrows(ExternalException.class,
                                                      () -> {
            service.executeGremlinQuery(this.mockClient(gremlin),
                                        new GremlinQuery("g.V()"));
        });

        Assert.assertEquals(502, error.status());
        Assert.assertEquals("gremlin.server.authentication-failed",
                            error.getMessage());
        Assert.assertEquals(0, error.args().length);
    }

    @Test
    public void testCypherSeparatesUpstreamUnauthorizedFromHubbleSession()
           throws Exception {
        ServerException server = new ServerException("Unauthorized");
        server.status(401);
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(Mockito.anyString())).thenThrow(server);
        HugeClient client = this.mockClient(this.mockGremlin());
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();

        ExternalException error = (ExternalException)
                                  Assert.assertThrows(ExternalException.class,
                                                      () -> {
            service.executeCypherQuery(client, "MATCH (n) RETURN n");
        });

        Assert.assertEquals(502, error.status());
        Assert.assertEquals("gremlin.server.authentication-failed",
                            error.getMessage());
        Assert.assertEquals(0, error.args().length);
    }

    @Test
    public void testAsyncQueriesSeparateUnauthorizedFromHubbleSession()
           throws Exception {
        ServerException server = new ServerException("Unauthorized");
        server.status(401);
        GremlinManager gremlin = Mockito.mock(GremlinManager.class);
        Mockito.when(gremlin.executeAsTask(Mockito.any())).thenThrow(server);
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.executeAsTask(Mockito.anyString()))
               .thenThrow(server);
        HugeClient client = this.mockClient(gremlin);
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();

        ExternalException gremlinError = (ExternalException)
                                         Assert.assertThrows(
                                         ExternalException.class, () -> {
            service.executeGremlinAsyncTask(client, new GremlinQuery("g.V()"));
        });
        ExternalException cypherError = (ExternalException)
                                        Assert.assertThrows(
                                        ExternalException.class, () -> {
            service.executeCypherAsyncTask(client, "MATCH (n) RETURN n");
        });

        Assert.assertEquals(502, gremlinError.status());
        Assert.assertEquals("gremlin.server.authentication-failed",
                            gremlinError.getMessage());
        Assert.assertEquals(502, cypherError.status());
        Assert.assertEquals("gremlin.server.authentication-failed",
                            cypherError.getMessage());
    }

    private void assertCypherPreview(String query, String dispatched, boolean defaultApplied) throws Exception {
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(dispatched)).thenReturn(this.resultSet());
        HugeClient client = this.mockClient(this.mockGremlin());
        Mockito.when(client.cypher()).thenReturn(cypher);
        GremlinResult result = this.serviceWithConfig().executeCypherQuery(client, query, true);
        Mockito.verify(cypher).cypher(dispatched);
        Assert.assertEquals(defaultApplied, result.isDefaultLimitApplied());
        Assert.assertEquals(300, result.getDefaultLimit());
    }

    private JsonNode serializedQuery(GremlinResult result) throws Exception {
        // Use the same element serializers registered by Hubble's HTTP layer.
        SimpleModule module = new SimpleModule();
        module.addSerializer(Vertex.class, new JacksonConfig.VertexSerializer());
        module.addSerializer(Edge.class, new JacksonConfig.EdgeSerializer());
        ObjectMapper mapper = new ObjectMapper().registerModule(module);
        return mapper.readTree(mapper.writeValueAsString(result));
    }

    private QueryService serviceWithConfig() throws Exception {
        return this.serviceWithConfig(300);
    }

    private QueryService serviceWithConfig(int defaultLimit) throws Exception {
        QueryService service = new QueryService();
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT))
               .thenReturn(defaultLimit);
        Mockito.when(config.get(HubbleOptions.GREMLIN_VERTEX_DEGREE_LIMIT))
               .thenReturn(100);
        Mockito.when(config.get(HubbleOptions.GREMLIN_BATCH_QUERY_IDS))
               .thenReturn(100);
        Mockito.when(config.get(HubbleOptions.GREMLIN_EDGES_TOTAL_LIMIT))
               .thenReturn(500);
        this.setField(service, "config", config);
        VertexLabelService vlService = Mockito.mock(VertexLabelService.class);
        Mockito.when(vlService.get(Mockito.eq("person"), Mockito.any()))
               .thenReturn(VertexLabelEntity.builder()
                                            .name("person")
                                            .idStrategy(IdStrategy.CUSTOMIZE_STRING)
                                            .build());
        this.setField(service, "vlService", vlService);
        return service;
    }

    private HugeClient mockClient(GremlinManager gremlin) {
        HugeClient client = Mockito.mock(HugeClient.class);
        Mockito.when(client.gremlin()).thenReturn(gremlin);
        return client;
    }

    private GremlinManager mockGremlin() throws Exception {
        return this.mockGremlin(this.resultSet());
    }

    private GremlinManager mockGremlin(ResultSet... resultSets) throws Exception {
        GremlinManager gremlin = Mockito.mock(GremlinManager.class);
        Mockito.when(gremlin.gremlin(Mockito.anyString()))
               .thenAnswer(invocation -> new GremlinRequest.Builder(
                       invocation.getArgument(0), gremlin));
        Mockito.when(gremlin.execute(Mockito.any()))
               .thenReturn(resultSets[0], Arrays.copyOfRange(resultSets, 1,
                                                              resultSets.length));
        return gremlin;
    }

    private GremlinManager mockGremlinFailure(ServerException failure) {
        GremlinManager gremlin = Mockito.mock(GremlinManager.class);
        Mockito.when(gremlin.gremlin(Mockito.anyString()))
               .thenAnswer(invocation -> new GremlinRequest.Builder(
                       invocation.getArgument(0), gremlin));
        Mockito.when(gremlin.execute(Mockito.any())).thenThrow(failure);
        return gremlin;
    }

    private ResultSet resultSet(Object... data) throws Exception {
        ResultSet resultSet = new ResultSet();
        this.setField(resultSet, "data", Arrays.asList(data));
        resultSet.graphManager(Mockito.mock(GraphManager.class));
        return resultSet;
    }

    private void setField(Object object, String name, Object value)
                          throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
