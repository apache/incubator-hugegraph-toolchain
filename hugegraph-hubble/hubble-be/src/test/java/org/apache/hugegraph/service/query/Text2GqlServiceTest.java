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

package org.apache.hugegraph.service.query;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.entity.enums.QueryLanguage;
import org.apache.hugegraph.entity.query.Text2GqlRequest;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Text2GqlServiceTest {

    @Test
    public void testOpenAiCompatibleRequestWithOllamaAndReasoningResponse() throws Exception {
        AtomicReference<JsonNode> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = server();
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            reply(exchange, 200, "{\"choices\":[{\"index\":0,\"finish_reason\":\"stop\"," +
                                 "\"message\":{\"role\":\"assistant\",\"reasoning_content\":\"ignored\"," +
                                 "\"content\":\"```gremlin\\ng.V().hasLabel('person').limit(10)\\n```\"}}]}");
        });
        server.start();
        try {
            Text2GqlRequest request = request(server);
            request.setApiKey("");
            String result = new Text2GqlService().generate(graphClient(), request);
            assertEquals("g.V().hasLabel('person').limit(10)", result);
            assertEquals("Bearer ollama", authorization.get());
            assertEquals("test-model", body.get().get("model").asText());
            assertFalse(body.get().has("response_format"));
            assertFalse(body.get().has("tools"));
            assertFalse(body.get().has("temperature"));
            String prompt = body.get().get("messages").get(0).get("content").asText();
            assertTrue(prompt.contains("person"));
            assertTrue(prompt.contains("cardinality"));
            assertFalse(prompt.contains("private-style"));
            assertEquals("Find people", body.get().get("messages").get(1).get("content").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testPreservesCompatibleProviderBasePaths() throws Exception {
        for (String prefix : List.of("/v1", "/api/paas/v4")) {
            AtomicReference<String> method = new AtomicReference<>();
            AtomicReference<String> path = new AtomicReference<>();
            HttpServer server = server();
            server.createContext("/", exchange -> {
                method.set(exchange.getRequestMethod());
                path.set(exchange.getRequestURI().getPath());
                reply(exchange, 200, "{\"choices\":[{\"index\":0,\"finish_reason\":\"stop\"," +
                                     "\"message\":{\"role\":\"assistant\",\"content\":\"g.V().limit(10)\"}}]}");
            });
            server.start();
            try {
                for (String suffix : List.of("/", "////")) {
                    Text2GqlRequest request = request(server);
                    request.setBaseUrl("http://localhost:" + server.getAddress().getPort() + prefix + suffix);
                    assertEquals("g.V().limit(10)", new Text2GqlService().generate(graphClient(), request));
                    assertEquals("POST", method.get());
                    assertEquals(prefix + "/chat/completions", path.get());
                }
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    public void testBaseUrlLengthBoundaryAtGenerationEntry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> path = new AtomicReference<>();
        HttpServer server = server();
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            path.set(exchange.getRequestURI().getPath());
            reply(exchange, 200, "{\"choices\":[{\"index\":0,\"finish_reason\":\"stop\"," +
                                 "\"message\":{\"role\":\"assistant\",\"content\":\"g.V().limit(10)\"}}]}");
        });
        server.start();
        try {
            Text2GqlRequest request = request(server);
            String prefix = request.getBaseUrl() + "/";
            String baseUrl = prefix + "a".repeat(2048 - prefix.length());
            assertEquals(2048, baseUrl.length());
            request.setBaseUrl(baseUrl);
            assertEquals("g.V().limit(10)", new Text2GqlService().generate(graphClient(), request));
            assertEquals("/v1/" + "a".repeat(2048 - prefix.length()) + "/chat/completions", path.get());
            assertEquals(1, calls.get());

            request.setBaseUrl(baseUrl + "a");
            assertEquals(2049, request.getBaseUrl().length());
            HugeClient client = Mockito.mock(HugeClient.class);
            ExternalException failure = failure(() -> new Text2GqlService().generate(client, request));
            assertEquals(400, failure.status());
            assertEquals("text2gql.invalid-request", failure.getMessage());
            Mockito.verifyNoInteractions(client);
            assertEquals(1, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testCypherUsesSameSdkProtocolAndItsOwnPromptAndOutputGuard() throws Exception {
        AtomicReference<JsonNode> body = new AtomicReference<>();
        HttpServer server = server();
        String cypher = "MATCH (n:person) WHERE n.name = 'Alice' RETURN n LIMIT 10";
        server.createContext("/v1/chat/completions", exchange -> {
            ObjectMapper mapper = new ObjectMapper();
            body.set(mapper.readTree(exchange.getRequestBody()));
            reply(exchange, 200, mapper.writeValueAsString(Map.of("choices", List.of(Map.of(
                    "index", 0, "finish_reason", "stop", "message", Map.of(
                            "role", "assistant", "reasoning_content", "ignored", "content",
                            "```cypher\n" + cypher + "\n```"))))));
        });
        server.start();
        try {
            Text2GqlRequest request = request(server);
            request.setLanguage(QueryLanguage.CYPHER);
            assertEquals(cypher, new Text2GqlService().generate(graphClient(), request));
            String prompt = body.get().get("messages").get(0).get("content").asText();
            assertTrue(prompt.contains("openCypher"));
            assertTrue(prompt.contains("person"));
            assertFalse(prompt.contains("g.V()"));
            assertFalse(body.get().has("response_format"));
            assertFalse(body.get().has("tools"));
            assertFalse(body.get().has("temperature"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testGeneratedLanguageCannotBeConfusedWithAnotherDialect() {
        assertEquals(502, failure(() -> Text2GqlService.normalize("g.V()", QueryLanguage.CYPHER)).status());
        assertEquals(502, failure(() -> Text2GqlService.normalize("RETURN 1", QueryLanguage.GREMLIN)).status());
        assertEquals(502, failure(() -> Text2GqlService.normalize(
                "```gremlin\nRETURN 1\n```", QueryLanguage.CYPHER)).status());
    }

    @Test
    public void testFailuresNeverExposeUpstreamBodyOrCredentialsAndDoNotRetry() throws Exception {
        for (int status : new int[]{401, 429, 503}) {
            AtomicInteger calls = new AtomicInteger();
            HttpServer server = server();
            server.createContext("/v1/chat/completions", exchange -> {
                calls.incrementAndGet();
                reply(exchange, status, "{\"error\":{\"message\":\"secret-key private upstream body\"}}");
            });
            server.start();
            try {
                ExternalException failure = failure(() ->
                        new Text2GqlService().generate(graphClient(), request(server)));
                assertEquals(502, failure.status());
                assertEquals("text2gql.model-failed", failure.getMessage());
                assertNull(failure.getCause());
                assertEquals(1, calls.get());
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    public void testTimeoutIsSafeGatewayTimeout() throws Exception {
        HttpServer server = server();
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.start();
        try {
            ExternalException failure = failure(() ->
                    new Text2GqlService(Duration.ofMillis(50)).generate(graphClient(), request(server)));
            assertEquals(504, failure.status());
            assertEquals("text2gql.model-timeout", failure.getMessage());
            assertNull(failure.getCause());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSlowResponseBodyTotalDeadlineMapsToGatewayTimeout() throws Exception {
        HttpServer server = server();
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            try {
                for (int i = 0; i < 20; i++) {
                    exchange.getResponseBody().write(' ');
                    exchange.getResponseBody().flush();
                    Thread.sleep(30);
                }
            } catch (IOException ignored) {
                // The total deadline closes the client while this fixture is still sending its body.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            ExternalException failure = failure(() ->
                    new Text2GqlService(Duration.ofMillis(150)).generate(graphClient(), request(server)));
            assertEquals(504, failure.status());
            assertEquals("text2gql.model-timeout", failure.getMessage());
            assertNull(failure.getCause());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testRedirectDoesNotForwardKey() throws Exception {
        AtomicInteger redirected = new AtomicInteger();
        HttpServer server = server();
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("Location", "/steal-key");
            reply(exchange, 307, "redirect");
        });
        server.createContext("/steal-key", exchange -> {
            redirected.incrementAndGet();
            reply(exchange, 200, "{}");
        });
        server.start();
        try {
            assertEquals(502, failure(() -> new Text2GqlService()
                    .generate(graphClient(), request(server))).status());
            assertEquals(0, redirected.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testRejectsUnsafeUrlAndOutput() {
        for (String url : List.of("file:///tmp/key", "http://169.254.169.254/latest", "http://0.0.0.0/v1",
                                  "http://224.0.0.1/v1", "https://100.100.100.200/v1",
                                  "https://255.255.255.255/v1", "http://[fd00:ec2::254]/v1",
                                  "https://[fd00:ec2::254]/v1",
                                  "https://[FD00:0EC2:0000:0000:0000:0000:0000:0254]/v1",
                                  "https://user:password@localhost/v1",
                                  "https://localhost/v1?key=secret", "https://localhost/v1#secret",
                                  "http://8.8.8.8/v1", "http://2130706433/v1", "http://localhost:0/v1")) {
            assertEquals(400, failure(() -> Text2GqlService.validateBaseUrl(url)).status());
        }
        for (String output : List.of("g.V().drop()", "g.V();g.E()", "Here is your query: g.V()", "")) {
            assertEquals(502, failure(() -> Text2GqlService.normalize(output, QueryLanguage.GREMLIN)).status());
        }
        assertEquals("http://127.0.0.1:11434/v1",
                     Text2GqlService.validateBaseUrl("http://127.0.0.1:11434/v1/"));
        assertEquals("http://[fd00:1234::1]:11434/v1",
                     Text2GqlService.validateBaseUrl("http://[fd00:1234::1]:11434/v1"));
    }

    @Test
    public void testInvalidInputDoesNotReadSchemaOrCallModel() {
        HugeClient client = Mockito.mock(HugeClient.class);
        Text2GqlRequest request = new Text2GqlRequest();
        request.setQuestion(" ");
        request.setApiKey("secret-key");
        assertEquals(400, failure(() -> new Text2GqlService().generate(client, request)).status());
        Mockito.verifyNoInteractions(client);
        assertFalse(request.toString().contains("secret-key"));
    }

    @Test
    public void testSchemaSupportsEdgesWithMultipleSourceTargetPairs() throws Exception {
        HugeClient client = graphClient();
        EdgeLabel edge = new EdgeLabel("knows");
        edge.links().add(Map.of("person", "person"));
        edge.links().add(Map.of("person", "company"));
        Mockito.when(client.schema().getEdgeLabels()).thenReturn(List.of(edge));
        JsonNode schema = new ObjectMapper().readTree(Text2GqlService.schema(client));
        assertEquals(2, schema.get("edge_labels").get(0).get("links").size());
        assertEquals("knows", schema.get("edge_labels").get(0).get("name").asText());
    }

    @Test
    public void testSchemaFailureAndSizeLimitDoNotReachModel() {
        HugeClient client = Mockito.mock(HugeClient.class);
        SchemaManager schema = Mockito.mock(SchemaManager.class);
        Mockito.when(client.schema()).thenReturn(schema);
        Mockito.when(client.supportsCypher()).thenReturn(true);
        Mockito.when(schema.getPropertyKeys()).thenThrow(new IllegalStateException("private graph token"));
        ExternalException failure = failure(() -> Text2GqlService.schema(client));
        assertEquals(502, failure.status());
        assertEquals("text2gql.schema-failed", failure.getMessage());
        assertNull(failure.getCause());
        Mockito.doReturn(List.of(new PropertyKey("x".repeat(65536)))).when(schema).getPropertyKeys();
        Mockito.when(schema.getVertexLabels()).thenReturn(List.of());
        Mockito.when(schema.getEdgeLabels()).thenReturn(List.of());
        assertEquals("text2gql.schema-too-large", failure(() -> Text2GqlService.schema(client)).getMessage());
    }

    @Test
    public void testUnsupportedCypherFailsBeforeReadingSchemaOrCallingModel() {
        HugeClient client = Mockito.mock(HugeClient.class);
        Text2GqlRequest request = new Text2GqlRequest();
        request.setLanguage(QueryLanguage.CYPHER);
        ExternalException failure = failure(() -> new Text2GqlService().generate(client, request));
        assertEquals(400, failure.status());
        assertEquals("text2gql.language-unavailable", failure.getMessage());
        Mockito.verify(client).supportsCypher();
        Mockito.verifyNoMoreInteractions(client);
    }

    private static HugeClient graphClient() {
        HugeClient client = Mockito.mock(HugeClient.class);
        SchemaManager schema = Mockito.mock(SchemaManager.class);
        Mockito.when(client.schema()).thenReturn(schema);
        Mockito.when(client.supportsCypher()).thenReturn(true);
        PropertyKey key = new PropertyKey("name");
        VertexLabel person = new VertexLabel("person");
        person.properties().add("name");
        person.userdata().put("style", "private-style");
        Mockito.when(schema.getPropertyKeys()).thenReturn(List.of(key));
        Mockito.when(schema.getVertexLabels()).thenReturn(List.of(person));
        Mockito.when(schema.getEdgeLabels()).thenReturn(List.of());
        return client;
    }

    private static HttpServer server() throws IOException {
        return HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    private static Text2GqlRequest request(HttpServer server) {
        Text2GqlRequest request = new Text2GqlRequest();
        request.setQuestion("Find people");
        request.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        request.setApiKey("secret-key");
        request.setModel("test-model");
        return request;
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange,
                              int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static ExternalException failure(Runnable task) {
        try {
            task.run();
            fail("Expected a safe Text2GQL error");
            return null;
        } catch (ExternalException exception) {
            return exception;
        }
    }
}
