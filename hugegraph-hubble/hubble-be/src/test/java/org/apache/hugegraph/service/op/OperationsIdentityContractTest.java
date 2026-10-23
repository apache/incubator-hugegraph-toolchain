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

package org.apache.hugegraph.service.op;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.MetricsManager;
import org.apache.hugegraph.driver.VersionManager;
import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class OperationsIdentityContractTest {

    private static final String PD_USER = "pd-identity";
    private static final String PD_SECRET = "pd-secret-canary";
    private static final String STORE_USER = "store-identity";
    private static final String STORE_SECRET = "store-secret-canary";
    private static final Clock CLOCK = Clock.fixed(
            Instant.ofEpochMilli(2000L), ZoneOffset.UTC);

    @Test
    public void testPdAndStoreRequestsUseOnlyTheirOwnIdentities() {
        assertDiscoveryIdentities(200);
    }

    @Test
    public void testLegacyTargetFallbackKeepsPdAndStoreIdentitiesSeparate() {
        assertDiscoveryIdentities(404);
        assertDiscoveryIdentities(405);
    }

    @Test
    public void testSchemeLessAdvertisedPdUsesConfiguredHttpsIdentity() {
        assertDiscoveryIdentities(200, "https");
    }

    private static void assertDiscoveryIdentities(int dedicatedStatus) {
        assertDiscoveryIdentities(dedicatedStatus, "http");
    }

    private static void assertDiscoveryIdentities(int dedicatedStatus, String pdScheme) {
        IdentityCapturingHttpClient http = new IdentityCapturingHttpClient(dedicatedStatus, pdScheme);
        LiveOperationsCollector collector = new LiveOperationsCollector(
                true, pdScheme + "://pd.internal:8620", PD_USER, PD_SECRET,
                STORE_USER, STORE_SECRET, "server-under-test", http,
                new OperationsPayloadParser(new ObjectMapper()), CLOCK,
                2, 1000, Set.of("http://store.internal:8520"));
        try {
            collector.collect(serverClient(), true);
        } finally {
            collector.close();
        }

        Assert.assertTrue(http.pdRequests > 0);
        Assert.assertEquals(1, http.memberRequests);
        Assert.assertEquals(1, http.dedicatedRequests);
        Assert.assertEquals(1, http.pdMetricRequests);
        Assert.assertEquals(dedicatedStatus == 200 ? 0 : 1, http.legacyRequests);
        Assert.assertEquals(3, http.storeRequests);
        Assert.assertFalse(http.captures.isEmpty());
        for (Capture capture : http.captures) {
            if (capture.store) {
                Assert.assertEquals(STORE_USER, capture.username);
                Assert.assertEquals(STORE_SECRET, capture.password);
                Assert.assertFalse(PD_USER.equals(capture.username));
                Assert.assertFalse(PD_SECRET.equals(capture.password));
            } else {
                Assert.assertEquals(PD_USER, capture.username);
                Assert.assertEquals(PD_SECRET, capture.password);
                Assert.assertFalse(STORE_USER.equals(capture.username));
                Assert.assertFalse(STORE_SECRET.equals(capture.password));
            }
        }
    }

    private static HugeClient serverClient() {
        HugeClient client = Mockito.mock(HugeClient.class);
        VersionManager versions = Mockito.mock(VersionManager.class);
        Mockito.when(versions.getCoreVersion()).thenReturn("1.7.0");
        Mockito.when(client.versionManager()).thenReturn(versions);
        Mockito.when(client.metrics()).thenReturn(Mockito.mock(
                                                  MetricsManager.class));
        return client;
    }

    private static final class IdentityCapturingHttpClient
                         extends OperationsHttpClient {

        private final List<Capture> captures = new ArrayList<>();
        private int pdRequests;
        private int memberRequests;
        private int storeRequests;
        private int dedicatedRequests;
        private int legacyRequests;
        private int pdMetricRequests;
        private final int dedicatedStatus;
        private final String pdScheme;

        private IdentityCapturingHttpClient(int dedicatedStatus, String pdScheme) {
            super(1000, 1000, 8192);
            this.dedicatedStatus = dedicatedStatus;
            this.pdScheme = pdScheme;
        }

        @Override
        public synchronized String get(URI target, String username,
                                       String password) {
            return this.capture(target, username, password);
        }

        @Override
        public synchronized String get(URI target, String username,
                                       String password,
                                       Set<String> allowedTargets) {
            return this.capture(target, username, password);
        }

        @Override
        public synchronized String get(URI target, String username,
                                       String password,
                                       Set<String> allowedTargets,
                                       String accept) {
            return this.capture(target, username, password);
        }

        private String capture(URI target, String username, String password) {
            boolean store = "store.internal".equals(target.getHost());
            this.captures.add(new Capture(store, username, password));
            if (store) {
                this.storeRequests++;
                if (target.getPath().endsWith("/drive")) {
                    return "{\"disk\":{\"total_space\":10}}";
                }
                if (target.getPath().endsWith("/raft")) {
                    return "{\"0\":{\"enabled\":true}}";
                }
                return "{\"heap\":{\"used\":1}}";
            }
            this.pdRequests++;
            Assert.assertEquals(this.pdScheme, target.getScheme());
            switch (target.getPath()) {
                case "/v1/members":
                    this.memberRequests++;
                    return members();
                case "/v1/stores":
                    return stores();
                case "/v1/prom/targets/store":
                    this.dedicatedRequests++;
                    if (this.dedicatedStatus != 200) {
                        throw new UpstreamRequestException("upstream_http_status_" + this.dedicatedStatus);
                    }
                    return targets();
                case "/v1/prom/targets-all":
                    this.legacyRequests++;
                    return targets();
                case "/actuator/prometheus":
                    this.pdMetricRequests++;
                    return "process_uptime_seconds 1\n";
                default:
                    throw new AssertionError("Unexpected PD path");
            }
        }

        private static String members() {
            return "{\"status\":0,\"data\":{\"state\":\"Cluster_OK\"," +
                   "\"pdList\":[{\"restUrl\":\"pd.internal:8620\",\"state\":\"Up\"}],\"stores\":[]}}";
        }

        private static String stores() {
            return "{\"status\":0,\"data\":{\"stores\":[{" +
                   "\"storeId\":\"1\",\"address\":\"store.internal:8500\"," +
                   "\"restAddress\":\"store.internal:8520\"," +
                   "\"state\":\"Up\"}]}}";
        }

        private static String targets() {
            return "[{\"targets\":[\"store.internal:8520\"],\"labels\":{" +
                   "\"__app_name\":\"store\",\"__scheme__\":\"http\"}}]";
        }
    }

    private static final class Capture {

        private final boolean store;
        private final String username;
        private final String password;

        private Capture(boolean store, String username, String password) {
            this.store = store;
            this.username = username;
            this.password = password;
        }
    }
}
