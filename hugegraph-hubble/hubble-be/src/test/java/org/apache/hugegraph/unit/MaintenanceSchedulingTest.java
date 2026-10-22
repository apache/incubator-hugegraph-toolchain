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
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hugegraph.config.AsyncConfig;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.config.Text2GqlStreamConfig;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
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
import org.mockito.Mockito;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zaxxer.hikari.HikariDataSource;

public class MaintenanceSchedulingTest {

    @Test
    public void testScheduledMaintenanceRunsWhileLoaderWorkersAreOccupied() throws Exception {
        try (Fixture fixture = new Fixture(false)) {
            fixture.start();

            await(fixture.metadataWritten, "Progress and history cleanup must run before the loaders finish");
            assertTrue(fixture.rows("progress") > 0);
            assertTrue(fixture.rows("history") > 0);
            assertEquals(8L, fixture.loadersFinished.getCount());
            assertEquals(8, fixture.loaderExecutor().getActiveCount());
            assertEquals("Maintenance must not enter the Loader queue", 0,
                         fixture.loaderExecutor().getThreadPoolExecutor().getQueue().size());
        }
    }

    @Test
    public void testSlowMaintenanceDoesNotOverlapOrDelayHeartbeats() throws Exception {
        try (Fixture fixture = new Fixture(true)) {
            fixture.start();
            await(fixture.maintenanceEntered, "All four actual scheduled methods must start independently");
            ThreadPoolTaskScheduler scheduler = fixture.context.getBean(
                    "maintenanceScheduler", ThreadPoolTaskScheduler.class);

            assertEquals(4, scheduler.getActiveCount());
            fixture.observeHeartbeats();
            assertEquals("Blocked periodic tasks must not accumulate subsequent executions", 0,
                         scheduler.getScheduledThreadPoolExecutor().getQueue().size());

            // Leave only progress blocked, with spare maintenance workers available.
            fixture.releaseCleanup.countDown();
            await(fixture.cleanupFinished, "Other maintenance methods must finish independently of progress");
            fixture.observeHeartbeats();
            assertEquals("A slow progress update must not overlap itself", 1,
                         fixture.calls.get("progress").get());
            assertEquals("No duplicate progress invocation may wait for the task lock", 1,
                         scheduler.getActiveCount());
            assertEquals("No maintenance work may accumulate behind the long-running loaders", 0,
                         fixture.loaderExecutor().getThreadPoolExecutor().getQueue().size());
        }
    }

    private static void await(CountDownLatch latch, String message) {
        await(latch, message, 10);
    }

    private static void await(CountDownLatch latch, String message, int seconds) {
        try {
            assertTrue(message, latch.await(seconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(message, e);
        }
    }

    @Configuration
    @EnableScheduling
    @EnableTransactionManagement
    static class Scheduling {
    }

    private static final class Fixture implements AutoCloseable {

        private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        private final HikariDataSource datasource = new HikariDataSource();
        private final JdbcTemplate jdbc = new JdbcTemplate(this.datasource);
        private final CountDownLatch loadersStarted = new CountDownLatch(8);
        private final CountDownLatch loadersFinished = new CountDownLatch(8);
        private final CountDownLatch releaseLoaders = new CountDownLatch(1);
        private final CountDownLatch maintenanceEntered = new CountDownLatch(4);
        private final CountDownLatch metadataWritten = new CountDownLatch(2);
        private final CountDownLatch cleanupFinished = new CountDownLatch(3);
        private final CountDownLatch releaseProgress;
        private final CountDownLatch releaseCleanup;
        private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
        private LoadContext nativeContext;

        private Fixture(boolean blockMaintenance) {
            this.releaseProgress = new CountDownLatch(blockMaintenance ? 1 : 0);
            this.releaseCleanup = new CountDownLatch(blockMaintenance ? 1 : 0);
        }

        private void start() {
            this.datasource.setJdbcUrl("jdbc:h2:mem:maintenance-" + UUID.randomUUID());
            this.datasource.setMaximumPoolSize(4);
            this.datasource.setMinimumIdle(0);
            this.jdbc.execute("CREATE TABLE maintenance_probe(kind VARCHAR(24))");
            this.context.registerBean("dataSource", HikariDataSource.class, () -> this.datasource);
            this.context.registerBean("transactionManager", DataSourceTransactionManager.class,
                                      () -> new DataSourceTransactionManager(this.datasource));

            HugeConfig config = Mockito.mock(HugeConfig.class);
            Mockito.when(config.get(HubbleOptions.EXECUTE_HISTORY_SHOW_LIMIT)).thenReturn(10);
            Mockito.when(config.get(HubbleOptions.UPLOAD_FILE_MAX_TIME_CONSUMING)).thenReturn(3600L);
            LoadTaskMapper loads = Mockito.mock(LoadTaskMapper.class);
            Mockito.when(loads.updateById(Mockito.any(LoadTask.class))).thenAnswer(invocation -> {
                this.maintenance("progress");
                return 1;
            });
            ExecuteHistoryMapper histories = Mockito.mock(ExecuteHistoryMapper.class);
            Mockito.doAnswer(invocation -> {
                this.maintenance("history");
                return null;
            }).when(histories).deleteExceedLimit(Mockito.anyInt());
            FileMappingMapper files = Mockito.mock(FileMappingMapper.class);
            Mockito.when(files.selectList(Mockito.any())).thenAnswer(invocation -> {
                this.maintenance(invocation.getArgument(0) == null ? "orphaned" : "unfinished");
                return Collections.emptyList();
            });
            this.register("hugeConfig", config);
            this.register("loadTaskMapper", loads);
            this.register("executeHistoryMapper", histories);
            this.register("fileMappingMapper", files);
            this.register("jobManagerMapper", Mockito.mock(JobManagerMapper.class));
            this.register("vertexLabelService", Mockito.mock(VertexLabelService.class));
            this.register("edgeLabelService", Mockito.mock(EdgeLabelService.class));

            this.nativeContext = LoadContext.forOffline(new LoadOptions(), new SchemaCache(null));
            HugeGraphLoader loader = Mockito.mock(HugeGraphLoader.class);
            Mockito.when(loader.context()).thenReturn(this.nativeContext);
            LoadTask progress = new LoadTask();
            progress.setId(991);
            progress.setStatus(LoadStatus.RUNNING);
            progress.setLoader(loader);
            LoadTaskService service = new LoadTaskService();
            ReflectionTestUtils.setField(service, "runningTaskContainer", Map.of(991, progress));
            this.context.registerBean(LoadTaskService.class, () -> service);
            this.context.register(AsyncConfig.class, Text2GqlStreamConfig.class, Scheduling.class,
                                  LoadTaskExecutor.class, FileMappingService.class, ExecuteHistoryService.class);
            // Occupy the real @Async executor before ContextRefreshedEvent starts @Scheduled methods.
            this.context.registerBean("occupyLoader", SmartInitializingSingleton.class, () -> this::occupyLoader);
            this.context.refresh();
        }

        private void register(String name, Object bean) {
            this.context.getBeanFactory().registerSingleton(name, bean);
        }

        private void occupyLoader() {
            LoadTaskExecutor executor = this.context.getBean(LoadTaskExecutor.class);
            assertTrue("Use the production @Async dispatcher", AopUtils.isAopProxy(executor));
            for (int i = 0; i < 8; i++) {
                LoadTask task = new LoadTask() {
                    @Override
                    public void run() {
                        Fixture.this.loadersStarted.countDown();
                        await(Fixture.this.releaseLoaders, "Release the controlled long-running Loader task", 30);
                    }
                };
                task.setId(i);
                executor.execute(task, this.loadersFinished::countDown);
            }
            await(this.loadersStarted, "All eight Loader core workers must be occupied");
        }

        private void maintenance(String kind) {
            this.calls.computeIfAbsent(kind, ignored -> new AtomicInteger()).incrementAndGet();
            this.maintenanceEntered.countDown();
            await("progress".equals(kind) ? this.releaseProgress : this.releaseCleanup,
                  "Release the controlled metadata operation", 30);
            // Only the Mapper boundary is controlled; the scheduled service body and H2 write are real.
            this.jdbc.update("INSERT INTO maintenance_probe VALUES (?)", kind);
            if ("progress".equals(kind)) {
                assertTrue("The scheduled progress method must retain its transaction",
                           TransactionSynchronizationManager.isActualTransactionActive());
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        Fixture.this.metadataWritten.countDown();
                    }
                });
            } else if ("history".equals(kind)) {
                this.metadataWritten.countDown();
            }
            if (!"progress".equals(kind)) {
                this.cleanupFinished.countDown();
            }
        }

        private int rows(String kind) {
            return this.jdbc.queryForObject("SELECT COUNT(*) FROM maintenance_probe WHERE kind = ?",
                                           Integer.class, kind);
        }

        private ThreadPoolTaskExecutor loaderExecutor() {
            return this.context.getBean("getAsyncExecutor", ThreadPoolTaskExecutor.class);
        }

        private void observeHeartbeats() {
            ThreadPoolTaskScheduler heartbeats = this.context.getBean(
                    "text2GqlHeartbeats", ThreadPoolTaskScheduler.class);
            CountDownLatch ticks = new CountDownLatch(3);
            ScheduledFuture<?> heartbeat = heartbeats.scheduleAtFixedRate(ticks::countDown, Duration.ofSeconds(1));
            try {
                await(ticks, "The SSE heartbeat scheduler must remain responsive across progress periods");
            } finally {
                heartbeat.cancel(false);
            }
        }

        @Override
        public void close() {
            this.releaseLoaders.countDown();
            this.releaseProgress.countDown();
            this.releaseCleanup.countDown();
            try {
                this.context.close();
            } finally {
                this.datasource.close();
                if (this.nativeContext != null) {
                    this.nativeContext.close(false);
                }
            }
        }
    }
}
