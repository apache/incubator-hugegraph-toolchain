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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.api.gremlin.GremlinRequest;
import org.apache.hugegraph.config.HugeConfig;
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
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.testutil.Assert;

public class QueryServiceTest {

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

    @Test
    public void testEmptyResultsAreIsolatedAcrossQueryLanguages() throws Exception {
        HugeClient client = this.mockClient(this.mockGremlin());
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(Mockito.anyString())).thenReturn(this.resultSet());
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();

        GremlinResult first = service.executeGremlinQuery(client, new GremlinQuery("g.V()"));
        first.getGraphView().getStatistics().put("stale", true);
        first.getTableView().setHeader(List.of("stale"));
        first.getTableView().setRows(List.of("stale"));

        GremlinResult second = service.executeCypherQuery(client, "MATCH (n) RETURN n");
        GremlinResult third = service.executeGremlinQuery(client, new GremlinQuery("g.V()"));
        ObjectMapper mapper = new ObjectMapper();
        for (GremlinResult result : List.of(second, third)) {
            Assert.assertEquals(GremlinResult.Type.EMPTY, result.getType());
            Assert.assertNull(result.getJsonView().getData());
            Assert.assertEquals(mapper.readTree("{\"header\":null,\"rows\":null}"),
                                mapper.valueToTree(result.getTableView()));
            Assert.assertEquals(mapper.readTree("{\"vertices\":null,\"edges\":null,\"statistics\":{}}"),
                                mapper.valueToTree(result.getGraphView()));
            Assert.assertNotSame(first.getTableView(), result.getTableView());
            Assert.assertNotSame(first.getGraphView(), result.getGraphView());
        }
    }

    @Test
    public void testScalarQueriesHaveTheSameResultShape() throws Exception {
        ResultSet data = this.resultSet(1, "name", Map.of("count", 2));
        HugeClient client = this.mockClient(this.mockGremlin(data));
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(Mockito.anyString())).thenReturn(data);
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();

        GremlinResult gremlin = service.executeGremlinQuery(client, new GremlinQuery("g.V().count()"));
        GremlinResult cypherResult = service.executeCypherQuery(client, "RETURN 1");

        Assert.assertEquals(GremlinResult.Type.GENERAL, gremlin.getType());
        Assert.assertEquals(List.of("result"), gremlin.getTableView().getHeader());
        Assert.assertEquals(List.of(Map.of("result", 1), Map.of("result", "name"),
                                   Map.of("result", Map.of("count", 2))), gremlin.getTableView().getRows());
        ObjectMapper mapper = new ObjectMapper();
        Assert.assertEquals(mapper.valueToTree(gremlin), mapper.valueToTree(cypherResult));
    }

    @Test
    public void testPathTablePreservesScalarAndNullValues() throws Exception {
        Vertex vertex = new Vertex("person");
        vertex.id("marko");
        Edge edge = new Edge("knows");
        edge.id("edge1");
        Path path = new Path(Arrays.asList(vertex, edge, "step", null));
        HugeClient client = this.mockClient(this.mockGremlin(this.resultSet(path)));
        QueryService service = this.serviceWithConfig();

        GremlinResult result = service.executeGremlinQuery(client, new GremlinQuery("g.V().path()"));

        Assert.assertEquals(GremlinResult.Type.PATH, result.getType());
        Assert.assertEquals(List.of("path"), result.getTableView().getHeader());
        Assert.assertEquals(List.of(Map.of("path", Arrays.asList("marko", "edge1", "step", null))),
                            result.getTableView().getRows());
        Assert.assertEquals(List.of(path), result.getJsonView().getData());
        Assert.assertNull(result.getGraphView().getVertices());
        Assert.assertTrue(result.getGraphView().getStatistics().isEmpty());
    }

    @Test
    public void testEdgeTableFiltersOtherResultTypes() throws Exception {
        Vertex source = new Vertex("person");
        source.id("marko");
        Vertex target = new Vertex("person");
        target.id("vadas");
        Edge first = new Edge("knows");
        first.id("edge1");
        first.sourceId(source.id());
        first.targetId(target.id());
        Edge second = new Edge("knows");
        second.id("edge2");
        second.sourceId(source.id());
        second.targetId(target.id());
        HugeClient client = this.mockClient(this.mockGremlin(this.resultSet(first, second, "scalar"),
                                                            this.resultSet(source, target)));
        QueryService service = this.serviceWithConfig();

        GremlinResult result = service.executeGremlinQuery(client, new GremlinQuery("g.E()"));

        Assert.assertEquals(GremlinResult.Type.EDGE, result.getType());
        Assert.assertEquals(List.of("id", "label", "source", "target", "properties"),
                            result.getTableView().getHeader());
        Assert.assertEquals(List.of(first, second), result.getTableView().getRows());
        Assert.assertEquals(List.of(first, second, "scalar"), result.getJsonView().getData());
        Assert.assertEquals(2, result.getGraphView().getVertices().size());
        Assert.assertEquals(2, result.getGraphView().getEdges().size());
    }

    @Test
    public void testCypherNodeRowsBuildVertexViews() throws Exception {
        Map<String, Object> node = Map.of("_type", "node", "_id", "marko", "_label", "person", "name", "Marko");
        HugeClient client = this.mockClient(this.mockGremlin());
        CypherManager cypher = Mockito.mock(CypherManager.class);
        Mockito.when(cypher.cypher(Mockito.anyString())).thenReturn(this.resultSet(Map.of("n", node)));
        Mockito.when(client.cypher()).thenReturn(cypher);
        QueryService service = this.serviceWithConfig();

        GremlinResult result = service.executeCypherQuery(client, "MATCH (n) RETURN n");

        Assert.assertEquals(GremlinResult.Type.VERTEX, result.getType());
        Assert.assertEquals(List.of("id", "label", "properties"), result.getTableView().getHeader());
        Vertex vertex = (Vertex) result.getTableView().getRows().get(0);
        Assert.assertEquals("marko", vertex.id());
        Assert.assertEquals("person", vertex.label());
        Assert.assertEquals("Marko", vertex.property("name"));
        Assert.assertEquals(1, result.getGraphView().getVertices().size());
        Assert.assertTrue(result.getGraphView().getEdges().isEmpty());
    }

    private QueryService serviceWithConfig() throws Exception {
        QueryService service = new QueryService();
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.GREMLIN_SUFFIX_LIMIT))
               .thenReturn(250);
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
