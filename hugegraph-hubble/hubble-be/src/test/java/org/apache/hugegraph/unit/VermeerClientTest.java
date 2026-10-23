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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.space.VermeerClient;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class VermeerClientTest {

    private static final String LOAD_PERMISSION_ERROR =
            "{\"errcode\":1,\"message\":\"admin permission is required to load from HugeGraph\"}";

    @Test
    public void testHugeGraphLoadPermissionExplainsServiceTokenConfiguration() throws Exception {
        try (Fixture fixture = new Fixture()) {
            for (String type : new String[]{"load", "reload"}) {
                try {
                    fixture.client.request("POST", "tasks/create", Map.of("task_type", type));
                    Assert.fail("Expected the Vermeer administrator requirement");
                } catch (VermeerClient.ConfigurationException e) {
                    Assert.assertEquals(
                            "Vermeer HugeGraph loading requires an administrator service token; " +
                            "check vermeer.token_file", e.getMessage());
                    Assert.assertNull(e.getCause());
                }
            }
        }
    }

    @Test
    public void testOtherFailuresStayGenericAndDoNotExposeUpstreamBody() throws Exception {
        try (Fixture fixture = new Fixture()) {
            assertGenericFailure(fixture, "GET", "tasks/create", Map.of("task_type", "load"));
            assertGenericFailure(fixture, "POST", "graphs", Map.of("task_type", "load"));
            assertGenericFailure(fixture, "POST", "tasks/create", Map.of("task_type", "compute"));
            assertGenericFailure(fixture, "POST", "tasks/create", null);
            for (String response : new String[]{
                    LOAD_PERMISSION_ERROR.replace("\"errcode\":1", "\"errcode\":2"),
                    LOAD_PERMISSION_ERROR.replace("HugeGraph", "HugeGraph; token=private-value"),
                    "private-value", "[]", "null"}) {
                fixture.response.set(response);
                assertGenericFailure(fixture, "POST", "tasks/create", Map.of("task_type", "load"));
            }
            fixture.response.set(LOAD_PERMISSION_ERROR);
            fixture.status.set(503);
            assertGenericFailure(fixture, "POST", "tasks/create", Map.of("task_type", "load"));
        }
    }

    private static void assertGenericFailure(Fixture fixture, String method, String path,
                                              Map<String, ?> body) {
        try {
            fixture.client.request(method, path, body);
            Assert.fail("Expected an upstream request failure");
        } catch (ExternalException e) {
            Assert.assertFalse(e instanceof VermeerClient.ConfigurationException);
            Assert.assertEquals("Vermeer request failed (HTTP " + fixture.status.get() + ")",
                                e.getMessage());
            Assert.assertNull(e.getCause());
        }
    }

    private static final class Fixture implements AutoCloseable {

        private final AtomicInteger status = new AtomicInteger(400);
        private final AtomicReference<String> response = new AtomicReference<>(LOAD_PERMISSION_ERROR);
        private final HttpServer server;
        private final VermeerClient client = new VermeerClient();

        private Fixture() throws Exception {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            this.server.createContext("/api/v1/", exchange -> {
                byte[] value = this.response.get().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(this.status.get(), value.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(value);
                }
            });
            this.server.start();
            HugeConfig config = Mockito.mock(HugeConfig.class);
            Mockito.when(config.get(HubbleOptions.VERMEER_URL))
                   .thenReturn("http://127.0.0.1:" + this.server.getAddress().getPort());
            ReflectionTestUtils.setField(this.client, "config", config);
        }

        @Override
        public void close() {
            this.server.stop(0);
        }
    }
}
