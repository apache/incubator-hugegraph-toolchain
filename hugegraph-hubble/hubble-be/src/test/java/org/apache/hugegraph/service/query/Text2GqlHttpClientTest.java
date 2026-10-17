/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.service.query;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Text2GqlHttpClientTest {

    private static final String HOST = "text2gql.test";

    @Test
    public void shouldResolveAndPinAddressesAtConnectTimeAndKeepOriginalHost() throws Exception {
        AtomicInteger dnsLookups = new AtomicInteger();
        AtomicInteger redirectedRequests = new AtomicInteger();
        AtomicReference<String> hostHeader = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            hostHeader.set(exchange.getRequestHeaders().getFirst("Host"));
            if ("/redirect".equals(exchange.getRequestURI().getPath())) {
                exchange.getResponseHeaders().set("Location", "/target");
                exchange.sendResponseHeaders(302, -1);
            } else if ("/target".equals(exchange.getRequestURI().getPath())) {
                redirectedRequests.incrementAndGet();
                exchange.sendResponseHeaders(200, -1);
            }
            exchange.close();
        });
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host -> {
                dnsLookups.incrementAndGet();
                assertEquals(HOST, host);
                return List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1}));
            });
            HttpClient client = adapter.build();
            assertEquals("DNS must not be resolved before the request", 0, dnsLookups.get());

            HttpRequest request = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url(url(server, "/redirect"))
                    .build();
            try {
                client.execute(request);
                fail("Expected redirects to be rejected");
            } catch (HttpException exception) {
                assertEquals(302, exception.statusCode());
            }

            assertEquals(1, dnsLookups.get());
            assertEquals(0, redirectedRequests.get());
            assertEquals(HOST + ":" + server.getAddress().getPort(), hostHeader.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldStopWhenConnectTimeDnsValidationRejectsHost() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = server(exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host -> {
                throw new UnknownHostException("rejected by address policy");
            });
            HttpRequest request = HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .url(url(server, "/blocked"))
                    .build();
            try {
                adapter.build().execute(request);
                fail("Expected address validation to reject the connection");
            } catch (RuntimeException exception) {
                assertNotNull(findCause(exception, UnknownHostException.class));
            }
            assertEquals(0, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldNotRetryFailedRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = server(exchange -> {
            requests.incrementAndGet();
            exchange.close();
        });
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            try {
                adapter.build().execute(HttpRequest.builder()
                        .method(HttpMethod.GET)
                        .url(url(server, "/drop"))
                        .build());
                fail("Expected the server connection to close");
            } catch (RuntimeException ignored) {
                assertEquals(1, requests.get());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldRejectDeclaredOversizedErrorBodyWithoutExposingIt() throws Exception {
        String marker = "upstream-secret-marker";
        HttpServer server = server(exchange -> {
            byte[] prefix = marker.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(503, Text2GqlHttpClient.MAX_RESPONSE_BODY_BYTES + 1L);
            try {
                exchange.getResponseBody().write(prefix);
            } catch (IOException ignored) {
                // The client rejects the declared length before reading the body.
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            try {
                adapter.build().execute(HttpRequest.builder()
                        .method(HttpMethod.GET)
                        .url(url(server, "/declared-error"))
                        .build());
                fail("Expected an oversized error body to be rejected");
            } catch (HttpException exception) {
                assertEquals(503, exception.statusCode());
                assertFalse(exception.getMessage().contains(marker));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldRejectDeclaredOversizedSuccessBody() throws Exception {
        HttpServer server = server(exchange -> {
            exchange.sendResponseHeaders(200, Text2GqlHttpClient.MAX_RESPONSE_BODY_BYTES + 1L);
            try {
                exchange.getResponseBody().write("prefix".getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // The client rejects the declared length before reading the body.
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            try {
                adapter.build().execute(HttpRequest.builder()
                        .method(HttpMethod.GET)
                        .url(url(server, "/declared-success"))
                        .build());
                fail("Expected an oversized success body to be rejected");
            } catch (IllegalStateException exception) {
                assertEquals("Upstream response exceeds the size limit", exception.getMessage());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldRejectOversizedChunkedSuccessBody() throws Exception {
        HttpServer server = server(exchange -> writeOversizedChunkedBody(exchange, 200, null));
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            try {
                adapter.build().execute(HttpRequest.builder()
                        .method(HttpMethod.GET)
                        .url(url(server, "/chunked-success"))
                        .build());
                fail("Expected an oversized chunked response to be rejected");
            } catch (IllegalStateException exception) {
                assertEquals("Upstream response exceeds the size limit", exception.getMessage());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldRejectOversizedChunkedErrorBodyWithoutExposingIt() throws Exception {
        String marker = "upstream-secret-marker";
        HttpServer server = server(exchange -> writeOversizedChunkedBody(exchange, 503, marker));
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            try {
                adapter.build().execute(HttpRequest.builder()
                        .method(HttpMethod.GET)
                        .url(url(server, "/chunked-error"))
                        .build());
                fail("Expected an oversized chunked error body to be rejected");
            } catch (HttpException exception) {
                assertEquals(503, exception.statusCode());
                assertFalse(exception.getMessage().contains(marker));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldRetainTimeoutCauseForSilentResponse() throws Exception {
        HttpServer server = server(exchange -> {
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException(exception);
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            adapter.connectTimeout(Duration.ofSeconds(1)).readTimeout(Duration.ofMillis(100));
            try {
                adapter.build().execute(HttpRequest.builder()
                        .method(HttpMethod.GET)
                        .url(url(server, "/slow"))
                        .build());
                fail("Expected the configured read timeout");
            } catch (RuntimeException exception) {
                assertTrue(findCause(exception, SocketTimeoutException.class) != null ||
                           findCause(exception, InterruptedIOException.class) != null);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void shouldEnforceTotalCallDeadlineForDripResponse() throws Exception {
        HttpServer server = server(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try {
                for (int i = 0; i < 30; i++) {
                    exchange.getResponseBody().write('x');
                    exchange.getResponseBody().flush();
                    Thread.sleep(50);
                }
            } catch (IOException ignored) {
                // The call deadline closes the response while the server is still writing.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException(exception);
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            Duration timeout = Duration.ofMillis(250);
            Text2GqlHttpClient adapter = new Text2GqlHttpClient(host ->
                    List.of(InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1})));
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .baseUrl("http://" + HOST + ":" + server.getAddress().getPort() + "/v1")
                    .apiKey("test-key")
                    .modelName("test-model")
                    .timeout(timeout)
                    .maxRetries(0)
                    .httpClientBuilder(adapter)
                    .build();
            long start = System.nanoTime();
            try {
                model.chat("drip response");
                fail("Expected the total call deadline to stop a drip response");
            } catch (RuntimeException exception) {
                long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
                assertNotNull(findCause(exception, InterruptedIOException.class));
                assertTrue("Call exceeded expected total deadline: " + elapsedMs, elapsedMs < 1000);
            }
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer server(HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler::handle);
        return server;
    }

    private static String url(HttpServer server, String path) {
        return "http://" + HOST + ":" + server.getAddress().getPort() + path;
    }

    private static void writeOversizedChunkedBody(HttpExchange exchange, int status, String prefix)
            throws IOException {
        byte[] body = new byte[Text2GqlHttpClient.MAX_RESPONSE_BODY_BYTES + 1024];
        java.util.Arrays.fill(body, (byte) 'x');
        if (prefix != null) {
            byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        }
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, 0);
        try {
            exchange.getResponseBody().write(body);
        } catch (IOException ignored) {
            // The client closes the response as soon as it reads the overflow sentinel.
        } finally {
            exchange.close();
        }
    }

    private static <T extends Throwable> T findCause(Throwable exception, Class<T> type) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface HttpHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
