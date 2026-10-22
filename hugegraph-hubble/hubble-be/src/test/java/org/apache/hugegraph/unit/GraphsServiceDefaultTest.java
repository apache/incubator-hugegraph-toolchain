/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import org.apache.hugegraph.api.graph.GraphMetricsAPI;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.GraphsManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.graphs.GraphStatisticsEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.auth.AuthModeService;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.service.HugeClientPoolService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.hugegraph.service.schema.SchemaService;
import org.apache.hugegraph.structure.gremlin.ResultSet;

import com.baomidou.mybatisplus.core.metadata.IPage;

public class GraphsServiceDefaultTest {

    private HugeClient client;
    private GraphsManager graphs;
    private GraphsService service;
    private AuthModeService authMode;

    @Before
    public void setup() {
        this.client = Mockito.mock(HugeClient.class);
        this.graphs = Mockito.mock(GraphsManager.class);
        Mockito.when(this.client.graphs()).thenReturn(this.graphs);
        this.service = new GraphsService();
        ReflectionTestUtils.setField(this.service, "clientPool", Mockito.mock(HugeClientPoolService.class));
        this.authMode = Mockito.mock(AuthModeService.class);
        Mockito.when(this.authMode.enabled()).thenReturn(true);
        ReflectionTestUtils.setField(this.service, "authModeService", this.authMode);
    }

    @Test
    public void testSelectedGraphKeepsVermeerLoadTime() {
        Mockito.when(this.graphs.getGraph("graph"))
               .thenReturn(Collections.singletonMap("backend", "hstore"));
        Map<String, String> vermeer = new HashMap<>();
        vermeer.put("status", "loaded");
        vermeer.put("last_load_time", "2026-10-22T01:02:03Z");

        Map<String, Object> result = this.service.get(
                this.client, "space", "graph", vermeer);

        Assert.assertEquals("loaded", result.get("status"));
        Assert.assertEquals("2026-10-22T01:02:03Z", result.get("last_load_time"));
        Assert.assertEquals("graph", result.get("nickname"));
    }

    @Test
    public void testGraphListReturnsCoreMetadataWithoutStatisticsOrSchema() {
        GraphsService service = Mockito.spy(this.service);
        SchemaService schema = Mockito.mock(SchemaService.class);
        ReflectionTestUtils.setField(service, "schemaService", schema);
        Map<String, Object> first = profile("z", true);
        Map<String, Object> second = profile("b", false);
        Map<String, Object> third = profile("a", false);
        Mockito.when(this.graphs.listProfile(""))
               .thenReturn(Arrays.asList(second, first, third));

        IPage<Map<String, Object>> result = service.queryPage(
                this.client, "space", "", "", "", 2, 1,
                false, Collections.emptyMap());

        Assert.assertEquals(3L, result.getTotal());
        Assert.assertEquals("a", result.getRecords().get(0).get("name"));
        Assert.assertEquals(Collections.emptyMap(),
                            result.getRecords().get(0).get("statistic"));
        Mockito.verify(service, Mockito.never())
               .evCount(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verifyNoInteractions(schema);
        Mockito.verify(this.client, Mockito.never())
               .assignGraph(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testBriefGraphListOnlyReturnsNamesWithoutStatisticsOrSchema() {
        GraphsService service = Mockito.spy(this.service);
        SchemaService schema = Mockito.mock(SchemaService.class);
        ReflectionTestUtils.setField(service, "schemaService", schema);
        Map<String, Object> graph = profile("graph", false);
        graph.put("nickname", "Graph");
        graph.put("pd.peers", "private");
        Mockito.when(this.graphs.listProfile(""))
               .thenReturn(Collections.singletonList(graph));

        Map<String, Object> result = service.queryBriefPage(
                this.client, "space", "", "", 1, 100, "graph");

        Assert.assertEquals(1L, result.get("total"));
        Assert.assertEquals("Graph", briefRecords(result).get(0).get("nickname"));
        Assert.assertEquals(2, briefRecords(result).get(0).size());
        Assert.assertEquals(briefRecords(result).get(0), result.get("selected"));
        Assert.assertFalse(graph.containsKey("statistic"));
        Assert.assertFalse(graph.containsKey("status"));
        Mockito.verify(service, Mockito.never())
               .evCount(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verifyNoInteractions(schema);
        Mockito.verify(this.client, Mockito.never())
               .assignGraph(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testBriefGraphListPagesAboveTenThousandWithoutLosingTheSelectedGraph() {
        GraphsService service = Mockito.spy(this.service);
        SchemaService schema = Mockito.mock(SchemaService.class);
        ReflectionTestUtils.setField(service, "schemaService", schema);
        List<Map<String, Object>> authorized = new ArrayList<>();
        for (int index = 10000; index >= 0; index--) {
            Map<String, Object> graph = profile(String.format("graph_%05d", index), false);
            graph.put("nickname", "Alias " + index);
            graph.put("pd.peers", "private");
            authorized.add(graph);
        }
        Mockito.when(this.graphs.listProfile("")).thenReturn(authorized);

        Map<String, Object> first = service.queryBriefPage(
                this.client, "space", "", "", 1, 100, "graph_10000");

        Assert.assertEquals(10001L, first.get("total"));
        Assert.assertEquals(100, briefRecords(first).size());
        for (int index = 0; index < 100; index++) {
            Map<String, Object> graph = briefRecords(first).get(index);
            Assert.assertEquals(String.format("graph_%05d", index), graph.get("name"));
            Assert.assertEquals("Alias " + index, graph.get("nickname"));
            Assert.assertEquals(2, graph.size());
        }
        Map<?, ?> selected = (Map<?, ?>) first.get("selected");
        Assert.assertEquals("graph_10000", selected.get("name"));
        Assert.assertEquals("Alias 10000", selected.get("nickname"));
        Assert.assertEquals(2, selected.size());
        Mockito.verify(this.graphs).listProfile("");

        Map<String, Object> last = service.queryBriefPage(
                this.client, "space", "", "", 101, 100, "graph_00000");

        Assert.assertEquals(1, briefRecords(last).size());
        Assert.assertEquals("graph_10000", briefRecords(last).get(0).get("name"));
        Assert.assertEquals("graph_00000", ((Map<?, ?>) last.get("selected")).get("name"));
        Assert.assertTrue(authorized.stream().noneMatch(info -> info.containsKey("statistic") ||
                                                              info.containsKey("status")));
        Mockito.verify(this.graphs, Mockito.times(2)).listProfile("");
        Mockito.verifyNoMoreInteractions(this.graphs);
        Mockito.verify(service, Mockito.never())
               .evCount(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verifyNoInteractions(schema);
        Mockito.verify(this.client, Mockito.never())
               .assignGraph(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testBriefGraphSearchFindsAliasesOutsideTheFirstPageAndRetainsSelection() {
        Map<String, Object> selected = profile("first", false);
        selected.put("nickname", "Current graph");
        Map<String, Object> later = profile("last", false);
        later.put("nickname", "Late Needle Alias");
        Mockito.when(this.graphs.listProfile("")).thenReturn(Arrays.asList(selected, later));

        Map<String, Object> result = this.service.queryBriefPage(
                this.client, "space", "needle", "", 1, 1, "first");

        Assert.assertEquals(1L, result.get("total"));
        Assert.assertEquals("last", briefRecords(result).get(0).get("name"));
        Assert.assertEquals("first", ((Map<?, ?>) result.get("selected")).get("name"));
        Assert.assertEquals(2, ((Map<?, ?>) result.get("selected")).size());
        Mockito.verify(this.graphs).listProfile("");
        Mockito.verifyNoMoreInteractions(this.graphs);
    }

    @Test
    public void testBriefDefaultGraphPagesHaveStableNameTieBreaks() {
        Map<String, Object> first = profile("default_a", true);
        Map<String, Object> second = profile("default_b", true);
        first.put("default_update_time", "2026-10-22 10:00:00");
        second.put("default_update_time", "2026-10-22 10:00:00");
        Mockito.when(this.graphs.listProfile(""))
               .thenReturn(Arrays.asList(second, first), Arrays.asList(first, second));

        Map<String, Object> pageOne = this.service.queryBriefPage(
                this.client, "space", "", "", 1, 1, "");
        Map<String, Object> pageTwo = this.service.queryBriefPage(
                this.client, "space", "", "", 2, 1, "");

        Assert.assertEquals("default_a", briefRecords(pageOne).get(0).get("name"));
        Assert.assertEquals("default_b", briefRecords(pageTwo).get(0).get("name"));
        Mockito.verify(this.graphs, Mockito.times(2)).listProfile("");
        Mockito.verifyNoMoreInteractions(this.graphs);
    }

    @Test
    public void testBriefGraphListDoesNotInventAnUnavailableSelection() {
        Mockito.when(this.graphs.listProfile(""))
               .thenReturn(Collections.singletonList(profile("visible", false)));

        Map<String, Object> result = this.service.queryBriefPage(
                this.client, "space", "", "", 1, 100, "private");

        Assert.assertNull(result.get("selected"));
        Assert.assertEquals("visible", briefRecords(result).get(0).get("name"));
        Mockito.verify(this.graphs).listProfile("");
        Mockito.verifyNoMoreInteractions(this.graphs);
    }

    @Test
    public void testBriefGraphListDoesNotFallBackAfterForbiddenResponse() {
        ServerException forbidden = new ServerException("forbidden");
        forbidden.status(403);
        Mockito.when(this.graphs.listProfile("")).thenThrow(forbidden);

        try {
            this.service.queryBriefPage(this.client, "space", "", "", 1, 100, "private");
            Assert.fail("Expected forbidden response");
        } catch (ServerException actual) {
            Assert.assertSame(forbidden, actual);
        }

        Mockito.verify(this.graphs).listProfile("");
        Mockito.verifyNoMoreInteractions(this.graphs);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> briefRecords(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("records");
    }

    private static Map<String, Object> profile(String name, boolean isDefault) {
        Map<String, Object> graph = new HashMap<>();
        graph.put("name", name);
        graph.put("default", isDefault);
        return graph;
    }

    @Test
    public void testSetDefaultReplacesAllPreviousDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("default_graph", Arrays.asList("old_a", "old_b"));
        Mockito.when(this.graphs.getDefault()).thenReturn(defaults);

        this.service.setDefault(this.client, "target");

        InOrder order = Mockito.inOrder(this.graphs);
        order.verify(this.graphs).getDefault();
        order.verify(this.graphs).setDefault("target");
        order.verify(this.graphs).unSetDefault("old_a");
        order.verify(this.graphs).unSetDefault("old_b");
    }

    @Test
    public void testClearGraphUsesExplicitDestructiveConfirmation() {
        Mockito.when(this.graphs.getDefault()).thenReturn(
                Collections.singletonMap("default_graph",
                                         Collections.singletonList("default")));

        this.service.clearGraph(this.client, "graph_a");

        InOrder order = Mockito.inOrder(this.graphs);
        order.verify(this.graphs).getDefault();
        order.verify(this.graphs).clearGraph(
              "graph_a", "I'm sure to delete all data");
    }

    @Test
    public void testClearGraphRejectsDefaultGraph() {
        Mockito.when(this.graphs.getDefault()).thenReturn(
                Collections.singletonMap("default_graph",
                                         Arrays.asList("other", "graph_a")));

        try {
            this.service.clearGraph(this.client, "graph_a");
            Assert.fail("Expected default graph rejection");
        } catch (ExternalException ignored) {
            // Expected: default graphs must not be cleared.
        }

        Mockito.verify(this.graphs).getDefault();
        Mockito.verify(this.graphs, Mockito.never())
               .clearGraph(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetDefaultIsNoopWhenTargetIsOnlyDefault() {
        Mockito.when(this.graphs.getDefault()).thenReturn(
                Collections.singletonMap("default_graph",
                                         Collections.singletonList("target")));

        this.service.setDefault(this.client, "target");

        Mockito.verify(this.graphs).getDefault();
        Mockito.verify(this.graphs, Mockito.never()).unSetDefault(Mockito.anyString());
        Mockito.verify(this.graphs, Mockito.never()).setDefault(Mockito.anyString());
    }

    @Test
    public void testSetDefaultKeepsTargetAndDeduplicatesOldDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("default_graph", Arrays.asList("old", "target", "old"));
        Mockito.when(this.graphs.getDefault()).thenReturn(defaults);

        this.service.setDefault(this.client, "target");

        Mockito.verify(this.graphs, Mockito.never()).setDefault(Mockito.anyString());
        Mockito.verify(this.graphs).unSetDefault("old");
        Mockito.verify(this.graphs, Mockito.never()).unSetDefault("target");
    }

    @Test
    public void testSetFailureDoesNotClearExistingDefault() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("default_graph", Collections.singletonList("old"));
        Mockito.when(this.graphs.getDefault()).thenReturn(defaults);
        Mockito.when(this.graphs.setDefault("target"))
               .thenThrow(new IllegalStateException("set failed"));

        try {
            this.service.setDefault(this.client, "target");
            Assert.fail("Expected set failure");
        } catch (IllegalStateException ignored) {
            // Expected: the failure propagates without clearing the old default.
        }

        Mockito.verify(this.graphs, Mockito.never()).unSetDefault(Mockito.anyString());
    }

    @Test
    public void testCreateStandaloneGraphIncludesRequiredFactory() {
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
        ReflectionTestUtils.setField(this.service, "config", config);

        this.service.create(this.client, "Demo", "demo", null);

        ArgumentCaptor<String> configJson = ArgumentCaptor.forClass(String.class);
        Mockito.verify(this.graphs).createGraph(Mockito.eq("demo"),
                                               configJson.capture());
        Assert.assertTrue(configJson.getValue().contains(
                "\"gremlin.graph\":\"org.apache.hugegraph.auth." +
                "HugeFactoryAuthProxy\""));
        Assert.assertTrue(configJson.getValue().contains(
                "\"backend\":\"rocksdb\""));
        Assert.assertTrue(configJson.getValue().contains(
                "\"task.scheduler_type\":\"local\""));
        Assert.assertTrue(configJson.getValue().contains(
                "\"rocksdb.data_path\":\"rocksdb-data/data_demo\""));
        Assert.assertTrue(configJson.getValue().contains(
                "\"rocksdb.wal_path\":\"rocksdb-data/wal_demo\""));
    }

    @Test
    public void testAnonymousGraphFactoryDoesNotDependOnPdMode() {
        Mockito.when(this.authMode.enabled()).thenReturn(false);
        for (boolean pdEnabled : new boolean[]{false, true}) {
            Mockito.reset(this.graphs);
            HugeConfig config = Mockito.mock(HugeConfig.class);
            Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(pdEnabled);
            ReflectionTestUtils.setField(this.service, "config", config);

            this.service.create(this.client, "Demo", "demo", null);

            ArgumentCaptor<String> configJson = ArgumentCaptor.forClass(String.class);
            Mockito.verify(this.graphs).createGraph(Mockito.eq("demo"), configJson.capture());
            Assert.assertTrue(configJson.getValue().contains(
                    "\"gremlin.graph\":\"org.apache.hugegraph.HugeFactory\""));
            Assert.assertFalse(configJson.getValue().contains("HugeFactoryAuthProxy"));
        }
    }

    @Test
    public void testCreateStandaloneGraphRejectsUnsafeName() {
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
        ReflectionTestUtils.setField(this.service, "config", config);

        try {
            this.service.create(this.client, "Unsafe", "../unsafe", null);
            Assert.fail("Expected unsafe graph name to be rejected");
        } catch (ExternalException ignored) {
            // Expected.
        }
        Mockito.verify(this.graphs, Mockito.never())
               .createGraph(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testExplicitStatisticsAreBoundedAndCachedForThirtyMinutes() {
        QueryService query = Mockito.mock(QueryService.class);
        HugeClientPoolService pool = Mockito.mock(HugeClientPoolService.class);
        Mockito.when(pool.statisticsPolicy(Mockito.any())).thenReturn("policy");
        Mockito.when(pool.statisticsContext(this.client)).thenReturn("server-a-user-a");
        ReflectionTestUtils.setField(this.service, "queryService", query);
        ReflectionTestUtils.setField(this.service, "clientPool", pool);
        AtomicLong now = new AtomicLong();
        Cache<String, GraphStatisticsEntity> cache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .ticker(new Ticker() {
                    @Override
                    public long read() {
                        return now.get();
                    }
                }).build();
        ReflectionTestUtils.setField(this.service, "boundedStatistics", cache);
        ResultSet vertices = Mockito.mock(ResultSet.class);
        ResultSet edges = Mockito.mock(ResultSet.class);
        Mockito.when(vertices.data()).thenReturn(Collections.singletonList(
                Collections.singletonMap("person", 100000L)));
        Mockito.when(edges.data()).thenReturn(Collections.singletonList(
                Collections.singletonMap("knows", 90000L)));
        Mockito.when(query.executeQueryCount(this.client,
                "g.V().limit(100000).groupCount().by(label)")).thenReturn(vertices);
        Mockito.when(query.executeQueryCount(this.client,
                "g.E().limit(100000).groupCount().by(label)")).thenReturn(edges);

        GraphStatisticsEntity first = this.service.getStatistics(this.client, "space", "graph");
        GraphStatisticsEntity cached = this.service.getStatistics(this.client, "space", "graph");

        Assert.assertSame(first, cached);
        Assert.assertEquals("100000", first.getVertexCount());
        Assert.assertEquals("90000", first.getEdgeCount());
        Assert.assertTrue(first.isApproximate());
        Assert.assertEquals(100000, first.getScanLimit());
        Mockito.verify(query, Mockito.times(2))
               .executeQueryCount(Mockito.eq(this.client), Mockito.anyString());
        Mockito.verify(this.client, Mockito.never()).graph();
        now.set(TimeUnit.SECONDS.toNanos(1799));
        Map<String, Object> overview = this.service.evCount(this.client, "space", "graph");
        Assert.assertEquals(100000L, overview.get("vertex"));
        Assert.assertEquals(90000L, overview.get("edge"));
        Assert.assertEquals(true, overview.get("approximate"));
        Assert.assertEquals(100000, overview.get("scan_limit"));
        Mockito.verify(this.client, Mockito.never()).graph();
        Mockito.verify(query, Mockito.times(2))
               .executeQueryCount(Mockito.eq(this.client), Mockito.anyString());

        now.set(TimeUnit.SECONDS.toNanos(1801));
        cache.cleanUp();
        Assert.assertEquals(0L, cache.size());
        this.service.getStatistics(this.client, "space", "graph");
        Mockito.verify(query, Mockito.times(4))
               .executeQueryCount(Mockito.eq(this.client), Mockito.anyString());

        Mockito.when(pool.statisticsContext(this.client)).thenReturn("server-b-user-a");
        this.service.getStatistics(this.client, "space", "graph");
        Mockito.when(pool.statisticsContext(this.client)).thenReturn("server-b-user-b");
        this.service.getStatistics(this.client, "space", "graph");
        Mockito.verify(query, Mockito.times(8))
               .executeQueryCount(Mockito.eq(this.client), Mockito.anyString());
    }

    @Test
    public void testStatisticsCachePartitionsByCurrentPolicyAndPreservesSnapshots() {
        GraphsService service = Mockito.spy(this.service);
        HugeClientPoolService pool = Mockito.mock(HugeClientPoolService.class);
        Mockito.when(pool.statisticsPolicy(Mockito.any())).thenReturn("policy");
        Mockito.when(pool.statisticsContext(this.client)).thenReturn("context");
        Mockito.when(pool.statisticsPolicy(this.client)).thenReturn("policy-a");
        ReflectionTestUtils.setField(service, "clientPool", pool);
        GraphStatisticsEntity first = GraphStatisticsEntity.emptyEntity();
        first.setVertexCount("12");
        GraphStatisticsEntity changed = GraphStatisticsEntity.emptyEntity();
        changed.setVertexCount("2");
        Mockito.doReturn(first, changed).when(service).collectBoundedStatistics(this.client);
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);

        Assert.assertSame(first, service.getStatistics(this.client, "space", "graph"));
        Assert.assertEquals(12L, service.evCount(this.client, "space", "graph").get("vertex"));
        Mockito.when(pool.statisticsPolicy(this.client)).thenReturn("policy-b");
        Assert.assertNull(service.evCount(this.client, "space", "graph").get("vertex"));
        Assert.assertSame(changed, service.getStatistics(this.client, "space", "graph"));
        Assert.assertEquals(2L, service.evCount(this.client, "space", "graph").get("vertex"));

        ServerException unavailable = new ServerException("Current policy unavailable");
        Mockito.when(pool.statisticsPolicy(this.client)).thenThrow(unavailable);
        org.apache.hugegraph.testutil.Assert.assertThrows(
                ServerException.class, () -> service.getStatistics(this.client, "space", "graph"));
        org.apache.hugegraph.testutil.Assert.assertThrows(
                ServerException.class, () -> service.evCount(this.client, "space", "graph"));
        Mockito.doReturn("policy-a").when(pool).statisticsPolicy(this.client);
        Assert.assertSame(first, service.getStatistics(this.client, "space", "graph"));
        Assert.assertEquals(12L, service.evCount(this.client, "space", "graph").get("vertex"));
        Mockito.verify(service, Mockito.times(2)).collectBoundedStatistics(this.client);

        HugeClient reusedTarget = Mockito.mock(HugeClient.class);
        Mockito.when(pool.statisticsContext(reusedTarget)).thenReturn("context");
        Mockito.when(pool.statisticsPolicy(reusedTarget)).thenReturn("policy-a");
        Assert.assertSame(first, service.getStatistics(reusedTarget, "space", "graph"));
        Mockito.verify(service, Mockito.never()).collectBoundedStatistics(reusedTarget);
    }

    @Test
    public void testStatisticsCacheEvictsOldIdentitiesAtCapacity() {
        GraphsService service = Mockito.spy(this.service);
        HugeClientPoolService pool = Mockito.mock(HugeClientPoolService.class);
        Mockito.when(pool.statisticsPolicy(Mockito.any())).thenReturn("policy");
        ReflectionTestUtils.setField(service, "clientPool", pool);
        Mockito.doReturn(GraphStatisticsEntity.emptyEntity()).when(service)
               .collectBoundedStatistics(this.client);
        for (int identity = 0; identity < 1025; identity++) {
            Mockito.when(pool.statisticsContext(this.client)).thenReturn("identity-" + identity);
            service.getStatistics(this.client, "space", "graph");
        }
        Cache<?, ?> cache = (Cache<?, ?>) ReflectionTestUtils.getField(service, "boundedStatistics");
        cache.cleanUp();
        Assert.assertTrue(cache.size() <= 1024L);
        Map<?, ?> asyncStatistics = (Map<?, ?>) ReflectionTestUtils.getField(service, "graphStatistics");
        Assert.assertTrue(asyncStatistics.isEmpty());
    }

    @Test
    public void testStatisticsLoadingDoesNotBlockAnotherKeyInTheSameCacheSegment() throws Exception {
        GraphsService service = Mockito.spy(this.service);
        HugeClient otherClient = Mockito.mock(HugeClient.class);
        HugeClientPoolService pool = Mockito.mock(HugeClientPoolService.class);
        Mockito.when(pool.statisticsPolicy(Mockito.any())).thenReturn("policy");
        Mockito.when(pool.statisticsContext(Mockito.any())).thenReturn("shared-context");
        ReflectionTestUtils.setField(service, "clientPool", pool);
        Cache<String, GraphStatisticsEntity> cache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(1024)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
        ReflectionTestUtils.setField(service, "boundedStatistics", cache);
        GraphStatisticsEntity slowStatistics = GraphStatisticsEntity.emptyEntity();
        GraphStatisticsEntity otherStatistics = GraphStatisticsEntity.emptyEntity();
        CountDownLatch collecting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            collecting.countDown();
            Assert.assertTrue(release.await(10, TimeUnit.SECONDS));
            return slowStatistics;
        }).when(service).collectBoundedStatistics(this.client);
        Mockito.doReturn(otherStatistics).when(service).collectBoundedStatistics(otherClient);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<GraphStatisticsEntity> slow = executor.submit(
                    () -> service.getStatistics(this.client, "space", "slow"));
            Assert.assertTrue(collecting.await(5, TimeUnit.SECONDS));
            Future<GraphStatisticsEntity> other = executor.submit(
                    () -> service.getStatistics(otherClient, "space", "other"));

            Assert.assertSame(otherStatistics, other.get(5, TimeUnit.SECONDS));
            Assert.assertFalse(slow.isDone());
            release.countDown();
            Assert.assertSame(slowStatistics, slow.get(5, TimeUnit.SECONDS));
            Assert.assertSame(otherStatistics, service.getStatistics(otherClient, "space", "other"));
            Mockito.verify(service).collectBoundedStatistics(otherClient);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testStatisticsCachePreservesOriginalFailureAndAllowsRetry() {
        GraphsService service = Mockito.spy(this.service);
        HugeClientPoolService pool = Mockito.mock(HugeClientPoolService.class);
        Mockito.when(pool.statisticsPolicy(Mockito.any())).thenReturn("policy");
        Mockito.when(pool.statisticsContext(this.client)).thenReturn("context");
        ReflectionTestUtils.setField(service, "clientPool", pool);
        ExternalException original = new ExternalException("gremlin.execute.failed");
        GraphStatisticsEntity statistics = GraphStatisticsEntity.emptyEntity();
        Mockito.doThrow(original).doReturn(statistics).when(service).collectBoundedStatistics(this.client);

        try {
            service.getStatistics(this.client, "space", "graph");
            Assert.fail("Expected statistics failure");
        } catch (ExternalException e) {
            Assert.assertSame(original, e);
        }
        Assert.assertSame(statistics, service.getStatistics(this.client, "space", "graph"));
        Assert.assertSame(statistics, service.getStatistics(this.client, "space", "graph"));
        Mockito.verify(service, Mockito.times(2)).collectBoundedStatistics(this.client);
    }

    @Test
    public void testStatisticsFailureNeverFetchesRawGraphElements() {
        QueryService query = Mockito.mock(QueryService.class);
        ReflectionTestUtils.setField(this.service, "queryService", query);
        Mockito.when(query.executeQueryCount(Mockito.eq(this.client), Mockito.anyString()))
               .thenThrow(new ExternalException("gremlin.execute.failed"));

        try {
            this.service.collectBoundedStatistics(this.client);
            Assert.fail("Expected statistics failure");
        } catch (ExternalException ignored) {
            // The caller can retry; no vertex or edge properties are downloaded.
        }
        Mockito.verify(this.client, Mockito.never()).graph();
    }

    @Test
    public void testStatisticsRejectsMalformedResultsWithoutRawFallback() {
        QueryService query = Mockito.mock(QueryService.class);
        ReflectionTestUtils.setField(this.service, "queryService", query);
        ResultSet empty = Mockito.mock(ResultSet.class);
        Mockito.when(empty.data()).thenReturn(Collections.emptyList());
        Mockito.when(query.executeQueryCount(Mockito.eq(this.client), Mockito.anyString()))
               .thenReturn(empty);

        try {
            this.service.collectBoundedStatistics(this.client);
            Assert.fail("Expected malformed statistics failure");
        } catch (ExternalException ignored) {
            // Malformed results do not trigger a costly raw-data fallback.
        }
        Mockito.verify(this.client, Mockito.never()).graph();
    }

    @Test
    public void testElementCountPrefersAvailableDailySnapshot() {
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);
        GraphMetricsAPI.ElementCount snapshot = new GraphMetricsAPI.ElementCount();
        snapshot.setVertices(12L);
        snapshot.setEdges(8L);
        Mockito.when(graph.getEVCount(Mockito.anyString())).thenReturn(snapshot);

        Map<String, Object> result =
                this.service.evCount(this.client, "DEFAULT", "demo");

        Assert.assertEquals(12L, result.get("vertex"));
        Assert.assertEquals(8L, result.get("edge"));
        Assert.assertNotNull(result.get("date"));
        Mockito.verify(graph, Mockito.never()).iterateVertices(Mockito.anyInt());
        Mockito.verify(graph, Mockito.never()).iterateEdges(Mockito.anyInt());
    }

    @Test
    public void testMissingSnapshotNeverScansGraphData() {
        QueryService query = Mockito.mock(QueryService.class);
        ReflectionTestUtils.setField(this.service, "queryService", query);
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);
        Mockito.when(graph.getEVCount(Mockito.anyString())).thenReturn(null);

        Map<String, Object> result =
                this.service.evCount(this.client, "DEFAULT", "demo");

        Assert.assertNull(result.get("vertex"));
        Assert.assertNull(result.get("edge"));
        Assert.assertNull(result.get("date"));
        Mockito.verify(graph, Mockito.times(2)).getEVCount(Mockito.anyString());
        Mockito.verify(graph, Mockito.never()).iterateVertices(Mockito.anyInt());
        Mockito.verify(graph, Mockito.never()).iterateEdges(Mockito.anyInt());
        Mockito.verifyNoInteractions(query);
    }

    @Test
    public void testRecordedSnapshotAlsoWorksInPdMode() {
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(true);
        ReflectionTestUtils.setField(this.service, "config", config);
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);
        GraphMetricsAPI.ElementCount snapshot = new GraphMetricsAPI.ElementCount();
        snapshot.setVertices(2L);
        snapshot.setEdges(1L);
        Mockito.when(graph.getEVCount(Mockito.anyString())).thenReturn(snapshot);

        Map<String, Object> result =
                this.service.evCount(this.client, "DEFAULT", "demo");

        Assert.assertEquals(2L, result.get("vertex"));
        Assert.assertEquals(1L, result.get("edge"));
        Mockito.verify(graph).getEVCount(Mockito.anyString());
        Mockito.verify(graph, Mockito.never()).iterateVertices(Mockito.anyInt());
        Mockito.verify(graph, Mockito.never()).iterateEdges(Mockito.anyInt());
    }

    @Test
    public void testSnapshotFailureRemainsUnknownWithoutLiveFallback() {
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);
        Mockito.when(graph.getEVCount(Mockito.anyString()))
               .thenThrow(new ExternalException("metrics.unavailable"));

        Map<String, Object> result =
                this.service.evCount(this.client, "DEFAULT", "demo");

        Assert.assertNull(result.get("vertex"));
        Assert.assertNull(result.get("edge"));
        Assert.assertNull(result.get("date"));
        Mockito.verify(graph, Mockito.never()).iterateVertices(Mockito.anyInt());
        Mockito.verify(graph, Mockito.never()).iterateEdges(Mockito.anyInt());
    }

    @Test
    public void testGraphProfilesFallBackForStandaloneServer() {
        Mockito.when(this.graphs.listProfile("huge"))
               .thenThrow(new ServerException("profile unsupported"));
        Mockito.when(this.graphs.listGraph())
               .thenReturn(Arrays.asList("hugegraph", "other"));
        Mockito.when(this.graphs.getGraph("hugegraph"))
               .thenReturn(Collections.singletonMap("backend", "rocksdb"));
        Mockito.when(this.client.assignGraph("DEFAULT", "hugegraph"))
               .thenReturn(this.client);
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);
        GraphMetricsAPI.ElementCount snapshot = new GraphMetricsAPI.ElementCount();
        snapshot.setVertices(2L);
        snapshot.setEdges(1L);
        Mockito.when(graph.getEVCount(Mockito.anyString())).thenReturn(snapshot);

        List<Map<String, Object>> result =
                this.service.sortedGraphsProfile(this.client, "DEFAULT", "huge",
                                                 "", false,
                                                 Collections.emptyMap());

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("hugegraph", result.get(0).get("name"));
        Assert.assertEquals("rocksdb", result.get(0).get("backend"));
        Assert.assertEquals("DEFAULT", result.get(0).get("graphspace"));
        Mockito.verify(this.graphs).getGraph("hugegraph");
        Mockito.verify(this.graphs, Mockito.never()).getGraph("other");
    }

    @Test
    public void testGraphProfilesFallBackWhenProfileListIsEmpty() {
        Mockito.when(this.graphs.listProfile(""))
               .thenReturn(Collections.emptyList());
        Mockito.when(this.graphs.listGraph())
               .thenReturn(Collections.singletonList("hugegraph"));
        Mockito.when(this.graphs.getGraph("hugegraph"))
               .thenReturn(Collections.singletonMap("backend", "hstore"));
        Mockito.when(this.client.assignGraph("DEFAULT", "hugegraph"))
               .thenReturn(this.client);
        GraphManager graph = Mockito.mock(GraphManager.class);
        Mockito.when(this.client.graph()).thenReturn(graph);
        GraphMetricsAPI.ElementCount snapshot = new GraphMetricsAPI.ElementCount();
        snapshot.setVertices(0L);
        snapshot.setEdges(0L);
        Mockito.when(graph.getEVCount(Mockito.anyString())).thenReturn(snapshot);

        List<Map<String, Object>> result =
                this.service.sortedGraphsProfile(this.client, "DEFAULT", "", "",
                                                 false,
                                                 Collections.emptyMap());

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("hugegraph", result.get(0).get("name"));
        Assert.assertEquals("hstore", result.get(0).get("backend"));
        Mockito.verify(this.graphs).listGraph();
        Mockito.verify(this.graphs).getGraph("hugegraph");
    }

    @Test
    public void testGraphProfilesDoNotMaskForbiddenResponse() {
        ServerException forbidden = new ServerException("forbidden");
        forbidden.status(403);
        Mockito.when(this.graphs.listProfile(""))
               .thenThrow(forbidden);

        try {
            this.service.sortedGraphsProfile(this.client, "DEFAULT", "", "",
                                             false, Collections.emptyMap());
            Assert.fail("Expected forbidden response");
        } catch (ServerException actual) {
            Assert.assertSame(forbidden, actual);
        }

        Mockito.verify(this.graphs, Mockito.never()).listGraph();
    }
}
