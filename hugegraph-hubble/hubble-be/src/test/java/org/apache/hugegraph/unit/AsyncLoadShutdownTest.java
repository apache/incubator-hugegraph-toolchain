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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hugegraph.config.AsyncConfig;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.handler.HubbleDisposableBean;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.task.TaskManager;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.util.HugeClientHolder;
import org.apache.hugegraph.loader.task.GlobalExecutorManager;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;
import org.apache.hugegraph.mapper.load.JobManagerMapper;
import org.apache.hugegraph.mapper.load.FileMappingMapper;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.zaxxer.hikari.HikariDataSource;

public class AsyncLoadShutdownTest {

    @Test
    public void testClosePausesRunningAndQueuedLoadsBeforePoolShutdown() throws Throwable {
        this.closeLoads(false, false, Admission.DIRECT);
    }

    @Test
    public void testCloseToleratesTerminalRuntimeBehindStaleRunningRow() throws Throwable {
        this.closeLoads(true, false, Admission.DIRECT);
    }

    @Test
    public void testCloseContinuesAfterStopAndMetadataFailures() throws Throwable {
        this.closeLoads(false, true, Admission.DIRECT);
    }

    @Test
    public void testRuntimePublishedBeforeAsyncSubmissionReturns() throws Throwable {
        this.closeLoads(false, false, Admission.SUBMISSION_HELD);
    }

    @Test
    public void testCloseWaitsForResumeReconnectAndAdmission() throws Throwable {
        this.closeLoads(false, false, Admission.RESUME_HELD);
    }

    @Test
    public void testCloseWaitsForRetryReconnectAndAdmission() throws Throwable {
        this.closeLoads(false, false, Admission.RETRY_HELD);
    }

    @Test
    public void testCloseWaitsForAlreadyRegisteredTransactionCommit() throws Throwable {
        this.closeLoads(false, false, Admission.COMMIT_HELD);
    }

    @Test
    public void testCloseWaitsForAlreadyRegisteredTransactionRollback() throws Throwable {
        this.closeLoads(false, false, Admission.ROLLBACK_HELD);
    }

    @Test
    public void testCommittedDispatchRejectionWaitsForFailureMarker() throws Throwable {
        this.closeLoads(false, false, Admission.REJECTED_HELD);
    }

    @Test
    public void testCommittedDispatchAndMarkerFailuresRemainVisible() throws Throwable {
        this.closeLoads(false, false, Admission.MARKER_FAILED_HELD);
    }

    @Test
    public void testUnstartedCleanupFailureStillAttemptsFailureMarker() throws Throwable {
        this.closeLoads(false, false, Admission.CLEANUP_FAILED_HELD);
    }

    @Test
    public void testCloseToleratesRollbackCleanupBeforeTicketCompletion() throws Throwable {
        this.closeLoads(false, false, Admission.ROLLBACK_CLEANUP_HELD);
    }

    @Test
    public void testCloseReportsCallbackFailureCompletedBeforeShutdownSnapshot() throws Throwable {
        this.closeLoads(true, false, Admission.PRE_CLOSE_CALLBACK_FAILED);
    }

    @Test
    public void testActiveRunnerClosesActualNativeTaskManagerAndContext() throws Exception {
        LoadOptions options = new LoadOptions();
        LoadContext nativeContext = LoadContext.forOffline(options, new SchemaCache(null));
        TaskManager manager = new TaskManager(nativeContext);
        HugeGraphLoader nativeLoader = this.nativeLoader(nativeContext, manager, options);
        // Only load I/O is controlled; shutdown is the real final SDK method.
        HugeGraphLoader loader = Mockito.spy(nativeLoader);
        ExecutorService batch = (ExecutorService) ReflectionTestUtils.getField(manager, "batchService");
        ExecutorService single = (ExecutorService) ReflectionTestUtils.getField(manager, "singleService");
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Runnable nativeWork = () -> {
            started.countDown();
            try {
                assertTrue(release.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        };
        batch.execute(nativeWork);
        single.execute(nativeWork);
        Mockito.doAnswer(invocation -> {
            release.countDown();
            return true;
        }).when(loader).load();
        LoadTask task = LoadTask.builder().id(17).options(options).loader(loader)
                                .status(LoadStatus.RUNNING).lastDuration(0L).currDuration(0L).build();
        try {
            assertTrue(started.await(10, TimeUnit.SECONDS));
            task.run();
            assertTrue("Actual native batch workers must terminate before run completion", batch.isTerminated());
            assertTrue("Actual native single workers must terminate before run completion", single.isTerminated());
            assertTrue("Actual native offline context must close", nativeContext.closed());
            assertEquals(LoadStatus.SUCCEED, task.getStatus());
            assertTrue(task.isFinished());
            Mockito.verify(loader).shutdown();
        } finally {
            release.countDown();
            loader.shutdown();
            assertTrue(batch.awaitTermination(10, TimeUnit.SECONDS));
            assertTrue(single.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testSaturatedResumeClosesNativeLoaderAndMarksFailure() throws Throwable {
        this.rejectRestart(true);
    }

    @Test
    public void testSaturatedRetryClosesNativeLoaderAndMarksFailure() throws Throwable {
        this.rejectRestart(false);
    }

    @Test
    public void testRepeatedSaturationReleasesTicketsAfterActualFailureMarkerCommit() throws Throwable {
        this.rejectedDispatchCommit(false);
    }

    @Test
    public void testFailedFailureMarkerCommitRetainsTicketAndOriginalDispatchCause() throws Throwable {
        this.rejectedDispatchCommit(true);
    }

    private void rejectedDispatchCommit(boolean commitFails) throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        HikariDataSource datasource = new HikariDataSource();
        datasource.setJdbcUrl("jdbc:h2:mem:dispatch-commit-" + commitFails);
        datasource.setMinimumIdle(0);
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        jdbc.execute("create table dispatch_state(id integer primary key, load_status integer)");
        jdbc.update("insert into dispatch_state values(9, ?)", LoadStatus.FAILED.getValue());
        TransactionSystemException commitFailure = new TransactionSystemException("controlled marker commit failure");
        DataSourceTransactionManager transactions = new DataSourceTransactionManager(datasource) {
            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                if (commitFails) {
                    throw commitFailure;
                }
                super.doCommit(status);
            }
        };
        transactions.setRollbackOnCommitFailure(true);
        context.registerBean("dataSource", HikariDataSource.class, () -> datasource);
        ObservedLoadTaskService service = new ObservedLoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        List<LoadTask> attempts = new ArrayList<>();
        List<HugeGraphLoader> loaders = new ArrayList<>();
        List<ExecutorService> nativePools = new ArrayList<>();
        List<Integer> ticketCounts = new ArrayList<>();
        AtomicInteger beforeCommits = new AtomicInteger();
        AtomicInteger committed = new AtomicInteger();
        AtomicInteger completionStatus = new AtomicInteger(-1);
        Mockito.when(mapper.selectList(null)).thenReturn(Collections.emptyList());
        Mockito.when(mapper.selectById(9)).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(LoadStatus.FAILED.getValue().intValue(),
                         jdbc.queryForObject("select load_status from dispatch_state where id=9", Integer.class)
                             .intValue());
            LoadOptions options = new LoadOptions();
            LoadTask task = Mockito.spy(LoadTask.builder().id(9).status(LoadStatus.FAILED).options(options)
                    .lastDuration(0L).currDuration(0L).build());
            attempts.add(task);
            Mockito.doAnswer(reconnect -> {
                LoadContext nativeContext = LoadContext.forOffline(options, new SchemaCache(null));
                TaskManager manager = new TaskManager(nativeContext);
                HugeGraphLoader loader = this.nativeLoader(nativeContext, manager, options);
                loaders.add(loader);
                task.setLoader(loader);
                for (String field : new String[]{"batchService", "singleService"}) {
                    ExecutorService pool = (ExecutorService) ReflectionTestUtils.getField(manager, field);
                    nativePools.add(pool);
                    pool.submit(() -> { }).get(10, TimeUnit.SECONDS);
                }
                return null;
            }).when(task).reconnect("controlled-session");
            return task;
        });
        Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
            LoadTask task = invocation.getArgument(0);
            return jdbc.update("update dispatch_state set load_status=? where id=9", task.getStatus().getValue());
        });
        Mockito.when(mapper.update(Mockito.isNull(), Mockito.any())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            LoadTask task = attempts.get(attempts.size() - 1);
            Set<?> accepted = (Set<?>) ReflectionTestUtils.getField(service, "acceptedRuns");
            CompletableFuture<?> current = null;
            for (Object run : accepted) {
                if (ReflectionTestUtils.getField(run, "task") == task) {
                    current = (CompletableFuture<?>) ReflectionTestUtils.getField(run, "completed");
                }
            }
            assertNotNull(current);
            CompletableFuture<?> ticket = current;
            assertFalse(ticket.isDone());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    assertFalse("Marker update returning does not complete the cleanup ticket", ticket.isDone());
                    beforeCommits.incrementAndGet();
                }

                @Override
                public void afterCommit() {
                    assertFalse("Ticket must include actual transaction commit", ticket.isDone());
                    committed.incrementAndGet();
                }

                @Override
                public void afterCompletion(int status) {
                    completionStatus.set(status);
                }
            });
            return jdbc.update("update dispatch_state set load_status=? where id=9", LoadStatus.FAILED.getValue());
        });
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "transactionManager", transactions);
        context.getBeanFactory().registerSingleton("loadTaskService", service);
        context.register(HubbleDisposableBean.class, LoadTaskExecutor.class, AsyncConfig.class);
        context.refresh();
        ThreadPoolTaskExecutor executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        service.executor = executor;
        ReflectionTestUtils.setField(service, "taskExecutor", context.getBean(LoadTaskExecutor.class));
        CountDownLatch workers = new CountDownLatch(16);
        CountDownLatch release = new CountDownLatch(1);
        Throwable primary = null;
        try {
            Runnable blocked = () -> {
                workers.countDown();
                try {
                    assertTrue(release.await(20, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(failure);
                }
            };
            for (int i = 0; i < 1040; i++) {
                executor.execute(blocked);
            }
            assertTrue(workers.await(10, TimeUnit.SECONDS));
            assertEquals(1024, executor.getThreadPoolExecutor().getQueue().size());
            RejectedExecutionException rejection = null;
            int count = commitFails ? 1 : 2;
            for (int i = 0; i < count; i++) {
                try {
                    service.retry(9, "controlled-session");
                    throw new AssertionError("Saturated executor must reject each fresh native run");
                } catch (RejectedExecutionException original) {
                    rejection = original;
                }
                LoadTask task = attempts.get(i);
                assertTrue(task.isFinished());
                assertEquals(LoadStatus.FAILED, task.getStatus());
                assertFalse(task.isCleanupFailed());
                assertNull(task.getLoader());
                assertTrue(loaders.get(i).context().closed());
                for (ExecutorService pool : nativePools) {
                    assertTrue(pool.isTerminated());
                }
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals(commitFails ? LoadStatus.RUNNING.getValue().intValue() :
                             LoadStatus.FAILED.getValue().intValue(),
                             jdbc.queryForObject("select load_status from dispatch_state where id=9", Integer.class)
                                 .intValue());
                ticketCounts.add(((Set<?>) ReflectionTestUtils.getField(service, "acceptedRuns")).size());
            }
            assertNotNull(rejection);
            assertEquals(count, beforeCommits.get());
            assertEquals(commitFails ? 0 : count, committed.get());
            assertEquals(commitFails ? TransactionSynchronization.STATUS_ROLLED_BACK :
                         TransactionSynchronization.STATUS_COMMITTED, completionStatus.get());
            if (commitFails) {
                assertTrue(java.util.Arrays.asList(rejection.getSuppressed()).contains(commitFailure));
                assertEquals(Collections.singletonList(1), ticketCounts);
            } else {
                assertTrue(attempts.get(0) != attempts.get(1));
                assertTrue(attempts.get(0).getOptions() != attempts.get(1).getOptions());
                assertEquals("Resolved saturation attempts must not retain old tasks or runtime options",
                             java.util.Arrays.asList(0, 0), ticketCounts);
            }
            release.countDown();
            context.close();
            assertEquals(!commitFails, service.pauseCompleted);
            assertSame(commitFails ? rejection : null, service.failure);
            assertTrue(executor.getThreadPoolExecutor().isTerminated());
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            release.countDown();
            try {
                for (HugeGraphLoader loader : loaders) {
                    loader.shutdown();
                }
                context.close();
                for (ExecutorService pool : nativePools) {
                    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
                }
                assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
            } catch (Throwable cleanup) {
                if (primary == null) {
                    throw cleanup;
                }
                if (primary != cleanup) {
                    primary.addSuppressed(cleanup);
                }
            }
        }
    }

    @Test
    public void testSameDispatchAndNativeCleanupCauseDoesNotAbortFailureMarker() throws Throwable {
        this.rejectRestart(true, true);
    }

    private void rejectRestart(boolean resume) throws Throwable {
        this.rejectRestart(resume, false);
    }

    private void rejectRestart(boolean resume, boolean sharedFailure) throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        HikariDataSource datasource = new HikariDataSource();
        datasource.setJdbcUrl("jdbc:h2:mem:rejected-restart-" + resume);
        datasource.setMinimumIdle(0);
        context.registerBean("dataSource", HikariDataSource.class, () -> datasource);
        ObservedLoadTaskService service = new ObservedLoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadOptions options = new LoadOptions();
        LoadContext nativeContext = Mockito.spy(LoadContext.forOffline(options, new SchemaCache(null)));
        RejectedExecutionException sharedCause = new RejectedExecutionException("controlled shared lifecycle cause");
        if (sharedFailure) {
            Mockito.doAnswer(invocation -> {
                invocation.callRealMethod();
                throw sharedCause;
            }).when(nativeContext).close(Mockito.anyBoolean());
        }
        TaskManager manager = new TaskManager(nativeContext);
        HugeGraphLoader loader = this.nativeLoader(nativeContext, manager, options);
        ExecutorService batch = (ExecutorService) ReflectionTestUtils.getField(manager, "batchService");
        ExecutorService single = (ExecutorService) ReflectionTestUtils.getField(manager, "singleService");
        batch.submit(() -> { }).get(10, TimeUnit.SECONDS);
        single.submit(() -> { }).get(10, TimeUnit.SECONDS);
        LoadTask task = Mockito.spy(LoadTask.builder().id(9).options(options)
                                          .status(resume ? LoadStatus.PAUSED : LoadStatus.STOPPED)
                                          .lastDuration(0L).currDuration(0L).build());
        Mockito.doAnswer(invocation -> {
            // Control reconstruction only; the reconstructed SDK and all its
            // shutdown/resource handling are actual native implementations.
            task.setLoader(loader);
            return null;
        }).when(task).reconnect("controlled-session");
        Mockito.when(mapper.selectById(9)).thenReturn(task);
        Mockito.when(mapper.selectList(null)).thenReturn(Collections.emptyList());
        Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenReturn(1);
        Mockito.when(mapper.update(Mockito.isNull(), Mockito.any())).thenReturn(1);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "transactionManager", new DataSourceTransactionManager(datasource));
        context.getBeanFactory().registerSingleton("loadTaskService", service);
        context.register(HubbleDisposableBean.class, LoadTaskExecutor.class, AsyncConfig.class);
        context.refresh();
        ThreadPoolTaskExecutor executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        service.executor = executor;
        if (sharedFailure) {
            LoadTaskExecutor rejectedDispatcher = Mockito.mock(LoadTaskExecutor.class);
            Mockito.doThrow(sharedCause).when(rejectedDispatcher).execute(Mockito.eq(task), Mockito.any());
            ReflectionTestUtils.setField(service, "taskExecutor", rejectedDispatcher);
        } else {
            ReflectionTestUtils.setField(service, "taskExecutor", context.getBean(LoadTaskExecutor.class));
        }
        CountDownLatch saturatedWorkers = new CountDownLatch(16);
        CountDownLatch releasePool = new CountDownLatch(1);
        Throwable primary = null;
        try {
            Runnable blocked = () -> {
                saturatedWorkers.countDown();
                try {
                    assertTrue(releasePool.await(15, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            };
            // Fill the actual 16 workers and 1024 queue, without changing the
            // production capacity or mocking the dispatch rejection.
            if (!sharedFailure) {
                for (int i = 0; i < 1040; i++) {
                    executor.execute(blocked);
                }
                assertTrue(saturatedWorkers.await(10, TimeUnit.SECONDS));
                assertEquals(1024, executor.getThreadPoolExecutor().getQueue().size());
            }
            RuntimeException rejected = null;
            try {
                if (resume) {
                    service.resume(9, "controlled-session");
                } else {
                    service.retry(9, "controlled-session");
                }
                throw new AssertionError("Saturated native executor must reject restart");
            } catch (RuntimeException failure) {
                rejected = failure;
            }
            if (sharedFailure) {
                assertSame(sharedCause, rejected);
            }
            releasePool.countDown();
            context.close();
            assertNotNull("Frontend must receive the original dispatch rejection", rejected);
            assertSame(sharedFailure ? rejected : null, service.failure);
            assertEquals(!sharedFailure, service.pauseCompleted);
            assertEquals(LoadStatus.FAILED, task.getStatus());
            assertTrue(task.isFinished());
            if (sharedFailure) {
                assertTrue(task.isCleanupFailed());
                assertSame(sharedCause, task.getCleanupFailure());
                Mockito.verify(nativeContext).close(Mockito.anyBoolean());
                assertSame(loader, task.getLoader());
                Map<?, ?> running = (Map<?, ?>) ReflectionTestUtils.getField(service, "runningTaskContainer");
                assertSame(task, running.get(9));
                assertEquals(0, sharedCause.getSuppressed().length);
            } else {
                assertNull(task.getLoader());
            }
            assertTrue(nativeContext.closed());
            assertTrue(batch.isTerminated());
            assertTrue(single.isTerminated());
            Mockito.verify(mapper).update(Mockito.isNull(), Mockito.any());
            assertTrue(executor.getThreadPoolExecutor().isTerminated());
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            releasePool.countDown();
            try {
                context.close();
                loader.shutdown();
                assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
                assertTrue(batch.awaitTermination(10, TimeUnit.SECONDS));
                assertTrue(single.awaitTermination(10, TimeUnit.SECONDS));
            } catch (Throwable cleanup) {
                if (primary == null) {
                    throw cleanup;
                }
                primary.addSuppressed(cleanup);
            }
        }
    }

    @Test
    public void testActualNativePureLoadFailureKeepsPausedAndClosesResources() throws Throwable {
        this.nativeCallbackFailure(false, NativeRequest.NONE);
    }

    @Test
    public void testActualNativeCleanupFailureRemainsInCallbackTicketAndRuntime() throws Throwable {
        this.nativeCallbackFailure(true, NativeRequest.NONE);
    }

    @Test
    public void testForegroundPauseRetainsActualNativeCleanupFailureRuntime() throws Throwable {
        this.nativeCallbackFailure(true, NativeRequest.PAUSE);
    }

    @Test
    public void testForegroundStopRetainsActualNativeCleanupFailureRuntime() throws Throwable {
        this.nativeCallbackFailure(true, NativeRequest.STOP);
    }

    @Test
    public void testOpenContextCannotReplaceUnsafeRuntimeFromFreshDatabaseEntity() throws Throwable {
        this.nativeCallbackFailure(true, NativeRequest.RESTART_GUARD);
    }

    @Test
    public void testCompletedNativeCallbackErrorKeepsOriginalIdentityOnClose() throws Throwable {
        this.nativeCallbackFailure(true, NativeRequest.NONE, true);
    }

    @Test
    public void testCancelledNativeCallbackTicketStillClosesRetainedRuntimeAndUpdatesStaleRows() throws Throwable {
        this.nativeCallbackFailure(true, NativeRequest.CANCELLED_CALLBACK);
    }

    private void nativeCallbackFailure(boolean cleanupFails, NativeRequest request) throws Throwable {
        this.nativeCallbackFailure(cleanupFails, request, false);
    }

    private void nativeCallbackFailure(boolean cleanupFails, NativeRequest request, boolean cleanupError)
            throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        HikariDataSource datasource = new HikariDataSource();
        datasource.setJdbcUrl("jdbc:h2:mem:native-callback-" + cleanupFails);
        context.registerBean("dataSource", HikariDataSource.class, () -> datasource);
        LoadOptions options = new LoadOptions();
        LoadContext nativeContext = Mockito.spy(LoadContext.forOffline(options, new SchemaCache(null)));
        TaskManager manager = new TaskManager(nativeContext);
        HugeGraphLoader loader = Mockito.spy(this.nativeLoader(nativeContext, manager, options));
        ExecutorService batch = (ExecutorService) ReflectionTestUtils.getField(manager, "batchService");
        ExecutorService single = (ExecutorService) ReflectionTestUtils.getField(manager, "singleService");
        batch.submit(() -> { }).get(10, TimeUnit.SECONDS);
        single.submit(() -> { }).get(10, TimeUnit.SECONDS);
        boolean cancelled = request == NativeRequest.CANCELLED_CALLBACK;
        Throwable nativeFailure = cancelled ? new CancellationException("controlled native cleanup cancellation") :
                                  cleanupError ? new AssertionError("controlled native lifecycle Error") :
                                  new IllegalStateException("controlled native lifecycle failure");
        if (cancelled) {
            nativeFailure.initCause(new IllegalStateException("controlled cancellation cause"));
        }
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch stopIntent = new CountDownLatch(1);
        if (cleanupFails) {
            Mockito.doAnswer(invocation -> {
                loading.countDown();
                if (request == NativeRequest.PAUSE || request == NativeRequest.STOP) {
                    assertTrue(stopIntent.await(10, TimeUnit.SECONDS));
                }
                return true;
            }).when(loader).load();
            Mockito.doAnswer(invocation -> {
                invocation.callRealMethod();
                stopIntent.countDown();
                return null;
            }).when(nativeContext).stopLoading();
            Mockito.doAnswer(invocation -> {
                invocation.callRealMethod();
                throw nativeFailure;
            }).when(nativeContext).close(Mockito.anyBoolean());
        } else {
            Mockito.doAnswer(invocation -> {
                nativeContext.failLoading(nativeFailure);
                throw nativeFailure;
            }).when(loader).load();
        }
        LoadTask task = LoadTask.builder().id(31).options(options).loader(loader)
                                .status(cleanupFails ? LoadStatus.RUNNING : LoadStatus.PAUSED)
                                .lastDuration(0L).currDuration(0L).build();
        ObservedLoadTaskService service = new ObservedLoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        AtomicReference<LoadStatus> written = new AtomicReference<>();
        Mockito.when(mapper.selectList(null)).thenReturn(Collections.emptyList());
        Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
            written.set(((LoadTask) invocation.getArgument(0)).getStatus());
            return 1;
        });
        ReflectionTestUtils.setField(service, "mapper", mapper);
        context.getBeanFactory().registerSingleton("loadTaskService", service);
        context.register(HubbleDisposableBean.class, LoadTaskExecutor.class, AsyncConfig.class);
        context.refresh();
        ThreadPoolTaskExecutor executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        service.executor = executor;
        ReflectionTestUtils.setField(service, "taskExecutor", context.getBean(LoadTaskExecutor.class));
        CountDownLatch invocationReturned = new CountDownLatch(1);
        executor.setTaskDecorator(invocation -> () -> {
            try {
                invocation.run();
            } finally {
                invocationReturned.countDown();
            }
        });
        HugeGraphLoader retainedLoader = null;
        List<ExecutorService> retainedPools = new ArrayList<>();
        LoadTask staleRow = LoadTask.builder().id(101).status(LoadStatus.RUNNING).build();
        AtomicInteger staleUpdates = new AtomicInteger();
        Throwable primary = null;
        try {
            if (cancelled) {
                LoadOptions retainedOptions = new LoadOptions();
                LoadContext retainedContext = LoadContext.forOffline(retainedOptions, new SchemaCache(null));
                TaskManager retainedManager = new TaskManager(retainedContext);
                retainedLoader = this.nativeLoader(retainedContext, retainedManager, retainedOptions);
                for (String field : new String[]{"batchService", "singleService"}) {
                    ExecutorService pool = (ExecutorService) ReflectionTestUtils.getField(retainedManager, field);
                    retainedPools.add(pool);
                    pool.submit(() -> { }).get(10, TimeUnit.SECONDS);
                }
                LoadTask retainedTask = LoadTask.builder().id(52).loader(retainedLoader).options(retainedOptions)
                                               .status(LoadStatus.PAUSED).finished(true)
                                               .lastDuration(0L).currDuration(0L).build();
                @SuppressWarnings("unchecked")
                Map<Integer, LoadTask> running =
                        (Map<Integer, LoadTask>) ReflectionTestUtils.getField(service, "runningTaskContainer");
                // Only the independent recovery handle is seeded. The cancelled
                // ticket below is registered by actual execute and callback.
                running.put(52, retainedTask);
                Mockito.when(mapper.selectList(null)).thenReturn(Collections.singletonList(staleRow));
                Mockito.doAnswer(update -> {
                    staleUpdates.incrementAndGet();
                    return 1;
                }).when(mapper).updateById(staleRow);
            }
            ReflectionTestUtils.invokeMethod(service, "execute", task);
            if (request == NativeRequest.PAUSE || request == NativeRequest.STOP) {
                assertTrue(loading.await(10, TimeUnit.SECONDS));
                if (request == NativeRequest.PAUSE) {
                    service.pause(31);
                } else {
                    service.stop(31);
                }
            }
            assertTrue(invocationReturned.await(10, TimeUnit.SECONDS));
            if (request == NativeRequest.PAUSE || request == NativeRequest.STOP) {
                Map<?, ?> running = (Map<?, ?>) ReflectionTestUtils.getField(service, "runningTaskContainer");
                assertSame("Foreground stop must retain an unsafe runtime before context close", task, running.get(31));
            }
            assertTrue(batch.isTerminated());
            assertTrue(single.isTerminated());
            assertTrue(nativeContext.closed());
            Mockito.verify(nativeContext).close(Mockito.anyBoolean());
            assertTrue(task.isFinished());
            assertEquals(cleanupFails, task.isCleanupFailed());
            assertEquals(cleanupFails ? LoadStatus.FAILED : LoadStatus.PAUSED, written.get());
            if (cancelled) {
                Set<?> accepted = (Set<?>) ReflectionTestUtils.getField(service, "acceptedRuns");
                assertEquals(1, accepted.size());
                CompletableFuture<?> ticket = (CompletableFuture<?>)
                        ReflectionTestUtils.getField(accepted.iterator().next(), "completed");
                assertTrue("Actual callback completion encodes a direct join cancellation", ticket.isCancelled());
            }
            if (request == NativeRequest.RESTART_GUARD) {
                LoadTask fresh = LoadTask.builder().id(31).status(LoadStatus.FAILED).options(new LoadOptions())
                                         .lastDuration(0L).currDuration(0L).build();
                Mockito.when(mapper.selectById(31)).thenReturn(fresh);
                try (MockedConstruction<HugeGraphLoader> constructed =
                             Mockito.mockConstruction(HugeGraphLoader.class)) {
                    RuntimeException resumeFailure = null;
                    RuntimeException retryFailure = null;
                    try {
                        service.resume(31, "controlled-session");
                    } catch (RuntimeException failure) {
                        resumeFailure = failure;
                    }
                    try {
                        service.retry(31, "controlled-session");
                    } catch (RuntimeException failure) {
                        retryFailure = failure;
                    }
                    assertEquals("Unsafe retained runtime must reject before any Loader construction",
                                 0, constructed.constructed().size());
                    assertNotNull(resumeFailure);
                    assertNotNull(retryFailure);
                    assertTrue(context.isActive());
                    Map<?, ?> running = (Map<?, ?>) ReflectionTestUtils.getField(service, "runningTaskContainer");
                    assertSame(task, running.get(31));
                    Mockito.verify(mapper, Mockito.never()).selectById(31);
                }
            }
            context.close();
            if (cancelled) {
                assertTrue("A direct join cancellation must not skip the next actual native cleanup",
                           retainedLoader.context().closed());
                for (ExecutorService pool : retainedPools) {
                    assertTrue(pool.isTerminated());
                }
                assertEquals(1, staleUpdates.get());
                assertEquals(LoadStatus.PAUSED, staleRow.getStatus());
                Map<?, ?> running = (Map<?, ?>) ReflectionTestUtils.getField(service, "runningTaskContainer");
                assertFalse(running.containsKey(52));
            }
            if (cleanupFails) {
                assertSame(nativeFailure, task.getCleanupFailure());
                assertSame(nativeFailure, service.failure);
                assertFalse(service.pauseCompleted);
                assertSame("Cleanup failure must retain the native recovery handle", loader, task.getLoader());
                Map<?, ?> running = (Map<?, ?>) ReflectionTestUtils.getField(service, "runningTaskContainer");
                assertSame(task, running.get(31));
            } else {
                assertNull(task.getCleanupFailure());
                assertNull(service.failure);
                assertTrue(service.pauseCompleted);
                assertNull(task.getLoader());
            }
            com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .valueToTree(task);
            assertFalse(json.has("cleanupFailure"));
            assertFalse(json.has("cleanupFailed"));
            assertTrue(executor.getThreadPoolExecutor().isTerminated());
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                stopIntent.countDown();
                context.close();
                loader.shutdown();
                if (retainedLoader != null) {
                    retainedLoader.shutdown();
                }
                for (ExecutorService pool : retainedPools) {
                    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
                }
                assertTrue(batch.awaitTermination(10, TimeUnit.SECONDS));
                assertTrue(single.awaitTermination(10, TimeUnit.SECONDS));
                assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
            } catch (Throwable cleanup) {
                if (primary == null) {
                    throw cleanup;
                }
                primary.addSuppressed(cleanup);
            }
        }
    }

    @Test
    public void testNativeCleanupErrorStillClosesOtherRetainedRuntimeAndUpdatesStaleRows() throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        HikariDataSource datasource = new HikariDataSource();
        datasource.setJdbcUrl("jdbc:h2:mem:retained-native-error");
        datasource.setMinimumIdle(0);
        context.registerBean("dataSource", HikariDataSource.class, () -> datasource);
        ObservedLoadTaskService service = new ObservedLoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        LoadTask staleFailed = LoadTask.builder().id(100).status(LoadStatus.RUNNING).build();
        LoadTask staleRemaining = LoadTask.builder().id(101).status(LoadStatus.RUNNING).build();
        Mockito.when(mapper.selectList(null)).thenReturn(java.util.Arrays.asList(staleFailed, staleRemaining));
        AssertionError nativeFailure = new AssertionError("controlled first native cleanup Error");
        AssertionError metadataFailure = new AssertionError("controlled subsequent stale-row Error");
        List<Integer> updateAttempts = new ArrayList<>();
        Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
            LoadTask row = invocation.getArgument(0);
            updateAttempts.add(row.getId());
            if (row.getId() == 100) {
                throw metadataFailure;
            }
            return 1;
        });
        ReflectionTestUtils.setField(service, "mapper", mapper);
        List<HugeGraphLoader> loaders = new ArrayList<>();
        List<ExecutorService> pools = new ArrayList<>();
        Map<Integer, LoadTask> retained = new LinkedHashMap<>();
        Throwable primary = null;
        ThreadPoolTaskExecutor executor = null;
        try {
            for (int i = 1; i <= 2; i++) {
                LoadOptions options = new LoadOptions();
                LoadContext nativeContext = Mockito.spy(LoadContext.forOffline(options, new SchemaCache(null)));
                TaskManager manager = new TaskManager(nativeContext);
                HugeGraphLoader loader = this.nativeLoader(nativeContext, manager, options);
                loaders.add(loader);
                for (String field : new String[]{"batchService", "singleService"}) {
                    ExecutorService pool = (ExecutorService) ReflectionTestUtils.getField(manager, field);
                    pools.add(pool);
                    pool.submit(() -> { }).get(10, TimeUnit.SECONDS);
                }
                if (i == 1) {
                    Mockito.doAnswer(invocation -> {
                        invocation.callRealMethod();
                        throw nativeFailure;
                    }).when(nativeContext).close(Mockito.anyBoolean());
                }
                retained.put(i, LoadTask.builder().id(i).loader(loader).options(options).status(LoadStatus.PAUSED)
                                       .finished(true).lastDuration(0L).currDuration(0L).build());
            }
            // This specifically models retained recovery handles. Ordering is
            // explicit so the failure precedes the second actual native close;
            // it does not replace the separate real execute/admission tests.
            ReflectionTestUtils.setField(service, "runningTaskContainer", retained);
            context.getBeanFactory().registerSingleton("loadTaskService", service);
            context.register(HubbleDisposableBean.class, LoadTaskExecutor.class, AsyncConfig.class);
            context.refresh();
            executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
            service.executor = executor;
            context.close();
            assertSame(nativeFailure, service.failure);
            assertFalse(service.pauseCompleted);
            assertTrue("First SDK must execute its actual context close", loaders.get(0).context().closed());
            assertTrue("An earlier Error must not skip the next actual native close",
                       loaders.get(1).context().closed());
            for (ExecutorService pool : pools) {
                assertTrue("Every owned native worker pool must terminate", pool.isTerminated());
            }
            assertEquals(java.util.Arrays.asList(100, 101), updateAttempts);
            assertEquals(LoadStatus.PAUSED, staleRemaining.getStatus());
            assertTrue(java.util.Arrays.asList(nativeFailure.getSuppressed()).contains(metadataFailure));
            assertTrue(retained.get(1).isCleanupFailed());
            Mockito.verify(retained.get(1).context(), Mockito.atLeastOnce()).close(Mockito.anyBoolean());
            assertSame(loaders.get(0), retained.get(1).getLoader());
            assertFalse(retained.containsKey(2));
            assertTrue(executor.getThreadPoolExecutor().isTerminated());
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            Throwable cleanupFailure = null;
            for (HugeGraphLoader loader : loaders) {
                try {
                    loader.shutdown();
                } catch (Throwable cleanup) {
                    if (cleanupFailure == null) {
                        cleanupFailure = cleanup;
                    } else if (cleanupFailure != cleanup) {
                        cleanupFailure.addSuppressed(cleanup);
                    }
                }
            }
            try {
                context.close();
                for (ExecutorService pool : pools) {
                    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
                }
                if (executor != null) {
                    assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
                }
            } catch (Throwable cleanup) {
                if (cleanupFailure == null) {
                    cleanupFailure = cleanup;
                } else if (cleanupFailure != cleanup) {
                    cleanupFailure.addSuppressed(cleanup);
                }
            }
            if (cleanupFailure != null) {
                if (primary == null) {
                    throw cleanupFailure;
                }
                if (primary != cleanupFailure) {
                    primary.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    @Test
    public void testStartSaveFailureClosesNativeResourcesWithoutPoisoningClose() throws Throwable {
        this.preAdmissionFailure(Preparation.START, false);
    }

    @Test
    public void testStartSaveAndCleanupFailureRetainsNullIdTicket() throws Throwable {
        this.preAdmissionFailure(Preparation.START, true);
    }

    @Test
    public void testResumeUpdateFailureClosesNativeResourcesWithoutPoisoningClose() throws Throwable {
        this.preAdmissionFailure(Preparation.RESUME, false);
    }

    @Test
    public void testResumeUpdateAndCleanupFailureRetainsRecoveryHandle() throws Throwable {
        this.preAdmissionFailure(Preparation.RESUME, true);
    }

    @Test
    public void testRetryUpdateFailureClosesNativeResourcesWithoutPoisoningClose() throws Throwable {
        this.preAdmissionFailure(Preparation.RETRY, false);
    }

    @Test
    public void testRetryUpdateAndCleanupFailureRetainsRecoveryHandle() throws Throwable {
        this.preAdmissionFailure(Preparation.RETRY, true);
    }

    private void preAdmissionFailure(Preparation preparation, boolean cleanupFails) throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        HikariDataSource datasource = new HikariDataSource();
        datasource.setJdbcUrl("jdbc:h2:mem:preparation-" + preparation + "-" + cleanupFails);
        datasource.setMinimumIdle(0);
        context.registerBean("dataSource", HikariDataSource.class, () -> datasource);
        ObservedLoadTaskService service = new ObservedLoadTaskService();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.selectList(null)).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(service, "mapper", mapper);
        context.getBeanFactory().registerSingleton("loadTaskService", service);
        context.register(HubbleDisposableBean.class, LoadTaskExecutor.class, AsyncConfig.class);
        context.refresh();
        ThreadPoolTaskExecutor executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        service.executor = executor;
        ReflectionTestUtils.setField(service, "taskExecutor", context.getBean(LoadTaskExecutor.class));
        IllegalStateException metadataFailure = new IllegalStateException("controlled preparation metadata failure");
        IllegalStateException cleanupFailure =
                new IllegalStateException("controlled native preparation cleanup failure");
        AtomicReference<LoadTask> prepared = new AtomicReference<>();
        AtomicReference<HugeGraphLoader> ownedLoader = new AtomicReference<>();
        List<ExecutorService> nativePools = new ArrayList<>();
        java.util.function.Consumer<LoadTask> capture = task -> {
            prepared.set(task);
            HugeGraphLoader loader = task.getLoader();
            ownedLoader.set(loader);
            TaskManager manager = (TaskManager) ReflectionTestUtils.getField(loader, "manager");
            ExecutorService batch = (ExecutorService) ReflectionTestUtils.getField(manager, "batchService");
            ExecutorService single = (ExecutorService) ReflectionTestUtils.getField(manager, "singleService");
            nativePools.add(batch);
            nativePools.add(single);
            try {
                batch.submit(() -> { }).get(10, TimeUnit.SECONDS);
                single.submit(() -> { }).get(10, TimeUnit.SECONDS);
                if (cleanupFails) {
                    LoadContext controlled = Mockito.spy(loader.context());
                    Mockito.doAnswer(invocation -> {
                        invocation.callRealMethod();
                        throw cleanupFailure;
                    }).when(controlled).close(Mockito.anyBoolean());
                    ReflectionTestUtils.setField(loader, "context", controlled);
                }
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        };
        Mockito.when(mapper.insert(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
            capture.accept(invocation.getArgument(0));
            throw metadataFailure;
        });
        Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
            capture.accept(invocation.getArgument(0));
            throw metadataFailure;
        });
        Path directory = Files.createTempDirectory("hubble-preparation-");
        int batchThreads = (Integer) ReflectionTestUtils.getField(GlobalExecutorManager.class, "batchThreadCount");
        int singleThreads = (Integer) ReflectionTestUtils.getField(GlobalExecutorManager.class, "singleThreadCount");
        Throwable primary = null;
        try (MockedStatic<HugeClientHolder> clients = Mockito.mockStatic(HugeClientHolder.class);
             MockedStatic<LoadMapping> mappings = Mockito.mockStatic(LoadMapping.class)) {
            // Actual start constructs LoadTask, LoadContext, TaskManager and SDK.
            // Control only parsing and the client factory, so no server request occurs.
            mappings.when(() -> LoadMapping.of(Mockito.anyString()))
                    .thenReturn(new LoadMapping(Collections.emptyList()));
            RuntimeException observed = null;
            try {
                if (preparation == Preparation.START) {
                    FileSetting setting = new FileSetting();
                    setting.setColumnNames(Collections.singletonList("name"));
                    FileMapping mapping = new FileMapping().setId(1).setJobId(1).setName("controlled.csv")
                            .setPath(directory.resolve("controlled.csv").toString()).setFileSetting(setting)
                            .setLoadParameter(new LoadParameter()).setVertexMappings(Collections.emptySet())
                            .setEdgeMappings(Collections.emptySet());
                    GraphConnection connection = new GraphConnection();
                    connection.setId(1);
                    connection.setGraph("hugegraph");
                    mapping.setGraph("hugegraph");
                    JobManagerMapper parents = Mockito.mock(JobManagerMapper.class);
                    Mockito.when(parents.selectById(1)).thenReturn(
                            JobManager.builder().id(1).graph("hugegraph").build());
                    FileMappingMapper currentMappings = Mockito.mock(FileMappingMapper.class);
                    Mockito.when(currentMappings.selectById(1)).thenReturn(mapping);
                    ReflectionTestUtils.setField(service, "jobManagerMapper", parents);
                    ReflectionTestUtils.setField(service, "fileMappingMapper", currentMappings);
                    service.start(connection, mapping, null);
                } else {
                    LoadOptions options = new LoadOptions();
                    LoadContext nativeContext = LoadContext.forOffline(options, new SchemaCache(null));
                    HugeGraphLoader loader = this.nativeLoader(nativeContext, new TaskManager(nativeContext), options);
                    LoadTask task = Mockito.spy(LoadTask.builder().id(41).options(options)
                            .status(preparation == Preparation.RESUME ? LoadStatus.PAUSED : LoadStatus.STOPPED)
                            .lastDuration(0L).currDuration(0L).build());
                    Mockito.doAnswer(invocation -> {
                        task.setLoader(loader);
                        return null;
                    }).when(task).reconnect("controlled-session");
                    Mockito.when(mapper.selectById(41)).thenReturn(task);
                    if (preparation == Preparation.RESUME) {
                        service.resume(41, "controlled-session");
                    } else {
                        service.retry(41, "controlled-session");
                    }
                }
            } catch (RuntimeException failure) {
                observed = failure;
            }
            assertSame("Frontend must retain the original persistence cause", metadataFailure, observed);
            LoadTask task = prepared.get();
            assertNotNull(task);
            HugeGraphLoader loader = ownedLoader.get();
            assertTrue("Preparation failure must close the actual native context", loader.context().closed());
            if (cleanupFails) {
                Mockito.verify(loader.context(), Mockito.atLeastOnce()).close(Mockito.anyBoolean());
            }
            for (ExecutorService pool : nativePools) {
                assertTrue("Preparation failure must terminate actual native workers", pool.isTerminated());
            }
            assertTrue(task.isFinished());
            assertEquals(cleanupFails, task.isCleanupFailed());
            Map<?, ?> runtime = (Map<?, ?>) ReflectionTestUtils.getField(service, "runningTaskContainer");
            if (cleanupFails) {
                assertSame(loader, task.getLoader());
                assertTrue(java.util.Arrays.asList(observed.getSuppressed()).contains(cleanupFailure));
                assertEquals(preparation == Preparation.START ? 0 : 1, runtime.size());
                if (preparation != Preparation.START) {
                    assertSame(task, runtime.get(41));
                }
            } else {
                assertNull(task.getLoader());
                assertTrue(runtime.isEmpty());
            }
            context.close();
            assertSame(cleanupFails ? metadataFailure : null, service.failure);
            assertEquals(!cleanupFails, service.pauseCompleted);
            assertTrue(datasource.isClosed());
            assertTrue(executor.getThreadPoolExecutor().isTerminated());
            Mockito.verify(mapper, Mockito.never()).update(Mockito.isNull(), Mockito.any());
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                if (ownedLoader.get() != null) {
                    ownedLoader.get().shutdown();
                }
                context.close();
                for (ExecutorService pool : nativePools) {
                    assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
                }
                assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
                try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                    for (Path path : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator) {
                        Files.delete(path);
                    }
                }
            } catch (Throwable cleanup) {
                if (primary == null) {
                    throw cleanup;
                }
                primary.addSuppressed(cleanup);
            } finally {
                GlobalExecutorManager.setBatchThreadCount(batchThreads);
                GlobalExecutorManager.setSingleThreadCount(singleThreads);
            }
        }
    }

    private HugeGraphLoader nativeLoader(LoadContext context, TaskManager manager, LoadOptions options)
            throws Exception {
        // Match the existing Loader TaskManagerTest native fixture: bypass only
        // HTTP client construction, then exercise the actual final SDK shutdown.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        HugeGraphLoader loader = (HugeGraphLoader) unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), HugeGraphLoader.class);
        ReflectionTestUtils.setField(loader, "context", context);
        ReflectionTestUtils.setField(loader, "manager", manager);
        ReflectionTestUtils.setField(loader, "options", options);
        return loader;
    }

    private void closeLoads(boolean terminal, boolean failures, Admission admission) throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ObservedLoadTaskService service = new ObservedLoadTaskService();
        HikariDataSource metadata = new HikariDataSource();
        metadata.setJdbcUrl("jdbc:h2:mem:async-shutdown-" + admission + "-" + terminal + "-" + failures);
        metadata.setMinimumIdle(0);
        metadata.setMaximumPoolSize(2);
        context.registerBean("dataSource", HikariDataSource.class, () -> metadata);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(metadata));
        List<LoadTask> tasks = new ArrayList<>();
        List<LoadContext> loadContexts = new ArrayList<>();
        List<HugeGraphLoader> loaders = new ArrayList<>();
        List<CountDownLatch> stopRequests = new ArrayList<>();
        Map<Integer, LoadTask> running = new ConcurrentHashMap<>();
        Map<Integer, LoadStatus> writtenStatuses = new ConcurrentHashMap<>();
        Set<Thread> workers = ConcurrentHashMap.newKeySet();
        CountDownLatch started = new CountDownLatch(8);
        CountDownLatch pauseRequests = new CountDownLatch(terminal ||
                admission == Admission.ROLLBACK_CLEANUP_HELD ? 8 : 9);
        CountDownLatch cleanupUnlocked = new CountDownLatch(1);
        CountDownLatch rollbackPaused = new CountDownLatch(1);
        CountDownLatch releaseCleanup = new CountDownLatch(1);
        CountDownLatch admissionEntered = new CountDownLatch(1);
        CountDownLatch releaseAdmission = new CountDownLatch(1);
        CountDownLatch markerEntered = new CountDownLatch(1);
        CountDownLatch releaseMarker = new CountDownLatch(1);
        CountDownLatch firstWritten = new CountDownLatch(1);
        CountDownLatch earlyInvocationReturned = new CountDownLatch(1);
        AtomicBoolean earlyFailureThrown = new AtomicBoolean();
        IllegalStateException earlierWriteFailure = new IllegalStateException("controlled pre-close callback failure");
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean cleanup = new AtomicBoolean();
        AtomicInteger localLoaderRuns = new AtomicInteger();
        LoadTaskMapper mapper = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(mapper.selectList(null)).thenAnswer(invocation -> {
            List<LoadTask> rows = new ArrayList<>();
            for (LoadTask runtime : tasks) {
                LoadTask row = new LoadTask();
                row.setId(runtime.getId());
                row.setStatus(LoadStatus.RUNNING);
                rows.add(row);
            }
            Collections.reverse(rows); // The queued row comes first.
            return rows;
        });
        Mockito.when(mapper.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
            LoadTask task = invocation.getArgument(0);
            if (task.getId() == 1 && !terminal) {
                callbackEntered.countDown();
                assertTrue("Controlled metadata callback must be released",
                           releaseCallback.await(20, TimeUnit.SECONDS));
            }
            if (task.getId() == 1 && admission == Admission.PRE_CLOSE_CALLBACK_FAILED) {
                earlyFailureThrown.set(true);
                throw earlierWriteFailure;
            }
            writtenStatuses.put(task.getId(), task.getStatus());
            if (task.getId() == 1) {
                firstWritten.countDown();
            }
            if (failures && (task.getId() == 2 || task.getId() == 3)) {
                throw new IllegalStateException("controlled update " + task.getId());
            }
            return 1;
        });
        Mockito.when(mapper.update(Mockito.isNull(), Mockito.any())).thenAnswer(invocation -> {
            markerEntered.countDown();
            assertTrue(releaseMarker.await(20, TimeUnit.SECONDS));
            if (admission == Admission.MARKER_FAILED_HELD) {
                throw new IllegalStateException("controlled failure marker");
            }
            writtenStatuses.put(9, LoadStatus.FAILED);
            return 1;
        });
        ReflectionTestUtils.setField(service, "transactionManager", transaction.getTransactionManager());
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "runningTaskContainer", running);
        context.getBeanFactory().registerSingleton("loadTaskService", service);
        context.registerBean("lifecycleProcessor", DefaultLifecycleProcessor.class, () -> {
            DefaultLifecycleProcessor processor = new DefaultLifecycleProcessor();
            // Bound the unfixed phase wait in this test, not production policy.
            processor.setTimeoutPerShutdownPhase(200);
            return processor;
        });
        // Adversarial registration order: the pause hook is registered before
        // the executor factory, so incidental reverse creation order is unsafe.
        context.register(HubbleDisposableBean.class, LoadTaskExecutor.class, AsyncConfig.class);
        context.refresh();
        // The controlled Mapper replaces MyBatis I/O; preserve its production
        // transitive dependency on the datasource in this real bean lifecycle.
        context.getBeanFactory().registerDependentBean("dataSource", "hubbleDisposableBean");
        ThreadPoolTaskExecutor executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        service.executor = executor;
        executor.setTaskDecorator(invocation -> () -> {
            try {
                invocation.run();
            } finally {
                if (earlyFailureThrown.get()) {
                    earlyInvocationReturned.countDown();
                }
            }
        });
        LoadTaskExecutor dispatcher = context.getBean(LoadTaskExecutor.class);
        ReflectionTestUtils.setField(service, "taskExecutor", dispatcher);
        ExecutorService closer = Executors.newSingleThreadExecutor();
        ExecutorService submitter = Executors.newSingleThreadExecutor();
        Future<?> submitting = null;
        Future<?> closing = null;
        CompletableFuture<?> pending = null;
        Throwable primary = null;
        try {
            assertEquals(8, executor.getCorePoolSize());
            assertEquals(16, executor.getMaxPoolSize());
            assertEquals(1024, executor.getQueueCapacity());
            for (int id = 1; id <= 9; id++) {
                LoadOptions options = new LoadOptions();
                LoadContext loadContext = Mockito.spy(LoadContext.forOffline(options, new SchemaCache(null)));
                CountDownLatch stopRequested = new CountDownLatch(1);
                final int taskId = id;
                Mockito.doAnswer(invocation -> {
                    invocation.callRealMethod();
                    stopRequested.countDown();
                    pauseRequests.countDown();
                    if (failures && taskId == 4) {
                        throw new IllegalStateException("controlled stop after native signal");
                    }
                    return null;
                }).when(loadContext).stopLoading();
                HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
                Mockito.when(loader.context()).thenReturn(loadContext);
                Mockito.doAnswer(invocation -> {
                    if (loadContext.closed()) {
                        return null;
                    }
                    loadContext.close();
                    if (taskId == 9 && admission == Admission.CLEANUP_FAILED_HELD) {
                        throw new IllegalStateException("controlled cleanup after native close");
                    }
                    return null;
                }).when(loader).shutdown();
                Mockito.when(loader.load()).thenAnswer(invocation -> {
                    if (!cleanup.get()) {
                        workers.add(Thread.currentThread());
                    }
                    localLoaderRuns.incrementAndGet();
                    started.countDown();
                    try {
                        assertTrue("Controlled Loader must receive its actual stop request",
                                   stopRequested.await(20, TimeUnit.SECONDS));
                        return true;
                    } catch (InterruptedException e) {
                        interrupted.set(true);
                        throw e;
                    } finally {
                        localLoaderRuns.decrementAndGet();
                    }
                });
                LoadTask task = Mockito.spy(new LoadTask());
                task.setId(id);
                task.setStatus(LoadStatus.RUNNING);
                task.setOptions(options);
                task.setLastDuration(0L);
                task.setCurrDuration(0L);
                task.setLoader(loader);
                if (taskId == 9 && admission == Admission.ROLLBACK_CLEANUP_HELD) {
                    Mockito.doAnswer(invocation -> {
                        invocation.callRealMethod();
                        rollbackPaused.countDown();
                        return null;
                    }).when(task).setStatus(LoadStatus.PAUSED);
                    Mockito.doAnswer(invocation -> {
                        invocation.callRealMethod();
                        if (task.getLoader() == null && !cleanup.get()) {
                            cleanupUnlocked.countDown();
                            assertTrue(releaseCleanup.await(20, TimeUnit.SECONDS));
                        }
                        return null;
                    }).when(task).unlock();
                }
                tasks.add(task);
                loadContexts.add(loadContext);
                loaders.add(loader);
                stopRequests.add(stopRequested);
                // Exercise the real service admission path; never prefill its map.
                if (id == 9 && admission != Admission.DIRECT && admission != Admission.PRE_CLOSE_CALLBACK_FAILED) {
                    if (admission == Admission.SUBMISSION_HELD) {
                        LoadTaskExecutor heldDispatcher = Mockito.mock(LoadTaskExecutor.class);
                        Mockito.doAnswer(invocation -> {
                            dispatcher.execute(invocation.getArgument(0), invocation.getArgument(1));
                            admissionEntered.countDown();
                            assertTrue(releaseAdmission.await(20, TimeUnit.SECONDS));
                            return null;
                        }).when(heldDispatcher).execute(Mockito.any(), Mockito.any());
                        ReflectionTestUtils.setField(service, "taskExecutor", heldDispatcher);
                    } else if (admission.dispatchRejected()) {
                        LoadTaskExecutor rejecting = Mockito.mock(LoadTaskExecutor.class);
                        Mockito.doThrow(new RejectedExecutionException("controlled dispatch rejection"))
                               .when(rejecting).execute(Mockito.any(), Mockito.any());
                        ReflectionTestUtils.setField(service, "taskExecutor", rejecting);
                    } else if (admission == Admission.RESUME_HELD || admission == Admission.RETRY_HELD) {
                        task.setStatus(admission == Admission.RESUME_HELD ? LoadStatus.PAUSED : LoadStatus.STOPPED);
                        Mockito.when(mapper.selectById(9)).thenReturn(task);
                        Mockito.doAnswer(invocation -> {
                            // Control only reconnect's native construction boundary.
                            admissionEntered.countDown();
                            assertTrue(releaseAdmission.await(20, TimeUnit.SECONDS));
                            return null;
                        }).when(task).reconnect("controlled-session");
                    }
                    submitting = submitter.submit(() -> {
                        if (admission == Admission.SUBMISSION_HELD) {
                            ReflectionTestUtils.invokeMethod(service, "execute", task);
                        } else if (admission == Admission.RESUME_HELD) {
                            service.resume(9, "controlled-session");
                        } else if (admission == Admission.RETRY_HELD) {
                            service.retry(9, "controlled-session");
                        } else {
                            transaction.execute(status -> {
                                ReflectionTestUtils.invokeMethod(service, "executeAfterCommit", task);
                                admissionEntered.countDown();
                                try {
                                    assertTrue(releaseAdmission.await(20, TimeUnit.SECONDS));
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new IllegalStateException(e);
                                }
                                if (admission.rollback()) {
                                    status.setRollbackOnly();
                                }
                                return null;
                            });
                        }
                        return null;
                    });
                    assertTrue(admissionEntered.await(10, TimeUnit.SECONDS));
                    if (admission.transactional()) {
                        Set<?> accepted = (Set<?>) ReflectionTestUtils.getField(service, "acceptedRuns");
                        for (Object run : accepted) {
                            if (ReflectionTestUtils.getField(run, "task") == task) {
                                pending = (CompletableFuture<?>) ReflectionTestUtils.getField(run, "completed");
                            }
                        }
                        assertNotNull(pending);
                        assertFalse(pending.isDone());
                    }
                    if (admission == Admission.SUBMISSION_HELD) {
                        assertTrue("Accepted runtime must be published before submission returns/commit",
                                   running.get(9) == task);
                    } else if (admission.transactional()) {
                        assertFalse("Uncommitted row must not enter scheduled progress updates",
                                    running.containsKey(9));
                    }
                } else {
                    ReflectionTestUtils.invokeMethod(service, "execute", task);
                }
            }
            assertTrue("All core workers must run before close", started.await(10, TimeUnit.SECONDS));
            assertEquals(8, workers.size());
            int queued = admission != Admission.DIRECT && admission != Admission.SUBMISSION_HELD &&
                         admission != Admission.PRE_CLOSE_CALLBACK_FAILED ? 0 : 1;
            assertEquals(queued, executor.getThreadPoolExecutor().getQueue().size());
            if (admission == Admission.PRE_CLOSE_CALLBACK_FAILED) {
                Set<?> accepted = (Set<?>) ReflectionTestUtils.getField(service, "acceptedRuns");
                for (Object run : accepted) {
                    if (ReflectionTestUtils.getField(run, "task") == tasks.get(0)) {
                        pending = (CompletableFuture<?>) ReflectionTestUtils.getField(run, "completed");
                    }
                }
                assertNotNull(pending);
            }
            if (terminal) {
                // Natural completion precedes a stale RUNNING database snapshot.
                stopRequests.get(0).countDown();
                if (admission == Admission.PRE_CLOSE_CALLBACK_FAILED) {
                    assertTrue("Entire async invocation must finish before taking shutdown snapshot",
                               earlyInvocationReturned.await(10, TimeUnit.SECONDS));
                    assertTrue(pending.isCompletedExceptionally());
                } else {
                    assertTrue(firstWritten.await(10, TimeUnit.SECONDS));
                }
                assertEquals(LoadStatus.SUCCEED, tasks.get(0).getStatus());
            }
            if (admission == Admission.ROLLBACK_CLEANUP_HELD) {
                releaseAdmission.countDown();
                assertTrue(cleanupUnlocked.await(10, TimeUnit.SECONDS));
                assertFalse("Cleanup finished but its accepted ticket still belongs to shutdown", pending.isDone());
            }
            closing = closer.submit(context::close);
            assertTrue("The real Hubble pause hook must run", service.entered.await(10, TimeUnit.SECONDS));
            assertFalse("Pause hook must precede native pool shutdown", service.poolShutdownAtPause);
            assertFalse("Phase stop must not block workers before their pause hook", service.phaseStoppedAtPause);
            if (admission != Admission.DIRECT && admission != Admission.PRE_CLOSE_CALLBACK_FAILED) {
                assertFalse("Close cannot pass an admission/transaction still in progress", closing.isDone());
                if (admission.transactional() && admission != Admission.ROLLBACK_CLEANUP_HELD) {
                    assertTrue("Pending transactional run must receive stop before commit/rollback",
                               stopRequests.get(8).await(10, TimeUnit.SECONDS));
                }
                if (admission == Admission.ROLLBACK_CLEANUP_HELD) {
                    // Hook entry precedes the snapshot; keep the rollback ticket until it is paused.
                    assertTrue(rollbackPaused.await(10, TimeUnit.SECONDS));
                }
                releaseAdmission.countDown();
                releaseCleanup.countDown();
                if (admission.dispatchRejected()) {
                    assertTrue(markerEntered.await(10, TimeUnit.SECONDS));
                    assertFalse("Completion ticket includes the actual failure marker attempt", pending.isDone());
                    assertFalse(metadata.isClosed());
                    releaseMarker.countDown();
                }
                submitting.get(10, TimeUnit.SECONDS);
            }
            assertTrue("All accepted tasks must receive pause before any completion wait",
                       pauseRequests.await(3, TimeUnit.SECONDS));
            if (!terminal) {
                assertTrue("The actual metadata callback must run", callbackEntered.await(10, TimeUnit.SECONDS));
                assertFalse("Close must wait for metadata callback completion", closing.isDone());
                assertFalse(service.pauseCompleted);
                assertFalse("Datasource must outlive the actual metadata callback", metadata.isClosed());
                releaseCallback.countDown();
            }
            closing.get(20, TimeUnit.SECONDS);
            if (admission == Admission.PRE_CLOSE_CALLBACK_FAILED) {
                assertFalse("Earlier failed metadata callback cannot become successful shutdown",
                            service.pauseCompleted);
                assertSame("Shutdown must retain the original mapper cause", earlierWriteFailure, service.failure);
                assertTrue("Unpersisted runtime must remain available after failed reconciliation",
                           running.get(1) == tasks.get(0));
                assertNull(writtenStatuses.get(1));
            } else if (admission.dispatchRejected()) {
                boolean unresolved = admission == Admission.MARKER_FAILED_HELD ||
                                     admission == Admission.CLEANUP_FAILED_HELD;
                assertEquals(!unresolved, service.pauseCompleted);
                assertEquals(unresolved, pending.isCompletedExceptionally());
                if (unresolved) {
                    assertNotNull(service.failure);
                    assertEquals("controlled dispatch rejection", service.failure.getMessage());
                    assertEquals(1, service.failure.getSuppressed().length);
                } else {
                    assertNull(service.failure);
                }
            } else if (failures) {
                assertFalse(service.pauseCompleted);
                assertNotNull(service.failure);
                assertEquals("controlled stop after native signal", service.failure.getMessage());
                assertEquals(2, service.failure.getSuppressed().length);
            } else {
                assertTrue(service.pauseCompleted);
                assertNull(service.failure);
            }
            assertTrue("Datasource closes after the pause hook completes", metadata.isClosed());
            assertFalse("Context close must not interrupt a controlled Loader", interrupted.get());
            for (LoadTask task : tasks) {
                boolean unstarted = (admission.rollback() || admission.dispatchRejected()) &&
                                    task.getId() == 9;
                assertTrue("A runner or undispatched cleanup attempt has completed", task.isFinished());
                LoadStatus expected = terminal && task.getId() == 1 ? LoadStatus.SUCCEED : LoadStatus.PAUSED;
                if (admission.dispatchRejected() && task.getId() == 9) {
                    expected = LoadStatus.FAILED;
                }
                assertEquals(expected, task.getStatus());
                // Mapper is a controlled boundary: this is a PAUSED update
                // invocation, not an independent H2/database readback claim.
                boolean noUpdate = (admission.rollback() || admission == Admission.MARKER_FAILED_HELD) &&
                                   task.getId() == 9;
                noUpdate |= admission == Admission.PRE_CLOSE_CALLBACK_FAILED && task.getId() == 1;
                if (!noUpdate) {
                    assertEquals(expected, writtenStatuses.get(task.getId()));
                }
                if (admission == Admission.CLEANUP_FAILED_HELD && task.getId() == 9) {
                    assertTrue(task.isCleanupFailed());
                    assertNotNull("Unsafe cleanup must retain its recovery handle", task.getLoader());
                } else {
                    assertNull("Safely closed task must release its Loader handle", task.getLoader());
                }
            }
            if (admission.transactional()) {
                Mockito.verify(loaders.get(8), Mockito.never()).load();
                Mockito.verify(loaders.get(8), Mockito.atLeastOnce()).shutdown();
                assertTrue(loadContexts.get(8).closed());
            }
            if (admission == Admission.PRE_CLOSE_CALLBACK_FAILED) {
                assertEquals(1, running.size());
            } else if (failures) {
                assertEquals(2, running.size());
                assertTrue(running.containsKey(2));
                assertTrue(running.containsKey(3));
            } else if (admission == Admission.MARKER_FAILED_HELD ||
                       admission == Admission.CLEANUP_FAILED_HELD) {
                assertEquals(1, running.size());
                assertTrue(running.containsKey(9));
            } else {
                assertTrue(running.isEmpty());
            }
            this.assertRejected(() -> ReflectionTestUtils.invokeMethod(service, "execute", new LoadTask()));
            this.assertRejected(() -> service.start(null, null, null));
            this.assertRejected(() -> service.resume(9, "controlled-session"));
            this.assertRejected(() -> service.retry(9, "controlled-session"));
            Mockito.verify(mapper, Mockito.atLeastOnce()).selectList(null);
            assertEquals(0, localLoaderRuns.get());
            assertTrue(executor.getThreadPoolExecutor().isShutdown());
            assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
            for (Thread worker : workers) {
                worker.join(TimeUnit.SECONDS.toMillis(10));
                assertFalse("Owned async worker must terminate", worker.isAlive());
            }
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            cleanup.set(true);
            releaseCallback.countDown();
            releaseAdmission.countDown();
            releaseMarker.countDown();
            releaseCleanup.countDown();
            for (CountDownLatch stopRequest : stopRequests) {
                stopRequest.countDown();
            }
            Throwable cleanupFailure = null;
            try {
                // Drain only this fixture on RED. A discarded queued task must
                // still finish so the real pause/stop loop can return.
                executor.shutdown();
                assertTrue("Owned pool must terminate during cleanup",
                           executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
                for (LoadTask task : tasks) {
                    if (!task.isFinished() && task.getLoader() != null) {
                        task.run();
                    }
                }
                if (closing != null) {
                    closing.get(10, TimeUnit.SECONDS);
                } else {
                    context.close();
                }
            } catch (Throwable failure) {
                cleanupFailure = failure;
            } finally {
                try {
                    submitter.shutdownNow();
                    assertTrue("Owned admission worker must terminate",
                               submitter.awaitTermination(10, TimeUnit.SECONDS));
                    closer.shutdownNow();
                    assertTrue("Owned context-close worker must terminate",
                               closer.awaitTermination(10, TimeUnit.SECONDS));
                } catch (Throwable failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else if (cleanupFailure != failure) {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
                for (LoadContext loadContext : loadContexts) {
                    try {
                        loadContext.close();
                    } catch (Throwable failure) {
                        if (cleanupFailure == null) {
                            cleanupFailure = failure;
                        } else if (cleanupFailure != failure) {
                            cleanupFailure.addSuppressed(failure);
                        }
                    }
                }
            }
            if (cleanupFailure != null) {
                if (primary == null) {
                    throw cleanupFailure;
                }
                if (primary != cleanupFailure) {
                    primary.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    private void assertRejected(Runnable admission) {
        try {
            admission.run();
            throw new AssertionError("Closing service must reject new admission");
        } catch (RejectedExecutionException expected) {
            // Rejection precedes runtime construction, database mutation or submission.
        }
    }

    private enum Preparation {
        START, RESUME, RETRY
    }

    private enum NativeRequest {
        NONE, PAUSE, STOP, RESTART_GUARD, CANCELLED_CALLBACK
    }

    private enum Admission {
        DIRECT, SUBMISSION_HELD, RESUME_HELD, RETRY_HELD,
        COMMIT_HELD, ROLLBACK_HELD, REJECTED_HELD, MARKER_FAILED_HELD,
        CLEANUP_FAILED_HELD, ROLLBACK_CLEANUP_HELD, PRE_CLOSE_CALLBACK_FAILED;

        private boolean transactional() {
            return this == COMMIT_HELD || this.rollback() || this.dispatchRejected();
        }

        private boolean rollback() {
            return this == ROLLBACK_HELD || this == ROLLBACK_CLEANUP_HELD;
        }

        private boolean dispatchRejected() {
            return this == REJECTED_HELD || this == MARKER_FAILED_HELD || this == CLEANUP_FAILED_HELD;
        }
    }

    private static class ObservedLoadTaskService extends LoadTaskService {

        private final CountDownLatch entered = new CountDownLatch(1);
        private ThreadPoolTaskExecutor executor;
        private volatile boolean poolShutdownAtPause;
        private volatile boolean phaseStoppedAtPause;
        private volatile boolean pauseCompleted;
        private volatile Throwable failure;

        @Override
        public void pauseAllTasks() {
            this.poolShutdownAtPause = this.executor.getThreadPoolExecutor().isShutdown();
            this.phaseStoppedAtPause = !this.executor.isRunning();
            this.entered.countDown();
            try {
                super.pauseAllTasks();
                this.pauseCompleted = true;
            } catch (RuntimeException | Error e) {
                this.failure = e;
                throw e;
            }
        }
    }
}
