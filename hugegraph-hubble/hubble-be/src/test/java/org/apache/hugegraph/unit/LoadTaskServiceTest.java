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

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.ListFormat;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.util.LoadUtil;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.testutil.NativeCleanupTestSupport;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockedConstruction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.net.httpserver.HttpServer;

public class LoadTaskServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testRunnerClosesBeforePublishingCompletion() {
        for (boolean success : new boolean[]{true, false}) {
            HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
            LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(loader.context()).thenReturn(context);
            Mockito.when(loader.load()).thenReturn(success);
            LoadTask task = LoadTask.builder().id(9).loader(loader).options(new LoadOptions())
                                    .status(LoadStatus.RUNNING).lastDuration(0L).build();
            Mockito.doAnswer(invocation -> {
                Assert.assertFalse(task.isFinished());
                Assert.assertEquals(LoadStatus.RUNNING, task.getStatus());
                return null;
            }).when(loader).shutdown();

            task.run();

            Mockito.verify(loader).shutdown();
            Assert.assertTrue(task.isFinished());
            Assert.assertEquals(success ? LoadStatus.SUCCEED : LoadStatus.FAILED, task.getStatus());
        }
    }

    @Test
    public void testCancelledLoadFailureDoesNotOverwriteRequestedStatus() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/versions", exchange -> {
            byte[] body = "{\"versions\":{\"core\":\"1.8.0\",\"api\":\"0.74\"}}"
                          .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            for (LoadStatus status : new LoadStatus[]{LoadStatus.PAUSED, LoadStatus.STOPPED}) {
                LoadOptions options = new LoadOptions();
                options.host = "127.0.0.1";
                options.port = server.getAddress().getPort();
                options.file = this.folder.newFile("cancel-" + status + ".json").getAbsolutePath();
                HugeGraphLoader loader = Mockito.spy(new HugeGraphLoader(
                        options, new LoadMapping(Collections.emptyList())));
                IllegalStateException failure = new IllegalStateException("load failed");
                LoadTask task = LoadTask.builder().id(9).loader(loader).options(options)
                                        .status(LoadStatus.RUNNING).lastDuration(0L).build();
                Mockito.doAnswer(invocation -> {
                    task.setStatus(status);
                    loader.context().failLoading(failure);
                    throw failure;
                }).when(loader).load();
                Field field = HugeGraphLoader.class.getDeclaredField("shutdownHook");
                field.setAccessible(true);
                Thread hook = (Thread) field.get(loader);
                try {
                    task.run();

                    Assert.assertTrue(loader.context().closed());
                    Assert.assertFalse(Runtime.getRuntime().removeShutdownHook(hook));
                    Assert.assertTrue(task.isFinished());
                    Assert.assertEquals(status, task.getStatus());
                    Assert.assertEquals(0, failure.getSuppressed().length);
                } finally {
                    loader.shutdown();
                    Runtime.getRuntime().removeShutdownHook(hook);
                }
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testCancelledLoadWithIndependentCleanupFailureIsFailed() {
        HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
        LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(loader.context()).thenReturn(context);
        IllegalStateException failure = new IllegalStateException("load failed");
        IllegalStateException cleanup = new IllegalStateException("cleanup failed");
        Mockito.when(loader.load()).thenThrow(failure);
        Mockito.doThrow(cleanup).when(loader).shutdown();
        LoadTask task = LoadTask.builder().id(9).loader(loader).options(new LoadOptions())
                                .status(LoadStatus.STOPPED).lastDuration(0L).build();

        task.run();

        Assert.assertTrue(task.isFinished());
        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
        Assert.assertEquals(1, failure.getSuppressed().length);
        Assert.assertSame(cleanup, failure.getSuppressed()[0]);
    }

    @Test
    public void testCancelledRecordedLoadFailureRethrowKeepsRequestedStatus() {
        for (Throwable original : new Throwable[]{new IllegalStateException("load failed"),
                                                  new Exception("load failed"),
                                                  new AssertionError("load failed"),
                                                  new UndeclaredThrowableException(
                                                  new RuntimeException(new IllegalStateException("wrapped")))}) {
            HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
            LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(loader.context()).thenReturn(context);
            RuntimeException loadFailure = LoadUtil.targetRuntimeException(original);
            RuntimeException recorded = original instanceof RuntimeException ? (RuntimeException) original :
                                        new LoadException("recorded failure", original);
            RuntimeException rethrown = original instanceof RuntimeException ? (RuntimeException) original :
                                        new LoadException("recorded failure", original);
            Mockito.when(loader.load()).thenThrow(loadFailure);
            Mockito.doThrow(recorded).when(context).throwIfFailed();
            Mockito.doThrow(rethrown).when(loader).shutdown();
            LoadTask task = LoadTask.builder().id(9).loader(loader).options(new LoadOptions())
                                    .status(LoadStatus.PAUSED).lastDuration(0L).build();

            task.run();

            Assert.assertEquals(LoadStatus.PAUSED, task.getStatus());
            Assert.assertTrue(task.isFinished());
            Assert.assertEquals(0, loadFailure.getSuppressed().length);
        }
    }

    @Test
    public void testCancelledRethrowWithNewSuppressedCleanupFailureIsFailed() {
        HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
        LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(loader.context()).thenReturn(context);
        IllegalStateException failure = new IllegalStateException("load failed");
        IllegalStateException cleanup = new IllegalStateException("close failed");
        Mockito.when(loader.load()).thenThrow(failure);
        Mockito.doThrow(failure).when(context).throwIfFailed();
        Mockito.doAnswer(invocation -> {
            failure.addSuppressed(cleanup);
            throw failure;
        }).when(loader).shutdown();
        LoadTask task = LoadTask.builder().id(9).loader(loader).options(new LoadOptions())
                                .status(LoadStatus.STOPPED).lastDuration(0L).build();

        task.run();

        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
        Assert.assertTrue(task.isFinished());
        Assert.assertSame(cleanup, failure.getSuppressed()[0]);
    }

    @Test
    public void testAfterCommitDispatchErrorClosesLoaderAndPreservesError() throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
        LoadTask task = LoadTask.builder().id(9).loader(loader).options(new LoadOptions())
                                .status(LoadStatus.RUNNING).build();
        Error failure = new AssertionError("dispatch failed");
        IllegalStateException cleanup = new IllegalStateException("close failed");
        Mockito.doThrow(failure).when(executor).execute(Mockito.eq(task), Mockito.any());
        Mockito.doThrow(cleanup).when(loader).shutdown();
        Mockito.when(mapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);
        this.setField(service, "mapper", mapper);
        this.setField(service, "taskExecutor", executor);
        ConcurrentHashMap<Integer, LoadTask> running = new ConcurrentHashMap<>();
        this.setField(service, "runningTaskContainer", running);
        PlatformTransactionManager transactions = Mockito.mock(PlatformTransactionManager.class);
        Mockito.when(transactions.getTransaction(Mockito.any())).thenReturn(new SimpleTransactionStatus());
        this.setField(service, "transactionManager", transactions);
        Method dispatch = LoadTaskService.class.getDeclaredMethod("executeAfterCommit", LoadTask.class);
        dispatch.setAccessible(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            dispatch.invoke(service, task);
            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager.getSynchronizations().get(0);
            try {
                synchronization.afterCommit();
                Assert.fail("Expected dispatch Error");
            } catch (Error actual) {
                Assert.assertSame(failure, actual);
            }
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            Mockito.verify(loader).shutdown();
            Mockito.verify(mapper).update(Mockito.isNull(), Mockito.any());
            Assert.assertTrue(task.isFinished());
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertSame(cleanup, failure.getSuppressed()[0]);
            Assert.assertTrue(task.isCleanupFailed());
            Assert.assertSame(task, running.get(9));
            Assert.assertThrows(RuntimeException.class, () -> service.retry(9, "current-token"));
            Assert.assertThrows(RuntimeException.class, () -> service.resume(9, "current-token"));
            Assert.assertSame(failure, Assert.assertThrows(Error.class, () -> service.remove(9)));
            Mockito.verify(mapper, Mockito.never()).deleteById(Mockito.anyInt());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void testCleanupFailureWithoutLoadFailureStillMarksTaskFailed() {
        HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
        LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(loader.context()).thenReturn(context);
        Mockito.when(loader.load()).thenReturn(true);
        Mockito.doThrow(new IllegalStateException("cleanup failed")).when(loader).shutdown();
        LoadTask task = LoadTask.builder().id(9).loader(loader).options(new LoadOptions())
                                .status(LoadStatus.PAUSED).lastDuration(0L).build();

        task.run();

        Assert.assertTrue(task.isFinished());
        Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
    }

    @Test
    public void testResumeAndRetryCloseLoaderOnUpdateFailure() throws Exception {
        for (boolean resume : new boolean[]{true, false}) {
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
            LoadTask task = Mockito.spy(LoadTask.builder().id(9).loader(loader)
                                               .options(new LoadOptions())
                                               .status(resume ? LoadStatus.PAUSED : LoadStatus.STOPPED)
                                               .build());
            Mockito.doNothing().when(task).reconnect("current-token");
            Mockito.when(mapper.selectById(9)).thenReturn(task);
            IllegalStateException failure = new IllegalStateException("update failed");
            IllegalStateException cleanup = new IllegalStateException("cleanup failed");
            Mockito.when(mapper.updateById(task)).thenThrow(failure);
            Mockito.doThrow(cleanup).when(loader).shutdown();
            ConcurrentHashMap<Integer, LoadTask> running = new ConcurrentHashMap<>();
            this.setField(service, "mapper", mapper);
            this.setField(service, "taskExecutor", executor);
            this.setField(service, "runningTaskContainer", running);

            try {
                if (resume) {
                    service.resume(9, "current-token");
                } else {
                    service.retry(9, "current-token");
                }
                Assert.fail("Expected update failure");
            } catch (IllegalStateException actual) {
                Assert.assertSame(failure, actual);
                Assert.assertEquals(1, actual.getSuppressed().length);
                Assert.assertSame(cleanup, actual.getSuppressed()[0]);
            }
            Mockito.verify(loader).shutdown();
            Assert.assertTrue(task.isFinished());
            Assert.assertTrue(task.isCleanupFailed());
            Assert.assertSame(task, running.get(9));
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertThrows(RuntimeException.class, () -> service.retry(9, "current-token"));
            Assert.assertThrows(RuntimeException.class, () -> service.resume(9, "current-token"));
            Mockito.verify(task).reconnect("current-token");
            Mockito.verify(executor, Mockito.never()).execute(Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void testResumeAndRetryCloseLoaderOnDispatchRejection() throws Exception {
        for (boolean resume : new boolean[]{true, false}) {
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
            HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
            LoadTask task = Mockito.spy(LoadTask.builder().id(9).loader(loader)
                                               .options(new LoadOptions())
                                               .status(resume ? LoadStatus.PAUSED : LoadStatus.STOPPED)
                                               .build());
            Mockito.doNothing().when(task).reconnect("current-token");
            Mockito.when(mapper.selectById(9)).thenReturn(task);
            Mockito.when(mapper.updateById(task)).thenReturn(1);
            RejectedExecutionException rejected = new RejectedExecutionException("queue full");
            Mockito.doThrow(rejected).when(executor).execute(Mockito.eq(task), Mockito.any());
            ConcurrentHashMap<Integer, LoadTask> running = new ConcurrentHashMap<>();
            this.setField(service, "mapper", mapper);
            this.setField(service, "taskExecutor", executor);
            this.setField(service, "runningTaskContainer", running);
            PlatformTransactionManager transactions = Mockito.mock(PlatformTransactionManager.class);
            Mockito.when(transactions.getTransaction(Mockito.any()))
                   .thenReturn(new SimpleTransactionStatus());
            Mockito.when(mapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);
            this.setField(service, "transactionManager", transactions);

            Throwable actual = Assert.assertThrows(RejectedExecutionException.class, () -> {
                if (resume) {
                    service.resume(9, "current-token");
                } else {
                    service.retry(9, "current-token");
                }
            });
            Assert.assertSame(rejected, actual);
            Mockito.verify(loader).shutdown();
            Assert.assertTrue(task.isFinished());
            Assert.assertEquals(LoadStatus.FAILED, task.getStatus());
            Assert.assertTrue(running.isEmpty());
            Mockito.verify(mapper).update(Mockito.isNull(), Mockito.any());
        }
    }

    @Test
    public void testPreviewAlwaysClosesLoader() {
        for (boolean success : new boolean[]{true, false}) {
            try (MockedConstruction<HugeGraphLoader> construction = Mockito.mockConstruction(
                    HugeGraphLoader.class, (loader, ignored) -> {
                        if (success) {
                            Mockito.when(loader.load()).thenReturn(true);
                        } else {
                            Mockito.when(loader.load()).thenThrow(new IllegalStateException("load failed"));
                        }
                    })) {
                new LoadTaskService().loader(new LoadOptions());
                Mockito.verify(construction.constructed().get(0)).shutdown();
            }
        }
    }

    @Test
    public void testListByIdsAppliesGraphScope() throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Field field = LoadTaskService.class.getDeclaredField("mapper");
        field.setAccessible(true);
        field.set(service, mapper);

        service.list("space-a", "graph-a", 7, Arrays.asList(1, 2));

        ArgumentCaptor<QueryWrapper> query =
                ArgumentCaptor.forClass(QueryWrapper.class);
        Mockito.verify(mapper).selectList(query.capture());
        Assert.assertContains("job_id", query.getValue().getSqlSegment());
        Mockito.verify(mapper, Mockito.never()).selectBatchIds(Mockito.any());
    }

    @Test
    public void testLoadOptionsIgnorePasswordAndUseToken()
           throws Exception {
        GraphConnection connection = this.connection("admin-pass",
                                                     "session-token");

        LoadOptions options = this.buildLoadOptions(connection);

        Assert.assertEquals("admin", options.username);
        Assert.assertNull(options.password);
        Assert.assertEquals("session-token", options.token);
    }

    @Test
    public void testLoadExecutionWaitsForOuterTransactionCommit()
           throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        this.setField(service, "taskExecutor", executor);
        this.setField(service, "runningTaskContainer", new ConcurrentHashMap<>());
        LoadTask task = LoadTask.builder().id(9).build();
        Method method = LoadTaskService.class.getDeclaredMethod(
                "executeAfterCommit", LoadTask.class);
        method.setAccessible(true);

        TransactionSynchronizationManager.initSynchronization();
        try {
            method.invoke(service, task);
            Mockito.verify(executor, Mockito.never())
                   .execute(Mockito.any(), Mockito.any());

            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager.getSynchronizations()
                                                     .get(0);
            synchronization.afterCommit();
            Mockito.verify(executor).execute(Mockito.eq(task), Mockito.any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void testResumeRehydratesLoaderWithCurrentSessionToken()
           throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        LoadTask task = Mockito.mock(LoadTask.class);
        LoadOptions options = new LoadOptions();
        Mockito.when(mapper.selectById(9)).thenReturn(task);
        Mockito.when(mapper.updateById(task)).thenReturn(1);
        Mockito.when(task.getStatus()).thenReturn(LoadStatus.PAUSED);
        Mockito.when(task.getOptions()).thenReturn(options);
        this.setField(service, "mapper", mapper);
        this.setField(service, "taskExecutor", executor);
        this.setField(service, "runningTaskContainer",
                      new ConcurrentHashMap<>());

        LoadTask resumed = service.resume(9, "current-session-token");

        Assert.assertSame(task, resumed);
        Mockito.verify(task).reconnect("current-session-token");
        Mockito.verify(executor).execute(Mockito.eq(task), Mockito.any());
    }

    @Test
    public void testResumeDoesNotPersistRunningStateWhenReconnectFails()
           throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        LoadTask task = Mockito.mock(LoadTask.class);
        Mockito.when(mapper.selectById(9)).thenReturn(task);
        Mockito.when(mapper.updateById(task)).thenReturn(1);
        Mockito.when(task.getStatus()).thenReturn(LoadStatus.PAUSED);
        Mockito.when(task.getOptions()).thenReturn(new LoadOptions());
        Mockito.doThrow(new IllegalArgumentException("missing token"))
               .when(task).reconnect("current-session-token");
        this.setField(service, "mapper", mapper);
        this.setField(service, "taskExecutor", executor);
        this.setField(service, "runningTaskContainer",
                      new ConcurrentHashMap<>());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            service.resume(9, "current-session-token");
        });

        Mockito.verify(mapper, Mockito.never()).updateById(task);
        Mockito.verify(executor, Mockito.never())
               .execute(Mockito.any(), Mockito.any());
    }

    @Test
    public void testRetryRehydratesLoaderWithCurrentSessionToken()
           throws Exception {
        LoadTaskService service = new LoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
        LoadTask task = Mockito.mock(LoadTask.class);
        Mockito.when(mapper.selectById(9)).thenReturn(task);
        Mockito.when(mapper.updateById(task)).thenReturn(1);
        Mockito.when(task.getStatus()).thenReturn(LoadStatus.STOPPED);
        Mockito.when(task.getOptions()).thenReturn(new LoadOptions());
        this.setField(service, "mapper", mapper);
        this.setField(service, "taskExecutor", executor);
        this.setField(service, "runningTaskContainer",
                      new ConcurrentHashMap<>());

        LoadTask retried = service.retry(9, "current-session-token");

        Assert.assertSame(task, retried);
        Mockito.verify(task).reconnect("current-session-token");
        Mockito.verify(executor).execute(Mockito.eq(task), Mockito.any());
    }

    @Test
    public void testRetryAndResumeRejectUnfinishedOrCleanupFailedRuntime() throws Exception {
        for (boolean resume : new boolean[]{false, true}) {
            for (boolean cleanupFailed : new boolean[]{false, true}) {
                LoadTaskService service = new LoadTaskService();
                LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
                LoadTaskExecutor executor = Mockito.mock(LoadTaskExecutor.class);
                LoadTask persisted = Mockito.mock(LoadTask.class);
                Mockito.when(persisted.getStatus()).thenReturn(LoadStatus.FAILED);
                Mockito.when(persisted.getOptions()).thenReturn(new LoadOptions());
                Mockito.when(mapper.selectById(9)).thenReturn(persisted);
                Mockito.when(mapper.updateById(persisted)).thenReturn(1);
                LoadTask retained = LoadTask.builder().id(9).status(LoadStatus.FAILED)
                                            .finished(cleanupFailed).cleanupFailed(cleanupFailed).build();
                ConcurrentHashMap<Integer, LoadTask> running = new ConcurrentHashMap<>();
                running.put(9, retained);
                this.setField(service, "mapper", mapper);
                this.setField(service, "taskExecutor", executor);
                this.setField(service, "runningTaskContainer", running);

                Assert.assertThrows(RuntimeException.class, () -> {
                    if (resume) {
                        service.resume(9, "current-session-token");
                    } else {
                        service.retry(9, "current-session-token");
                    }
                });

                Assert.assertSame(retained, running.get(9));
                Mockito.verify(persisted, Mockito.never()).reconnect(Mockito.anyString());
                Mockito.verify(mapper, Mockito.never()).updateById(Mockito.any(LoadTask.class));
                Mockito.verify(executor, Mockito.never()).execute(Mockito.any(), Mockito.any());
            }
        }
    }

    @Test
    public void testNativeResetFailureAndPurePauseStopCloseResources() throws Exception {
        for (LoadStatus status : new LoadStatus[]{LoadStatus.PAUSED, LoadStatus.STOPPED}) {
            for (boolean rejectReset : new boolean[]{false, true}) {
                LoadTask task = LoadTask.builder().id(9).status(LoadStatus.RUNNING).lastDuration(0L).build();
                try (NativeCleanupTestSupport nativeCleanup = new NativeCleanupTestSupport(
                        task, this.folder.newFile("native-" + status + rejectReset + ".json").toPath(),
                        rejectReset, status)) {
                    task.run();

                    Assert.assertEquals(rejectReset ? LoadStatus.FAILED : status, task.getStatus());
                    Assert.assertEquals(rejectReset, task.isCleanupFailed());
                    if (rejectReset) {
                        Assert.assertNotSame(nativeCleanup.original, nativeCleanup.shutdownFailure.get());
                    } else {
                        Assert.assertSame(nativeCleanup.original, nativeCleanup.shutdownFailure.get());
                    }
                    Assert.assertEquals(rejectReset ? "LOADING" : "NONE", nativeCleanup.mode());
                    nativeCleanup.assertResourcesClosed();
                }
            }
        }
    }

    @Test
    public void testPauseAndStopKeepCleanupFailedRuntimeAndLoader() throws Exception {
        for (boolean pause : new boolean[]{false, true}) {
            LoadTaskService service = new LoadTaskService();
            LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
            HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
            LoadContext context = Mockito.mock(LoadContext.class);
            Mockito.when(loader.context()).thenReturn(context);
            Mockito.when(context.closed()).thenReturn(true);
            LoadTask task = LoadTask.builder().id(9).status(LoadStatus.RUNNING).loader(loader).build();
            Mockito.doAnswer(invocation -> {
                task.setStatus(LoadStatus.FAILED);
                task.setCleanupFailed(true);
                task.setFinished(true);
                return null;
            }).when(context).stopLoading();
            Mockito.when(mapper.updateById(task)).thenReturn(1);
            ConcurrentHashMap<Integer, LoadTask> running = new ConcurrentHashMap<>();
            running.put(9, task);
            this.setField(service, "mapper", mapper);
            this.setField(service, "runningTaskContainer", running);

            if (pause) {
                service.pause(9);
            } else {
                service.stop(9);
            }

            Assert.assertSame(task, running.get(9));
            Assert.assertSame(loader, task.getLoader());
            Assert.assertTrue(task.isCleanupFailed());
        }
    }

    @Test
    public void testLoadOptionsUseTokenWithoutCredentialPassword()
           throws Exception {
        GraphConnection connection = this.connection(null, "session-token");

        LoadOptions options = this.buildLoadOptions(connection);

        Assert.assertEquals("admin", options.username);
        Assert.assertNull(options.password);
        Assert.assertEquals("session-token", options.token);
    }

    @Test
    public void testLoadOptionsKeepPdRoutingWithoutHostPort()
           throws Exception {
        GraphConnection connection = this.pdConnection();

        LoadOptions options = this.buildLoadOptions(connection);

        Assert.assertEquals("analytics", options.graphSpace);
        Assert.assertEquals("hugegraph", options.graph);
        Assert.assertEquals("127.0.0.1:8686", options.pdPeers);
        Assert.assertEquals("cluster-a", options.cluster);
        Assert.assertEquals("BOTH", options.routeType);
        Assert.assertEquals("admin", options.username);
        Assert.assertNull(options.password);
        Assert.assertEquals("session-token", options.token);
        Assert.assertEquals("localhost", options.host);
        Assert.assertEquals(8080, options.port);
    }

    @Test
    public void testBuildFileSourcePreservesPhysicalHeaderFlag()
            throws Exception {
        LoadTaskService service = new LoadTaskService();
        Method method = LoadTaskService.class.getDeclaredMethod(
                "buildFileSource", FileMapping.class);
        method.setAccessible(true);
        FileMapping mapping = this.fileMapping();
        FileSetting setting = FileSetting.builder()
                                         .columnNames(Arrays.asList("name"))
                                         .hasHeader(false)
                                         .format("CSV")
                                         .delimiter(",")
                                         .charset("UTF-8")
                                         .dateFormat("yyyy-MM-dd HH:mm:ss")
                                         .timeZone("GMT+8")
                                         .skippedLine("(^#|^//).*")
                                         .listFormat(new ListFormat())
                                         .build();
        mapping.setFileSetting(setting);

        FileSource source = (FileSource) method.invoke(service, mapping);

        Assert.assertFalse(source.hasHeader());
        setting.setHasHeader(true);
        source = (FileSource) method.invoke(service, mapping);
        Assert.assertTrue(source.hasHeader());
    }

    @Test
    public void testLoadOptionsPreferDirectServerOverPdPeers()
           throws Exception {
        GraphConnection connection = this.connection("admin-pass",
                                                     "session-token");
        connection.setCluster("cluster-a");
        connection.setRouteType("BOTH");
        connection.setPdPeers("127.0.0.1:8686");
        connection.setGraphSpace("DEFAULT");

        LoadOptions options = this.buildLoadOptions(connection);

        Assert.assertNull(options.pdPeers);
        Assert.assertEquals("127.0.0.1", options.host);
        Assert.assertEquals(8080, options.port);
        Assert.assertEquals("DEFAULT", options.graphSpace);
        Assert.assertEquals("hugegraph", options.graph);
        Assert.assertNull(options.password);
        Assert.assertEquals("session-token", options.token);
    }

    private LoadOptions buildLoadOptions(GraphConnection connection)
            throws Exception {
        LoadTaskService service = new LoadTaskService();
        Method method = LoadTaskService.class.getDeclaredMethod(
                "buildLoadOptions", GraphConnection.class, FileMapping.class);
        method.setAccessible(true);
        return (LoadOptions) method.invoke(service, connection,
                                           this.fileMapping());
    }

    private void setField(Object object, String name, Object value)
                          throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private GraphConnection connection(String password, String token) {
        GraphConnection connection = new GraphConnection();
        connection.setGraph("hugegraph");
        connection.setHost("127.0.0.1");
        connection.setPort(8080);
        connection.setUsername("admin");
        connection.setPassword(password);
        connection.setToken(token);
        connection.setProtocol("http");
        return connection;
    }

    private GraphConnection pdConnection() {
        GraphConnection connection = new GraphConnection();
        connection.setCluster("cluster-a");
        connection.setRouteType("BOTH");
        connection.setPdPeers("127.0.0.1:8686");
        connection.setGraphSpace("analytics");
        connection.setGraph("hugegraph");
        connection.setUsername("admin");
        connection.setPassword("admin-pass");
        connection.setToken("session-token");
        return connection;
    }

    private FileMapping fileMapping() {
        FileMapping fileMapping = new FileMapping();
        fileMapping.setPath("/tmp/hubble-loader/edges.csv");
        fileMapping.setLoadParameter(LoadParameter.builder()
                                                  .checkVertex(false)
                                                  .insertTimeout(60)
                                                  .maxParseErrors(1)
                                                  .maxInsertErrors(1)
                                                  .retryTimes(1)
                                                  .retryInterval(1)
                                                  .build());
        return fileMapping;
    }
}
