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
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.driver.HugeClientBuilder;

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
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.loader.task.BatchInsertTask;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.util.LoadUtil;

import org.apache.hugegraph.testutil.Assert;

public class TaskManagerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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
        struct.input(new FileSource());
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
            assertFailureLines(context, struct, "line1", "line2");
            Assert.assertEquals(4L, metrics.insertFailure(mapping));
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
        struct.input(new FileSource());
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
        struct.input(new FileSource());
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
        struct.input(new FileSource());
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
                List<Record> submitted = Arrays.asList(
                        new Record("concurrent-" + i, new Edge("knows")));
                futures.add(executor.submit(() -> {
                    taskManager.submitBatch(struct, mapping, submitted);
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

            Assert.assertEquals(10L, metrics.insertSuccess(mapping) +
                                         metrics.insertFailure(mapping));

            long before = FailingConcurrentGraphManager.BATCH_CALLS.get();
            taskManager.submitBatch(struct, mapping, batch);
            taskManager.waitFinished();
            Assert.assertEquals(before, FailingConcurrentGraphManager.BATCH_CALLS.get());
            context.failureLogger(struct).close();
            List<String> failureLines = Files.readAllLines(failureFile(context, struct).toPath());
            Assert.assertEquals(2L * metrics.insertFailure(mapping), failureLines.size());

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
        this.assertStoppedBatch(false, true);
    }

    @Test(timeout = 15000)
    public void testReadLimitStopDoesNotFailWaitingBatch() throws Exception {
        this.assertStoppedBatch(true, false);
    }

    @Test(timeout = 15000)
    public void testErrorAfterReadLimitFailsWaitingBatch() throws Exception {
        this.assertStoppedBatch(true, true);
    }

    private void assertStoppedBatch(boolean readLimit, boolean failed) throws Exception {
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
        struct.input(new FileSource());
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
            semaphore.acquire(1 + options.batchInsertThreads);

            Future<?> blocked = executor.submit(() -> {
                taskManager.submitBatch(struct, mapping, batch);
            });

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!semaphore.hasQueuedThreads() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            Assert.assertTrue(semaphore.hasQueuedThreads());
            if (readLimit) {
                context.stopLoadingAtReadLimit();
                if (failed) {
                    context.occurredError();
                }
            } else {
                context.stopLoading();
            }
            semaphore.release(1 + options.batchInsertThreads);

            blocked.get(5, TimeUnit.SECONDS);

            taskManager.waitFinished();

            Assert.assertTrue(context.stopped());
            Assert.assertEquals(!failed, context.noError());
            Assert.assertEquals(failed ? batch.size() : 0L, metrics.insertFailure(mapping));
            if (!failed) {
                context.failureLogger(struct).close();
                Assert.assertFalse(failureFile(context, struct).exists());
            } else {
                assertFailureLines(context, struct, "line1", "line2");
            }
            Assert.assertEquals(0L, flightingCount(metrics));
            int expectedPermits = 1 + options.batchInsertThreads;
            Assert.assertEquals(expectedPermits,
                                getSemaphorePermits(taskManager, "batchSemaphore"));
        } finally {
            executor.shutdownNow();
            taskManager.shutdown();
        }
    }

    @Test(timeout = 15000)
    public void testShutdownWaitsForInlineCompletionHandler() throws Exception {
        this.assertShutdownWaitsForCallerWriter(false, false);
    }

    @Test(timeout = 15000)
    public void testShutdownWaitsForStoppedBatchWriter() throws Exception {
        this.assertShutdownWaitsForCallerWriter(true, false);
    }

    @Test(timeout = 15000)
    public void testShutdownWaitsForSubmitterPausedBeforePermitAcquisition() throws Exception {
        this.assertShutdownWaitsForCallerWriter(false, true);
    }

    private void assertShutdownWaitsForCallerWriter(boolean stopped, boolean pause) throws Exception {
        LoadOptions options = new LoadOptions();
        options.retryTimes = 0;
        options.batchFailureFallback = false;
        LoadContext context = newTestContext(options);
        TaskManager manager = new TaskManager(context);
        setField(manager, "batchService", new DirectExecutor());
        ClosingClient client = (ClosingClient) allocateInstance(ClosingClient.class);
        client.closed = new CountDownLatch(1);
        client.manager = manager;
        setField(context, "client", client);
        setField(context, "indirectClient", client);
        Field graphField = HugeClient.class.getDeclaredField("graph");
        graphField.setAccessible(true);
        graphField.set(client, allocateInstance(RetryableGraphManager.class));
        InputStruct struct = newEdgeStruct(context);
        EdgeMapping mapping = struct.edges().get(0);
        Object writer = getField(context.failureLogger(struct), "writer");
        ((BufferedWriter) getField(writer, "writer")).close();
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> writerThread = new AtomicReference<>();
        if (pause) {
            setField(manager, "batchSemaphore", new Semaphore(1 + options.batchInsertThreads) {
                @Override
                public void acquire() throws InterruptedException {
                    writerThread.set(Thread.currentThread());
                    writing.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new InterruptedException("Timed out before permit acquisition");
                    }
                    super.acquire();
                }
            });
        }
        setField(writer, "writer", new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(failureFile(context, struct)), StandardCharsets.UTF_8)) {
            @Override
            public void write(String text, int offset, int length) throws IOException {
                writerThread.set(Thread.currentThread());
                writing.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IOException("Timed out waiting to write failure data");
                    }
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
                super.write(text, offset, length);
            }
        });
        if (stopped) {
            context.stopLoading();
        }
        HugeGraphLoader loader = (HugeGraphLoader) allocateInstance(HugeGraphLoader.class);
        setField(loader, "context", context);
        setField(loader, "manager", manager);
        setField(loader, "options", options);
        ExecutorService callers = Executors.newFixedThreadPool(2);
        AtomicReference<Thread> producerThread = new AtomicReference<>();
        try {
            Future<?> producer = callers.submit(() -> {
                producerThread.set(Thread.currentThread());
                Runnable submit = () -> manager.submitBatch(struct, mapping,
                        Arrays.asList(new Record("caller-record", new Edge("knows"))));
                if (pause) {
                    Assert.assertThrows(LoadException.class, submit::run);
                } else {
                    submit.run();
                }
            });
            Assert.assertTrue(writing.await(5, TimeUnit.SECONDS));
            Assert.assertSame(producerThread.get(), writerThread.get());
            Future<Boolean> shutdown = callers.submit(() -> {
                if (!pause) {
                    Thread.currentThread().interrupt();
                }
                Assert.assertThrows(LoadException.class, loader::shutdown);
                return Thread.currentThread().isInterrupted();
            });
            Assert.assertFalse(client.closed.await(100, TimeUnit.MILLISECONDS));
            release.countDown();
            producer.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(!pause, shutdown.get(5, TimeUnit.SECONDS));
            Assert.assertThrows(LoadException.class, () -> manager.submitBatch(struct, mapping,
                                Arrays.asList(new Record("late-record", new Edge("knows")))));
            if (!pause) {
                Assert.assertTrue(Files.readAllLines(failureFile(context, struct).toPath()).contains("caller-record"));
            }
            Assert.assertFalse(new File(LoadProgress.format(options, "test")).exists());
        } finally {
            release.countDown();
            callers.shutdownNow();
            callers.awaitTermination(5, TimeUnit.SECONDS);
            manager.shutdown();
        }
    }

    private static final class DirectExecutor extends ThreadPoolExecutor {
        private DirectExecutor() {
            super(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    @Test(timeout = 15000)
    public void testInterruptedShutdownDrainsInFlightBatchBeforeClosingClients() throws Exception {
        this.assertInterruptedShutdown(false, false, false);
    }

    @Test(timeout = 15000)
    public void testInterruptedShutdownWaitsForCancelledBatchBeforeClosingClients() throws Exception {
        this.assertInterruptedShutdown(true, false, false);
    }

    @Test(timeout = 15000)
    public void testShutdownTimeoutKeepsInFlightDependenciesOpen() throws Exception {
        this.assertInterruptedShutdown(true, true, false);
    }

    @Test(timeout = 15000)
    public void testQueuedBatchCancellationCompletesHandlerAndReleasesPermit() throws Exception {
        this.assertInterruptedShutdown(true, false, true);
    }

    private void assertInterruptedShutdown(boolean cancel, boolean timeout, boolean queued) throws Exception {
        LoadOptions options = new LoadOptions();
        if (cancel) {
            options.shutdownTimeout = 1;
        }
        if (queued) {
            options.batchInsertThreads = 1;
        }
        LoadContext context = newTestContext(options);
        TaskManager manager = new TaskManager(context);
        ClosingClient client = (ClosingClient) allocateInstance(ClosingClient.class);
        client.closed = new CountDownLatch(1);
        client.manager = manager;
        setField(context, "client", client);
        setField(context, "indirectClient", client);
        BlockingGraphManager graph = (BlockingGraphManager) allocateInstance(BlockingGraphManager.class);
        graph.started = new CountDownLatch(1);
        graph.release = new CountDownLatch(1);
        graph.cancelled = new CountDownLatch(1);
        Field graphField = HugeClient.class.getDeclaredField("graph");
        graphField.setAccessible(true);
        graphField.set(client, graph);
        InputStruct struct = newEdgeStruct(context);
        EdgeMapping mapping = struct.edges().get(0);
        HugeGraphLoader loader = (HugeGraphLoader) allocateInstance(HugeGraphLoader.class);
        setField(loader, "context", context);
        setField(loader, "manager", manager);
        setField(loader, "options", options);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            manager.submitBatch(struct, mapping, Arrays.asList(new Record("in-flight", new Edge("knows"))));
            Assert.assertTrue(graph.started.await(5, TimeUnit.SECONDS));
            if (queued) {
                manager.submitBatch(struct, mapping, Arrays.asList(new Record("queued", new Edge("knows"))));
            }
            Future<Boolean> shutdown = caller.submit(() -> {
                Thread.currentThread().interrupt();
                Assert.assertThrows(LoadException.class, loader::shutdown);
                return Thread.currentThread().isInterrupted();
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            ExecutorService batchService = (ExecutorService) getField(manager, "batchService");
            while (!batchService.isShutdown() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            Assert.assertTrue(batchService.isShutdown());
            if (cancel) {
                Assert.assertTrue(graph.cancelled.await(5, TimeUnit.SECONDS));
            }
            Assert.assertFalse(client.closed.await(100, TimeUnit.MILLISECONDS));
            if (!timeout) {
                graph.release.countDown();
            }
            Assert.assertTrue(shutdown.get(5, TimeUnit.SECONDS));
            Assert.assertEquals(!timeout, context.closed());
            Assert.assertFalse(client.closedWhileRunning);
            Assert.assertFalse(context.noError());
            if (!timeout) {
                Assert.assertEquals(0L, flightingCount(context.summary().metrics(struct)));
                if (queued) {
                    Assert.assertEquals(1L, context.summary().metrics(struct).insertSuccess(mapping));
                    Assert.assertEquals(1 + options.batchInsertThreads,
                                        getSemaphorePermits(manager, "batchSemaphore"));
                }
            } else {
                Assert.assertEquals(1L, flightingCount(context.summary().metrics(struct)));
                Assert.assertEquals(1L, client.closed.getCount());
            }
            Assert.assertFalse(new File(LoadProgress.format(options, "test")).exists());
        } finally {
            graph.release.countDown();
            caller.shutdownNow();
            caller.awaitTermination(5, TimeUnit.SECONDS);
            manager.shutdown();
        }
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static final class ClosingClient extends HugeClient {
        private CountDownLatch closed;
        private TaskManager manager;
        private boolean closedWhileRunning;

        private ClosingClient() {
            super((HugeClientBuilder) null);
        }

        @Override
        public void close() {
            // Model a dependency close that consumes interruption internally.
            Thread.interrupted();
            try {
                this.closedWhileRunning =
                        !((ExecutorService) getField(this.manager, "batchService")).isTerminated() ||
                        !((ExecutorService) getField(this.manager, "singleService")).isTerminated();
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                this.closed.countDown();
            }
        }
    }

    private static final class BlockingGraphManager extends GraphManager {
        private CountDownLatch started;
        private CountDownLatch release;
        private CountDownLatch cancelled;

        private BlockingGraphManager() {
            super(null, null, null);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
            this.started.countDown();
            boolean interrupted = false;
            while (this.release.getCount() != 0) {
                try {
                    this.release.await();
                } catch (InterruptedException e) {
                    interrupted = true;
                    this.cancelled.countDown();
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return edges;
        }
    }

    @Test
    public void testInterruptedWaitDoesNotAddPermits() throws Exception {
        LoadOptions options = new LoadOptions();
        LoadContext context = newTestContext(options);
        TaskManager manager = new TaskManager(context);
        try {
            Thread.currentThread().interrupt();
            Assert.assertThrows(LoadException.class, manager::waitFinished);
            Assert.assertTrue(Thread.currentThread().isInterrupted());
            Assert.assertFalse(context.noError());
            Assert.assertEquals(1 + options.batchInsertThreads,
                                getSemaphorePermits(manager, "batchSemaphore"));
            Assert.assertEquals(2 * options.singleInsertThreads,
                                getSemaphorePermits(manager, "singleSemaphore"));
        } finally {
            Thread.interrupted();
            manager.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testFailureLogErrorIsPropagatedWithoutLeakingPermits() throws Exception {
        this.assertFailureLogError(false);
    }

    @Test(timeout = 10000)
    public void testSingleFallbackLogErrorIsPropagatedWithoutLeakingPermits() throws Exception {
        this.assertFailureLogError(true);
    }

    private void assertFailureLogError(boolean fallback) throws Exception {
        LoadOptions options = new LoadOptions();
        options.batchFailureFallback = fallback;
        options.retryTimes = 0;
        LoadContext context = newTestContext(options);
        TaskManager manager = new TaskManager(context);
        InputStruct struct = newEdgeStruct(context);
        EdgeMapping mapping = struct.edges().get(0);
        LoadMetrics metrics = context.summary().metrics(struct);
        setField(context.client(), "graph", allocateInstance(RetryableGraphManager.class));
        Assert.assertTrue(failureFile(context, struct).mkdirs());
        try {
            manager.submitBatch(struct, mapping,
                                Arrays.asList(new Record("unsaved", new Edge("knows"))));
            Assert.assertThrows(LoadException.class,
                                manager::waitFinished);
            Assert.assertTrue(context.stopped());
            Assert.assertFalse(context.noError());
            Assert.assertEquals(0L, flightingCount(metrics));
            Assert.assertEquals(1 + options.batchInsertThreads,
                                getSemaphorePermits(manager, "batchSemaphore"));
            Assert.assertEquals(2 * options.singleInsertThreads,
                                getSemaphorePermits(manager, "singleSemaphore"));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testZeroRetriesReportsBatchFailure() throws Exception {
        LoadOptions options = new LoadOptions();
        options.retryTimes = 0;
        LoadContext context = newTestContext(options);
        InputStruct struct = newEdgeStruct(context);
        EdgeMapping mapping = struct.edges().get(0);
        setField(context.client(), "graph", allocateInstance(RetryableGraphManager.class));
        List<Record> batch = Arrays.asList(new Record("retry", new Edge("knows")));
        Assert.assertThrows(ClientException.class, () -> {
            new BatchInsertTask(context, struct, mapping, batch).execute();
        });
        Assert.assertEquals(0L, context.summary().metrics(struct).insertSuccess(mapping));
    }

    private static InputStruct newEdgeStruct(LoadContext context) {
        EdgeMapping mapping = new EdgeMapping(Arrays.asList("s"), false, Arrays.asList("t"), false);
        mapping.label("knows");
        InputStruct struct = new InputStruct(new ArrayList<>(), new ArrayList<>());
        struct.id("1");
        struct.input(new FileSource());
        struct.add(mapping);
        context.summary().inputMetricsMap().put(struct.id(), new LoadMetrics(struct));
        return struct;
    }

    private static File failureFile(LoadContext context, InputStruct struct) {
        return new File(LoadUtil.getStructDirPrefix(context.options()),
                        "failure-data/" + struct.id() + ".error");
    }

    private static void assertFailureLines(LoadContext context, InputStruct struct,
                                           String... lines) throws Exception {
        context.failureLogger(struct).close();
        List<String> saved = Files.readAllLines(failureFile(context, struct).toPath());
        Assert.assertEquals(2 * lines.length, saved.size());
        for (String line : lines) {
            Assert.assertTrue(saved.contains(line));
        }
    }

    private static final class RetryableGraphManager extends GraphManager {
        private RetryableGraphManager() {
            super(null, null, null);
        }

        @Override
        public List<Edge> addEdges(List<Edge> edges, boolean checkVertex) {
            throw new ClientException("retryable connection failure");
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

    private LoadContext newTestContext(LoadOptions options)
                                              throws Exception {
        options.file = this.folder.newFile("mapping.json").getAbsolutePath();
        LoadContext context = (LoadContext) allocateInstance(LoadContext.class);
        setField(context, "timestamp", "test");
        setField(context, "closed", false);
        setField(context, "stopReason", null);
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
                Thread.currentThread().interrupt();
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
