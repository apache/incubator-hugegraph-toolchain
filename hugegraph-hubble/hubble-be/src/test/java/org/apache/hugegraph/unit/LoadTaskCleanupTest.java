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

package org.apache.hugegraph.unit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class LoadTaskCleanupTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSuccessIsPublishedAfterShutdown() {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        Mockito.when(loader.load()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            Assert.assertEquals(LoadStatus.RUNNING, task.getStatus());
            Assert.assertFalse(task.isFinished());
            return null;
        }).when(loader).shutdown();

        task.run();

        Mockito.verify(loader).shutdown();
        Assert.assertEquals(LoadStatus.SUCCEED, task.getStatus());
        Assert.assertTrue(task.isFinished());
        Assert.assertEquals(Long.valueOf(7L), task.getFileReadLines());
        Assert.assertEquals(Long.valueOf(12L), task.getLastDuration());
    }

    @Test
    public void testLoadFailureKeepsCleanupFailureSuppressed() {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        RuntimeException loadFailure = new RuntimeException("load failed");
        RuntimeException cleanupFailure = new RuntimeException("cleanup failed");
        Mockito.when(loader.load()).thenThrow(loadFailure);
        Mockito.doThrow(cleanupFailure).when(loader).shutdown();

        task.run();

        Mockito.verify(loader).shutdown();
        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
        Assert.assertTrue(task.isFinished());
        Assert.assertArrayEquals(new Throwable[]{cleanupFailure}, loadFailure.getSuppressed());
    }

    @Test
    public void testFalseLoadResultStillShutsDown() {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        Mockito.when(loader.load()).thenReturn(false);

        task.run();

        Mockito.verify(loader).shutdown();
        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
        Assert.assertTrue(task.isFinished());
    }

    @Test
    public void testCleanupFailureMarksPausedAndStoppedTasksFailed() {
        for (LoadStatus status : new LoadStatus[]{LoadStatus.RUNNING, LoadStatus.PAUSED, LoadStatus.STOPPED}) {
            HugeGraphLoader loader = this.loader();
            LoadTask task = this.task(loader);
            task.setStatus(status);
            Mockito.when(loader.load()).thenReturn(true);
            Mockito.doThrow(new RuntimeException("cleanup failed")).when(loader).shutdown();

            task.run();

            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertTrue(task.isFinished());
        }
    }

    @Test
    public void testSameFailureIsNotSuppressedOntoItself() {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        RuntimeException failure = new RuntimeException("same failure");
        Mockito.when(loader.load()).thenThrow(failure);
        Mockito.doThrow(failure).when(loader).shutdown();

        task.run();

        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
        Assert.assertTrue(task.isFinished());
        Assert.assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    public void testStopLeavesShutdownToRunAndWaitsForCleanup() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadContext context = loader.context();
        LoadTask task = this.task(loader);
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch releaseLoad = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        CountDownLatch cleaning = new CountDownLatch(1);
        CountDownLatch releaseCleanup = new CountDownLatch(1);
        Mockito.when(loader.load()).thenAnswer(invocation -> {
            loading.countDown();
            Assert.assertTrue(releaseLoad.await(5, TimeUnit.SECONDS));
            return true;
        });
        Mockito.doAnswer(invocation -> {
            stopped.countDown();
            return null;
        }).when(context).stopLoading();
        Mockito.doAnswer(invocation -> {
            cleaning.countDown();
            Assert.assertTrue(releaseCleanup.await(5, TimeUnit.SECONDS));
            Mockito.when(context.closed()).thenReturn(true);
            return null;
        }).when(loader).shutdown();
        Thread running = new Thread(task);
        Thread stopping = new Thread(task::stop);
        try {
            running.start();
            Assert.assertTrue(loading.await(5, TimeUnit.SECONDS));
            task.setStatus(LoadStatus.PAUSED);
            stopping.start();
            Assert.assertTrue(stopped.await(5, TimeUnit.SECONDS));
            Mockito.verify(loader, Mockito.never()).shutdown();
            releaseLoad.countDown();
            Assert.assertTrue(cleaning.await(5, TimeUnit.SECONDS));
            Assert.assertFalse(task.isFinished());
            Assert.assertTrue(stopping.isAlive());
            Assert.assertSame(loader, task.getLoader());
            releaseCleanup.countDown();
            running.join(5000);
            stopping.join(5000);
            Assert.assertFalse(running.isAlive());
            Assert.assertFalse(stopping.isAlive());
            Mockito.verify(loader).shutdown();
            Assert.assertNull(task.getLoader());
            Assert.assertEquals(LoadStatus.PAUSED, task.getStatus());
        } finally {
            releaseLoad.countDown();
            releaseCleanup.countDown();
            running.join(5000);
            stopping.join(5000);
        }
    }

    @Test
    public void testPreviewShutsDownAfterSuccess() {
        try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                HugeGraphLoader.class, (loader, context) -> Mockito.when(loader.load()).thenReturn(true))) {
            new LoadTaskService().loader(new LoadOptions());

            Assert.assertEquals(1, construction.constructed().size());
            Mockito.verify(construction.constructed().get(0)).shutdown();
        }
    }

    @Test
    public void testPreviewKeepsPrimaryFailureWhenShutdownFails() {
        RuntimeException loadFailure = new RuntimeException("preview load failed");
        RuntimeException cleanupFailure = new RuntimeException("preview cleanup failed");
        try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                HugeGraphLoader.class, (loader, context) -> {
                    Mockito.when(loader.load()).thenThrow(loadFailure);
                    Mockito.doThrow(cleanupFailure).when(loader).shutdown();
                })) {
            new LoadTaskService().loader(new LoadOptions());

            Mockito.verify(construction.constructed().get(0)).shutdown();
            Assert.assertArrayEquals(new Throwable[]{cleanupFailure}, loadFailure.getSuppressed());
        }
    }

    @Test
    public void testSaveFailureClosesEagerLoaderAndKeepsPrimaryFailure() throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        set(service, "mapper", mapper);
        RuntimeException failure = new RuntimeException("database unavailable");
        RuntimeException cleanup = new RuntimeException("cleanup failed");
        Mockito.when(mapper.insert(Mockito.any(LoadTask.class))).thenThrow(failure);
        FileMapping mapping = new FileMapping();
        mapping.setPath(this.folder.newFile("input.csv").getAbsolutePath());
        mapping.setVertexMappings(Collections.emptySet());
        mapping.setEdgeMappings(Collections.emptySet());
        mapping.setLoadParameter(new LoadParameter());
        FileSetting setting = new FileSetting();
        setting.setColumnNames(Collections.singletonList("id"));
        mapping.setFileSetting(setting);
        try (MockedConstruction<HugeGraphLoader> owners = Mockito.mockConstruction(
                HugeGraphLoader.class, (loader, context) -> {
                    LoadContext resources = Mockito.mock(LoadContext.class);
                    Mockito.when(loader.context()).thenReturn(resources);
                    Mockito.when(resources.closed()).thenReturn(true);
                    Mockito.doThrow(cleanup).when(loader).shutdown();
                })) {
            try {
                service.start(new GraphConnection(), mapping, null);
                Assert.fail("save must fail");
            } catch (RuntimeException actual) {
                Assert.assertSame(failure, actual);
            }
            Assert.assertEquals(1, owners.constructed().size());
            Mockito.verify(owners.constructed().get(0)).shutdown();
            Assert.assertArrayEquals(new Throwable[]{cleanup}, failure.getSuppressed());
        }
    }

    @Test
    public void testOuterRollbackClosesUnscheduledLoader() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        LoadTaskService service = new LoadTaskService();
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        set(service, "taskExecutor", executor);
        TransactionSynchronizationManager.initSynchronization();
        try {
            dispatch(service, task);
            TransactionSynchronizationManager.getSynchronizations().get(0)
                                             .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            Mockito.verify(loader).shutdown();
            Mockito.verifyNoInteractions(executor);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void testDispatchRejectionClosesUnscheduledLoader() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        LoadTaskService service = new LoadTaskService();
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        RuntimeException rejection = new java.util.concurrent.RejectedExecutionException("executor closed");
        RuntimeException cleanup = new RuntimeException("cleanup failed");
        Mockito.doThrow(rejection).when(executor).execute(Mockito.eq(task), Mockito.any());
        Mockito.doThrow(cleanup).when(loader).shutdown();
        set(service, "taskExecutor", executor);
        dispatch(service, task);
        Mockito.verify(loader).shutdown();
        Assert.assertArrayEquals(new Throwable[]{cleanup}, rejection.getSuppressed());
        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
    }

    @Test
    public void testFinishedIncludesPersistenceCallback() {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        Mockito.when(loader.load()).thenReturn(true);
        new LoadTaskExecutor().execute(task, () -> {
            Mockito.verify(loader).shutdown();
            Assert.assertFalse("runner is not finished until its callback returns", task.isFinished());
        });
        Assert.assertTrue(task.isFinished());
    }

    @Test
    public void testStopOfPausedOwnerWaitsForRunner() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        task.setStatus(LoadStatus.PAUSED);
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch stopReturned = new CountDownLatch(1);
        Mockito.when(loader.load()).thenAnswer(invocation -> {
            loading.countDown();
            Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
            return true;
        });
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.updateById(task)).thenReturn(1);
        set(service, "mapper", mapper);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        owners.put(9, task);
        set(service, "runningTaskContainer", owners);
        Thread runner = new Thread(() -> new LoadTaskExecutor().execute(task, () -> service.update(task)));
        Thread stopper = new Thread(() -> {
            service.stop(9);
            stopReturned.countDown();
        });
        try {
            runner.start();
            Assert.assertTrue(loading.await(5, TimeUnit.SECONDS));
            stopper.start();
            Assert.assertFalse("PAUSED owner is still loading", stopReturned.await(300, TimeUnit.MILLISECONDS));
            Assert.assertSame(task, owners.get(9));
        } finally {
            release.countDown();
            runner.join(5000);
            stopper.join(5000);
        }
        Assert.assertFalse(runner.isAlive());
        Assert.assertFalse(stopper.isAlive());
        Assert.assertEquals(LoadStatus.STOPPED, task.getStatus());
        Assert.assertTrue(owners.isEmpty());
    }

    @Test
    public void testStopAndRetryCannotPassOldPersistenceCallback() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        task.setStatus(LoadStatus.PAUSED);
        Mockito.when(loader.load()).thenReturn(true);
        LoadTask restored = Mockito.mock(LoadTask.class);
        Mockito.when(restored.getStatus()).thenReturn(LoadStatus.STOPPED);
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.selectById(9)).thenReturn(
                LoadTask.builder().id(9).status(LoadStatus.RUNNING).build(), restored);
        CountDownLatch callback = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch stopReturned = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<LoadStatus> persisted =
                new java.util.concurrent.atomic.AtomicReference<>();
        Mockito.when(mapper.updateById(task)).thenAnswer(invocation -> {
            if (callback.getCount() > 0) {
                callback.countDown();
                Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
            }
            persisted.set(task.getStatus());
            return 1;
        });
        set(service, "mapper", mapper);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        Thread[] runner = new Thread[1];
        Mockito.doAnswer(invocation -> {
            Runnable completion = invocation.getArgument(1);
            runner[0] = new Thread(() -> new LoadTaskExecutor().execute(task, completion));
            runner[0].start();
            return null;
        }).when(executor).execute(Mockito.eq(task), Mockito.any());
        set(service, "taskExecutor", executor);
        dispatch(service, task);
        Thread stopper = new Thread(() -> {
            service.stop(9);
            stopReturned.countDown();
        });
        CountDownLatch retryReturned = new CountDownLatch(1);
        Thread retry = new Thread(() -> {
            try {
                service.retry(9, "token");
            } catch (RuntimeException expected) {
                // The mock restored task cannot reconnect, after the old callback has finished.
            } finally {
                retryReturned.countDown();
            }
        });
        try {
            Assert.assertTrue(callback.await(5, TimeUnit.SECONDS));
            Assert.assertFalse(task.isFinished());
            stopper.start();
            Assert.assertFalse(stopReturned.await(300, TimeUnit.MILLISECONDS));
            retry.start();
            Assert.assertFalse(retryReturned.await(300, TimeUnit.MILLISECONDS));
            Mockito.verify(restored, Mockito.never()).reconnect(Mockito.anyString());
        } finally {
            release.countDown();
            runner[0].join(5000);
            stopper.join(5000);
            retry.join(5000);
        }
        Assert.assertFalse(runner[0].isAlive());
        Assert.assertFalse(stopper.isAlive());
        Assert.assertFalse(retry.isAlive());
        Assert.assertEquals(LoadStatus.STOPPED, persisted.get());
    }

    @Test
    public void testFastRunnerDoesNotLeaveFinishedOwnerInContainer() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        Mockito.when(loader.load()).thenReturn(true);
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.updateById(task)).thenReturn(1);
        set(service, "mapper", mapper);
        set(service, "taskExecutor", new LoadTaskExecutor());
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        dispatch(service, task);
        Assert.assertTrue(task.isFinished());
        Assert.assertTrue(owners.isEmpty());
    }

    @Test
    public void testStopRetriesFailedCallbackBeforeRemovingOwner() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        task.setStatus(LoadStatus.PAUSED);
        Mockito.when(loader.load()).thenReturn(true);
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.updateById(task)).thenThrow(new IllegalStateException("database offline"))
               .thenReturn(1);
        set(service, "mapper", mapper);
        set(service, "taskExecutor", new LoadTaskExecutor());
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        dispatch(service, task);
        Assert.assertTrue(task.isFinished());
        Assert.assertSame(task, owners.get(9));
        service.stop(9);
        Mockito.verify(mapper, Mockito.times(2)).updateById(task);
        Mockito.verify(loader).shutdown();
        Assert.assertEquals(LoadStatus.STOPPED, task.getStatus());
        Assert.assertTrue(owners.isEmpty());
    }

    @Test
    public void testResumeSaveFailureClosesNewOwnerAndKeepsPrimaryFailure() throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        LoadTask task = LoadTask.builder().id(9).options(new LoadOptions()).status(LoadStatus.PAUSED).build();
        Mockito.when(mapper.selectById(9)).thenReturn(task);
        RuntimeException failure = new RuntimeException("database unavailable");
        RuntimeException cleanup = new RuntimeException("cleanup failed");
        Mockito.when(mapper.update(Mockito.eq(task), Mockito.any())).thenThrow(failure);
        set(service, "mapper", mapper);
        try (MockedConstruction<HugeGraphLoader> owners = Mockito.mockConstruction(
                HugeGraphLoader.class, (loader, context) -> {
                    LoadContext resources = Mockito.mock(LoadContext.class);
                    Mockito.when(loader.context()).thenReturn(resources);
                    Mockito.when(resources.closed()).thenReturn(true);
                    Mockito.doThrow(cleanup).when(loader).shutdown();
                })) {
            try {
                service.resume(9, "session-token");
                Assert.fail("save must fail");
            } catch (RuntimeException actual) {
                Assert.assertSame(failure, actual);
            }
            Mockito.verify(owners.constructed().get(0)).shutdown();
            Assert.assertArrayEquals(new Throwable[]{cleanup}, failure.getSuppressed());
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertTrue(task.isFinished());
        }
    }

    @Test(timeout = 15000)
    public void testRealWorkerTimeoutRetainsOwnerUntilContextCloses() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/versions", exchange -> {
            byte[] body = "{\"versions\":{\"core\":\"1.8.0\",\"api\":\"0.74\"}}"
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        HugeGraphLoader loader = null;
        Thread hook = null;
        try {
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.getAddress().getPort();
            options.file = this.folder.newFile("timeout.json").getAbsolutePath();
            options.batchInsertThreads = 1;
            options.singleInsertThreads = 1;
            options.shutdownTimeout = 1;
            loader = Mockito.spy(new HugeGraphLoader(options,
                    new org.apache.hugegraph.loader.mapping.LoadMapping(Collections.emptyList())));
            Mockito.doReturn(true).when(loader).load();
            Field hookField = HugeGraphLoader.class.getDeclaredField("shutdownHook");
            hookField.setAccessible(true);
            hook = (Thread) hookField.get(loader);
            Field managerField = HugeGraphLoader.class.getDeclaredField("manager");
            managerField.setAccessible(true);
            Object manager = managerField.get(loader);
            Field poolField = manager.getClass().getDeclaredField("batchService");
            poolField.setAccessible(true);
            java.util.concurrent.ExecutorService pool =
                    (java.util.concurrent.ExecutorService) poolField.get(manager);
            pool.submit(() -> {
                running.countDown();
                try {
                    while (release.getCount() > 0) {
                        try {
                            release.await(100, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException ignored) {
                            // Deliberately model a backend call that ignores cancellation.
                        }
                    }
                } finally {
                    stopped.countDown();
                }
            });
            Assert.assertTrue(running.await(5, TimeUnit.SECONDS));
            LoadTask task = this.task(loader);
            task.setId(9);
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
            Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenReturn(1);
            set(service, "mapper", mapper);
            set(service, "taskExecutor", new LoadTaskExecutor());
            Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
            set(service, "runningTaskContainer", owners);
            dispatch(service, task);
            Assert.assertTrue(task.isFinished());
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertFalse(loader.context().closed());
            Assert.assertSame(task, owners.get(9));
            Assert.assertFalse(pool.isTerminated());
            task.stop();
            Assert.assertSame(loader, task.getLoader());
            RuntimeException primary;
            try {
                loader.context().throwIfFailed();
                throw new AssertionError("timeout diagnosis is missing");
            } catch (org.apache.hugegraph.loader.exception.LoadException failure) {
                primary = failure;
            }
            Assert.assertTrue(Runtime.getRuntime().removeShutdownHook(hook));
            Runtime.getRuntime().addShutdownHook(hook);
            LoadTask restored = LoadTask.builder().id(9).options(options).status(LoadStatus.FAILED)
                                        .lastDuration(0L).build();
            Mockito.when(mapper.selectById(9)).thenReturn(restored);
            try (MockedConstruction<HugeGraphLoader> fresh = Mockito.mockConstruction(HugeGraphLoader.class)) {
                org.apache.hugegraph.testutil.Assert.assertThrows(
                        RuntimeException.class, () -> service.retry(9, "session-token"));
                org.apache.hugegraph.testutil.Assert.assertThrows(
                        RuntimeException.class, () -> service.resume(9, "session-token"));
                Assert.assertTrue(fresh.constructed().isEmpty());
                Assert.assertSame(task, owners.get(9));
                release.countDown();
                Assert.assertTrue(stopped.await(5, TimeUnit.SECONDS));
                try {
                    loader.shutdown();
                    Assert.fail("original timeout must remain visible");
                } catch (org.apache.hugegraph.loader.exception.LoadException failure) {
                    Assert.assertSame(primary, failure);
                }
                Assert.assertTrue(pool.isTerminated());
                Assert.assertTrue(loader.context().closed());
                Assert.assertFalse(Runtime.getRuntime().removeShutdownHook(hook));
                set(service, "taskExecutor", Mockito.mock(LoadTaskExecutor.class));
                service.retry(9, "session-token");
                Assert.assertEquals(1, fresh.constructed().size());
                Assert.assertSame(restored, owners.get(9));
                Assert.assertEquals(LoadStatus.RUNNING, restored.getStatus());
                fresh.constructed().get(0).shutdown();
            }
        } finally {
            release.countDown();
            if (loader != null) {
                try {
                    loader.shutdown();
                } catch (org.apache.hugegraph.loader.exception.LoadException ignored) {
                    // Preserve the original timeout diagnosis after closing resources.
                }
            }
            if (hook != null) {
                Runtime.getRuntime().removeShutdownHook(hook);
            }
            server.stop(0);
        }
    }

    @Test
    public void testUnscheduledResumeKeepsCheckpointAndFileReaderOffset() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/versions", exchange -> {
            byte[] body = "{\"versions\":{\"core\":\"1.8.0\",\"api\":\"0.74\"}}"
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            java.io.File input = this.folder.newFile("checkpoint.csv");
            java.nio.file.Files.writeString(input.toPath(), "prefix0\nprefix1\nsuffix2\n");
            org.apache.hugegraph.loader.source.file.FileSource source =
                    new org.apache.hugegraph.loader.source.file.FileSource();
            source.path(input.getAbsolutePath());
            source.header(new String[]{"id"});
            source.hasHeader(false);
            org.apache.hugegraph.loader.mapping.VertexMapping vertex =
                    new org.apache.hugegraph.loader.mapping.VertexMapping("id", true);
            vertex.label("record");
            org.apache.hugegraph.loader.mapping.InputStruct struct =
                    new org.apache.hugegraph.loader.mapping.InputStruct(
                            Collections.singletonList(vertex), Collections.emptyList());
            struct.id("1");
            struct.input(source);
            LoadOptions options = new LoadOptions();
            options.host = "127.0.0.1";
            options.port = server.getAddress().getPort();
            options.file = this.folder.newFile("checkpoint.json").getAbsolutePath();
            options.incrementalMode = true;
            org.apache.hugegraph.loader.util.MappingUtil.write(
                    new org.apache.hugegraph.loader.mapping.LoadMapping(Collections.singletonList(struct)),
                    options.file);
            org.apache.hugegraph.loader.progress.LoadProgress checkpoint =
                    new org.apache.hugegraph.loader.progress.LoadProgress();
            org.apache.hugegraph.loader.progress.InputItemProgress item =
                    new org.apache.hugegraph.loader.progress.FileItemProgress(input.getName(), input.lastModified(),
                            String.valueOf(org.apache.commons.io.FileUtils.checksumCRC32(input)), 2L);
            item.offset(2L);
            item.confirmOffset();
            checkpoint.addStruct(struct).addLoadingItem(input.getName(), item);
            checkpoint.plusVertexLoaded(2L);
            java.nio.file.Path saved = java.nio.file.Paths.get(
                    org.apache.hugegraph.loader.progress.LoadProgress.format(options, "20000101-000000"));
            java.nio.file.Files.createDirectories(saved.getParent());
            String original = org.apache.hugegraph.loader.util.JsonUtil.toJson(checkpoint);
            java.nio.file.Files.writeString(saved, original);
            LoadTask restored = LoadTask.builder().id(9).options(options).status(LoadStatus.PAUSED).build();
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
            Mockito.when(mapper.selectById(9)).thenReturn(restored);
            RuntimeException failure = new RuntimeException("database update rejected");
            Mockito.when(mapper.update(Mockito.eq(restored), Mockito.any())).thenThrow(failure);
            set(service, "mapper", mapper);
            try {
                service.resume(9, "session-token");
                Assert.fail("database update must fail before dispatch");
            } catch (RuntimeException actual) {
                Assert.assertSame(failure, actual);
            }
            Assert.assertTrue(restored.context().closed());
            LoadContext next = new LoadContext(options);
            try {
                org.apache.hugegraph.loader.reader.file.LocalFileReader scanner =
                        new org.apache.hugegraph.loader.reader.file.LocalFileReader(source);
                try (org.apache.hugegraph.loader.reader.InputReader reader = scanner.split().get(0)) {
                    reader.init(next, struct);
                    String first = reader.next().rawLine();
                    Assert.assertEquals("suffix2", first);
                    Assert.assertEquals(2L, next.oldProgress().totalInputRead());
                    Assert.assertEquals(2L, next.oldProgress().vertexLoaded());
                    Assert.assertEquals(original, java.nio.file.Files.readString(saved));
                    try (java.util.stream.Stream<java.nio.file.Path> paths =
                                 java.nio.file.Files.list(saved.getParent())) {
                        Assert.assertEquals(1L, paths.count());
                    }
                }
            } finally {
                next.close();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testProgressPollRetriesTerminalCallbackFailure() throws Exception {
        this.assertTerminalPersistenceRecovery(false);
    }

    @Test
    public void testProgressPollRetriesStopPersistenceFailure() throws Exception {
        this.assertTerminalPersistenceRecovery(true);
    }

    private void assertTerminalPersistenceRecovery(boolean stop) throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        if (stop) {
            task.setStatus(LoadStatus.PAUSED);
        }
        Mockito.when(loader.load()).thenReturn(true);
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        RuntimeException callbackFailure = new RuntimeException("callback database unavailable");
        enablePollTransactions(service);
        RuntimeException stopFailure = new RuntimeException("stop database unavailable");
        if (stop) {
            Mockito.when(mapper.updateById(task)).thenThrow(callbackFailure).thenThrow(stopFailure).thenReturn(1);
        } else {
            Mockito.when(mapper.updateById(task)).thenThrow(callbackFailure).thenReturn(1);
        }
        set(service, "mapper", mapper);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        CountDownLatch release = new CountDownLatch(1);
        Thread[] runner = new Thread[1];
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        Mockito.doAnswer(invocation -> {
            Runnable callback = invocation.getArgument(1);
            runner[0] = new Thread(() -> {
                try {
                    Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
                    new LoadTaskExecutor().execute(task, callback);
                } catch (Throwable failure) {
                    Assert.assertSame(callbackFailure, failure);
                }
            });
            runner[0].start();
            return null;
        }).when(executor).execute(Mockito.eq(task), Mockito.any());
        set(service, "taskExecutor", executor);
        dispatch(service, task);
        release.countDown();
        runner[0].join(5000);
        Assert.assertFalse(runner[0].isAlive());
        Assert.assertTrue(task.isFinished());
        Assert.assertSame(task, owners.get(9));
        if (stop) {
            try {
                service.stop(9);
                Assert.fail("stop must preserve its database failure");
            } catch (RuntimeException failure) {
                Assert.assertSame(stopFailure, failure);
            }
        }
        service.updateLoadTaskProgress();
        Mockito.verify(mapper, Mockito.times(stop ? 3 : 2)).updateById(task);
        Assert.assertTrue(owners.isEmpty());
        Assert.assertEquals(stop ? LoadStatus.STOPPED : LoadStatus.SUCCEED, task.getStatus());
    }

    @Test
    public void testDatabaseOnlyPauseDoesNotRemoveResumedOwner() throws Exception {
        this.assertDatabaseOnlyTransitionKeepsNewOwner(true);
    }

    @Test
    public void testDatabaseOnlyStopDoesNotRemoveRetriedOwner() throws Exception {
        this.assertDatabaseOnlyTransitionKeepsNewOwner(false);
    }

    private void assertDatabaseOnlyTransitionKeepsNewOwner(boolean pause) throws Exception {
        LoadTask old = LoadTask.builder().id(9).options(new LoadOptions())
                               .status(pause ? LoadStatus.RUNNING : LoadStatus.PAUSED).build();
        LoadTask fresh = LoadTask.builder().id(9).options(new LoadOptions()).lastDuration(0L)
                                 .status(pause ? LoadStatus.PAUSED : LoadStatus.STOPPED).build();
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.selectById(9)).thenReturn(old, fresh);
        CountDownLatch saved = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<LoadStatus> row =
                new java.util.concurrent.atomic.AtomicReference<>();
        org.mockito.stubbing.Answer<Integer> saveOld = invocation -> {
            row.set(old.getStatus());
            saved.countDown();
            Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
            return 1;
        };
        Mockito.when(mapper.updateById(old)).thenAnswer(saveOld);
        Mockito.when(mapper.update(Mockito.eq(old), Mockito.any())).thenAnswer(saveOld);
        org.mockito.stubbing.Answer<Integer> saveFresh = invocation -> {
            row.set(fresh.getStatus());
            return 1;
        };
        Mockito.when(mapper.updateById(fresh)).thenAnswer(saveFresh);
        Mockito.when(mapper.update(Mockito.eq(fresh), Mockito.any())).thenAnswer(saveFresh);
        set(service, "mapper", mapper);
        set(service, "taskExecutor", Mockito.mock(LoadTaskExecutor.class));
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread stopping = new Thread(() -> {
            try {
                if (pause) {
                    service.pause(9);
                } else {
                    service.stop(9);
                }
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(HugeGraphLoader.class)) {
            try {
                stopping.start();
                Assert.assertTrue(saved.await(5, TimeUnit.SECONDS));
                if (pause) {
                    peer(service).resume(9, "session-token");
                } else {
                    peer(service).retry(9, "session-token");
                }
                Assert.assertSame(fresh, owners.get(9));
                release.countDown();
                stopping.join(5000);
                Assert.assertFalse(stopping.isAlive());
                Assert.assertNull(failure.get());
                Assert.assertSame(fresh, owners.get(9));
                Assert.assertEquals(LoadStatus.RUNNING, row.get());
            } finally {
                release.countDown();
                stopping.join(5000);
                if (!construction.constructed().isEmpty()) {
                    construction.constructed().get(0).shutdown();
                }
            }
        }
    }

    @Test
    public void testFinalProgressPollDoesNotSaveReplacedOwner() throws Exception {
        HugeGraphLoader loader = this.loader();
        LoadTask old = Mockito.spy(this.task(loader));
        old.setId(9);
        Mockito.when(loader.load()).thenReturn(false);
        old.run();
        LoadTask fresh = LoadTask.builder().id(9).options(new LoadOptions()).lastDuration(0L)
                                 .status(LoadStatus.FAILED).build();
        LoadTaskService service = new LoadTaskService();
        enablePollTransactions(service);
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.selectById(9)).thenReturn(fresh);
        Mockito.when(mapper.updateById(fresh)).thenReturn(1);
        set(service, "mapper", mapper);
        set(service, "taskExecutor", Mockito.mock(LoadTaskExecutor.class));
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        owners.put(9, old);
        set(service, "runningTaskContainer", owners);
        CountDownLatch selected = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("old-owner-poll")) {
                selected.countDown();
                Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
            }
            return invocation.callRealMethod();
        }).when(old).lock();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread poll = new Thread(() -> {
            try {
                service.updateLoadTaskProgress();
            } catch (Throwable error) {
                failure.set(error);
            }
        }, "old-owner-poll");
        try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(HugeGraphLoader.class)) {
            try {
                poll.start();
                Assert.assertTrue(selected.await(5, TimeUnit.SECONDS));
                service.retry(9, "session-token");
                release.countDown();
                poll.join(5000);
                Assert.assertFalse(poll.isAlive());
                Assert.assertNull(failure.get());
                Mockito.verify(mapper, Mockito.never()).updateById(old);
                Mockito.verify(mapper).update(Mockito.eq(fresh), Mockito.any());
                Assert.assertSame(fresh, owners.get(9));
            } finally {
                release.countDown();
                poll.join(5000);
                if (!construction.constructed().isEmpty()) {
                    construction.constructed().get(0).shutdown();
                }
            }
        }
    }

    @Test(timeout = 15000)
    public void testStopRoutesToOwnerPublishedDuringDatabaseRead() throws Exception {
        this.assertOwnerPublishedDuringDatabaseRead(false);
    }

    @Test(timeout = 15000)
    public void testPauseRoutesToOwnerPublishedDuringDatabaseRead() throws Exception {
        this.assertOwnerPublishedDuringDatabaseRead(true);
    }

    private void assertOwnerPublishedDuringDatabaseRead(boolean pause) throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:owner-routing-" + java.util.UUID.randomUUID());
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        CountDownLatch selecting = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);
        CountDownLatch releaseRun = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean stopped = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean closed =
                                    new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<LoadTask> returned =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<LoadTask> snapshot =
                new java.util.concurrent.atomic.AtomicReference<>();
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        Thread[] runner = new Thread[1];
        try (java.sql.Connection keeper = dataSource.getConnection()) {
            jdbc.execute("CREATE TABLE routing (id INT PRIMARY KEY, load_status TINYINT NOT NULL)");
            jdbc.update("INSERT INTO routing VALUES (9, ?)", LoadStatus.PAUSED.getValue());
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.selectById(9)).thenAnswer(invocation -> {
                boolean operation = Thread.currentThread().getName().equals("routing-operation");
                if (operation) {
                    selecting.countDown();
                    Assert.assertTrue(releaseRead.await(5, TimeUnit.SECONDS));
                }
                byte code = jdbc.queryForObject("SELECT load_status FROM routing WHERE id=9", Byte.class);
                LoadStatus status = java.util.Arrays.stream(LoadStatus.values())
                        .filter(value -> value.getValue() == code).findFirst().get();
                LoadTask task = LoadTask.builder().id(9).options(new LoadOptions())
                                        .lastDuration(0L).status(status).build();
                if (operation) {
                    snapshot.set(task);
                }
                return task;
            });
            Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                return jdbc.update("UPDATE routing SET load_status=? WHERE id=?",
                                   task.getStatus().getValue(), task.getId());
            });
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<LoadTask> condition =
                        invocation.getArgument(1);
                condition.getSqlSegment();
                Byte expected = (Byte) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Byte).findFirst().get();
                return jdbc.update("UPDATE routing SET load_status=? WHERE id=? AND load_status=?",
                                   task.getStatus().getValue(), task.getId(), expected);
            });
            LoadTaskService service = new LoadTaskService();
            set(service, "mapper", mapper);
            set(service, "runningTaskContainer", owners);
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            Mockito.doAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                Runnable callback = invocation.getArgument(1);
                runner[0] = new Thread(() -> {
                    try {
                        Assert.assertTrue(releaseRun.await(5, TimeUnit.SECONDS));
                        new LoadTaskExecutor().execute(task, callback);
                    } catch (Throwable error) {
                        failure.compareAndSet(null, error);
                    }
                });
                runner[0].start();
                return null;
            }).when(executor).execute(Mockito.any(), Mockito.any());
            set(service, "taskExecutor", executor);
            Thread operation = new Thread(() -> {
                try {
                    returned.set(pause ? service.pause(9) : service.stop(9));
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            }, "routing-operation");
            try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                    HugeGraphLoader.class, (loader, ignored) -> {
                        LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
                        Mockito.when(loader.context()).thenReturn(context);
                        Mockito.when(context.closed()).thenAnswer(invocation -> closed.get());
                        Mockito.when(context.stopped()).thenAnswer(invocation -> stopped.get());
                        Mockito.doAnswer(invocation -> {
                            stopped.set(true);
                            return null;
                        }).when(context).stopLoading();
                        Mockito.doAnswer(invocation -> {
                            closed.set(true);
                            return null;
                        }).when(loader).shutdown();
                        Mockito.when(loader.load()).thenAnswer(invocation -> {
                            if (!stopped.get()) {
                                reads.incrementAndGet();
                            }
                            return true;
                        });
                    })) {
                try {
                    operation.start();
                    Assert.assertTrue(selecting.await(5, TimeUnit.SECONDS));
                    LoadTask owner = peer(service).resume(9, "session-token");
                    releaseRead.countDown();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                    while (!stopped.get() && System.nanoTime() < deadline) {
                        Thread.sleep(5);
                    }
                    releaseRun.countDown();
                    runner[0].join(5000);
                    operation.join(5000);
                    Assert.assertFalse(runner[0].isAlive());
                    Assert.assertFalse(operation.isAlive());
                    Assert.assertNull(failure.get());
                    Assert.assertNotSame(snapshot.get(), owner);
                    Assert.assertSame(owner, returned.get());
                    Assert.assertTrue(stopped.get());
                    Assert.assertTrue(closed.get());
                    Assert.assertEquals(0, reads.get());
                    LoadStatus expected = pause ? LoadStatus.PAUSED : LoadStatus.STOPPED;
                    Assert.assertEquals(expected, owner.getStatus());
                    Assert.assertEquals(expected.getValue(), jdbc.queryForObject(
                            "SELECT load_status FROM routing WHERE id=9", Byte.class));
                    Assert.assertTrue(owners.isEmpty());
                } finally {
                    releaseRead.countDown();
                    releaseRun.countDown();
                    if (runner[0] != null) {
                        runner[0].join(5000);
                    }
                    operation.join(5000);
                    for (HugeGraphLoader loader : construction.constructed()) {
                        loader.shutdown();
                    }
                }
            }
        }
    }

    @Test(timeout = 15000)
    public void testConditionalResumeRejectsCommittedDatabaseOnlyStop() throws Exception {
        this.assertConditionalTransitionWinner(true);
    }

    @Test(timeout = 15000)
    public void testConditionalDatabaseOnlyStopRejectsCommittedResume() throws Exception {
        this.assertConditionalTransitionWinner(false);
    }

    @Test(timeout = 15000)
    public void testConditionalDatabaseOnlyPauseRejectsCommittedStop() throws Exception {
        this.assertConditionalTransitionWinner(true, true);
    }

    private void assertConditionalTransitionWinner(boolean stopWins) throws Exception {
        this.assertConditionalTransitionWinner(stopWins, false);
    }

    private void assertConditionalTransitionWinner(boolean stopWins, boolean pauseLoser) throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:state-winner-" + java.util.UUID.randomUUID());
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        CountDownLatch selected = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<LoadTask> snapshot =
                new java.util.concurrent.atomic.AtomicReference<>();
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        try (java.sql.Connection keeper = dataSource.getConnection()) {
            jdbc.execute("CREATE TABLE state_transition (id INT PRIMARY KEY, load_status TINYINT NOT NULL)");
            jdbc.update("INSERT INTO state_transition VALUES (9, ?)",
                        (pauseLoser ? LoadStatus.RUNNING : LoadStatus.PAUSED).getValue());
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.selectById(9)).thenAnswer(invocation -> {
                byte state = jdbc.queryForObject("SELECT load_status FROM state_transition WHERE id=9", Byte.class);
                LoadStatus status = java.util.Arrays.stream(LoadStatus.values())
                        .filter(value -> value.getValue() == state).findFirst().get();
                LoadTask task = LoadTask.builder().id(9).options(new LoadOptions())
                                        .status(status).lastDuration(0L).build();
                if (Thread.currentThread().getName().equals("delayed-transition")) {
                    snapshot.set(task);
                    selected.countDown();
                    Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
                }
                return task;
            });
            Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                return jdbc.update("UPDATE state_transition SET load_status=? WHERE id=?",
                                   task.getStatus().getValue(), task.getId());
            });
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<LoadTask> condition =
                        invocation.getArgument(1);
                Assert.assertTrue(condition.getSqlSegment().contains("id ="));
                Assert.assertTrue(condition.getSqlSegment().contains("load_status ="));
                Byte expected = (Byte) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Byte).findFirst().get();
                Assert.assertTrue(condition.getParamNameValuePairs().containsValue(9));
                return jdbc.update("UPDATE state_transition SET load_status=? WHERE id=? AND load_status=?",
                                   task.getStatus().getValue(), task.getId(), expected);
            });
            LoadTaskService service = new LoadTaskService();
            set(service, "mapper", mapper);
            set(service, "taskExecutor", executor);
            set(service, "runningTaskContainer", owners);
            Thread delayed = new Thread(() -> {
                try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                        HugeGraphLoader.class, (loader, ignored) -> {
                            LoadContext context = Mockito.mock(LoadContext.class);
                            Mockito.when(loader.context()).thenReturn(context);
                            Mockito.when(context.closed()).thenReturn(true);
                        })) {
                    if (pauseLoser) {
                        service.pause(9);
                    } else if (stopWins) {
                        service.resume(9, "session-token");
                    } else {
                        service.stop(9);
                    }
                } catch (Throwable error) {
                    failure.set(error);
                }
            }, "delayed-transition");
            try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                    HugeGraphLoader.class, (loader, ignored) -> {
                        LoadContext context = Mockito.mock(LoadContext.class);
                        Mockito.when(loader.context()).thenReturn(context);
                        Mockito.when(context.closed()).thenReturn(true);
                    })) {
                try {
                    delayed.start();
                    Assert.assertTrue(selected.await(5, TimeUnit.SECONDS));
                    LoadTaskService competing = peer(service);
                    LoadTask winner = stopWins ? competing.stop(9) : competing.resume(9, "session-token");
                    LoadStatus expected = stopWins ? LoadStatus.STOPPED : LoadStatus.RUNNING;
                    Assert.assertEquals(expected, winner.getStatus());
                    release.countDown();
                    delayed.join(5000);
                    Assert.assertFalse(delayed.isAlive());
                    Assert.assertTrue(failure.get() instanceof org.apache.hugegraph.exception.ExternalException);
                    Assert.assertTrue(failure.get().getMessage().contains("state has changed"));
                    Assert.assertEquals(expected.getValue(), jdbc.queryForObject(
                            "SELECT load_status FROM state_transition WHERE id=9", Byte.class));
                    Assert.assertNotSame(winner, snapshot.get());
                    if (stopWins) {
                        if (!pauseLoser) {
                            Assert.assertEquals(LoadStatus.FAILED, snapshot.get().getStatus());
                            Assert.assertTrue(snapshot.get().isFinished());
                            Assert.assertTrue(snapshot.get().context().closed());
                        }
                        Assert.assertTrue(owners.isEmpty());
                        Mockito.verifyNoInteractions(executor);
                    } else {
                        Assert.assertSame(winner, owners.get(9));
                        Mockito.verify(executor).execute(Mockito.eq(winner), Mockito.any());
                    }
                } finally {
                    release.countDown();
                    delayed.join(5000);
                    for (HugeGraphLoader loader : construction.constructed()) {
                        loader.shutdown();
                    }
                }
            }
        }
    }

    @Test
    public void testUnscheduledCleanupFailureRetainsOwnerAndRejectsResume() throws Exception {
        LoadTask task = LoadTask.builder().id(9).options(new LoadOptions()).status(LoadStatus.PAUSED).build();
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.selectById(9)).thenReturn(task);
        RuntimeException primary = new RuntimeException("save rejected");
        RuntimeException cleanup = new RuntimeException("resources still open");
        Mockito.when(mapper.update(Mockito.eq(task), Mockito.any())).thenThrow(primary);
        set(service, "mapper", mapper);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                HugeGraphLoader.class, (loader, ignored) -> {
                    LoadContext context = Mockito.mock(LoadContext.class);
                    Mockito.when(loader.context()).thenReturn(context);
                    Mockito.when(context.closed()).thenReturn(false);
                    Mockito.doThrow(cleanup).when(loader).shutdown();
                })) {
            try {
                service.resume(9, "session-token");
                Assert.fail("save must fail");
            } catch (RuntimeException actual) {
                Assert.assertSame(primary, actual);
            }
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertTrue(task.isFinished());
            Assert.assertFalse(task.context().closed());
            Assert.assertSame(task, owners.get(9));
            Assert.assertArrayEquals(new Throwable[]{cleanup}, primary.getSuppressed());
            try {
                service.resume(9, "session-token");
                Assert.fail("the old Loader must close before a new one is constructed");
            } catch (RuntimeException expected) {
                Assert.assertTrue(expected.getMessage().contains("previous Loader has not closed"));
            }
            Assert.assertEquals(1, construction.constructed().size());
            Assert.assertSame(task, owners.get(9));
        }
    }

    @Test(timeout = 15000)
    public void testCapturedStopFinishesWhenResumeSaveFailsBeforeDispatch() throws Exception {
        LoadTask restored = LoadTask.builder().id(9).options(new LoadOptions())
                                    .status(LoadStatus.PAUSED).lastDuration(0L).build();
        CountDownLatch updating = new CountDownLatch(1);
        CountDownLatch selected = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<Integer, LoadTask>() {
            @Override
            public LoadTask get(Object id) {
                LoadTask owner = super.get(id);
                if (owner != null && Thread.currentThread().getName().equals("unscheduled-stop")) {
                    selected.countDown();
                }
                return owner;
            }
        };
        RuntimeException primary = new RuntimeException("resume update rejected");
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.selectById(9)).thenReturn(restored);
        Mockito.when(mapper.update(Mockito.eq(restored), Mockito.any())).thenAnswer(invocation -> {
            updating.countDown();
            Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
            throw primary;
        });
        LoadTaskService service = new LoadTaskService();
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        set(service, "mapper", mapper);
        set(service, "taskExecutor", executor);
        set(service, "runningTaskContainer", owners);
        java.util.concurrent.atomic.AtomicReference<Throwable> resumeFailure =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> stopFailure =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread resume = new Thread(() -> {
            try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                    HugeGraphLoader.class, (loader, ignored) -> {
                        LoadContext context = Mockito.mock(LoadContext.class);
                        Mockito.when(loader.context()).thenReturn(context);
                        Mockito.when(context.closed()).thenReturn(true);
                    })) {
                service.resume(9, "session-token");
            } catch (Throwable failure) {
                resumeFailure.set(failure);
            }
        }, "unscheduled-resume");
        Thread stop = new Thread(() -> {
            try {
                service.stop(9);
            } catch (Throwable failure) {
                stopFailure.set(failure);
            }
        }, "unscheduled-stop");
        stop.setDaemon(true);
        try {
            resume.start();
            Assert.assertTrue(updating.await(5, TimeUnit.SECONDS));
            stop.start();
            Assert.assertFalse("Stop waits for admission before selecting the unscheduled owner",
                               selected.await(300, TimeUnit.MILLISECONDS));
            Assert.assertSame(restored, owners.get(9));
            release.countDown();
            resume.join(5000);
            stop.join(1500);
            Assert.assertFalse(resume.isAlive());
            Assert.assertFalse("No unscheduled runner remains to finish this stop waiter", stop.isAlive());
            Assert.assertSame(primary, resumeFailure.get());
            Assert.assertNotNull("FAILED owners cannot accept a stop request", stopFailure.get());
            Assert.assertEquals(LoadStatus.FAILED, restored.getStatus());
            Assert.assertTrue(restored.isFinished());
            Assert.assertTrue(restored.context().closed());
            Assert.assertFalse(owners.containsKey(9));
            Mockito.verifyNoInteractions(executor);
        } finally {
            release.countDown();
            restored.setFinished(true);
            resume.join(5000);
            stop.join(5000);
        }
    }

    @Test
    public void testPublishedRestartIsLockedUntilInitialized() throws Exception {
        LoadTask restored = LoadTask.builder().id(9).options(new LoadOptions())
                                    .status(LoadStatus.PAUSED).lastDuration(0L).build();
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
        Mockito.when(mapper.selectById(9)).thenReturn(restored);
        Mockito.when(mapper.updateById(restored)).thenReturn(1);
        set(service, "mapper", mapper);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        set(service, "taskExecutor", executor);
        java.util.concurrent.atomic.AtomicBoolean locked = new java.util.concurrent.atomic.AtomicBoolean();
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<Integer, LoadTask>() {
            @Override
            public LoadTask putIfAbsent(Integer id, LoadTask task) {
                LoadTask previous = super.putIfAbsent(id, task);
                if (previous == null) {
                    Thread observer = new Thread(() -> {
                        try {
                            Field field = LoadTask.class.getDeclaredField("lock");
                            field.setAccessible(true);
                            java.util.concurrent.locks.ReentrantLock lock =
                                    (java.util.concurrent.locks.ReentrantLock) field.get(task);
                            boolean acquired = lock.tryLock();
                            if (acquired) {
                                lock.unlock();
                            }
                            locked.set(!acquired);
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                    });
                    observer.start();
                    try {
                        observer.join(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    Assert.assertFalse(observer.isAlive());
                }
                return previous;
            }
        };
        set(service, "runningTaskContainer", owners);
        try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(HugeGraphLoader.class)) {
            service.resume(9, "session-token");
            Assert.assertTrue("The published owner must already hold its task lock", locked.get());
            Assert.assertSame(restored, owners.get(9));
            Assert.assertSame(construction.constructed().get(0), restored.getLoader());
            Assert.assertEquals(LoadStatus.RUNNING, restored.getStatus());
            Mockito.verify(executor).execute(Mockito.eq(restored), Mockito.any());
        }
    }

    @Test
    public void testClosedCallbackKeepsEqualButDistinctOwner() throws Exception {
        LoadTask old = this.task(this.loader());
        old.setId(9);
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.updateById(old)).thenReturn(1);
        set(service, "mapper", mapper);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        java.util.concurrent.atomic.AtomicReference<Runnable> callback =
                new java.util.concurrent.atomic.AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            callback.set(invocation.getArgument(1));
            return null;
        }).when(executor).execute(Mockito.eq(old), Mockito.any());
        set(service, "taskExecutor", executor);
        dispatch(service, old);
        old.run();
        LoadTask replacement = this.equalRuntimeReplacement(old);
        owners.put(9, replacement);
        callback.get().run();
        Assert.assertEquals(old, replacement);
        Assert.assertSame(replacement, owners.get(9));
        Mockito.verify(mapper).updateById(old);
    }

    @Test
    public void testNoTransactionRemovalKeepsEqualButDistinctOwner() throws Exception {
        LoadTask old = this.task(this.loader());
        old.setId(9);
        old.run();
        LoadTask replacement = this.equalRuntimeReplacement(old);
        LoadTaskService service = new LoadTaskService();
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        owners.put(9, replacement);
        set(service, "runningTaskContainer", owners);
        Assert.assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        Method remove = LoadTaskService.class.getDeclaredMethod("removeOwnerAfterCommit", LoadTask.class);
        remove.setAccessible(true);
        remove.invoke(service, old);
        Assert.assertSame(replacement, owners.get(9));
    }

    @Test
    public void testCommittedPollKeepsEqualButDistinctOwner() throws Exception {
        this.assertPollCommitBoundary(false, true);
    }

    private LoadTask equalRuntimeReplacement(LoadTask previous) {
        HugeGraphLoader loader = this.loader();
        Mockito.when(loader.context().closed()).thenReturn(false);
        LoadTask replacement = LoadTask.builder().id(previous.getId()).options(previous.getOptions())
                .loader(loader).finished(false).status(previous.getStatus()).fileReadLines(previous.getFileReadLines())
                .lastDuration(previous.getLastDuration()).currDuration(previous.getCurrDuration()).build();
        Assert.assertEquals(previous, replacement);
        Assert.assertNotSame(previous, replacement);
        Assert.assertNotSame(previous.getLoader(), replacement.getLoader());
        Assert.assertTrue(previous.isFinished());
        Assert.assertFalse(replacement.isFinished());
        return replacement;
    }

    @Test
    public void testPollTransactionRollbackRetainsBothFinishedOwners() throws Exception {
        this.assertPollCommitBoundary(false);
    }

    @Test
    public void testPollCommitFailureRetainsBothFinishedOwners() throws Exception {
        this.assertPollCommitBoundary(true);
    }

    private void assertPollCommitBoundary(boolean commitFailure) throws Exception {
        this.assertPollCommitBoundary(commitFailure, false);
    }

    private void assertPollCommitBoundary(boolean commitFailure, boolean replaceBeforeCommit) throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:cleanup-poll-" + java.util.UUID.randomUUID());
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        try (java.sql.Connection keeper = dataSource.getConnection()) {
            jdbc.execute("CREATE TABLE task_progress (id INT PRIMARY KEY, load_status VARCHAR(16))");
            jdbc.update("INSERT INTO task_progress VALUES (1, 'RUNNING'), (2, 'RUNNING')");
            LoadTask first = this.task(this.loader());
            LoadTask second = this.task(this.loader());
            first.setId(1);
            second.setId(2);
            first.run();
            second.run();
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenReturn(1);
            java.util.concurrent.atomic.AtomicBoolean fail = new java.util.concurrent.atomic.AtomicBoolean(true);
            RuntimeException failure = new RuntimeException("later owner SQL failed");
            Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                if (!commitFailure && task == second && fail.get()) {
                    throw failure;
                }
                return jdbc.update("UPDATE task_progress SET load_status=? WHERE id=?",
                                   task.getStatus().name(), task.getId());
            });
            Map<Integer, LoadTask> owners = new java.util.concurrent.ConcurrentSkipListMap<>();
            owners.put(1, first);
            owners.put(2, second);
            set(service, "mapper", mapper);
            set(service, "runningTaskContainer", owners);
            org.springframework.jdbc.datasource.DataSourceTransactionManager manager =
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource) {
                        @Override
                        protected void doCommit(
                                org.springframework.transaction.support.DefaultTransactionStatus status) {
                            if (commitFailure && fail.get()) {
                                throw failure;
                            }
                            super.doCommit(status);
                        }
                    };
            manager.setRollbackOnCommitFailure(true);
            set(service, "transactionManager", manager);
            org.springframework.transaction.support.TransactionTemplate transaction =
                    new org.springframework.transaction.support.TransactionTemplate(manager);
            try {
                transaction.execute(status -> {
                    service.updateLoadTaskProgress();
                    if (!commitFailure) {
                        status.setRollbackOnly();
                    }
                    return null;
                });
                if (commitFailure) {
                    Assert.fail("commit must fail");
                }
            } catch (RuntimeException actual) {
                Assert.assertSame(failure, actual);
            }
            Assert.assertEquals("RUNNING", jdbc.queryForObject(
                    "SELECT load_status FROM task_progress WHERE id=1", String.class));
            Assert.assertEquals("RUNNING", jdbc.queryForObject(
                    "SELECT load_status FROM task_progress WHERE id=2", String.class));
            Assert.assertSame(first, owners.get(1));
            Assert.assertSame(second, owners.get(2));
            fail.set(false);
            LoadTask replacement = replaceBeforeCommit ? this.equalRuntimeReplacement(first) : null;
            transaction.execute(status -> {
                service.updateLoadTaskProgress();
                Assert.assertSame(first, owners.get(1));
                Assert.assertSame(second, owners.get(2));
                if (replaceBeforeCommit) {
                    owners.put(1, replacement);
                }
                return null;
            });
            if (replaceBeforeCommit) {
                Assert.assertSame(replacement, owners.get(1));
                Assert.assertNull(owners.get(2));
            } else {
                Assert.assertTrue(owners.isEmpty());
            }
            Assert.assertEquals("FAILED", jdbc.queryForObject(
                    "SELECT load_status FROM task_progress WHERE id=1", String.class));
            Assert.assertEquals("FAILED", jdbc.queryForObject(
                    "SELECT load_status FROM task_progress WHERE id=2", String.class));
        }
    }

    @Test(timeout = 15000)
    public void testTaskAdmissionPreventsPauseResumeStatusAba() throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:admission-" + java.util.UUID.randomUUID());
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(ds);
        CountDownLatch saving = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch competitors = new CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Throwable> stopFailure =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> pauseFailure =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> resumeFailure =
                new java.util.concurrent.atomic.AtomicReference<>();
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "mapper", mapper);
        set(service, "taskExecutor", executor);
        set(service, "runningTaskContainer", owners);
        try (java.sql.Connection keeper = ds.getConnection()) {
            jdbc.execute("CREATE TABLE admission (id INT PRIMARY KEY, load_status TINYINT NOT NULL)");
            jdbc.update("INSERT INTO admission VALUES (9, ?)", LoadStatus.RUNNING.getValue());
            Mockito.when(mapper.selectById(9)).thenAnswer(invocation -> {
                reads.incrementAndGet();
                byte code = jdbc.queryForObject("SELECT load_status FROM admission WHERE id=9", Byte.class);
                LoadStatus status = java.util.Arrays.stream(LoadStatus.values())
                        .filter(value -> value.getValue() == code).findFirst().get();
                return LoadTask.builder().id(9).options(new LoadOptions()).status(status).build();
            });
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<LoadTask> condition =
                        invocation.getArgument(1);
                condition.getSqlSegment();
                Byte expected = (Byte) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Byte).findFirst().get();
                saving.countDown();
                Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
                return jdbc.update("UPDATE admission SET load_status=? WHERE id=? AND load_status=?",
                                   task.getStatus().getValue(), task.getId(), expected);
            });
            Thread stop = new Thread(() -> {
                try {
                    service.stop(9);
                } catch (Throwable error) {
                    stopFailure.set(error);
                }
            });
            Thread pause = new Thread(() -> {
                competitors.countDown();
                try {
                    service.pause(9);
                } catch (Throwable error) {
                    pauseFailure.set(error);
                }
            });
            Thread resume = new Thread(() -> {
                competitors.countDown();
                try {
                    service.resume(9, "session-token");
                } catch (Throwable error) {
                    resumeFailure.set(error);
                }
            });
            try {
                stop.start();
                Assert.assertTrue(saving.await(5, TimeUnit.SECONDS));
                pause.start();
                resume.start();
                Assert.assertTrue(competitors.await(5, TimeUnit.SECONDS));
                Thread.sleep(100);
                Assert.assertTrue(pause.isAlive());
                Assert.assertTrue(resume.isAlive());
                Assert.assertEquals("Later controls cannot read an ABA snapshot", 1, reads.get());
                Assert.assertEquals(LoadStatus.RUNNING.getValue(), jdbc.queryForObject(
                        "SELECT load_status FROM admission WHERE id=9", Byte.class));
                Mockito.verifyNoInteractions(executor);
            } finally {
                release.countDown();
                stop.join(5000);
                pause.join(5000);
                resume.join(5000);
            }
            Assert.assertFalse(stop.isAlive());
            Assert.assertFalse(pause.isAlive());
            Assert.assertFalse(resume.isAlive());
            Assert.assertNull(stopFailure.get());
            Assert.assertTrue(pauseFailure.get() instanceof org.apache.hugegraph.exception.ExternalException);
            Assert.assertTrue(resumeFailure.get() instanceof org.apache.hugegraph.exception.ExternalException);
            Assert.assertEquals(LoadStatus.STOPPED.getValue(), jdbc.queryForObject(
                    "SELECT load_status FROM admission WHERE id=9", Byte.class));
            Assert.assertTrue(owners.isEmpty());
            Mockito.verifyNoInteractions(executor);
        }
    }

    @Test(timeout = 15000)
    public void testInitialPublicationWaitsForTaskAdmissionAfterCommit() throws Exception {
        LoadTaskService service = new LoadTaskService();
        HugeGraphLoader loader = this.loader();
        LoadTask task = this.task(loader);
        task.setId(9);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        set(service, "taskExecutor", executor);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "runningTaskContainer", owners);
        Field field = LoadTaskService.class.getDeclaredField("taskControls");
        field.setAccessible(true);
        com.google.common.util.concurrent.Striped<java.util.concurrent.locks.Lock> controls =
                (com.google.common.util.concurrent.Striped<java.util.concurrent.locks.Lock>) field.get(service);
        java.util.concurrent.locks.Lock control = controls.get(9);
        CountDownLatch committed = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        Thread publication = new Thread(() -> {
            TransactionSynchronizationManager.initSynchronization();
            try {
                dispatch(service, task);
                TransactionSynchronization synchronization =
                        TransactionSynchronizationManager.getSynchronizations().get(0);
                committed.countDown();
                synchronization.afterCommit();
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        });
        control.lock();
        try {
            publication.start();
            Assert.assertTrue(committed.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            Assert.assertTrue(publication.isAlive());
            Assert.assertTrue(owners.isEmpty());
            Mockito.verifyNoInteractions(executor);
            int independentId = 10;
            while (controls.get(independentId) == control) {
                independentId++;
            }
            LoadTask independent = this.task(this.loader());
            independent.setId(independentId);
            dispatch(service, independent);
            Assert.assertSame(independent, owners.get(independentId));
            Mockito.verify(executor).execute(Mockito.eq(independent), Mockito.any());
        } finally {
            control.unlock();
            publication.join(5000);
            loader.shutdown();
        }
        Assert.assertFalse(publication.isAlive());
        Assert.assertNull(failure.get());
        Assert.assertSame(task, owners.get(9));
        Mockito.verify(executor).execute(Mockito.eq(task), Mockito.any());
    }

    @Test(timeout = 15000)
    public void testInitialPublicationHonorsCommittedStop() throws Exception {
        this.assertInitialPublicationHonorsTerminalRequest(false, false);
    }

    @Test(timeout = 15000)
    public void testInitialPublicationHonorsCommittedPause() throws Exception {
        this.assertInitialPublicationHonorsTerminalRequest(true, false);
    }

    @Test(timeout = 15000)
    public void testCancelledInitialPublicationRetainsUnclosedOwner() throws Exception {
        this.assertInitialPublicationHonorsTerminalRequest(false, true);
    }

    @Test(timeout = 15000)
    public void testCancelledInitialPublicationRetainsStillOpenContext() throws Exception {
        this.assertInitialPublicationHonorsTerminalRequest(false, true, false);
    }

    private void assertInitialPublicationHonorsTerminalRequest(boolean pause, boolean cleanupFailure) throws Exception {
        this.assertInitialPublicationHonorsTerminalRequest(pause, cleanupFailure, true);
    }

    private void assertInitialPublicationHonorsTerminalRequest(boolean pause, boolean cleanupFailure,
                                                              boolean throwsOnCleanup) throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:late-publication-" + java.util.UUID.randomUUID());
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(ds);
        HugeGraphLoader loader = this.loader();
        Mockito.when(loader.context().closed()).thenReturn(!cleanupFailure);
        if (cleanupFailure && throwsOnCleanup) {
            Mockito.doThrow(new RuntimeException("worker cleanup failed")).when(loader).shutdown();
        }
        LoadTask initial = this.task(loader);
        initial.setId(9);
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        set(service, "mapper", mapper);
        set(service, "taskExecutor", executor);
        set(service, "runningTaskContainer", owners);
        CountDownLatch registered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (java.sql.Connection keeper = ds.getConnection()) {
            jdbc.execute("CREATE TABLE initial_task (id INT PRIMARY KEY, load_status TINYINT NOT NULL)");
            jdbc.update("INSERT INTO initial_task VALUES (9, ?)", LoadStatus.RUNNING.getValue());
            Mockito.when(mapper.selectById(9)).thenAnswer(invocation -> {
                byte code = jdbc.queryForObject("SELECT load_status FROM initial_task WHERE id=9", Byte.class);
                LoadStatus status = java.util.Arrays.stream(LoadStatus.values())
                        .filter(value -> value.getValue() == code).findFirst().get();
                return LoadTask.builder().id(9).options(new LoadOptions()).status(status).build();
            });
            Mockito.when(mapper.update(Mockito.any(LoadTask.class), Mockito.any())).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<LoadTask> condition =
                        invocation.getArgument(1);
                condition.getSqlSegment();
                Byte expected = (Byte) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Byte).findFirst().get();
                return jdbc.update("UPDATE initial_task SET load_status=? WHERE id=? AND load_status=?",
                                   task.getStatus().getValue(), task.getId(), expected);
            });
            Thread publication = new Thread(() -> {
                TransactionSynchronizationManager.initSynchronization();
                try {
                    dispatch(service, initial);
                    TransactionSynchronization synchronization =
                            TransactionSynchronizationManager.getSynchronizations().get(0);
                    registered.countDown();
                    Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
                    synchronization.afterCommit();
                    synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    TransactionSynchronizationManager.clearSynchronization();
                }
            });
            try {
                publication.start();
                Assert.assertTrue(registered.await(5, TimeUnit.SECONDS));
                LoadTask terminal = pause ? service.pause(9) : service.stop(9);
                Assert.assertEquals(pause ? LoadStatus.PAUSED : LoadStatus.STOPPED, terminal.getStatus());
                Assert.assertNotSame(initial, terminal);
            } finally {
                release.countDown();
                publication.join(5000);
            }
            Assert.assertFalse(publication.isAlive());
            Assert.assertNull(failure.get());
            LoadStatus expected = pause ? LoadStatus.PAUSED : LoadStatus.STOPPED;
            Assert.assertEquals(expected.getValue(), jdbc.queryForObject(
                    "SELECT load_status FROM initial_task WHERE id=9", Byte.class));
            Assert.assertEquals(cleanupFailure ? LoadStatus.FAILED : expected, initial.getStatus());
            Assert.assertTrue(initial.isFinished());
            Mockito.verify(loader).shutdown();
            Mockito.verify(loader, Mockito.never()).load();
            Mockito.verifyNoInteractions(executor);
            if (cleanupFailure) {
                Assert.assertSame(initial, owners.get(9));
                Assert.assertFalse(initial.context().closed());
            } else {
                Assert.assertTrue(initial.context().closed());
                Assert.assertTrue(owners.isEmpty());
            }
        }
    }

    @Test
    public void testStaleInitialPublicationDoesNotDispatchMissingOrTerminalRow() throws Exception {
        for (LoadStatus status : new LoadStatus[]{null, LoadStatus.FAILED, LoadStatus.SUCCEED, LoadStatus.INIT}) {
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.selectById(9)).thenReturn(status == null ? null :
                    LoadTask.builder().id(9).status(status).build());
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            HugeGraphLoader loader = this.loader();
            LoadTask old = this.task(loader);
            old.setId(9);
            set(service, "mapper", mapper);
            set(service, "taskExecutor", executor);
            dispatch(service, old);
            Assert.assertTrue(old.isFinished());
            Assert.assertTrue(loader.context().closed());
            Mockito.verify(loader, Mockito.never()).load();
            Mockito.verify(mapper, Mockito.never()).updateById(Mockito.any(LoadTask.class));
            Mockito.verify(mapper, Mockito.never()).update(Mockito.any(), Mockito.any());
            Mockito.verifyNoInteractions(executor);
        }
    }

    @Test
    public void testStaleInitialPublicationClosesOnlyItsOwnLoader() throws Exception {
        LoadTaskService service = new LoadTaskService();
        HugeGraphLoader oldLoader = this.loader();
        HugeGraphLoader newLoader = this.loader();
        LoadTask old = this.task(oldLoader);
        old.setId(9);
        LoadTask fresh = LoadTask.builder().id(9).options(old.getOptions()).lastDuration(0L)
                                 .status(LoadStatus.RUNNING).loader(newLoader).build();
        Assert.assertEquals(old, fresh);
        Assert.assertNotSame(old, fresh);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        owners.put(9, fresh);
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.selectById(9)).thenReturn(fresh);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        set(service, "mapper", mapper);
        set(service, "taskExecutor", executor);
        set(service, "runningTaskContainer", owners);
        dispatch(service, old);
        Assert.assertSame(fresh, owners.get(9));
        Assert.assertEquals(LoadStatus.RUNNING, fresh.getStatus());
        Assert.assertFalse(fresh.isFinished());
        Assert.assertFalse(newLoader.context().closed());
        Assert.assertTrue(old.isFinished());
        Assert.assertTrue(oldLoader.context().closed());
        Mockito.verify(newLoader, Mockito.never()).shutdown();
        Mockito.verifyNoInteractions(executor);
        Mockito.verify(mapper, Mockito.never()).update(Mockito.any(), Mockito.any());
        Mockito.verify(mapper, Mockito.never()).updateById(Mockito.any(LoadTask.class));
    }

    @Test
    public void testInitialPublicationRejectsUnusableOwnLoader() throws Exception {
        for (int state = 0; state < 4; state++) {
            LoadTaskService service = new LoadTaskService();
            HugeGraphLoader loader = this.loader();
            LoadTask old = this.task(loader);
            old.setId(9);
            if (state == 0) {
                old.setFinished(true);
            } else if (state == 1) {
                old.setLoader(null);
            } else if (state == 2) {
                Mockito.when(loader.context().closed()).thenReturn(true);
            } else {
                Mockito.when(loader.context().stopped()).thenReturn(true);
            }
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.selectById(9)).thenReturn(LoadTask.builder().id(9).status(LoadStatus.RUNNING).build());
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            set(service, "mapper", mapper);
            set(service, "taskExecutor", executor);
            dispatch(service, old);
            Assert.assertTrue(old.isFinished());
            Mockito.verifyNoInteractions(executor);
            Mockito.verify(mapper, Mockito.never()).update(Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void testDeleteRejectsTerminalSnapshotWithUnclosedOwner() throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.selectById(9)).thenReturn(LoadTask.builder().id(9).status(LoadStatus.PAUSED).build());
        LoadTask owner = this.task(this.loader());
        owner.setId(9);
        Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        owners.put(9, owner);
        set(service, "mapper", mapper);
        set(service, "runningTaskContainer", owners);
        org.apache.hugegraph.testutil.Assert.assertThrows(RuntimeException.class, () -> service.remove(9));
        Assert.assertSame(owner, owners.get(9));
        Mockito.verify(mapper, Mockito.never()).delete(Mockito.any());
    }

    @Test
    public void testGraphDeletionCommitsTaskCleanupOutsideCallerTransaction() throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            fixture.enableGraphDeletion();
            new org.springframework.transaction.support.TransactionTemplate(fixture.manager).execute(status -> {
                fixture.service.removeByGraph("space", "graph");
                Assert.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                try (java.sql.Connection other = fixture.dataSource.getConnection();
                     java.sql.Statement statement = other.createStatement();
                     java.sql.ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM delete_tasks")) {
                    Assert.assertTrue(rows.next());
                    Assert.assertEquals(0, rows.getInt(1));
                } catch (java.sql.SQLException failure) {
                    throw new RuntimeException(failure);
                }
                status.setRollbackOnly();
                return null;
            });
            Assert.assertEquals(Integer.valueOf(0), fixture.jdbc.queryForObject(
                    "SELECT COUNT(*) FROM delete_jobs", Integer.class));
            Mockito.verify(fixture.jobs, Mockito.never()).deleteByMap(Mockito.anyMap());
        }
    }

    @Test
    public void testGraphDeletionKeepsNewJobsAndOtherGraphScopes() throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            fixture.enableGraphDeletion();
            fixture.jdbc.update("INSERT INTO delete_jobs VALUES (8, ?, 'other-space', 'graph'), " +
                                "(9, ?, 'space', 'other-graph')", 0, 0);
            Mockito.doAnswer(invocation -> {
                java.util.List<org.apache.hugegraph.entity.load.JobManager> snapshot =
                        Collections.singletonList(fixture.jobs.selectById(7));
                fixture.jdbc.update("INSERT INTO delete_jobs VALUES (11, ?, 'space', 'graph')", 0);
                fixture.jdbc.update("INSERT INTO delete_tasks VALUES (12, ?, 11)", LoadStatus.RUNNING.getValue());
                return snapshot;
            }).when(fixture.jobs).selectList(Mockito.any());

            fixture.service.removeByGraph("space", "graph");

            Assert.assertEquals(java.util.Arrays.asList(8, 9, 11), fixture.jdbc.queryForList(
                    "SELECT id FROM delete_jobs ORDER BY id", Integer.class));
            Assert.assertEquals(Collections.singletonList(12), fixture.jdbc.queryForList(
                    "SELECT id FROM delete_tasks", Integer.class));
            Assert.assertEquals(LoadStatus.RUNNING.getValue(), fixture.jdbc.queryForObject(
                    "SELECT load_status FROM delete_tasks WHERE id=12", Byte.class));
            Mockito.verify(fixture.jobs, Mockito.never()).deleteByMap(Mockito.anyMap());
        }
    }

    @Test
    public void testGraphDeletionFailureRetainsFailedJobAndCommittedStops() throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            fixture.enableGraphDeletion();
            fixture.jdbc.update("INSERT INTO delete_jobs VALUES (8, ?, 'space', 'graph')", 0);
            fixture.jdbc.update("INSERT INTO delete_tasks VALUES (11, ?, 8)", LoadStatus.RUNNING.getValue());
            RuntimeException refusal = new IllegalStateException("metadata deletion rejected");
            Mockito.doThrow(refusal).when(fixture.jobs).deleteById(8);

            org.apache.hugegraph.testutil.Assert.assertThrows(IllegalStateException.class,
                    () -> fixture.service.removeByGraph("space", "graph"));

            Assert.assertEquals(Collections.singletonList(8), fixture.jdbc.queryForList(
                    "SELECT id FROM delete_jobs", Integer.class));
            Assert.assertEquals(Collections.singletonList(11), fixture.jdbc.queryForList(
                    "SELECT id FROM delete_tasks", Integer.class));
            Assert.assertEquals(LoadStatus.STOPPED.getValue(), fixture.jdbc.queryForObject(
                    "SELECT load_status FROM delete_tasks WHERE id=11", Byte.class));
        }
    }

    @Test
    public void testJobDeletionCommitsStopsBeforeCallerTransactionCompletes() throws Exception {
        for (boolean scoped : new boolean[]{false, true}) {
            try (JobDeleteFixture fixture = new JobDeleteFixture()) {
                new org.springframework.transaction.support.TransactionTemplate(fixture.manager).execute(status -> {
                    fixture.delete(scoped);
                    Assert.assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                    // Independent connections must see committed deletion before the caller commits.
                    try (java.sql.Connection other = fixture.dataSource.getConnection();
                         java.sql.Statement statement = other.createStatement();
                         java.sql.ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM delete_tasks")) {
                        Assert.assertTrue(rows.next());
                        Assert.assertEquals(0, rows.getInt(1));
                    } catch (java.sql.SQLException failure) {
                        throw new RuntimeException(failure);
                    }
                    status.setRollbackOnly();
                    return null;
                });
                Assert.assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM delete_tasks", Integer.class)
                                                  .intValue());
                Assert.assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM delete_jobs", Integer.class)
                                                  .intValue());
            }
        }
    }

    @Test
    public void testMetadataRollbackKeepsCommittedStopsAndAllTaskRows() throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            RuntimeException primary = new RuntimeException("job metadata delete rejected");
            Mockito.doThrow(primary).when(fixture.jobs).deleteById(7);
            try {
                fixture.delete(true);
                Assert.fail("metadata must roll back");
            } catch (RuntimeException actual) {
                Assert.assertSame(primary, actual);
            }
            Assert.assertEquals(2, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM delete_tasks", Integer.class)
                                              .intValue());
            Assert.assertEquals(2, fixture.jdbc.queryForObject(
                    "SELECT COUNT(*) FROM delete_tasks WHERE load_status=?", Integer.class,
                    LoadStatus.STOPPED.getValue()).intValue());
            Assert.assertEquals(1, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM delete_jobs", Integer.class)
                                              .intValue());
        }
    }

    @Test
    public void testRetryWinningBeforeMetadataDeleteRollsBackEarlierTaskDeletion() throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            LoadTask fresh = this.task(this.loader());
            fresh.setId(9);
            Mockito.doAnswer(invocation -> {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LoadTask> condition =
                        invocation.getArgument(0);
                condition.getSqlSegment();
                Integer id = (Integer) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Integer).findFirst().get();
                Byte expected = (Byte) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Byte).findFirst().get();
                int deleted = fixture.jdbc.update("DELETE FROM delete_tasks WHERE id=? AND load_status=?",
                                                  id, expected);
                if (id == 10) {
                    // A separately committed restart wins before the next task is admitted for deletion.
                    try (java.sql.Connection other = fixture.dataSource.getConnection();
                         java.sql.PreparedStatement update = other.prepareStatement(
                                 "UPDATE delete_tasks SET load_status=? WHERE id=9 AND load_status=?")) {
                        update.setByte(1, LoadStatus.RUNNING.getValue());
                        update.setByte(2, LoadStatus.STOPPED.getValue());
                        Assert.assertEquals(1, update.executeUpdate());
                    }
                    fixture.owners.put(9, fresh);
                }
                return deleted;
            }).when(fixture.tasks).delete(Mockito.any());
            org.apache.hugegraph.testutil.Assert.assertThrows(RuntimeException.class, () -> fixture.delete(false));
            Assert.assertSame(fresh, fixture.owners.get(9));
            Assert.assertFalse(fresh.context().closed());
            Assert.assertEquals(LoadStatus.RUNNING.getValue(), fixture.jdbc.queryForObject(
                    "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
            Assert.assertEquals(LoadStatus.STOPPED.getValue(), fixture.jdbc.queryForObject(
                    "SELECT load_status FROM delete_tasks WHERE id=10", Byte.class));
            Assert.assertEquals(1, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM delete_jobs", Integer.class)
                                              .intValue());
        }
    }

    @Test
    public void testDispatchCommitFailureKeepsClosedOwnerForConditionalRecovery() throws Exception {
        for (LoadStatus advanced : new LoadStatus[]{LoadStatus.RUNNING, LoadStatus.SUCCEED, null}) {
            try (JobDeleteFixture fixture = new JobDeleteFixture()) {
                LoadTask task = this.task(this.loader());
                task.setId(9);
                task.setJobId(7);
                LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
                Mockito.doThrow(new java.util.concurrent.RejectedExecutionException("queue full"))
                       .when(executor).execute(Mockito.any(), Mockito.any());
                org.springframework.jdbc.datasource.DataSourceTransactionManager rejecting =
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(fixture.dataSource) {
                            @Override
                            protected void doCommit(
                                    org.springframework.transaction.support.DefaultTransactionStatus status) {
                                throw new org.springframework.transaction.TransactionSystemException("commit failed");
                            }
                        };
                rejecting.setRollbackOnCommitFailure(true);
                set(fixture.load, "taskExecutor", executor);
                set(fixture.load, "transactionManager", rejecting);
                set(fixture.load, "jobManagerMapper", fixture.jobs);

                dispatch(fixture.load, task);

                Assert.assertSame(task, fixture.owners.get(9));
                Assert.assertTrue(task.getLoader().context().closed());
                Assert.assertEquals(LoadStatus.RUNNING.getValue(), fixture.jdbc.queryForObject(
                        "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
                if (advanced == null) {
                    fixture.jdbc.update("DELETE FROM delete_tasks WHERE id=9");
                } else {
                    fixture.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=9", advanced.getValue());
                }
                set(fixture.load, "transactionManager", fixture.manager);
                new org.springframework.transaction.support.TransactionTemplate(fixture.manager).execute(status -> {
                    fixture.load.updateLoadTaskProgress();
                    Assert.assertSame("retire only after recovery commits", task, fixture.owners.get(9));
                    return null;
                });
                Assert.assertFalse(fixture.owners.containsKey(9));
                Assert.assertEquals(advanced == null ? Collections.emptyList() : Collections.singletonList(
                        advanced == LoadStatus.RUNNING ? LoadStatus.FAILED.getValue() : advanced.getValue()),
                        fixture.jdbc.queryForList("SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
                Assert.assertEquals(advanced == LoadStatus.RUNNING ?
                                    org.apache.hugegraph.entity.enums.JobStatus.FAILED.getValue() :
                                    org.apache.hugegraph.entity.enums.JobStatus.LOADING.getValue(),
                                    fixture.jdbc.queryForObject("SELECT status FROM delete_jobs WHERE id=7",
                                                               Byte.class));
            }
        }
    }

    @Test
    public void testPollSavepointRollbackFailureAbortsOuterTransaction() throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        "jdbc:h2:mem:poll-deadlock-" + java.util.UUID.randomUUID() + ";LOCK_TIMEOUT=3000", "sa", "");
        org.springframework.jdbc.core.JdbcTemplate jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        org.springframework.jdbc.datasource.DataSourceTransactionManager manager =
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
        try (java.sql.Connection keeper = dataSource.getConnection()) {
            jdbc.execute("CREATE TABLE tasks (id INT PRIMARY KEY, load_status TINYINT, n INT)");
            jdbc.update("INSERT INTO tasks VALUES (9, ?, 0), (10, ?, 0), (11, ?, 0), (12, ?, 0)",
                        LoadStatus.RUNNING.getValue(), LoadStatus.RUNNING.getValue(),
                        LoadStatus.RUNNING.getValue(), LoadStatus.RUNNING.getValue());
            CountDownLatch otherLocked = new CountDownLatch(1);
            CountDownLatch pollAtSecond = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Throwable> otherFailure =
                    new java.util.concurrent.atomic.AtomicReference<>();
            Thread other = new Thread(() -> {
                try {
                    new org.springframework.transaction.support.TransactionTemplate(manager).execute(status -> {
                        jdbc.update("UPDATE tasks SET n=1 WHERE id=10");
                        jdbc.update("UPDATE tasks SET n=1 WHERE id=11");
                        otherLocked.countDown();
                        try {
                            Assert.assertTrue(pollAtSecond.await(10, TimeUnit.SECONDS));
                            Thread.sleep(150);
                        } catch (InterruptedException failure) {
                            throw new RuntimeException(failure);
                        }
                        jdbc.update("UPDATE tasks SET n=1 WHERE id=9");
                        return null;
                    });
                } catch (Throwable failure) {
                    otherFailure.set(failure);
                }
            }, "poll-deadlock-competitor");
            Map<Integer, LoadTask> owners = new java.util.concurrent.ConcurrentSkipListMap<>();
            for (int id : new int[]{9, 10, 12}) {
                owners.put(id, LoadTask.builder().id(id).finished(true).initialStateReadFailed(true)
                                       .status(LoadStatus.FAILED).build());
            }
            LoadTask first = owners.get(9);
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(mapper.update(Mockito.nullable(LoadTask.class), Mockito.any())).thenAnswer(invocation -> {
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<?> query = invocation.getArgument(1);
                query.getSqlSegment();
                int id = (Integer) query.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Integer).findFirst().get();
                if (id == 10) {
                    pollAtSecond.countDown();
                }
                return jdbc.update("UPDATE tasks SET load_status=? WHERE id=? AND load_status=?",
                                   LoadStatus.FAILED.getValue(), id, LoadStatus.RUNNING.getValue());
            });
            LoadTaskService service = new LoadTaskService();
            set(service, "mapper", mapper);
            set(service, "transactionManager", manager);
            set(service, "runningTaskContainer", owners);
            org.springframework.aop.framework.ProxyFactory factory =
                    new org.springframework.aop.framework.ProxyFactory(service);
            factory.setProxyTargetClass(true);
            factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                    manager, new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
            Throwable pollFailure = null;
            other.start();
            try {
                Assert.assertTrue(otherLocked.await(5, TimeUnit.SECONDS));
                try {
                    ((LoadTaskService) factory.getProxy()).updateLoadTaskProgress();
                } catch (Throwable failure) {
                    pollFailure = failure;
                }
            } finally {
                pollAtSecond.countDown();
                other.join(5000);
            }
            Assert.assertFalse(other.isAlive());
            Assert.assertNull(otherFailure.get());
            Assert.assertTrue(pollFailure instanceof org.springframework.transaction.TransactionSystemException);
            Assert.assertTrue(pollFailure.getCause() instanceof java.sql.SQLException);
            Assert.assertEquals(90063, ((java.sql.SQLException) pollFailure.getCause()).getErrorCode());
            Assert.assertEquals(LoadStatus.RUNNING.getValue(), jdbc.queryForObject(
                    "SELECT load_status FROM tasks WHERE id=9", Byte.class));
            Assert.assertEquals(LoadStatus.RUNNING.getValue(), jdbc.queryForObject(
                    "SELECT load_status FROM tasks WHERE id=12", Byte.class));
            Assert.assertSame(first, owners.get(9));
            Assert.assertEquals(3, owners.size());
            Mockito.verify(mapper, Mockito.times(2)).update(Mockito.nullable(LoadTask.class), Mockito.any());
        }
    }

    @Test
    public void testPollMissingFinalRowDoesNotBlockOtherOwners() throws Exception {
        this.assertPollFailureIsolation(true);
    }

    @Test
    public void testPollRollsBackFailedOwnerWriteAndContinuesOtherOwners() throws Exception {
        this.assertPollFailureIsolation(false);
    }

    @Test
    public void testPollConstraintFailureContinuesOtherOwners() throws Exception {
        this.assertPollFailureIsolation(false, true);
    }

    private void assertPollFailureIsolation(boolean missing) throws Exception {
        this.assertPollFailureIsolation(missing, false);
    }

    private void assertPollFailureIsolation(boolean missing, boolean constraint) throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            LoadTask first = this.task(this.loader());
            first.setId(9);
            first.run();
            LoadTask second = this.task(this.loader());
            second.setId(10);
            second.run();
            Map<Integer, LoadTask> owners = new java.util.concurrent.ConcurrentSkipListMap<>();
            owners.put(9, first);
            owners.put(10, second);
            set(fixture.load, "runningTaskContainer", owners);
            set(fixture.load, "transactionManager", fixture.manager);
            if (missing) {
                fixture.jdbc.update("DELETE FROM delete_tasks WHERE id=9");
            }
            if (constraint) {
                fixture.jdbc.execute("ALTER TABLE delete_tasks ADD CONSTRAINT reject_first_failed CHECK " +
                                     "(id <> 9 OR load_status <> " + LoadStatus.FAILED.getValue() + ")");
            }
            Mockito.when(fixture.tasks.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                int count = fixture.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=?",
                                                task.getStatus().getValue(), task.getId());
                if (!missing && !constraint && task == first) {
                    throw new IllegalStateException("failed after partial SQL");
                }
                return count;
            });

            new org.springframework.transaction.support.TransactionTemplate(fixture.manager).execute(status -> {
                fixture.load.updateLoadTaskProgress();
                Assert.assertSame(second, owners.get(10));
                return null;
            });

            Assert.assertFalse(owners.containsKey(10));
            Assert.assertEquals(missing ? null : first, owners.get(9));
            Assert.assertEquals(LoadStatus.FAILED.getValue(), fixture.jdbc.queryForObject(
                    "SELECT load_status FROM delete_tasks WHERE id=10", Byte.class));
            if (!missing) {
                Assert.assertEquals(LoadStatus.RUNNING.getValue(), fixture.jdbc.queryForObject(
                        "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
            }
        }
    }

    @Test
    public void testOwnDispatchRejectionConditionallyMarksTaskAndJobFailed() throws Exception {
        this.assertOwnDispatchFailureState(false);
    }

    @Test
    public void testOwnDispatchRejectionDoesNotOverwriteAdvancedTaskState() throws Exception {
        this.assertOwnDispatchFailureState(true);
    }

    private void assertOwnDispatchFailureState(boolean advanced) throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            HugeGraphLoader loader = this.loader();
            LoadTask task = this.task(loader);
            task.setId(9);
            task.setJobId(7);
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            Mockito.doAnswer(invocation -> {
                if (advanced) {
                    fixture.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=9",
                                        LoadStatus.SUCCEED.getValue());
                }
                throw new java.util.concurrent.RejectedExecutionException("own dispatch rejected");
            }).when(executor).execute(Mockito.eq(task), Mockito.any());
            set(fixture.load, "taskExecutor", executor);
            set(fixture.load, "transactionManager", fixture.manager);
            set(fixture.load, "jobManagerMapper", fixture.jobs);
            Method method = LoadTaskService.class.getDeclaredMethod("executeAfterCommit", LoadTask.class);
            method.setAccessible(true);
            method.invoke(fixture.load, task);
            Assert.assertEquals(advanced ? LoadStatus.SUCCEED.getValue() : LoadStatus.FAILED.getValue(),
                                fixture.jdbc.queryForObject(
                                        "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
            Assert.assertEquals(advanced ? org.apache.hugegraph.entity.enums.JobStatus.LOADING.getValue() :
                                          org.apache.hugegraph.entity.enums.JobStatus.FAILED.getValue(),
                                fixture.jdbc.queryForObject("SELECT status FROM delete_jobs WHERE id=7", Byte.class));
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertTrue(task.isFinished());
            Assert.assertTrue(loader.context().closed());
            Assert.assertFalse(fixture.owners.containsKey(9));
            Mockito.verify(loader, Mockito.never()).load();
        }
    }

    @Test
    public void testRecoveryReasonsAreRuntimeOnly() throws Exception {
        LoadTask task = LoadTask.builder().status(LoadStatus.FAILED).lastDuration(0L).currDuration(0L)
                                .fileReadLines(0L).fileTotalLines(0L).build();
        task.setInitialStateReadFailed(true);
        task.setDispatchFailurePending(true);
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode value = json.valueToTree(task);
        Assert.assertFalse(value.has("initialStateReadFailed"));
        Assert.assertFalse(value.has("initial_state_read_failed"));
        Assert.assertFalse(value.has("dispatchFailurePending"));
        Assert.assertFalse(value.has("dispatch_failure_pending"));
        LoadTask restored = json.readValue(
                "{\"initialStateReadFailed\":true,\"dispatchFailurePending\":true}", LoadTask.class);
        Assert.assertFalse(restored.isInitialStateReadFailed());
        Assert.assertFalse(restored.isDispatchFailurePending());
        Field field = LoadTask.class.getDeclaredField("initialStateReadFailed");
        Assert.assertFalse(field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class).exist());
        com.baomidou.mybatisplus.core.metadata.TableInfo table =
                com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                        new org.apache.ibatis.builder.MapperBuilderAssistant(
                                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "cleanup-test"),
                        LoadTask.class);
        Assert.assertFalse(table.getFieldList().stream()
                                .anyMatch(item -> item.getProperty().equals("initialStateReadFailed")));
        Assert.assertFalse(table.getFieldList().stream()
                                .anyMatch(item -> item.getProperty().equals("dispatchFailurePending")));
    }

    @Test
    public void testInitialReadFailurePollPersistsFailedAndRetiresOwner() throws Exception {
        this.assertInitialReadRecovery(LoadStatus.RUNNING, false, false);
    }

    @Test
    public void testInitialReadFailurePollPreservesSucceededTask() throws Exception {
        this.assertInitialReadRecovery(LoadStatus.SUCCEED, false, false);
    }

    @Test
    public void testInitialReadFailurePollDoesNotRecreateDeletedRow() throws Exception {
        this.assertInitialReadRecovery(null, false, false);
    }

    @Test
    public void testInitialReadFailurePollRetainsOwnerAfterSqlFailure() throws Exception {
        this.assertInitialReadRecovery(LoadStatus.RUNNING, true, false);
    }

    @Test
    public void testInitialReadFailurePollRetainsOwnerAfterCommitFailure() throws Exception {
        this.assertInitialReadRecovery(LoadStatus.RUNNING, false, true);
    }

    @Test
    public void testDifferentInitialOwnerCancelsBeforeDatabaseRead() throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            HugeGraphLoader oldLoader = this.loader();
            LoadTask old = this.task(oldLoader);
            old.setId(9);
            HugeGraphLoader newLoader = this.loader();
            LoadTask replacement = this.task(newLoader);
            replacement.setId(9);
            fixture.owners.put(9, replacement);
            Mockito.doThrow(new RuntimeException("must not read metadata for a replaced initial owner"))
                   .when(fixture.tasks).selectById(9);

            dispatch(fixture.load, old);

            Assert.assertSame(replacement, fixture.owners.get(9));
            Assert.assertTrue(oldLoader.context().closed());
            Assert.assertFalse(newLoader.context().closed());
            Mockito.verify(fixture.tasks, Mockito.never()).selectById(9);
            Mockito.verify(fixture.tasks, Mockito.never()).update(Mockito.any(), Mockito.any());
            Mockito.verify(newLoader, Mockito.never()).shutdown();
            newLoader.shutdown();
        }
    }

    private void assertInitialReadRecovery(LoadStatus afterRead, boolean sqlFailure,
                                           boolean commitFailure) throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            HugeGraphLoader loader = this.loader();
            LoadTask task = this.task(loader);
            task.setId(9);
            task.setJobId(7);
            java.util.concurrent.atomic.AtomicBoolean first = new java.util.concurrent.atomic.AtomicBoolean(true);
            RuntimeException readFailure = new RuntimeException("initial metadata read temporarily unavailable");
            Mockito.doAnswer(invocation -> {
                if (first.getAndSet(false)) {
                    throw readFailure;
                }
                throw new AssertionError("initial failure recovery must use conditional SQL, not another snapshot");
            }).when(fixture.tasks).selectById(9);

            dispatch(fixture.load, task);

            Assert.assertSame("the closed initial owner must remain recoverable", task, fixture.owners.get(9));
            Assert.assertTrue(loader.context().closed());
            Assert.assertTrue(task.isFinished());
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            if (afterRead == null) {
                fixture.jdbc.update("DELETE FROM delete_tasks WHERE id=9");
            } else {
                fixture.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=9", afterRead.getValue());
            }
            RuntimeException writeFailure = new RuntimeException("conditional failure write unavailable");
            java.util.concurrent.atomic.AtomicBoolean rejectSql =
                    new java.util.concurrent.atomic.AtomicBoolean(sqlFailure);
            Mockito.doAnswer(invocation -> {
                Assert.assertNull(invocation.getArgument(0));
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<LoadTask> condition =
                        invocation.getArgument(1);
                String sql = condition.getSqlSegment();
                Assert.assertTrue(sql.contains("id ="));
                Assert.assertTrue(sql.contains("load_status ="));
                Assert.assertTrue(condition.getParamNameValuePairs().containsValue(LoadStatus.RUNNING.getValue()));
                Assert.assertTrue(condition.getSqlSet().contains("load_status="));
                if (rejectSql.getAndSet(false)) {
                    throw writeFailure;
                }
                return fixture.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=9 AND load_status=?",
                                           LoadStatus.FAILED.getValue(), LoadStatus.RUNNING.getValue());
            }).when(fixture.tasks).update(Mockito.nullable(LoadTask.class), Mockito.any());
            org.springframework.jdbc.datasource.DataSourceTransactionManager tx = fixture.manager;
            if (commitFailure) {
                java.util.concurrent.atomic.AtomicBoolean rejectCommit =
                        new java.util.concurrent.atomic.AtomicBoolean(true);
                tx = new org.springframework.jdbc.datasource.DataSourceTransactionManager(fixture.dataSource) {
                    @Override
                    protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {
                        if (rejectCommit.getAndSet(false)) {
                            throw new org.springframework.transaction.TransactionSystemException("commit unavailable");
                        }
                        super.doCommit(status);
                    }
                };
                tx.setRollbackOnCommitFailure(true);
            }
            org.springframework.transaction.support.TransactionTemplate transaction =
                    new org.springframework.transaction.support.TransactionTemplate(tx);
            if (sqlFailure) {
                transaction.execute(status -> {
                    fixture.load.updateLoadTaskProgress();
                    return null;
                });
                Assert.assertSame(task, fixture.owners.get(9));
                Assert.assertEquals(LoadStatus.RUNNING.getValue(), fixture.jdbc.queryForObject(
                        "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
            } else if (commitFailure) {
                try {
                    transaction.execute(status -> {
                        fixture.load.updateLoadTaskProgress();
                        return null;
                    });
                    Assert.fail("the original SQL/commit failure must be propagated");
                } catch (RuntimeException failure) {
                    if (sqlFailure) {
                        Assert.assertSame(writeFailure, failure);
                    } else {
                        Assert.assertTrue(
                                failure instanceof org.springframework.transaction.TransactionSystemException);
                    }
                }
                Assert.assertSame(task, fixture.owners.get(9));
                Assert.assertEquals(LoadStatus.RUNNING.getValue(), fixture.jdbc.queryForObject(
                        "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class));
            }
            transaction.execute(status -> {
                fixture.load.updateLoadTaskProgress();
                Assert.assertSame("owner retirement waits for commit", task, fixture.owners.get(9));
                return null;
            });
            Assert.assertFalse(fixture.owners.containsKey(9));
            java.util.List<Byte> rows = fixture.jdbc.queryForList(
                    "SELECT load_status FROM delete_tasks WHERE id=9", Byte.class);
            Assert.assertEquals(afterRead == null ? Collections.emptyList() :
                                Collections.singletonList(afterRead == LoadStatus.RUNNING ?
                                                          LoadStatus.FAILED.getValue() : afterRead.getValue()), rows);
            Mockito.verify(fixture.tasks, Mockito.never()).updateById(Mockito.any(LoadTask.class));
            Mockito.verify(loader, Mockito.never()).load();
        }
    }

    @Test
    public void testDeleteAdmissionRejectsLaterControllerStart() throws Exception {
        this.assertJobAdmission(true, false);
    }

    @Test
    public void testControllerStartAdmissionMakesDeleteEnumerateNewTask() throws Exception {
        this.assertJobAdmission(false, false);
    }

    @Test
    public void testDeleteAdmissionRejectsLaterControllerCreate() throws Exception {
        this.assertJobAdmission(true, true);
    }

    private void assertJobAdmission(boolean deleteFirst, boolean createOnly) throws Exception {
        try (JobDeleteFixture fixture = new JobDeleteFixture()) {
            fixture.jdbc.update("UPDATE delete_jobs SET status=? WHERE id=7",
                                org.apache.hugegraph.entity.enums.JobStatus.SETTING.getValue());
            Mockito.doAnswer(invocation -> {
                java.util.List<Byte> rows = fixture.jdbc.queryForList(
                        "SELECT status FROM delete_jobs WHERE id=7", Byte.class);
                if (rows.isEmpty()) {
                    return null;
                }
                return org.apache.hugegraph.entity.load.JobManager.builder().id(7)
                        .jobStatus(java.util.Arrays.stream(org.apache.hugegraph.entity.enums.JobStatus.values())
                                .filter(status -> status.getValue().equals(rows.get(0))).findFirst().get()).build();
            }).when(fixture.jobs).selectById(7);
            Mockito.doAnswer(invocation -> fixture.jobs.selectById(7))
                   .when(fixture.jobs).selectOne(Mockito.any());
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<Throwable> startFailure =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicReference<Throwable> deleteFailure =
                    new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicInteger constructions = new java.util.concurrent.atomic.AtomicInteger();
            Mockito.doAnswer(invocation -> {
                java.util.List<LoadTask> snapshot = new java.util.ArrayList<>();
                for (Integer id : fixture.jdbc.queryForList("SELECT id FROM delete_tasks ORDER BY id", Integer.class)) {
                    snapshot.add(fixture.tasks.selectById(id));
                }
                if (deleteFirst) {
                    entered.countDown();
                    Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
                }
                return snapshot;
            }).when(fixture.tasks).selectList(Mockito.any());
            Mockito.when(fixture.tasks.insert(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                task.setId(11);
                if (!deleteFirst) {
                    entered.countDown();
                    Assert.assertTrue(release.await(5, TimeUnit.SECONDS));
                }
                return fixture.jdbc.update("INSERT INTO delete_tasks VALUES (11, ?)", task.getStatus().getValue());
            });
            Mockito.when(fixture.tasks.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                return fixture.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=?",
                                           task.getStatus().getValue(), task.getId());
            });
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            Mockito.doAnswer(invocation -> {
                new LoadTaskExecutor().execute(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(executor).execute(Mockito.any(), Mockito.any());
            set(fixture.load, "taskExecutor", executor);
            FileMapping mapping = new FileMapping();
            mapping.setId(8);
            mapping.setJobId(7);
            mapping.setPath(this.folder.newFile("job-admission.csv").getAbsolutePath());
            mapping.setVertexMappings(Collections.emptySet());
            mapping.setEdgeMappings(Collections.emptySet());
            mapping.setLoadParameter(new LoadParameter());
            FileSetting setting = new FileSetting();
            setting.setColumnNames(Collections.singletonList("id"));
            mapping.setFileSetting(setting);
            org.apache.hugegraph.service.load.FileMappingService files =
                    Mockito.mock(org.apache.hugegraph.service.load.FileMappingService.class);
            Mockito.when(files.get("space", "graph", 7, 8)).thenReturn(mapping);
            org.apache.hugegraph.config.HugeConfig config = Mockito.mock(org.apache.hugegraph.config.HugeConfig.class);
            Mockito.when(config.get(org.apache.hugegraph.options.HubbleOptions.PD_ENABLED)).thenReturn(false);
            Mockito.when(config.get(org.apache.hugegraph.options.HubbleOptions.SERVER_URL))
                   .thenReturn("http://127.0.0.1:19880");
            org.apache.hugegraph.controller.load.LoadTaskController controller =
                    new org.apache.hugegraph.controller.load.LoadTaskController(fixture.load) {
                        @Override
                        protected org.apache.hugegraph.driver.HugeClient requireGraphSpaceWrite(String graphSpace) {
                            return null;
                        }

                        @Override
                        protected org.apache.hugegraph.driver.HugeClient authClient(String graphSpace, String graph) {
                            return null;
                        }

                        @Override
                        protected String getToken() {
                            return "token";
                        }

                        @Override
                        protected String getUser() {
                            return null;
                        }
                    };
            Field jobField = org.apache.hugegraph.controller.load.LoadTaskController.class
                    .getDeclaredField("jobService");
            jobField.setAccessible(true);
            jobField.set(controller, fixture.service);
            Field filesField = org.apache.hugegraph.controller.load.LoadTaskController.class
                    .getDeclaredField("fmService");
            filesField.setAccessible(true);
            filesField.set(controller, files);
            Field configField = org.apache.hugegraph.controller.load.LoadTaskController.class
                    .getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(controller, config);
            Thread start = new Thread(() -> {
                try (MockedConstruction<HugeGraphLoader> loaders = Mockito.mockConstruction(
                        HugeGraphLoader.class, (loader, context) -> {
                            constructions.incrementAndGet();
                            LoadContext resources = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
                            java.util.concurrent.atomic.AtomicBoolean closed =
                                    new java.util.concurrent.atomic.AtomicBoolean();
                            Mockito.when(loader.context()).thenReturn(resources);
                            Mockito.when(resources.closed()).thenAnswer(invocation -> closed.get());
                            Mockito.when(resources.newProgress().totalInputRead()).thenReturn(0L);
                            Mockito.when(resources.summary().totalTime()).thenReturn(1L);
                            Mockito.when(loader.load()).thenReturn(true);
                            Mockito.doAnswer(invocation -> {
                                closed.set(true);
                                return null;
                            }).when(loader).shutdown();
                        })) {
                    if (createOnly) {
                        controller.create("space", "graph", 7, LoadTask.builder().status(LoadStatus.INIT).build());
                    } else {
                        controller.start("space", "graph", 7, Collections.singletonList(8));
                    }
                } catch (Throwable failure) {
                    startFailure.set(failure);
                    failure.printStackTrace();
                }
            });
            Thread delete = new Thread(() -> {
                try {
                    fixture.delete(true);
                } catch (Throwable failure) {
                    deleteFailure.set(failure);
                }
            });
            Thread first = deleteFirst ? delete : start;
            Thread second = deleteFirst ? start : delete;
            first.start();
            Assert.assertTrue(entered.await(5, TimeUnit.SECONDS));
            second.start();
            try {
                second.join(250);
                System.out.println("JOB_ADMISSION deleteFirst=" + deleteFirst + " createOnly=" + createOnly +
                                   " competitorWaiting=" + second.isAlive());
                Assert.assertTrue("same-job competitor must await admission", second.isAlive());
            } finally {
                release.countDown();
                first.join(5000);
                second.join(5000);
            }
            Assert.assertFalse(first.isAlive());
            Assert.assertFalse(second.isAlive());
            Assert.assertNull(deleteFailure.get());
            if (deleteFirst) {
                Assert.assertTrue(startFailure.get() instanceof org.apache.hugegraph.exception.ExternalException);
                Assert.assertEquals(0, constructions.get());
                Mockito.verifyNoInteractions(executor);
            } else {
                Assert.assertNull(startFailure.get());
                Assert.assertEquals(1, constructions.get());
                Mockito.verify(executor).execute(Mockito.any(), Mockito.any());
            }
            Assert.assertEquals(Integer.valueOf(0), fixture.jdbc.queryForObject(
                    "SELECT COUNT(*) FROM delete_tasks", Integer.class));
            Assert.assertEquals(Integer.valueOf(0), fixture.jdbc.queryForObject(
                    "SELECT COUNT(*) FROM delete_jobs", Integer.class));
            Assert.assertTrue(fixture.owners.isEmpty());
        }
    }

    private static class JobDeleteFixture implements AutoCloseable {
        private final org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        private final org.springframework.jdbc.core.JdbcTemplate jdbc;
        private final org.springframework.jdbc.datasource.DataSourceTransactionManager manager;
        private final java.sql.Connection keeper;
        private final LoadTaskMapper tasks = Mockito.mock(LoadTaskMapper.class);
        private final org.apache.hugegraph.mapper.load.JobManagerMapper jobs =
                Mockito.mock(org.apache.hugegraph.mapper.load.JobManagerMapper.class);
        private final Map<Integer, LoadTask> owners = new ConcurrentHashMap<>();
        private final org.apache.hugegraph.service.load.JobManagerService service;
        private final LoadTaskService load = new LoadTaskService();

        private JobDeleteFixture() throws Exception {
            this.dataSource.setDriverClassName("org.h2.Driver");
            this.dataSource.setUrl("jdbc:h2:mem:job-delete-" + java.util.UUID.randomUUID());
            this.keeper = this.dataSource.getConnection();
            this.jdbc = new org.springframework.jdbc.core.JdbcTemplate(this.dataSource);
            this.manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(this.dataSource);
            this.jdbc.execute("CREATE TABLE delete_tasks (id INT PRIMARY KEY, load_status TINYINT NOT NULL)");
            this.jdbc.execute("CREATE TABLE delete_jobs (id INT PRIMARY KEY, status TINYINT NOT NULL)");
            this.jdbc.update("INSERT INTO delete_tasks VALUES (9, ?), (10, ?)",
                             LoadStatus.RUNNING.getValue(), LoadStatus.RUNNING.getValue());
            this.jdbc.update("INSERT INTO delete_jobs VALUES (7, ?)",
                             org.apache.hugegraph.entity.enums.JobStatus.LOADING.getValue());
            Mockito.when(this.tasks.selectById(Mockito.anyInt())).thenAnswer(invocation -> {
                int id = invocation.getArgument(0);
                java.util.List<Byte> rows = this.jdbc.queryForList(
                        "SELECT load_status FROM delete_tasks WHERE id=?", Byte.class, id);
                if (rows.isEmpty()) {
                    return null;
                }
                LoadStatus status = java.util.Arrays.stream(LoadStatus.values())
                        .filter(value -> value.getValue().equals(rows.get(0))).findFirst().get();
                return LoadTask.builder().id(id).jobId(7).options(new LoadOptions()).status(status).build();
            });
            Mockito.when(this.tasks.selectList(Mockito.any())).thenAnswer(invocation ->
                    java.util.Arrays.asList(this.tasks.selectById(10), this.tasks.selectById(9)));
            Mockito.when(this.tasks.update(Mockito.nullable(LoadTask.class), Mockito.any())).thenAnswer(invocation -> {
                LoadTask task = invocation.getArgument(0);
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<LoadTask> condition =
                        invocation.getArgument(1);
                String where = condition.getSqlSegment();
                Byte expected = (Byte) condition.getParamNameValuePairs().entrySet().stream()
                        .filter(entry -> entry.getValue() instanceof Byte && where.contains("." + entry.getKey() + "}"))
                        .map(Map.Entry::getValue).findFirst().get();
                Integer id = (Integer) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Integer).findFirst().get();
                Byte target = task == null ? LoadStatus.FAILED.getValue() : task.getStatus().getValue();
                return this.jdbc.update("UPDATE delete_tasks SET load_status=? WHERE id=? AND load_status=?",
                                        target, id, expected);
            });
            Mockito.when(this.tasks.delete(Mockito.any())).thenAnswer(invocation -> {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LoadTask> condition =
                        invocation.getArgument(0);
                condition.getSqlSegment();
                Integer id = (Integer) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Integer).findFirst().get();
                Byte expected = (Byte) condition.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Byte).findFirst().get();
                return this.jdbc.update("DELETE FROM delete_tasks WHERE id=? AND load_status=?", id, expected);
            });
            org.apache.hugegraph.entity.load.JobManager job =
                    org.apache.hugegraph.entity.load.JobManager.builder().id(7)
                            .jobStatus(org.apache.hugegraph.entity.enums.JobStatus.LOADING).build();
            Mockito.when(this.jobs.selectById(7)).thenReturn(job);
            Mockito.when(this.jobs.selectOne(Mockito.any())).thenReturn(job);
            Mockito.when(this.jobs.deleteById(7)).thenAnswer(invocation ->
                    this.jdbc.update("DELETE FROM delete_jobs WHERE id=7"));
            Mockito.when(this.jobs.updateById(Mockito.any(org.apache.hugegraph.entity.load.JobManager.class)))
                   .thenAnswer(invocation -> {
                       org.apache.hugegraph.entity.load.JobManager updated = invocation.getArgument(0);
                       return this.jdbc.update("UPDATE delete_jobs SET status=? WHERE id=?",
                                               updated.getJobStatus().getValue(), updated.getId());
                   });
            set(this.load, "mapper", this.tasks);
            set(this.load, "runningTaskContainer", this.owners);
            set(this.load, "transactionManager", this.manager);
            org.apache.hugegraph.service.load.JobManagerService target =
                    new org.apache.hugegraph.service.load.JobManagerService();
            set(target, "mapper", this.jobs);
            set(target, "taskService", this.load);
            set(target, "transactionManager", this.manager);
            org.apache.hugegraph.service.load.FileMappingService files =
                    Mockito.mock(org.apache.hugegraph.service.load.FileMappingService.class);
            Mockito.when(files.listByJob(7)).thenReturn(Collections.emptyList());
            set(target, "fileMappingService", files);
            org.springframework.aop.framework.ProxyFactory factory =
                    new org.springframework.aop.framework.ProxyFactory(target);
            factory.setProxyTargetClass(true);
            factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                    this.manager,
                    new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
            this.service = (org.apache.hugegraph.service.load.JobManagerService) factory.getProxy();
        }

        private void delete(boolean scoped) {
            if (scoped) {
                this.service.deleteJob("space", "graph", 7);
            } else {
                this.service.deleteJob(7);
            }
        }

        private void enableGraphDeletion() {
            this.jdbc.execute("ALTER TABLE delete_jobs ADD graphspace VARCHAR(48) DEFAULT 'space'");
            this.jdbc.execute("ALTER TABLE delete_jobs ADD graph_name VARCHAR(48) DEFAULT 'graph'");
            this.jdbc.execute("ALTER TABLE delete_tasks ADD job_id INT DEFAULT 7");
            Mockito.when(this.jobs.selectList(Mockito.any())).thenAnswer(invocation -> {
                Assert.assertFalse("the graph cleanup must suspend the caller transaction",
                                   TransactionSynchronizationManager.isActualTransactionActive());
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?> query = invocation.getArgument(0);
                query.getSqlSegment();
                Assert.assertTrue(query.getSqlSegment().contains("graphspace"));
                Assert.assertTrue(query.getSqlSegment().contains("graph"));
                Assert.assertTrue(query.getParamNameValuePairs().containsValue("space"));
                Assert.assertTrue(query.getParamNameValuePairs().containsValue("graph"));
                java.util.List<org.apache.hugegraph.entity.load.JobManager> result = new java.util.ArrayList<>();
                for (Integer id : this.jdbc.queryForList(
                        "SELECT id FROM delete_jobs WHERE graphspace='space' AND graph_name='graph' ORDER BY id",
                        Integer.class)) {
                    result.add(this.jobs.selectById(id));
                }
                return result;
            });
            Mockito.when(this.jobs.selectById(Mockito.anyInt())).thenAnswer(invocation -> {
                int id = invocation.getArgument(0);
                return this.jdbc.queryForList("SELECT id FROM delete_jobs WHERE id=?", Integer.class, id).isEmpty() ?
                       null : org.apache.hugegraph.entity.load.JobManager.builder().id(id)
                               .graphSpace("space").graph("graph").build();
            });
            Mockito.when(this.jobs.selectOne(Mockito.any())).thenAnswer(invocation -> {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?> query = invocation.getArgument(0);
                query.getSqlSegment();
                int id = (Integer) query.getParamNameValuePairs().values().stream()
                        .filter(value -> value instanceof Integer).findFirst().get();
                return this.jobs.selectById(id);
            });
            Mockito.when(this.jobs.deleteById(Mockito.anyInt())).thenAnswer(invocation ->
                    this.jdbc.update("DELETE FROM delete_jobs WHERE id=?", (Integer) invocation.getArgument(0)));
            Mockito.when(this.jobs.deleteByMap(Mockito.anyMap())).thenAnswer(invocation ->
                    this.jdbc.update("DELETE FROM delete_jobs WHERE graphspace='space' AND graph_name='graph'"));
            Mockito.doAnswer(invocation -> {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?> query = invocation.getArgument(0);
                query.getSqlSegment();
                int jobId = (Integer) query.getParamNameValuePairs().values().stream().findFirst().get();
                java.util.List<LoadTask> result = new java.util.ArrayList<>();
                for (Integer id : this.jdbc.queryForList(
                        "SELECT id FROM delete_tasks WHERE job_id=? ORDER BY id", Integer.class, jobId)) {
                    result.add(this.tasks.selectById(id));
                }
                return result;
            }).when(this.tasks).selectList(Mockito.any());
        }

        @Override
        public void close() throws Exception {
            this.keeper.close();
        }
    }

    // Separate admission instances intentionally keep the defensive old-tail SQL/identity
    // controls concurrent. This is not a cross-Hubble lifecycle coordination guarantee.
    private static LoadTaskService peer(LoadTaskService source) throws Exception {
        LoadTaskService peer = new LoadTaskService();
        for (String name : new String[]{"mapper", "taskExecutor", "runningTaskContainer"}) {
            Field field = LoadTaskService.class.getDeclaredField(name);
            field.setAccessible(true);
            set(peer, name, field.get(source));
        }
        return peer;
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static void enablePollTransactions(LoadTaskService service) throws Exception {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:poll-" + java.util.UUID.randomUUID());
        set(service, "transactionManager",
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
    }

    private static void dispatch(LoadTaskService service, LoadTask task) throws Exception {
        Field mapperField = LoadTaskService.class.getDeclaredField("mapper");
        mapperField.setAccessible(true);
        LoadTaskMapper mapper = (LoadTaskMapper) mapperField.get(service);
        if (mapper == null) {
            mapper = Mockito.mock(LoadTaskMapper.class);
            set(service, "mapper", mapper);
        }
        boolean configured = Mockito.mockingDetails(mapper).getStubbings().stream()
                .anyMatch(stub -> stub.getInvocation().getMethod().getName().equals("selectById") &&
                                  java.util.Objects.equals(stub.getInvocation().getArguments()[0], task.getId()));
        if (!configured) {
            Mockito.when(mapper.selectById(task.getId())).thenReturn(
                    LoadTask.builder().id(task.getId()).status(LoadStatus.RUNNING).build());
        }
        Method method = LoadTaskService.class.getDeclaredMethod("executeAfterCommit", LoadTask.class);
        method.setAccessible(true);
        method.invoke(service, task);
    }

    private HugeGraphLoader loader() {
        HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
        LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(loader.context()).thenReturn(context);
        java.util.concurrent.atomic.AtomicBoolean closed =
                                    new java.util.concurrent.atomic.AtomicBoolean();
        Mockito.when(context.closed()).thenAnswer(invocation -> closed.get());
        Mockito.doAnswer(invocation -> {
            closed.set(true);
            return null;
        }).when(loader).shutdown();
        Mockito.when(context.newProgress().totalInputRead()).thenReturn(7L);
        Mockito.when(context.summary().totalTime()).thenReturn(12L);
        return loader;
    }

    private LoadTask task(HugeGraphLoader loader) {
        return LoadTask.builder().loader(loader).options(new LoadOptions()).status(LoadStatus.RUNNING)
                       .lastDuration(0L).build();
    }
}
