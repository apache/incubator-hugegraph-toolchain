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
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

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

public class TaskManagerTest {

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
    public void testConcurrentSubmitWhenStopping() throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchFailureFallback = false;
        options.batchInsertThreads = 2;
        options.singleInsertThreads = 1;

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

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch allowFirstFinish = new CountDownLatch(1);
        CountDownLatch failureCalled = new CountDownLatch(1);
        FailingConcurrentGraphManager.BATCH_CALLS.set(0);
        FailingConcurrentGraphManager.FIRST_STARTED = firstStarted;
        FailingConcurrentGraphManager.ALLOW_FIRST_FINISH = allowFirstFinish;
        FailingConcurrentGraphManager.FAILURE_CALLED = failureCalled;
        setField(context.client(), "graph", newFailingConcurrentGraphManager());

        List<Record> batch = new ArrayList<>();
        batch.add(new Record("line1", new Edge("knows")));
        batch.add(new Record("line2", new Edge("knows")));

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                futures.add(executor.submit(() -> {
                    taskManager.submitBatch(struct, mapping, batch);
                }));
            }

            Assert.assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(failureCalled.await(5, TimeUnit.SECONDS));
            waitStopped(context, 5, TimeUnit.SECONDS);
            allowFirstFinish.countDown();

            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }

            taskManager.waitFinished();

            int batchCalls = FailingConcurrentGraphManager.BATCH_CALLS.get();
            Assert.assertTrue(batchCalls >= 2 && batchCalls <= 3);
            Assert.assertEquals(0L, flightingCount(metrics));
            Assert.assertTrue(context.stopped());
            Assert.assertFalse(context.noError());

            long before = FailingConcurrentGraphManager.BATCH_CALLS.get();
            taskManager.submitBatch(struct, mapping, batch);
            taskManager.waitFinished();
            Assert.assertEquals(before, FailingConcurrentGraphManager.BATCH_CALLS.get());

            int expectedBatchPermits = 1 + options.batchInsertThreads;
            int expectedSinglePermits = 2 * options.singleInsertThreads;
            Assert.assertEquals(expectedBatchPermits,
                                getSemaphorePermits(taskManager, "batchSemaphore"));
            Assert.assertEquals(expectedSinglePermits,
                                getSemaphorePermits(taskManager, "singleSemaphore"));
        } finally {
            allowFirstFinish.countDown();
            executor.shutdownNow();
            taskManager.shutdown();
        }
    }

    @Test
    public void testStopCheckTimingInSubmitBatch() throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchFailureFallback = false;
        options.batchInsertThreads = 1;
        options.singleInsertThreads = 1;

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

        setField(context.client(), "graph", newSimpleGraphManager());

        List<Record> batch = new ArrayList<>();
        batch.add(new Record("line1", new Edge("knows")));
        batch.add(new Record("line2", new Edge("knows")));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            taskManager.submitBatch(struct, mapping, batch);
            taskManager.waitFinished();

            Semaphore semaphore = getSemaphore(taskManager, "batchSemaphore");
            semaphore.acquire();

            Future<?> blocked = executor.submit(() -> {
                taskManager.submitBatch(struct, mapping, batch);
            });

            Thread.sleep(50);
            context.stopLoading();
            semaphore.release();

            blocked.get(5, TimeUnit.SECONDS);

            taskManager.waitFinished();

            Assert.assertTrue(context.stopped());
            Assert.assertEquals(0L, flightingCount(metrics));
            int expectedPermits = 1 + options.batchInsertThreads;
            Assert.assertEquals(expectedPermits,
                                getSemaphorePermits(taskManager, "batchSemaphore"));
        } finally {
            executor.shutdownNow();
            taskManager.shutdown();
        }
    }

    private static void waitStopped(LoadContext context, long timeout,
                                    TimeUnit unit) throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (!context.stopped() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        Assert.assertTrue(context.stopped());
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

    private static Semaphore getSemaphore(Object target, String name)
                                          throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (Semaphore) field.get(target);
    }

    private static GraphManager newFailingConcurrentGraphManager()
                                                            throws Exception {
        return (GraphManager) allocateInstance(FailingConcurrentGraphManager.class);
    }

    private static GraphManager newFailingBatchGraphManager() throws Exception {
        return (GraphManager) allocateInstance(FailingBatchGraphManager.class);
    }

    private static GraphManager newSimpleGraphManager() throws Exception {
        return (GraphManager) allocateInstance(SimpleGraphManager.class);
    }

    private static final class SimpleGraphManager extends GraphManager {

        private SimpleGraphManager() {
            super(null, null, null);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
            return this.addEdges(edges);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges) {
            return edges;
        }
    }

    private static final class FailingConcurrentGraphManager extends GraphManager {

        private static final AtomicInteger BATCH_CALLS = new AtomicInteger();
        private static volatile CountDownLatch FIRST_STARTED;
        private static volatile CountDownLatch ALLOW_FIRST_FINISH;
        private static volatile CountDownLatch FAILURE_CALLED;

        private FailingConcurrentGraphManager() {
            super(null, null, null);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
            return this.addEdges(edges);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges) {
            int call = BATCH_CALLS.incrementAndGet();
            if (call == 1) {
                CountDownLatch started = FIRST_STARTED;
                if (started != null) {
                    started.countDown();
                }
                await(ALLOW_FIRST_FINISH);
                return edges;
            }
            if (call == 2) {
                CountDownLatch failed = FAILURE_CALLED;
                if (failed != null) {
                    failed.countDown();
                }
                throw new RuntimeException("batch insert failure");
            }
            return edges;
        }

        private void await(CountDownLatch latch) {
            if (latch == null) {
                return;
            }
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // Let the task finish on interruption.
            }
        }
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
