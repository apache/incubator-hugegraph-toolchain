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

package org.apache.hugegraph.loader.test.unit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.LoadMetrics;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.loader.progress.LoadProgress;
import org.apache.hugegraph.loader.task.TaskManager;
import org.apache.hugegraph.structure.graph.Edge;
import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;

public class LoadOptionsTest {

    @Test
    public void testBatchInsertFailureWithFallbackDisabled() throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchFailureFallback = false;
        Assert.assertFalse(options.batchFailureFallback);

        LoadContext context = newTestContext(options);
        TaskManager taskManager = new TaskManager(context);

        EdgeMapping mapping = new EdgeMapping(Arrays.asList("s"), false,
                                              Arrays.asList("t"), false);
        mapping.label("knows");

        InputStruct struct = new InputStruct(new ArrayList<>(),
                                             new ArrayList<>());
        struct.id("1");
        struct.add(mapping);

        LoadSummary summary = context.summary();
        summary.inputMetricsMap()
               .put(struct.id(), new LoadMetrics(struct));
        LoadMetrics metrics = summary.metrics(struct);

        setField(context.client(), "graph", newFailingBatchGraphManager());

        List<Record> batch = new ArrayList<>();
        batch.add(new Record("line1", new Edge("knows")));
        batch.add(new Record("line2", new Edge("knows")));

        ByteArrayOutputStream errOutput = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errOutput, true,
                                      StandardCharsets.UTF_8.name()));
        try {
            taskManager.submitBatch(struct, mapping, batch);
            taskManager.waitFinished();

            Assert.assertEquals(0L, flightingCount(metrics));
            Assert.assertTrue(context.stopped());
            Assert.assertFalse(context.noError());

            String errText = errOutput.toString(StandardCharsets.UTF_8.name());
            Assert.assertTrue(errText.contains(
                    "Batch insert edges failed, stop loading."));

            long before = flightingCount(metrics);
            taskManager.submitBatch(struct, mapping, batch);
            taskManager.waitFinished();
            Assert.assertEquals(before, flightingCount(metrics));
        } finally {
            System.setErr(originalErr);
            taskManager.shutdown();
        }
    }

    @Test
    public void testBatchInsertFailureWithFallbackEnabled() throws Exception {
        LoadOptions options = new LoadOptions();

        LoadContext context = newTestContext(options);
        TaskManager taskManager = new TaskManager(context);

        EdgeMapping mapping = new EdgeMapping(Arrays.asList("s"), false,
                                              Arrays.asList("t"), false);
        mapping.label("knows");

        InputStruct struct = new InputStruct(new ArrayList<>(),
                                             new ArrayList<>());
        struct.id("1");
        struct.add(mapping);

        LoadSummary summary = context.summary();
        summary.inputMetricsMap()
               .put(struct.id(), new LoadMetrics(struct));
        LoadMetrics metrics = summary.metrics(struct);

        FailingBatchGraphManager.BATCH_CALLS.set(0);
        FailingBatchGraphManager.SINGLE_CALLS.set(0);
        setField(context.client(), "graph", newFailingBatchGraphManager());

        List<Record> batch = new ArrayList<>();
        batch.add(new Record("line1", new Edge("knows")));
        batch.add(new Record("line2", new Edge("knows")));

        try {
            taskManager.submitBatch(struct, mapping, batch);
            taskManager.waitFinished();

            Assert.assertEquals(1, FailingBatchGraphManager.BATCH_CALLS.get());
            Assert.assertEquals(2, FailingBatchGraphManager.SINGLE_CALLS.get());
            Assert.assertEquals(0L, flightingCount(metrics));
            Assert.assertFalse(context.stopped());
            Assert.assertTrue(context.noError());
        } finally {
            taskManager.shutdown();
        }
    }

    @Test
    public void testMultipleBatchFailuresCounterConsistency() throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchFailureFallback = true;

        LoadContext context = newTestContext(options);
        TaskManager taskManager = new TaskManager(context);

        EdgeMapping mapping = new EdgeMapping(Arrays.asList("s"), false,
                                              Arrays.asList("t"), false);
        mapping.label("knows");

        InputStruct struct = new InputStruct(new ArrayList<>(),
                                             new ArrayList<>());
        struct.id("1");
        struct.add(mapping);

        LoadSummary summary = context.summary();
        summary.inputMetricsMap()
               .put(struct.id(), new LoadMetrics(struct));
        LoadMetrics metrics = summary.metrics(struct);

        FailingBatchGraphManager.BATCH_CALLS.set(0);
        FailingBatchGraphManager.SINGLE_CALLS.set(0);
        setField(context.client(), "graph", newFailingBatchGraphManager());

        List<Record> batch1 = new ArrayList<>();
        batch1.add(new Record("line1", new Edge("knows")));
        batch1.add(new Record("line2", new Edge("knows")));

        List<Record> batch2 = new ArrayList<>();
        batch2.add(new Record("line3", new Edge("knows")));
        batch2.add(new Record("line4", new Edge("knows")));

        try {
            taskManager.submitBatch(struct, mapping, batch1);
            taskManager.submitBatch(struct, mapping, batch2);
            taskManager.waitFinished();

            Assert.assertEquals(2, FailingBatchGraphManager.BATCH_CALLS.get());
            Assert.assertEquals(4, FailingBatchGraphManager.SINGLE_CALLS.get());
            Assert.assertEquals(0L, flightingCount(metrics));
            Assert.assertFalse(context.stopped());
            Assert.assertTrue(context.noError());

            int expectedBatchPermits = 1 + options.batchInsertThreads;
            int expectedSinglePermits = 2 * options.singleInsertThreads;
            Assert.assertEquals(expectedBatchPermits,
                                getSemaphorePermits(taskManager, "batchSemaphore"));
            Assert.assertEquals(expectedSinglePermits,
                                getSemaphorePermits(taskManager, "singleSemaphore"));
        } finally {
            taskManager.shutdown();
        }
    }

    @Test
    public void testConnectionPoolAutoAdjustWithDefaultBatchThreads() throws Exception {
        int cpus = readStaticInt(LoadOptions.class, "CPUS");
        LoadOptions options = new LoadOptions();

        Assert.assertEquals(cpus * 4, options.maxConnections);
        Assert.assertEquals(cpus * 2, options.maxConnectionsPerRoute);
    }

    @Test
    public void testConnectionPoolAutoAdjustWithCustomBatchThreads() throws Exception {
        int cpus = readStaticInt(LoadOptions.class, "CPUS");
        int defaultMaxConn = readStaticInt(LoadOptions.class, "DEFAULT_MAX_CONNECTIONS");
        int defaultMaxConnPerRoute = readStaticInt(LoadOptions.class,
                                                  "DEFAULT_MAX_CONNECTIONS_PER_ROUTE");
        LoadOptions options = new LoadOptions();
        options.batchInsertThreads = 20;

        CapturingAppender appender = attachAppender();
        try {
            invokeAdjustConnectionPool(options);
        } finally {
            detachAppender(appender);
        }

        int expectedMaxConn = defaultMaxConn;
        int expectedMaxConnPerRoute = defaultMaxConnPerRoute;
        if (defaultMaxConn == cpus * 4 && defaultMaxConn < 80) {
            expectedMaxConn = 80;
        }
        if (defaultMaxConnPerRoute == cpus * 2 && defaultMaxConnPerRoute < 40) {
            expectedMaxConnPerRoute = 40;
        }

        Assert.assertEquals(expectedMaxConn, options.maxConnections);
        Assert.assertEquals(expectedMaxConnPerRoute, options.maxConnectionsPerRoute);
        if (expectedMaxConn == 80 || expectedMaxConnPerRoute == 40) {
            Assert.assertTrue(appender.contains("Auto adjusted max-conn"));
        }
    }

    @Test
    public void testConnectionPoolNoAdjustWithCustomMaxConn() throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchInsertThreads = 20;
        options.maxConnections = 100;
        options.maxConnectionsPerRoute = 50;

        CapturingAppender appender = attachAppender();
        try {
            invokeAdjustConnectionPool(options);
        } finally {
            detachAppender(appender);
        }

        Assert.assertEquals(100, options.maxConnections);
        Assert.assertEquals(50, options.maxConnectionsPerRoute);
        Assert.assertFalse(appender.contains("Auto adjusted max-conn"));
    }

    @Test
    public void testParseThreadsMinValue() {
        LoadOptions.PositiveValidator validator =
                new LoadOptions.PositiveValidator();

        validator.validate("--parser-threads", "1");

        Assert.assertTrue(validateFails(validator, "--parser-threads", "0"));
        Assert.assertTrue(validateFails(validator, "--parser-threads", "-1"));
    }

    @Test
    public void testParseThreadsDefaultValue() throws Exception {
        int cpus = readStaticInt(LoadOptions.class, "CPUS");
        LoadOptions options = new LoadOptions();
        Assert.assertEquals(Math.max(2, cpus / 2), options.parseThreads);
    }

    @Test
    public void testDeprecatedParallelCountParameter() throws Exception {
        File mapping = createTempMapping();
        String[] args = new String[]{
                "-f", mapping.getPath(),
                "-g", "g",
                "-h", "localhost",
                "--parallel-count", "4"
        };

        CapturingAppender appender = attachAppender();
        try {
            LoadOptions options = LoadOptions.parseOptions(args);
            Assert.assertEquals(4, options.parseThreads);
            Assert.assertTrue(appender.contains("deprecated"));
        } finally {
            detachAppender(appender);
            mapping.delete();
        }
    }

    private static long flightingCount(LoadMetrics metrics)
                                       throws Exception {
        Field field = LoadMetrics.class.getDeclaredField("flightingNums");
        field.setAccessible(true);
        LongAdder adder = (LongAdder) field.get(metrics);
        return adder.longValue();
    }

    private static LoadContext newTestContext(LoadOptions options)
                                              throws Exception {
        LoadContext context = (LoadContext) allocateInstance(LoadContext.class);
        setField(context, "timestamp", "test");
        setField(context, "closed", false);
        setField(context, "stopped", false);
        setField(context, "noError", true);
        setField(context, "options", options);
        setField(context, "summary", new LoadSummary());
        setField(context, "oldProgress", new LoadProgress());
        setField(context, "newProgress", new LoadProgress());
        setField(context, "loggers", new ConcurrentHashMap<>());

        HugeClient client = (HugeClient) allocateInstance(HugeClient.class);
        setField(context, "client", client);
        setField(context, "indirectClient", client);
        setField(context, "schemaCache", null);
        setField(context, "parseGroup", null);
        return context;
    }

    private static Object allocateInstance(Class<?> type) throws Exception {
        Object unsafe = unsafe();
        Method method = unsafe.getClass()
                              .getMethod("allocateInstance", Class.class);
        return method.invoke(unsafe, type);
    }

    private static Object unsafe() throws Exception {
        Class<?> unsafeClass;
        try {
            unsafeClass = Class.forName("sun.misc.Unsafe");
        } catch (ClassNotFoundException e) {
            unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
        }
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setField(Object target, String name, Object value)
                                 throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int getSemaphorePermits(Object target, String name)
                                           throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Semaphore semaphore = (Semaphore) field.get(target);
        return semaphore.availablePermits();
    }

    private static int readStaticInt(Class<?> type, String name)
                                     throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static void invokeAdjustConnectionPool(LoadOptions options)
                                                   throws Exception {
        Method method = LoadOptions.class
                                   .getDeclaredMethod("adjustConnectionPoolIfDefault",
                                                      LoadOptions.class);
        method.setAccessible(true);
        method.invoke(null, options);
    }

    private static boolean validateFails(LoadOptions.PositiveValidator validator,
                                         String name, String value) {
        try {
            validator.validate(name, value);
            return false;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static File createTempMapping() throws Exception {
        File file = File.createTempFile("load-options-", ".json", new File("."));
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"version\":\"2.0\",\"structs\":[]}");
        }
        return file;
    }

    private static CapturingAppender attachAppender() {
        Logger logger = Logger.getLogger(LoadOptions.class.getName());
        CapturingAppender appender = new CapturingAppender();
        appender.setThreshold(Level.INFO);
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(CapturingAppender appender) {
        if (appender == null) {
            return;
        }
        Logger logger = Logger.getLogger(LoadOptions.class.getName());
        logger.removeAppender(appender);
    }

    private static final class CapturingAppender extends AppenderSkeleton {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        protected void append(LoggingEvent event) {
            if (event == null || event.getRenderedMessage() == null) {
                return;
            }
            buffer.append(event.getRenderedMessage()).append('\n');
        }

        boolean contains(String text) {
            return this.buffer.toString().contains(text);
        }

        @Override
        public void close() {
            // No-op.
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    private static GraphManager newFailingBatchGraphManager() throws Exception {
        return (GraphManager) allocateInstance(FailingBatchGraphManager.class);
    }

    private static final class FailingBatchGraphManager extends GraphManager {

        private static final AtomicInteger BATCH_CALLS = new AtomicInteger();
        private static final AtomicInteger SINGLE_CALLS = new AtomicInteger();

        private FailingBatchGraphManager() {
            super(null, null, null);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
            return this.addEdges(edges);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges) {
            if (edges.size() > 1) {
                BATCH_CALLS.incrementAndGet();
                throw new RuntimeException("batch insert failure");
            }
            SINGLE_CALLS.addAndGet(edges.size());
            return edges;
        }
    }

}
