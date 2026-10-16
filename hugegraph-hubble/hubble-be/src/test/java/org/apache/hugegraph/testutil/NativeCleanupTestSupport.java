/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.testutil;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.task.TaskManager;
import org.apache.hugegraph.util.JsonUtil;
import org.mockito.Mockito;

import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;

/** Real native shutdown and HTTP reset boundary; only load failure entry is controlled. */
public final class NativeCleanupTestSupport implements AutoCloseable {

    public final HugeGraphLoader loader;
    public final RuntimeException original = new IllegalStateException("recorded load failure");
    public final AtomicReference<Throwable> shutdownFailure = new AtomicReference<>();
    private final HttpServer server;
    private volatile String mode = "NONE";

    public NativeCleanupTestSupport(LoadTask task, Path input, boolean rejectReset,
                                    LoadStatus requestedStatus) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/", exchange -> {
            Object response;
            int status = 200;
            if (exchange.getRequestURI().getPath().equals("/versions")) {
                response = Map.of("versions", Map.of("core", "1.8.0", "api", "0.74"));
            } else if (exchange.getRequestURI().getPath().endsWith("/mode/owners") &&
                       exchange.getRequestMethod().equals("GET")) {
                // This fixture verifies the legacy enum reset failure path.
                status = 404;
                response = Map.of("exception", "jakarta.ws.rs.NotFoundException",
                                  "message", "HTTP 404 Not Found");
            } else if (exchange.getRequestURI().getPath().endsWith("/mode")) {
                if (exchange.getRequestMethod().equals("GET")) {
                    response = Map.of("mode", this.mode);
                } else {
                    String value = JsonUtil.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                                                                StandardCharsets.UTF_8), String.class);
                    if (rejectReset && value.equals("NONE")) {
                        status = 500;
                        response = Map.of("exception", "class java.lang.IllegalStateException",
                                          "message", "independent mode reset failed");
                    } else {
                        this.mode = value;
                        response = Map.of();
                    }
                }
            } else {
                status = 404;
                response = Map.of("message", "unsupported fixture path");
            }
            byte[] body = JsonUtil.toJson(response).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        this.server.start();
        LoadOptions options = new LoadOptions();
        options.host = "127.0.0.1";
        options.port = this.server.getAddress().getPort();
        options.file = input.toString();
        this.loader = Mockito.spy(new HugeGraphLoader(options, new LoadMapping(Collections.emptyList())));
        task.setLoader(this.loader);
        task.setOptions(options);
        this.loader.context().setLoadingMode();
        Mockito.doAnswer(invocation -> {
            task.setStatus(requestedStatus);
            this.loader.context().failLoading(this.original);
            throw this.original;
        }).when(this.loader).load();
        Mockito.doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();
            } catch (Throwable failure) {
                this.shutdownFailure.set(failure);
                throw failure;
            }
        }).when(this.loader).shutdown();
    }

    public String mode() {
        return this.mode;
    }

    public void assertResourcesClosed() throws Exception {
        Assert.assertTrue(this.loader.context().closed());
        TaskManager manager = (TaskManager) field(this.loader, HugeGraphLoader.class, "manager");
        Assert.assertTrue(((ExecutorService) field(manager, TaskManager.class, "batchService")).isTerminated());
        Assert.assertTrue(((ExecutorService) field(manager, TaskManager.class, "singleService")).isTerminated());
        Assert.assertFalse(Runtime.getRuntime().removeShutdownHook(
                (Thread) field(this.loader, HugeGraphLoader.class, "shutdownHook")));
        OkHttpClient transport = (OkHttpClient) fieldAny(fieldAny(this.loader.context().client(), "client"), "client");
        Assert.assertTrue(transport.dispatcher().executorService().isShutdown());
        Assert.assertEquals(0, transport.connectionPool().connectionCount());
        Set<Throwable> active = Collections.newSetFromMap(new IdentityHashMap<>());
        Assert.assertTrue(containsOriginalWithoutCycle(this.shutdownFailure.get(), active));
    }

    private boolean containsOriginalWithoutCycle(Throwable failure, Set<Throwable> active) {
        if (failure == null) {
            return false;
        }
        Assert.assertTrue("cyclic throwable graph", active.add(failure));
        boolean found = failure == this.original;
        found |= this.containsOriginalWithoutCycle(failure.getCause(), active);
        for (Throwable suppressed : failure.getSuppressed()) {
            found |= this.containsOriginalWithoutCycle(suppressed, active);
        }
        active.remove(failure);
        return found;
    }

    private static Object fieldAny(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                return field(object, type, name);
            } catch (NoSuchFieldException ignored) {
                // Continue through the SDK transport hierarchy.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object field(Object object, Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    @Override
    public void close() throws Exception {
        try {
            this.loader.shutdown();
        } finally {
            Runtime.getRuntime().removeShutdownHook(
                    (Thread) field(this.loader, HugeGraphLoader.class, "shutdownHook"));
            this.server.stop(0);
        }
    }
}
