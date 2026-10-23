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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.config.AsyncConfig;
import org.apache.hugegraph.config.H2DataSourceConfig;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.handler.HubbleDisposableBean;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.mapper.load.FileMappingMapper;
import org.apache.hugegraph.mapper.load.JobManagerMapper;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;
import org.apache.hugegraph.mapper.query.ExecuteHistoryMapper;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.env.MockEnvironment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.function.SingletonSupplier;

public class AsyncConfigLifecycleTest {

    @Test
    public void testContextCloseTerminatesAsyncWorkers() throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("dataSource", HikariDataSource.class, () -> new HikariDataSource());
        context.register(AsyncConfig.class, AsyncProbe.class);
        context.refresh();
        CountDownLatch started = new CountDownLatch(8);
        CountDownLatch release = new CountDownLatch(1);
        Set<Thread> workers = ConcurrentHashMap.newKeySet();
        ThreadPoolTaskExecutor executor = null;
        Throwable primaryFailure = null;
        try {
            AsyncProbe probe = context.getBean(AsyncProbe.class);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            futures.add(probe.execute(started, release, workers));
            executor = configuredExecutor(probe);
            for (int i = 1; i < 8; i++) {
                futures.add(probe.execute(started, release, workers));
            }
            assertTrue("All eight core workers must start", started.await(10, TimeUnit.SECONDS));
            assertEquals(8, workers.size());
            assertEquals(8, executor.getPoolSize());
            for (Thread worker : workers) {
                assertFalse(worker.isDaemon());
            }
            release.countDown();
            for (CompletableFuture<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            assertFalse(executor.getThreadPoolExecutor().isShutdown());
            AsyncConfig config = context.getBean(AsyncConfig.class);
            boolean managed = context.containsBean("getAsyncExecutor");
            if (managed) {
                assertSame(executor, context.getBean("getAsyncExecutor"));
                assertSame(executor, config.getAsyncExecutor());
                assertSame(executor, config.getAsyncExecutor());
            }
            context.close();

            assertTrue("Context close must shut down the actual async executor",
                       executor.getThreadPoolExecutor().isShutdown());
            assertTrue("The drained executor must terminate",
                       executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
            for (Thread worker : workers) {
                worker.join(TimeUnit.SECONDS.toMillis(10));
                assertFalse("The owned async worker must exit: " + worker.getName(), worker.isAlive());
            }
            assertTrue("The async executor must be a managed Spring bean", managed);
        } catch (Throwable failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            release.countDown();
            Throwable cleanupFailure = null;
            try {
                context.close();
            } catch (Throwable failure) {
                cleanupFailure = failure;
            } finally {
                if (executor != null) {
                    try {
                        // The unfixed configuration is unmanaged; clean up this test's own pool on RED.
                        executor.shutdown();
                        assertTrue("Test-owned executor cleanup must terminate",
                                   executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
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
                if (primaryFailure == null) {
                    throw cleanupFailure;
                }
                if (primaryFailure != cleanupFailure) {
                    primaryFailure.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    @Test
    public void testCloseDrainsActualScheduledMapperMethodsBeforeDatasource() throws Throwable {
        this.closeScheduledMetadata(false);
    }

    @Test
    public void testInterruptedCloseStillDrainsScheduledMetadataAndRestoresInterrupt() throws Throwable {
        this.closeScheduledMetadata(true);
    }

    private void closeScheduledMetadata(boolean interruptClose) throws Throwable {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:h2:mem:maintenance-drain-" + interruptClose);
        properties.setDriverClassName("org.h2.Driver");
        properties.setUsername("sa");
        properties.setPassword("");
        HikariDataSource datasource = new H2DataSourceConfig().dataSource(properties, new MockEnvironment());
        datasource.setMaximumPoolSize(2);
        datasource.setMinimumIdle(0);
        HugeConfig config = Mockito.mock(HugeConfig.class);
        Mockito.when(config.get(HubbleOptions.UPLOAD_FILE_MAX_TIME_CONSUMING)).thenReturn(3600L);
        Mockito.when(config.get(HubbleOptions.EXECUTE_HISTORY_SHOW_LIMIT)).thenReturn(10);
        FileMappingMapper files = Mockito.mock(FileMappingMapper.class);
        ExecuteHistoryMapper histories = Mockito.mock(ExecuteHistoryMapper.class);
        LoadTaskMapper loads = Mockito.mock(LoadTaskMapper.class);
        Mockito.when(loads.selectList(null)).thenReturn(Collections.emptyList());
        CountDownLatch writesEntered = new CountDownLatch(2);
        CountDownLatch releaseWrites = new CountDownLatch(1);
        CountDownLatch writesFinished = new CountDownLatch(2);
        CountDownLatch nativeAwaitEntered = new CountDownLatch(1);
        List<Connection> physicalConnections = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<Throwable> writeFailure = new AtomicReference<>();
        Runnable metadataWrite = () -> {
            try (Connection connection = datasource.getConnection()) {
                physicalConnections.add(connection.unwrap(Connection.class));
                writesEntered.countDown();
                assertTrue(releaseWrites.await(15, TimeUnit.SECONDS));
                try (java.sql.Statement statement = connection.createStatement()) {
                    statement.execute("SELECT 1");
                }
            } catch (Throwable failure) {
                writeFailure.compareAndSet(null, failure);
                throw new IllegalStateException(failure);
            } finally {
                writesFinished.countDown();
            }
        };
        Mockito.when(files.selectList(null)).thenReturn(Collections.emptyList());
        Mockito.when(files.selectList(Mockito.isNotNull())).thenAnswer(invocation -> {
            metadataWrite.run();
            return Collections.emptyList();
        });
        Mockito.doAnswer(invocation -> {
            metadataWrite.run();
            return null;
        }).when(histories).deleteExceedLimit(Mockito.anyInt());
        context.registerBean("dataSource", HikariDataSource.class, () -> datasource);
        context.registerBean("transactionManager", DataSourceTransactionManager.class,
                             () -> new DataSourceTransactionManager(datasource));
        context.getBeanFactory().registerSingleton("hugeConfig", config);
        context.getBeanFactory().registerSingleton("fileMappingMapper", files);
        context.getBeanFactory().registerSingleton("executeHistoryMapper", histories);
        context.getBeanFactory().registerSingleton("loadTaskMapper", loads);
        context.getBeanFactory().registerSingleton("jobManagerMapper", Mockito.mock(JobManagerMapper.class));
        context.getBeanFactory().registerSingleton("vertexLabelService", Mockito.mock(VertexLabelService.class));
        context.getBeanFactory().registerSingleton("edgeLabelService", Mockito.mock(EdgeLabelService.class));
        context.register(HubbleDisposableBean.class, LoadTaskService.class, LoadTaskExecutor.class,
                         FileMappingService.class, ExecuteHistoryService.class, AsyncConfig.class,
                         SchedulingTestConfig.class);
        context.refresh();
        ThreadPoolTaskExecutor executor = context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        ThreadPoolTaskScheduler scheduler = context.getBean("maintenanceScheduler", ThreadPoolTaskScheduler.class);
        LoadTaskService loadService = context.getBean(LoadTaskService.class);
        ScheduledThreadPoolExecutor actualPool = scheduler.getScheduledThreadPoolExecutor();
        // Delegate every operation to the actual native pool. Only observe entry
        // to its blocking await API; a signal before shutdown is insufficient.
        ScheduledThreadPoolExecutor observedPool = Mockito.mock(ScheduledThreadPoolExecutor.class,
                AdditionalAnswers.delegatesTo(actualPool));
        Mockito.doAnswer(invocation -> {
            nativeAwaitEntered.countDown();
            return actualPool.awaitTermination(invocation.getArgument(0), invocation.getArgument(1));
        }).when(observedPool).awaitTermination(Mockito.anyLong(), Mockito.any(TimeUnit.class));
        ReflectionTestUtils.setField(scheduler, "scheduledExecutor", observedPool);
        ReflectionTestUtils.setField(scheduler, "executor", observedPool);
        ExecutorService closer = Executors.newSingleThreadExecutor();
        AtomicReference<Thread> closeThread = new AtomicReference<>();
        AtomicBoolean closeInterrupted = new AtomicBoolean();
        Future<?> closing = null;
        Throwable primary = null;
        try {
            // The real @Scheduled registration starts both metadata methods
            // immediately; the other scheduled methods have no records to process.
            assertTrue(writesEntered.await(10, TimeUnit.SECONDS));
            closing = closer.submit(() -> {
                closeThread.set(Thread.currentThread());
                context.close();
                closeInterrupted.set(Thread.currentThread().isInterrupted());
            });
            assertTrue("Pool destruction must enter the actual native completion wait",
                       nativeAwaitEntered.await(3, TimeUnit.SECONDS));
            assertEquals(Boolean.TRUE, ReflectionTestUtils.getField(loadService, "closing"));
            assertFalse("Datasource must remain available for running scheduled writes", datasource.isClosed());
            assertFalse("Native drain cannot return while Mapper calls are blocked", closing.isDone());
            if (interruptClose) {
                closeThread.get().interrupt();
            }
            releaseWrites.countDown();
            assertTrue(writesFinished.await(10, TimeUnit.SECONDS));
            closing.get(10, TimeUnit.SECONDS);
            assertEquals(interruptClose, closeInterrupted.get());
            assertEquals(null, writeFailure.get());
            assertTrue(actualPool.isTerminated());
            assertTrue(executor.getThreadPoolExecutor().isTerminated());
            assertTrue(datasource.isClosed());
            // Check the two physical H2 connections actually used by this
            // fixture; Hikari.isClosed alone does not prove physical closure.
            assertEquals(2, physicalConnections.size());
            for (Connection connection : physicalConnections) {
                assertTrue(connection.isClosed());
            }
        } catch (Throwable failure) {
            primary = failure;
            throw failure;
        } finally {
            releaseWrites.countDown();
            Throwable cleanupFailure = null;
            try {
                if (closing == null) {
                    context.close();
                } else {
                    closing.get(10, TimeUnit.SECONDS);
                }
                scheduler.shutdown();
                executor.shutdown();
                assertTrue(actualPool.awaitTermination(10, TimeUnit.SECONDS));
                assertTrue(executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS));
            } catch (Throwable failure) {
                cleanupFailure = failure;
            } finally {
                closer.shutdownNow();
                if (!closer.awaitTermination(10, TimeUnit.SECONDS)) {
                    AssertionError failure = new AssertionError("Owned close worker must terminate");
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
                datasource.close();
            }
            if (cleanupFailure != null) {
                if (primary == null) {
                    throw cleanupFailure;
                }
                primary.addSuppressed(cleanupFailure);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingTestConfig {
    }

    private static ThreadPoolTaskExecutor configuredExecutor(AsyncProbe probe) {
        for (Advisor advisor : ((Advised) probe).getAdvisors()) {
            if (advisor.getAdvice() instanceof AsyncExecutionAspectSupport) {
                // Observe the pool selected by the real @Async dispatcher, including on RED
                // where AsyncConfig does not publish a managed executor bean.
                Object supplier = ReflectionTestUtils.getField(advisor.getAdvice(), "defaultExecutor");
                assertNotNull(supplier);
                return (ThreadPoolTaskExecutor) ((SingletonSupplier<?>) supplier).get();
            }
        }
        throw new AssertionError("The probe must use Spring's async dispatcher");
    }

    public static class AsyncProbe {

        @Async
        public CompletableFuture<Void> execute(CountDownLatch started,
                                                CountDownLatch release,
                                                Set<Thread> workers) throws InterruptedException {
            workers.add(Thread.currentThread());
            started.countDown();
            assertTrue("The controlled async task must be released", release.await(10, TimeUnit.SECONDS));
            return CompletableFuture.completedFuture(null);
        }
    }
}
