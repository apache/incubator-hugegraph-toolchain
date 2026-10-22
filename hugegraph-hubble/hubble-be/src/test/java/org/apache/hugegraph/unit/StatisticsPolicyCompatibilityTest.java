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
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.unit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.cache.Cache;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.graphs.GraphStatisticsEntity;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.service.SettingSSLService;
import org.apache.hugegraph.service.graphs.GraphsService;
import org.apache.hugegraph.service.query.QueryService;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.JsonUtil;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class StatisticsPolicyCompatibilityTest {

    @Test
    public void testLegacyBasicReaderNeverListsUsersOrReusesSnapshots() throws Exception {
        for (int listStatus : new int[]{200, 403}) {
            try (Fixture fixture = new Fixture(false);
                 HugeClient client = fixture.client()) {
                fixture.userListStatus.set(listStatus);
                Assert.assertTrue(client.requiresBasicGremlinAuth());
                Assert.assertFalse(client.supportsDefaultRole());
                Assert.assertNull(fixture.pool.statisticsPolicy(client));
                Assert.assertEquals(7L, fixture.graphs.evCount(client, "space", "graph").get("vertex"));
                Assert.assertEquals(7L, fixture.graphs.evCount(client, "space", "graph").get("vertex"));
                Assert.assertEquals(0, fixture.queries.size());
                Assert.assertEquals(2, fixture.metrics.get());

                GraphStatisticsEntity first = fixture.graphs.getStatistics(client, "space", "graph");
                GraphStatisticsEntity second = fixture.graphs.getStatistics(client, "space", "graph");
                Assert.assertNotSame(first, second);
                Assert.assertEquals("50", second.getVertexCount());
                Assert.assertEquals(4, fixture.queries.size());
                Assert.assertEquals(0L, fixture.cacheSize());
                Assert.assertEquals(0, fixture.userLists.get());
                Assert.assertEquals(0, fixture.namedUsers.get());
                Assert.assertEquals(0, fixture.roles.get());
                Assert.assertEquals(0, fixture.missingBasic.get());
                fixture.assertBoundedQueries();
            }
        }
    }

    @Test
    public void testModernUnavailableMetadataBypassesExistingCacheWithoutScanningOverview() throws Exception {
        try (Fixture fixture = new Fixture(true);
             HugeClient client = fixture.client()) {
            GraphStatisticsEntity cached = fixture.graphs.getStatistics(client, "space", "graph");
            Assert.assertSame(cached, fixture.graphs.getStatistics(client, "space", "graph"));
            Assert.assertEquals(2, fixture.queries.size());
            Assert.assertEquals(2, fixture.namedUsers.get());
            Assert.assertEquals(2, fixture.roles.get());
            for (AtomicInteger endpoint : new AtomicInteger[]{fixture.identityStatus, fixture.roleStatus}) {
                for (int status : new int[]{403, 404}) {
                    endpoint.set(status);
                    Assert.assertNull(fixture.pool.statisticsPolicy(client));
                    Assert.assertEquals(7L, fixture.graphs.evCount(client, "space", "graph").get("vertex"));
                    Assert.assertEquals(2, fixture.queries.size());
                    endpoint.set(200);
                }
            }
            fixture.roleStatus.set(403);
            Assert.assertNotSame(cached, fixture.graphs.getStatistics(client, "space", "graph"));
            Assert.assertNotSame(cached, fixture.graphs.getStatistics(client, "space", "graph"));
            Assert.assertEquals(6, fixture.queries.size());
            Assert.assertEquals(1L, fixture.cacheSize());
            fixture.roleStatus.set(200);
            Assert.assertSame(cached, fixture.graphs.getStatistics(client, "space", "graph"));
            Assert.assertEquals(6, fixture.queries.size());
            Assert.assertEquals(0, fixture.userLists.get());
            Assert.assertEquals(0, fixture.missingBasic.get());
            fixture.assertBoundedQueries();
        }
    }

    @Test
    public void testOtherMetadataFailuresStillFailBeforeCachedOrNativeStatistics() throws Exception {
        try (Fixture fixture = new Fixture(true);
             HugeClient client = fixture.client()) {
            fixture.graphs.getStatistics(client, "space", "graph");
            for (AtomicInteger endpoint : new AtomicInteger[]{fixture.identityStatus, fixture.roleStatus}) {
                for (int status : new int[]{401, 503}) {
                    endpoint.set(status);
                    Throwable overview = Assert.assertThrows(ServerException.class,
                            () -> fixture.graphs.evCount(client, "space", "graph"));
                    Assert.assertEquals(status, ((ServerException) overview).status());
                    Assert.assertThrows(ServerException.class,
                            () -> fixture.graphs.getStatistics(client, "space", "graph"));
                    endpoint.set(200);
                }
            }
            Assert.assertEquals(2, fixture.queries.size());
            Assert.assertEquals(0, fixture.metrics.get());
            Assert.assertEquals(0, fixture.userLists.get());
        }
    }

    private static final class Fixture implements AutoCloseable {

        private final HttpServer server;
        private final HugeClientPoolService pool = new HugeClientPoolService();
        private final GraphsService graphs = new GraphsService();
        private final AtomicInteger identityStatus = new AtomicInteger(200);
        private final AtomicInteger roleStatus = new AtomicInteger(200);
        private final AtomicInteger userLists = new AtomicInteger();
        private final AtomicInteger userListStatus = new AtomicInteger(403);
        private final AtomicInteger namedUsers = new AtomicInteger();
        private final AtomicInteger roles = new AtomicInteger();
        private final AtomicInteger metrics = new AtomicInteger();
        private final AtomicInteger missingBasic = new AtomicInteger();
        private final List<String> queries = Collections.synchronizedList(new ArrayList<>());

        private Fixture(boolean modern) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            this.server.createContext("/", exchange -> this.respond(exchange, modern));
            this.server.start();
            HugeConfig config = Mockito.mock(HugeConfig.class);
            Mockito.when(config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
            Mockito.when(config.get(HubbleOptions.SERVER_URL)).thenReturn(
                    "http://127.0.0.1:" + this.server.getAddress().getPort());
            Mockito.when(config.get(HubbleOptions.CLIENT_REQUEST_TIMEOUT)).thenReturn(2);
            ReflectionTestUtils.setField(this.pool, "config", config);
            ReflectionTestUtils.setField(this.pool, "sslService", Mockito.mock(SettingSSLService.class));
            ReflectionTestUtils.setField(this.graphs, "clientPool", this.pool);
            ReflectionTestUtils.setField(this.graphs, "queryService", new QueryService());
        }

        private HugeClient client() {
            return this.pool.createBasicClient("space", "graph", "reader", "fixture");
        }

        private long cacheSize() {
            return ((Cache<?, ?>) ReflectionTestUtils.getField(this.graphs, "boundedStatistics")).size();
        }

        private void assertBoundedQueries() {
            for (String query : this.queries) {
                Assert.assertTrue(query.equals("g.V().limit(100000).groupCount().by(label)") ||
                                  query.equals("g.E().limit(100000).groupCount().by(label)"));
            }
        }

        private void respond(HttpExchange exchange, boolean modern) throws IOException {
            String path = exchange.getRequestURI().getPath();
            int status = 200;
            String json;
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (!path.equals("/versions") && (auth == null || !auth.startsWith("Basic "))) {
                this.missingBasic.incrementAndGet();
            }
            if (path.equals("/versions")) {
                json = "{\"versions\":{\"api\":\"" + (modern ? "0.80" : "0.71") +
                       "\",\"core\":\"1.7.0\"}}";
            } else if (path.endsWith("/auth/users")) {
                if (exchange.getRequestURI().getQuery() != null &&
                    exchange.getRequestURI().getQuery().startsWith("name=")) {
                    this.namedUsers.incrementAndGet();
                    status = this.identityStatus.get();
                    json = "{\"id\":\"reader-id\",\"user_name\":\"reader\"}";
                } else {
                    this.userLists.incrementAndGet();
                    status = this.userListStatus.get();
                    json = "{\"users\":[{\"id\":\"reader-id\",\"user_name\":\"reader\"}]}";
                }
            } else if (path.endsWith("/auth/users/reader-id/role")) {
                this.roles.incrementAndGet();
                status = this.roleStatus.get();
                json = "{\"roles\":{}}";
            } else if (path.contains("/metrics/elementcount/")) {
                this.metrics.incrementAndGet();
                json = "{\"vertices\":7,\"edges\":3}";
            } else if (path.equals("/gremlin")) {
                Map<?, ?> request = JsonUtil.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                                                                StandardCharsets.UTF_8), Map.class);
                String query = (String) request.get("gremlin");
                this.queries.add(query);
                String labels = query.startsWith("g.V()") ? "{\"person\":50}" : "{\"link\":20}";
                json = "{\"status\":{\"code\":200},\"result\":{\"data\":[" + labels + "]}}";
            } else {
                status = 404;
                json = "{}";
            }
            if (status != 200) {
                json = "{\"exception\":\"MetadataUnavailable\",\"message\":\"fixture rejection\"}";
            }
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }

        @Override
        public void close() {
            this.server.stop(0);
        }
    }
}
