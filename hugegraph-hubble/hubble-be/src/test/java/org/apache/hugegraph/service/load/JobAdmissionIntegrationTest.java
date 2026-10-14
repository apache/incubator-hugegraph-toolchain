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

package org.apache.hugegraph.service.load;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.load.LoadTaskController;
import org.apache.hugegraph.controller.load.FileUploadController;
import org.apache.hugegraph.controller.load.FileMappingController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.FileMappingStatus;
import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.entity.load.ListFormat;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.task.TaskManager;
import org.springframework.scheduling.annotation.Async;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.testutil.Assert;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JobAdmissionIntegrationTest.Configuration.class,
                webEnvironment = SpringBootTest.WebEnvironment.NONE,
                properties = {
                    "spring.datasource.url=jdbc:h2:mem:job-admission;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa", "spring.datasource.password=",
                    "spring.sql.init.schema-locations=classpath:database/ingest-transaction-schema.sql",
                    "spring.sql.init.mode=always", "spring.datasource.hikari.maximum-pool-size=3",
                    "spring.autoconfigure.exclude=" +
                    "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration",
                    "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                    "mybatis-plus.configuration.use-generated-keys=true", "mybatis-plus.global-config.banner=false"
                })
public class JobAdmissionIntegrationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Autowired
    private GateTaskService tasks;
    @Autowired
    private JobManagerService jobs;
    @Autowired
    private GateMappingService mappings;
    @Autowired
    private HugeConfig config;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private GateLoadTaskExecutor taskExecutor;
    @MockBean
    private VertexLabelService vertexLabels;
    @MockBean
    private EdgeLabelService edgeLabels;

    private JdbcTemplate jdbc;
    private Path uploads;

    @Before
    public void setup() throws Exception {
        this.jdbc = new JdbcTemplate(this.dataSource);
        this.clearRows();
        this.mappings.getUploadingTokenLocks().clear();
        this.tasks.afterSnapshot(null);
        this.tasks.queuedFirst(null);
        this.tasks.prepareRestart(null);
        this.tasks.afterReservation(null);
        CallbackWriteFailure.thread = null;
        this.taskExecutor.callbackGate(null);
        this.taskExecutor.callbackFinished(null);
        this.mappings.afterSnapshot(null);
        this.uploads = this.folder.newFolder("uploads").toPath();
        Mockito.when(this.config.get(HubbleOptions.PD_ENABLED)).thenReturn(false);
        Mockito.when(this.config.get(HubbleOptions.SERVER_URL)).thenReturn("http://127.0.0.1:65530");
        Mockito.when(this.config.get(HubbleOptions.UPLOAD_FILE_LOCATION)).thenReturn(this.uploads.toString());
        Mockito.when(this.config.get(HubbleOptions.UPLOAD_FILE_FORMAT_LIST)).thenReturn(List.of("csv"));
        Mockito.when(this.config.get(HubbleOptions.UPLOAD_SINGLE_FILE_SIZE_LIMIT)).thenReturn(1024L);
        Mockito.when(this.config.get(HubbleOptions.UPLOAD_TOTAL_FILE_SIZE_LIMIT)).thenReturn(4096L);
    }

    @After
    public void teardown() throws Exception {
        this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
        this.clearRows();
    }

    @Test(timeout = 15000)
    public void testDeleteSnapshotRejectsLaterControllerStartBeforeLoaderConstruction() throws Exception {
        FileMapping mapping = this.seed();
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch delete = new CountDownLatch(1);
        this.tasks.afterSnapshot(() -> {
            snapshot.countDown();
            await(delete);
        });
        AtomicInteger constructions = new AtomicInteger();
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertTrue(snapshot.await(5, TimeUnit.SECONDS));
            CountDownLatch entering = new CountDownLatch(1);
            Future<?> starting = requests.submit(() -> {
                try (MockedConstruction<HugeGraphLoader> ignored = Mockito.mockConstruction(
                        HugeGraphLoader.class, (loader, context) -> constructions.incrementAndGet())) {
                    entering.countDown();
                    return this.controller().start("DEFAULT", "hugegraph", mapping.getJobId(),
                            List.of(mapping.getId()));
                }
            });
            Assert.assertTrue(entering.await(5, TimeUnit.SECONDS));
            assertBlocked(starting);
            Assert.assertEquals(0, constructions.get());
            Assert.assertEquals(0, this.count("load_task"));
            delete.countDown();
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertThrows(java.util.concurrent.ExecutionException.class, () -> starting.get(5, TimeUnit.SECONDS));
            Assert.assertEquals(0, constructions.get());
            Assert.assertEquals(0, this.count("job_manager"));
            Assert.assertEquals(0, this.count("load_task"));
        } finally {
            delete.countDown();
            stop(requests);
        }
    }

    @Test(timeout = 15000)
    public void testControllerStartBeforeDeleteMustFinishNativeCleanupBeforeCommit() throws Exception {
        FileMapping mapping = this.seed();
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch load = new CountDownLatch(1);
        CountDownLatch cleaning = new CountDownLatch(1);
        CountDownLatch cleanup = new CountDownLatch(1);
        AtomicReference<HugeGraphLoader> constructed = new AtomicReference<>();
        ExecutorService requests = Executors.newSingleThreadExecutor();
        try (MockedConstruction<HugeGraphLoader> ignored = Mockito.mockConstruction(HugeGraphLoader.class,
                (loader, construction) -> {
                    constructed.set(loader);
                    LoadContext context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
                    Mockito.when(loader.context()).thenReturn(context);
                    Mockito.when(loader.load()).thenAnswer(call -> {
                        loading.countDown();
                        await(load);
                        return false;
                    });
                    Mockito.doAnswer(call -> {
                        load.countDown();
                        return null;
                    }).when(context).stopLoading();
                    Mockito.doAnswer(call -> {
                        cleaning.countDown();
                        await(cleanup);
                        return null;
                    }).when(loader).shutdown();
                })) {
            LoadTask task = this.controller().start("DEFAULT", "hugegraph", mapping.getJobId(),
                                                    List.of(mapping.getId())).get(0);
            Assert.assertTrue(AopUtils.isAopProxy(this.tasks));
            Assert.assertTrue(AopUtils.isAopProxy(this.taskExecutor));
            Assert.assertTrue(loading.await(5, TimeUnit.SECONDS));
            Assert.assertSame(task, this.tasks.retained(task.getId()));
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertTrue(cleaning.await(5, TimeUnit.SECONDS));
            assertBlocked(deleting);
            Assert.assertFalse(task.isFinished());
            Assert.assertEquals(1, this.count("job_manager"));
            Assert.assertEquals(1, this.count("load_task"));
            cleanup.countDown();
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, this.count("job_manager"));
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertNull(this.tasks.retained(task.getId()));
            Mockito.verify(constructed.get()).shutdown();
        } finally {
            load.countDown();
            cleanup.countDown();
            stop(requests);
        }
    }

    @Test(timeout = 15000)
    public void testPublicSaveCannotInsertAfterDeleteSnapshot() throws Exception {
        this.publicSaveAfterDeletion(false);
    }

    @Test(timeout = 15000)
    public void testJobReservationSurvivesDeleteMethodAndReleasesOnOuterRollback() throws Exception {
        this.publicSaveAfterDeletion(true);
    }

    private void publicSaveAfterDeletion(boolean rollback) throws Exception {
        FileMapping mapping = this.seed();
        LoadTask task = this.metadata(mapping);
        CountDownLatch deleted = new CountDownLatch(1);
        CountDownLatch complete = new CountDownLatch(1);
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            TransactionTemplate transaction = new TransactionTemplate(this.transactionManager);
            Future<?> deleting = requests.submit(() -> transaction.execute(status -> {
                this.jobs.deleteJob(mapping.getJobId());
                deleted.countDown();
                await(complete);
                if (rollback) {
                    status.setRollbackOnly();
                }
                return null;
            }));
            Assert.assertTrue(deleted.await(5, TimeUnit.SECONDS));
            CountDownLatch entering = new CountDownLatch(1);
            Future<?> saving = requests.submit(() -> {
                entering.countDown();
                this.tasks.save(task);
            });
            Assert.assertTrue(entering.await(5, TimeUnit.SECONDS));
            assertBlocked(saving);
            Assert.assertEquals(0, this.count("load_task"));
            complete.countDown();
            deleting.get(5, TimeUnit.SECONDS);
            if (rollback) {
                saving.get(5, TimeUnit.SECONDS);
                Assert.assertEquals(1, this.count("job_manager"));
                Assert.assertEquals(1, this.count("load_task"));
            } else {
                Assert.assertThrows(java.util.concurrent.ExecutionException.class, () -> saving.get(5,
                        TimeUnit.SECONDS));
                Assert.assertEquals(0, this.count("job_manager"));
                Assert.assertEquals(0, this.count("load_task"));
            }
        } finally {
            complete.countDown();
            stop(requests);
        }
    }

    @Test
    public void testPublicStartChecksFreshParentIdentityBeforeConstructingLoader() throws Exception {
        FileMapping mapping = this.seed();
        GraphConnection wrongGraph = this.connection();
        wrongGraph.setGraph("another-graph");
        try (MockedConstruction<HugeGraphLoader> constructed = Mockito.mockConstruction(HugeGraphLoader.class)) {
            Assert.assertThrows(RuntimeException.class, () -> this.tasks.start(wrongGraph, mapping, this.client()));
            LoadTask wrongTask = this.metadata(mapping);
            wrongTask.setGraphSpace("another-space");
            Assert.assertThrows(RuntimeException.class, () -> this.tasks.save(wrongTask));
            this.jobs.deleteJob(mapping.getJobId());
            Assert.assertThrows(RuntimeException.class, () -> this.tasks.start(this.connection(), mapping,
                    this.client()));
            Assert.assertTrue(constructed.constructed().isEmpty());
            Assert.assertEquals(0, this.count("load_task"));
        }
    }

    @Test
    public void testPublicStartAcceptsIngestLoadingParentAndClosesAfterOuterRollback() throws Exception {
        FileMapping mapping = this.seed();
        JobManager job = this.jobs.get(mapping.getJobId());
        job.setJobStatus(JobStatus.LOADING);
        this.jobs.update(job);
        AtomicReference<LoadTask> created = new AtomicReference<>();
        try (MockedConstruction<HugeGraphLoader> constructed = Mockito.mockConstruction(HugeGraphLoader.class)) {
            new TransactionTemplate(this.transactionManager).execute(status -> {
                created.set(this.tasks.start(this.connection(), mapping, this.client()));
                status.setRollbackOnly();
                return null;
            });
            Assert.assertEquals(1, constructed.constructed().size());
            Mockito.verify(constructed.constructed().get(0)).shutdown();
            Assert.assertTrue(created.get().isFinished());
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertEquals(1, this.count("job_manager"));
        }
        // The outer rollback must have released the job stripe.
        this.tasks.save(this.metadata(mapping));
        Assert.assertEquals(1, this.count("load_task"));
    }

    @Test(timeout = 15000)
    public void testRuntimePublishedBeforeExecutorSubmissionCanBecomeObservable() throws Exception {
        FileMapping mapping = this.seed();
        LoadTaskExecutor original = this.taskExecutor;
        AtomicInteger submits = new AtomicInteger();
        LoadTaskExecutor observing = new LoadTaskExecutor() {
            @Override
            public void execute(LoadTask task, Runnable callback) {
                submits.incrementAndGet();
                Assert.assertSame("runtime must be registered before submission", task,
                                  JobAdmissionIntegrationTest.this.tasks.retained(task.getId()));
                super.execute(task, callback);
            }
        };
        ReflectionTestUtils.setField(this.tasks, "taskExecutor", observing);
        try (MockedConstruction<HugeGraphLoader> ignored = Mockito.mockConstruction(HugeGraphLoader.class,
                (loader, construction) -> Mockito.when(loader.context()).thenReturn(
                        Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS)))) {
            LoadTask task = this.tasks.start(this.connection(), mapping, this.client());
            Assert.assertEquals(1, submits.get());
            Assert.assertTrue(task.isFinished());
            Assert.assertSame(task, this.tasks.retained(task.getId()));
        } finally {
            ReflectionTestUtils.setField(this.tasks, "taskExecutor", original);
        }
    }

    @Test(timeout = 15000)
    public void testClearEmptySnapshotIncludesAnAlreadyAdmittedUpload() throws Exception {
        this.assertClearIncludesAdmittedUpload(false);
    }

    @Test(timeout = 15000)
    public void testClearNonemptySnapshotIncludesAnAlreadyAdmittedUpload() throws Exception {
        this.assertClearIncludesAdmittedUpload(true);
    }

    private void assertClearIncludesAdmittedUpload(boolean nonempty) throws Exception {
        JobManager job = nonempty ? this.jobs.get(this.seed().getJobId()) : this.seedJob();
        FileUploadController upload = this.uploadController();
        FileMappingController clear = this.mappingController();
        CountDownLatch admitted = new CountDownLatch(1);
        CountDownLatch createMapping = new CountDownLatch(1);
        CountDownLatch snapshot = new CountDownLatch(1);
        FileMappingService observed = Mockito.mock(FileMappingService.class, call -> {
            Object result = call.getMethod().invoke(this.mappings, call.getArguments());
            if (call.getMethod().getName().equals("listByJob") && call.getArguments().length == 3) {
                snapshot.countDown();
            }
            return result;
        });
        ReflectionTestUtils.setField(clear, "service", observed);
        java.util.concurrent.atomic.AtomicBoolean firstSize = new java.util.concurrent.atomic.AtomicBoolean(true);
        MockMultipartFile part = new MockMultipartFile("file", "admitted.csv", "text/csv", "name\nalice\n".getBytes()) {
            @Override
            public long getSize() {
                if (firstSize.compareAndSet(true, false)) {
                    admitted.countDown();
                    await(createMapping);
                }
                return super.getSize();
            }
        };
        String token = upload.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("admitted.csv"))
                             .get("admitted.csv");
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> uploading = requests.submit(() -> upload.upload("DEFAULT", "hugegraph", job.getId(),
                    part, "admitted.csv", 20L, token, 2, 0));
            Assert.assertTrue(admitted.await(5, TimeUnit.SECONDS));
            Future<?> clearing = requests.submit(() -> clear.clear("DEFAULT", "hugegraph", job.getId()));
            boolean prematureSnapshot = snapshot.await(500, TimeUnit.MILLISECONDS);
            assertBlocked(clearing);
            createMapping.countDown();
            uploading.get(5, TimeUnit.SECONDS);
            clearing.get(5, TimeUnit.SECONDS);
            Assert.assertTrue(this.mappings.listByJob(job.getId()).isEmpty());
            Assert.assertFalse(prematureSnapshot);
        } finally {
            createMapping.countDown();
            stop(requests);
        }
    }

    @Test
    public void testNativeCaseAliasCannotOverwriteAnExistingPartialUpload() throws Exception {
        this.assertNativeAliasProtection("name.csv", "NAME.csv");
    }

    @Test
    public void testNativeUnicodeAliasCannotOverwriteAnExistingPartialUpload() throws Exception {
        this.assertNativeAliasProtection("café.csv", "café.csv");
    }

    private void assertNativeAliasProtection(String name, String alias) throws Exception {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        var tokens = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of(name, alias));
        MockMultipartFile first = new MockMultipartFile("file", name, "text/csv", "name\nalpha\n".getBytes());
        controller.upload("DEFAULT", "hugegraph", job.getId(), first, name, 20L, tokens.get(name), 2, 0);
        FileMapping mapping = this.mappings.get("DEFAULT", "hugegraph", job.getId(), name);
        Path directory = Path.of(mapping.getPath());
        Path aliasDirectory = directory.resolveSibling(alias);
        boolean equivalent = Files.exists(aliasDirectory) && Files.isSameFile(directory, aliasDirectory);
        AtomicInteger aliasWrites = new AtomicInteger();
        MockMultipartFile second = new MockMultipartFile("file", alias, "text/csv", "other\nbeta\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                aliasWrites.incrementAndGet();
                super.transferTo(destination);
            }
        };
        if (equivalent) {
            Assert.assertThrows(RuntimeException.class, () -> controller.upload("DEFAULT", "hugegraph", job.getId(),
                    second, alias, 20L, tokens.get(alias), 2, 0));
            Assert.assertEquals(0, aliasWrites.get());
            Assert.assertTrue(this.mappings.get("DEFAULT", "hugegraph", job.getId(), alias) == null);
            Assert.assertEquals("name\nalpha\n", Files.readString(directory.resolve(name + "-0")));
        } else {
            var accepted = controller.upload("DEFAULT", "hugegraph", job.getId(), second, alias, 20L,
                                              tokens.get(alias), 2, 0);
            Assert.assertFalse(accepted.getStatus() ==
                               org.apache.hugegraph.entity.load.FileUploadResult.Status.FAILURE);
            Assert.assertFalse(Files.isSameFile(directory, aliasDirectory));
            Assert.assertEquals("name\nalpha\n", Files.readString(directory.resolve(name + "-0")));
            Assert.assertEquals("other\nbeta\n", Files.readString(aliasDirectory.resolve(alias + "-0")));
        }
    }

    @Test
    public void testFailedMappingInsertRemovesOnlyItsNewEmptyPartDirectory() throws Exception {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("rejected.csv"))
                                 .get("rejected.csv");
        try (var paths = Files.walk(this.uploads)) {
            Assert.assertEquals(1L, paths.count());
        }
        String constraint = "reject_upload_fixture";
        this.jdbc.execute("ALTER TABLE file_mapping ADD CONSTRAINT " + constraint +
                          " CHECK (job_id <> " + job.getId() + ")");
        try {
            Assert.assertThrows(RuntimeException.class, () -> controller.upload("DEFAULT", "hugegraph", job.getId(),
                    this.file("rejected.csv"), "rejected.csv", 20L, token, 2, 0));
            Assert.assertTrue(this.mappings.listByJob(job.getId()).isEmpty());
            Path directory = this.uploads.resolve("graph-connection-DEFAULT-hugegraph")
                                         .resolve("job-" + job.getId()).resolve("rejected.csv");
            Assert.assertFalse(Files.exists(directory));
        } finally {
            this.jdbc.execute("ALTER TABLE file_mapping DROP CONSTRAINT " + constraint);
        }
    }

    @Test
    public void testClearRejectsWrongGraphRouteBeforeTakingAMappingSnapshot() throws Exception {
        FileMapping mapping = this.seed();
        Assert.assertThrows(RuntimeException.class,
                            () -> this.mappingController().clear("DEFAULT", "other", mapping.getJobId()));
        Assert.assertEquals((Object) mapping.getJobId(), this.mappings.get(mapping.getId()).getJobId());
        Assert.assertTrue(Files.isRegularFile(Path.of(mapping.getPath())));
    }

    @Test(timeout = 15000)
    public void testSameJobFilesWriteConcurrentlyAndDeleteWaitsForBoth() throws Exception {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        var tokens = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("a.csv", "b.csv"));
        CountDownLatch writing = new CountDownLatch(2);
        CountDownLatch finish = new CountDownLatch(1);
        ExecutorService requests = Executors.newFixedThreadPool(3);
        try {
            Future<?> first = requests.submit(() -> controller.upload("DEFAULT", "hugegraph", job.getId(),
                    this.blockedFile("a.csv", writing, finish), "a.csv", 20L, tokens.get("a.csv"), 2, 0));
            Future<?> second = requests.submit(() -> controller.upload("DEFAULT", "hugegraph", job.getId(),
                    this.blockedFile("b.csv", writing, finish), "b.csv", 20L, tokens.get("b.csv"), 2, 0));
            Assert.assertTrue(writing.await(2, TimeUnit.SECONDS));
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(job.getId()));
            assertBlocked(deleting);
            finish.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, this.count("file_mapping"));
            Assert.assertTrue(this.mappings.getUploadingTokenLocks().isEmpty());
        } finally {
            finish.countDown();
            stop(requests);
        }
    }

    @Test(timeout = 15000)
    public void testSameFileTokenAliasesCompleteBeforeMergeAndPreserveContent() throws Exception {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("chunks.csv"))
                                 .get("chunks.csv");
        String alias = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("chunks.csv"))
                                 .get("chunks.csv");
        JobManager other = this.seedJob();
        String otherToken = controller.fileToken("DEFAULT", "hugegraph", other.getId(), List.of("chunks.csv"))
                                      .get("chunks.csv");
        Assert.assertSame(this.mappings.getUploadingTokenLocks().get(token),
                          this.mappings.getUploadingTokenLocks().get(alias));
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        AtomicInteger secondWrites = new AtomicInteger();
        MockMultipartFile firstPart = new MockMultipartFile("file", "chunks.csv", "text/csv", "name\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                super.transferTo(destination);
                writing.countDown();
                await(finish);
            }
        };
        MockMultipartFile secondPart = new MockMultipartFile("file", "chunks.csv", "text/csv", "alice\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                secondWrites.incrementAndGet();
                super.transferTo(destination);
            }
        };
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = requests.submit(() -> controller.upload("DEFAULT", "hugegraph", job.getId(),
                    firstPart, "chunks.csv", 11L, token, 2, 0));
            Assert.assertTrue(writing.await(5, TimeUnit.SECONDS));
            Future<?> second = requests.submit(() -> controller.upload("DEFAULT", "hugegraph", job.getId(),
                    secondPart, "chunks.csv", 11L, alias, 2, 1));
            assertBlocked(second);
            Assert.assertEquals(0, secondWrites.get());
            finish.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            FileMapping mapping = this.mappings.get("DEFAULT", "hugegraph", job.getId(), "chunks.csv");
            Assert.assertEquals(FileMappingStatus.COMPLETED, mapping.getFileStatus());
            Assert.assertEquals("name\nalice\n", Files.readString(Path.of(mapping.getPath())));
            Assert.assertEquals(11L, (long) mapping.getTotalSize());
            Assert.assertEquals(List.of("col-1"), mapping.getFileSetting().getColumnNames());
            Assert.assertEquals(List.of("name"), mapping.getFileSetting().getColumnValues());
            Assert.assertEquals(11L, (long) this.jobs.get(job.getId()).getJobSize());
            Assert.assertFalse(this.mappings.getUploadingTokenLocks().containsKey(token));
            Assert.assertFalse(this.mappings.getUploadingTokenLocks().containsKey(alias));
            Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(otherToken));
        } finally {
            finish.countDown();
            stop(requests);
        }
    }

    @Test
    public void testMappingIdDeleteRevokesOldMultipartTokenOnlyInItsJob() throws Exception {
        this.assertMappingDeleteRevokesToken(false);
    }

    @Test
    public void testMappingClearRevokesOldMultipartTokenOnlyInItsJob() throws Exception {
        this.assertMappingDeleteRevokesToken(true);
    }

    private void assertMappingDeleteRevokesToken(boolean clear) throws Exception {
        JobManager job = this.seedJob();
        JobManager other = this.seedJob();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("partial.csv"))
                                 .get("partial.csv");
        String otherToken = controller.fileToken("DEFAULT", "hugegraph", other.getId(), List.of("partial.csv"))
                                      .get("partial.csv");
        controller.upload("DEFAULT", "hugegraph", job.getId(), this.file("partial.csv"),
                          "partial.csv", 20L, token, 2, 0);
        FileMapping partial = this.mappings.get("DEFAULT", "hugegraph", job.getId(), "partial.csv");
        FileMappingController mappingController = this.mappingController();
        if (clear) {
            mappingController.clear("DEFAULT", "hugegraph", job.getId());
        } else {
            mappingController.delete("DEFAULT", "hugegraph", job.getId(), partial.getId());
        }
        var late = controller.upload("DEFAULT", "hugegraph", job.getId(), this.file("partial.csv"),
                                     "partial.csv", 20L, token, 2, 1);
        Assert.assertEquals(org.apache.hugegraph.entity.load.FileUploadResult.Status.FAILURE, late.getStatus());
        Assert.assertTrue(this.mappings.get("DEFAULT", "hugegraph", job.getId(), "partial.csv") == null);
        Assert.assertFalse(Files.exists(Path.of(partial.getPath())));
        Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(otherToken));
        var accepted = controller.upload("DEFAULT", "hugegraph", other.getId(), this.file("partial.csv"),
                                         "partial.csv", 20L, otherToken, 2, 0);
        Assert.assertFalse(accepted.getStatus() ==
                               org.apache.hugegraph.entity.load.FileUploadResult.Status.FAILURE);
    }

    @Test
    public void testTokenOnlyJobDeleteInvalidatesTokensAfterCommitButNotRollback() {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("pending.csv"))
                                 .get("pending.csv");
        new TransactionTemplate(this.transactionManager).execute(status -> {
            this.jobs.deleteJob(job.getId());
            Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(token));
            status.setRollbackOnly();
            return null;
        });
        Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(token));
        Assert.assertTrue(this.jobs.get(job.getId()) != null);
        this.jobs.deleteJob(job.getId());
        Assert.assertFalse(this.mappings.getUploadingTokenLocks().containsKey(token));
        Assert.assertTrue(this.jobs.get(job.getId()) == null);
    }

    @Test
    public void testTokenDeleteRollbackKeepsPartialUploadAndItsToken() {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("rollback.csv"))
                                 .get("rollback.csv");
        controller.upload("DEFAULT", "hugegraph", job.getId(), this.file("rollback.csv"),
                          "rollback.csv", 20L, token, 2, 0);
        FileMapping partial = this.mappings.get("DEFAULT", "hugegraph", job.getId(), "rollback.csv");
        new TransactionTemplate(this.transactionManager).execute(status -> {
            controller.delete("DEFAULT", "hugegraph", job.getId(), "rollback.csv", token);
            Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(token));
            status.setRollbackOnly();
            return null;
        });
        Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(token));
        Assert.assertEquals(job.getId(), this.mappings.get(partial.getId()).getJobId());
        Assert.assertTrue(Files.exists(Path.of(partial.getPath())));
    }

    @Test
    public void testExpiredUploadRevokesOnlyItsJobEvenWhenDiskCleanupFails() throws Exception {
        JobManager job = this.seedJob();
        JobManager other = this.seedJob();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", job.getId(), List.of("expired.csv"))
                                 .get("expired.csv");
        String otherToken = controller.fileToken("DEFAULT", "hugegraph", other.getId(), List.of("expired.csv"))
                                      .get("expired.csv");
        controller.upload("DEFAULT", "hugegraph", job.getId(), this.file("expired.csv"),
                          "expired.csv", 20L, token, 2, 0);
        FileMapping mapping = this.mappings.get("DEFAULT", "hugegraph", job.getId(), "expired.csv");
        mapping.setPath(this.folder.newFolder("outside-upload-root").getAbsolutePath());
        mapping.setUpdateTime(new Date(System.currentTimeMillis() - 10000));
        this.mappings.update(mapping);
        Mockito.when(this.config.get(HubbleOptions.UPLOAD_FILE_MAX_TIME_CONSUMING)).thenReturn(1L);
        // Invoke the target synchronously: the production scheduled operation and filesystem checks are unchanged.
        GateMappingService target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(this.mappings);
        target.deleteUnfinishedFile();
        Assert.assertFalse(this.mappings.getUploadingTokenLocks().containsKey(token));
        Assert.assertTrue(this.mappings.getUploadingTokenLocks().containsKey(otherToken));
        Assert.assertTrue(this.mappings.get(mapping.getId()) != null);
    }

    @Test(timeout = 15000)
    public void testTokenIssuanceHoldsAdmissionThroughRegistration() throws Exception {
        JobManager job = this.seedJob();
        FileUploadController controller = this.uploadController();
        CountDownLatch parentRead = new CountDownLatch(1);
        CountDownLatch register = new CountDownLatch(1);
        JobManagerService observed = Mockito.mock(JobManagerService.class, call -> {
            Object result = call.getMethod().invoke(this.jobs, call.getArguments());
            if (call.getMethod().getName().equals("get") && call.getArguments().length == 3) {
                parentRead.countDown();
                await(register);
            }
            return result;
        });
        ReflectionTestUtils.setField(controller, "jobService", observed);
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> issuing = requests.submit(() -> controller.fileToken("DEFAULT", "hugegraph", job.getId(),
                                                                            List.of("pending.csv")));
            Assert.assertTrue(parentRead.await(5, TimeUnit.SECONDS));
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(job.getId()));
            assertBlocked(deleting);
            register.countDown();
            issuing.get(5, TimeUnit.SECONDS);
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertTrue(this.mappings.getUploadingTokenLocks().isEmpty());
        } finally {
            register.countDown();
            stop(requests);
        }
    }

    @Test(timeout = 15000)
    public void testTokenIssuanceWaitsForDeletionThenRejectsMissingParent() throws Exception {
        JobManager job = this.seedJob();
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        this.mappings.afterSnapshot(() -> {
            snapshot.countDown();
            await(commit);
        });
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(job.getId()));
            Assert.assertTrue(snapshot.await(5, TimeUnit.SECONDS));
            Future<?> issuing = requests.submit(() -> this.uploadController().fileToken("DEFAULT", "hugegraph",
                    job.getId(), List.of("pending.csv")));
            assertBlocked(issuing);
            commit.countDown();
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertThrows(java.util.concurrent.ExecutionException.class, () -> issuing.get(5, TimeUnit.SECONDS));
            Assert.assertTrue(this.mappings.getUploadingTokenLocks().isEmpty());
        } finally {
            commit.countDown();
            stop(requests);
        }
    }

    @Test(timeout = 25000)
    public void testJobDeleteRefreshesStoppedSnapshotAfterRetryReleasesTaskReservation() throws Exception {
        // Two Spring workers let the replacement actually start while the old
        // callback returns; the submit observer keeps the task stripe until then.
        this.executor.setMaxPoolSize(2);
        this.executor.setCorePoolSize(2);
        FileMapping mapping = this.seed();
        LoadTask oldTask = this.metadata(mapping);
        oldTask.setStatus(LoadStatus.RUNNING);
        CountDownLatch oldLoading = new CountDownLatch(1);
        NativeRun oldRun = this.nativeRun(oldTask, oldLoading, null);
        this.tasks.save(oldTask);
        CountDownLatch oldCallbackEntered = new CountDownLatch(1);
        CountDownLatch releaseOldCallback = new CountDownLatch(1);
        this.taskExecutor.callbackGate(() -> {
            oldCallbackEntered.countDown();
            await(releaseOldCallback);
        });
        CountDownLatch retryReserved = new CountDownLatch(1);
        CountDownLatch stoppedSnapshot = new CountDownLatch(1);
        CountDownLatch newLoading = new CountDownLatch(1);
        CountDownLatch cleanupEntered = new CountDownLatch(1);
        CountDownLatch releaseCleanup = new CountDownLatch(1);
        CountDownLatch newCallbackEntered = new CountDownLatch(1);
        CountDownLatch releaseNewCallback = new CountDownLatch(1);
        AtomicReference<NativeRun> newRun = new AtomicReference<>();
        AtomicReference<LoadTask> newTask = new AtomicReference<>();
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            this.tasks.executeAfterCommit(oldTask);
            Assert.assertTrue(oldLoading.await(5, TimeUnit.SECONDS));
            this.tasks.stop(oldTask.getId());
            Assert.assertTrue(oldCallbackEntered.await(5, TimeUnit.SECONDS));
            oldRun.assertClosed();
            Assert.assertNull(this.tasks.retained(oldTask.getId()));
            this.tasks.prepareRestart(fresh -> {
                try {
                    NativeRun owned = this.nativeRun(fresh, newLoading, null);
                    Mockito.doAnswer(call -> {
                        cleanupEntered.countDown();
                        await(releaseCleanup);
                        return call.callRealMethod();
                    }).when(owned.loader).shutdown();
                    newRun.set(owned);
                    newTask.set(fresh);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            LoadTaskExecutor observing = new LoadTaskExecutor() {
                @Override
                public void execute(LoadTask task, Runnable callback) {
                    JobAdmissionIntegrationTest.this.taskExecutor.execute(task, callback);
                    if (task != oldTask) {
                        await(newLoading);
                    }
                }
            };
            ReflectionTestUtils.setField(this.tasks, "taskExecutor", observing);
            this.tasks.afterReservation(retryReserved::countDown);
            Future<LoadTask> retry = requests.submit(() -> this.tasks.retry(oldTask.getId(), "fixture-session"));
            Assert.assertTrue(retryReserved.await(5, TimeUnit.SECONDS));
            this.tasks.afterReservation(null);
            this.tasks.afterSnapshot(() -> {
                Assert.assertEquals(LoadStatus.STOPPED.getValue().intValue(), this.jdbc.queryForObject(
                        "SELECT load_status FROM load_task WHERE id = ?", Integer.class, oldTask.getId()).intValue());
                stoppedSnapshot.countDown();
            });
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertTrue("Delete must actually snapshot STOPPED before acquiring the held stripe",
                              stoppedSnapshot.await(5, TimeUnit.SECONDS));
            assertBlocked(deleting);
            this.taskExecutor.callbackGate(() -> {
                newCallbackEntered.countDown();
                await(releaseNewCallback);
            });
            releaseOldCallback.countDown();
            Assert.assertEquals(oldTask.getId(), retry.get(5, TimeUnit.SECONDS).getId());
            Assert.assertTrue(newLoading.await(5, TimeUnit.SECONDS));
            Assert.assertTrue("Delete must signal the replacement's actual native cleanup",
                              cleanupEntered.await(5, TimeUnit.SECONDS));
            Assert.assertFalse(newTask.get().isFinished());
            Assert.assertEquals(LoadStatus.RUNNING.getValue().intValue(), this.jdbc.queryForObject(
                    "SELECT load_status FROM load_task WHERE id = ?", Integer.class, oldTask.getId()).intValue());
            assertBlocked(deleting);
            Assert.assertEquals(1, this.count("load_task"));
            Assert.assertEquals(1, this.count("job_manager"));
            releaseCleanup.countDown();
            Assert.assertTrue(newCallbackEntered.await(5, TimeUnit.SECONDS));
            newRun.get().assertClosed();
            Assert.assertTrue(newTask.get().isFinished());
            assertBlocked(deleting);
            Assert.assertEquals("Deletion must join the replacement's actual callback before SQL delete",
                               1, this.count("load_task"));
            releaseNewCallback.countDown();
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertEquals(0, this.count("job_manager"));
            Assert.assertEquals(0, this.count("file_mapping"));
        } finally {
            this.taskExecutor.callbackGate(null);
            releaseOldCallback.countDown();
            releaseCleanup.countDown();
            releaseNewCallback.countDown();
            oldRun.stop.countDown();
            NativeRun replacement = newRun.get();
            if (replacement != null) {
                replacement.stop.countDown();
            }
            requests.shutdown();
            Assert.assertTrue(requests.awaitTermination(8, TimeUnit.SECONDS));
            this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
            oldRun.close();
            if (replacement != null) {
                replacement.close();
            }
            this.tasks.prepareRestart(null);
            this.tasks.afterReservation(null);
            ReflectionTestUtils.setField(this.tasks, "taskExecutor", this.taskExecutor);
            this.executor.setCorePoolSize(1);
            this.executor.setMaxPoolSize(1);
        }
    }

    @Test(timeout = 20000)
    public void testRetryWaitsForStoppedRunsActualCallbackBeforeWritingRunning() throws Exception {
        this.retryAfterCallback(false);
    }

    @Test(timeout = 20000)
    public void testRetryRefusesFailedActualCallbackWithoutCreatingAnotherLoader() throws Exception {
        this.retryAfterCallback(true);
    }

    private void retryAfterCallback(boolean callbackFails) throws Exception {
        FileMapping mapping = this.seed();
        LoadTask task = this.metadata(mapping);
        task.setStatus(LoadStatus.RUNNING);
        CountDownLatch oldLoading = new CountDownLatch(1);
        NativeRun oldRun = this.nativeRun(task, oldLoading, null);
        this.tasks.save(task);
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        CountDownLatch callbackFinished = new CountDownLatch(1);
        this.taskExecutor.callbackGate(() -> {
            CallbackWriteFailure.thread = Thread.currentThread();
            callbackEntered.countDown();
            await(releaseCallback);
        });
        this.taskExecutor.callbackFinished(callbackFinished);
        ExecutorService requests = Executors.newSingleThreadExecutor();
        AtomicInteger reconnected = new AtomicInteger();
        AtomicReference<NativeRun> restarted = new AtomicReference<>();
        CountDownLatch newLoading = new CountDownLatch(1);
        CountDownLatch reserved = new CountDownLatch(1);
        boolean trigger = false;
        try {
            this.tasks.executeAfterCommit(task);
            Assert.assertTrue(oldLoading.await(5, TimeUnit.SECONDS));
            this.tasks.stop(task.getId());
            Assert.assertTrue(callbackEntered.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(task.isFinished());
            oldRun.assertClosed();
            Assert.assertNull(this.tasks.retained(task.getId()));
            Assert.assertEquals(LoadStatus.STOPPED.getValue().intValue(), this.jdbc.queryForObject(
                    "SELECT load_status FROM load_task WHERE id = ?", Integer.class, task.getId()).intValue());
            if (callbackFails) {
                this.jdbc.execute("CREATE TRIGGER reject_old_callback BEFORE UPDATE ON load_task FOR EACH ROW CALL '" +
                                  CallbackWriteFailure.class.getName() + "'");
                trigger = true;
            }
            this.tasks.prepareRestart(fresh -> {
                reconnected.incrementAndGet();
                try {
                    restarted.set(this.nativeRun(fresh, newLoading, null));
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            this.tasks.afterReservation(reserved::countDown);
            Future<LoadTask> retry = requests.submit(() -> this.tasks.retry(task.getId(), "fixture-session"));
            Assert.assertTrue("Retry must actually own the task reservation", reserved.await(5, TimeUnit.SECONDS));
            assertBlocked(retry);
            Assert.assertEquals("An unfinished old callback must precede any reconnect", 0, reconnected.get());
            Assert.assertEquals(LoadStatus.STOPPED.getValue().intValue(), this.jdbc.queryForObject(
                    "SELECT load_status FROM load_task WHERE id = ?", Integer.class, task.getId()).intValue());
            this.taskExecutor.callbackGate(null);
            releaseCallback.countDown();
            Assert.assertTrue(callbackFinished.await(5, TimeUnit.SECONDS));
            if (callbackFails) {
                Throwable rejected = Assert.assertThrows(java.util.concurrent.ExecutionException.class,
                                                         () -> retry.get(5, TimeUnit.SECONDS));
                Throwable original = this.tasks.callbackFailure(task.getId());
                Assert.assertNotNull(original);
                Assert.assertSame("Restart must preserve the actual failed metadata callback cause",
                                  original, rejected.getCause());
                Assert.assertEquals(0, reconnected.get());
                Assert.assertEquals(LoadStatus.STOPPED.getValue().intValue(), this.jdbc.queryForObject(
                        "SELECT load_status FROM load_task WHERE id = ?", Integer.class, task.getId()).intValue());
                Assert.assertSame(original, Assert.assertThrows(RuntimeException.class,
                        () -> this.tasks.retry(task.getId(), "fixture-session")));
                Assert.assertThrows(RuntimeException.class, () -> this.jobs.deleteJob(mapping.getJobId()));
            } else {
                LoadTask running = retry.get(5, TimeUnit.SECONDS);
                Assert.assertTrue(newLoading.await(5, TimeUnit.SECONDS));
                Assert.assertEquals(1, reconnected.get());
                Assert.assertEquals(LoadStatus.RUNNING, running.getStatus());
                Assert.assertEquals("Completed old callbacks cannot overwrite the restarted H2 row",
                                   LoadStatus.RUNNING.getValue().intValue(), this.jdbc.queryForObject(
                        "SELECT load_status FROM load_task WHERE id = ?", Integer.class, task.getId()).intValue());
                this.tasks.stop(task.getId());
                this.jobs.deleteJob(mapping.getJobId());
                Assert.assertEquals(0, this.count("load_task"));
                restarted.get().assertClosed();
            }
        } finally {
            this.taskExecutor.callbackGate(null);
            releaseCallback.countDown();
            oldRun.stop.countDown();
            NativeRun newRun = restarted.get();
            if (newRun != null) {
                newRun.stop.countDown();
            }
            requests.shutdown();
            Assert.assertTrue(requests.awaitTermination(8, TimeUnit.SECONDS));
            this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
            oldRun.close();
            if (newRun != null) {
                newRun.close();
            }
            if (trigger) {
                this.jdbc.execute("DROP TRIGGER reject_old_callback");
            }
            CallbackWriteFailure.thread = null;
            this.tasks.prepareRestart(null);
            this.tasks.afterReservation(null);
            this.taskExecutor.callbackFinished(null);
        }
    }

    public static class CallbackWriteFailure implements org.h2.api.Trigger {
        private static volatile Thread thread;

        @Override
        public void fire(java.sql.Connection connection, Object[] oldRow, Object[] newRow)
                throws java.sql.SQLException {
            if (Thread.currentThread() == thread) {
                throw new java.sql.SQLException("controlled actual old callback write failure", "45000");
            }
        }
    }

    @Test
    public void testDirectStartDispatchErrorRetainsOneRecoveryTicket() throws Exception {
        FileMapping mapping = this.seed();
        Error dispatchFailure = new AssertionError("controlled direct dispatch Error");
        RuntimeException cleanupFailure = new IllegalStateException("controlled unscheduled cleanup failure");
        AtomicReference<LoadTask> observed = new AtomicReference<>();
        LoadTaskExecutor rejecting = Mockito.mock(LoadTaskExecutor.class);
        Mockito.doAnswer(call -> {
            observed.set(call.getArgument(0));
            throw dispatchFailure;
        }).when(rejecting).execute(Mockito.any(LoadTask.class), Mockito.any());
        ReflectionTestUtils.setField(this.tasks, "taskExecutor", rejecting);
        try (MockedConstruction<HugeGraphLoader> constructed = Mockito.mockConstruction(HugeGraphLoader.class,
                (loader, construction) -> Mockito.doThrow(cleanupFailure).when(loader).shutdown())) {
            Assert.assertSame(dispatchFailure, Assert.assertThrows(Error.class,
                    () -> this.tasks.start(this.connection(), mapping, this.client())));
            LoadTask task = observed.get();
            Assert.assertNotNull(task);
            Assert.assertTrue(task.isFinished());
            Assert.assertTrue(task.isCleanupFailed());
            Assert.assertSame(task, this.tasks.retained(task.getId()));
            Assert.assertEquals(LoadStatus.FAILED, this.tasks.get(task.getId()).getStatus());
            java.util.Set<?> runs = (java.util.Set<?>) ReflectionTestUtils.getField(this.tasks, "acceptedRuns");
            long matching = runs.stream().filter(run -> ReflectionTestUtils.getField(run, "task") == task).count();
            Assert.assertEquals("The dispatch and start catch paths own the same accepted run", 1L, matching);
            Assert.assertThrows(RuntimeException.class, () -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertEquals(1, this.count("job_manager"));
            Mockito.verify(constructed.constructed().get(0), Mockito.atLeastOnce()).shutdown();
        } finally {
            ReflectionTestUtils.setField(this.tasks, "taskExecutor", this.taskExecutor);
        }
    }

    @Test(timeout = 20000)
    public void testDeleteWaitsForActualCallbackAfterNativeFinished() throws Exception {
        FileMapping mapping = this.seed();
        LoadTask task = this.metadata(mapping);
        task.setStatus(LoadStatus.RUNNING);
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        CountDownLatch callbackFinished = new CountDownLatch(1);
        NativeRun nativeRun = this.nativeRun(task, null, null);
        this.tasks.save(task);
        this.taskExecutor.callbackGate(() -> {
            callbackEntered.countDown();
            await(releaseCallback);
        });
        this.taskExecutor.callbackFinished(callbackFinished);
        ExecutorService requests = Executors.newSingleThreadExecutor();
        CountDownLatch deleteSnapshot = new CountDownLatch(1);
        this.tasks.afterSnapshot(deleteSnapshot::countDown);
        try {
            this.tasks.executeAfterCommit(task);
            Assert.assertTrue(callbackEntered.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(task.isFinished());
            nativeRun.assertClosed();
            Future<?> deletion = requests.submit(() -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertTrue(deleteSnapshot.await(5, TimeUnit.SECONDS));
            assertBlocked(deletion);
            com.zaxxer.hikari.HikariDataSource dataSource =
                    (com.zaxxer.hikari.HikariDataSource) this.jdbc.getDataSource();
            Assert.assertEquals("Callback waiting must leave metadata connections available", 0,
                                dataSource.getHikariPoolMXBean().getActiveConnections());
            Assert.assertEquals("Native finished is not a callback certificate", 1, this.count("load_task"));
            Assert.assertEquals(1, this.count("job_manager"));
            releaseCallback.countDown();
            deletion.get(5, TimeUnit.SECONDS);
            Assert.assertTrue(callbackFinished.await(5, TimeUnit.SECONDS));
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertEquals(0, this.count("job_manager"));
            Assert.assertEquals(0, this.count("file_mapping"));
        } finally {
            releaseCallback.countDown();
            this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
            nativeRun.close();
            stop(requests);
            this.taskExecutor.callbackGate(null);
            this.taskExecutor.callbackFinished(null);
        }
    }

    @Test(timeout = 20000)
    public void testReplacementUploadTokenWaitsForFailedUploadCleanup() throws Exception {
        FileMapping parent = this.seed();
        FileUploadController controller = this.uploadController();
        int jobId = parent.getJobId();
        String name = "retry.csv";
        String token = controller.fileToken("DEFAULT", "hugegraph", jobId, List.of(name)).get(name);
        CountDownLatch cleaning = new CountDownLatch(1);
        CountDownLatch releaseCleanup = new CountDownLatch(1);
        this.mappings.beforeCleanup(() -> {
            cleaning.countDown();
            await(releaseCleanup);
        });
        MockMultipartFile upload = new MockMultipartFile("file", name, "text/csv", "name\nnew\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                super.transferTo(destination);
                // The merged result fails the final quota recheck.
                Mockito.when(config.get(HubbleOptions.UPLOAD_TOTAL_FILE_SIZE_LIMIT)).thenReturn(1L);
            }
        };
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> failing = requests.submit(() -> Assert.assertThrows(RuntimeException.class,
                    () -> controller.upload("DEFAULT", "hugegraph", jobId, upload, name, null, token, 1, 0)));
            Assert.assertTrue(cleaning.await(5, TimeUnit.SECONDS));
            Future<String> replacement = requests.submit(() ->
                    controller.fileToken("DEFAULT", "hugegraph", jobId, List.of(name)).get(name));
            assertBlocked(replacement);
            Mockito.when(this.config.get(HubbleOptions.UPLOAD_TOTAL_FILE_SIZE_LIMIT)).thenReturn(4096L);
            releaseCleanup.countDown();
            failing.get(5, TimeUnit.SECONDS);
            String nextToken = replacement.get(5, TimeUnit.SECONDS);
            MockMultipartFile next = new MockMultipartFile("file", name, "text/csv", "name\nnew\n".getBytes());
            controller.upload("DEFAULT", "hugegraph", jobId, next, name, null, nextToken, 1, 0);
            FileMapping completed = this.mappings.get("DEFAULT", "hugegraph", jobId, name);
            Assert.assertNotNull(completed);
            Assert.assertEquals(FileMappingStatus.COMPLETED, completed.getFileStatus());
            Assert.assertTrue(Files.isRegularFile(Path.of(completed.getPath())));
        } finally {
            releaseCleanup.countDown();
            this.mappings.beforeCleanup(null);
            stop(requests);
        }
    }

    @Test(timeout = 20000)
    public void testDeleteRejectsPendingCallbackInsideExistingTransaction() throws Exception {
        FileMapping mapping = this.seed();
        LoadTask task = this.metadata(mapping);
        task.setStatus(LoadStatus.RUNNING);
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        NativeRun nativeRun = this.nativeRun(task, null, null);
        this.tasks.save(task);
        this.taskExecutor.callbackGate(() -> {
            callbackEntered.countDown();
            await(releaseCallback);
        });
        try {
            this.tasks.executeAfterCommit(task);
            Assert.assertTrue(callbackEntered.await(5, TimeUnit.SECONDS));
            new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
                Throwable failure = Assert.assertThrows(RuntimeException.class,
                        () -> this.jobs.deleteJob(mapping.getJobId()));
                Assert.assertTrue(failure.getMessage().contains("retry after the current transaction"));
                Assert.assertEquals(1, this.count("load_task"));
                Assert.assertEquals(1, this.count("job_manager"));
                Assert.assertEquals(1, this.count("file_mapping"));
                Assert.assertEquals(LoadStatus.RUNNING, this.tasks.get(task.getId()).getStatus());
            });
            releaseCallback.countDown();
            this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
            new TransactionTemplate(this.transactionManager).executeWithoutResult(status ->
                    this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertEquals(0, this.count("job_manager"));
            nativeRun.assertClosed();
        } finally {
            releaseCallback.countDown();
            this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
            nativeRun.close();
            this.taskExecutor.callbackGate(null);
        }
    }

    @Test(timeout = 25000)
    public void testQueuedFirstJobDeleteSignalsEveryNativeOwnerBeforeWaiting() throws Exception {
        FileMapping mapping = this.seed();
        this.executor.setMaxPoolSize(8);
        this.executor.setCorePoolSize(8);
        CountDownLatch active = new CountDownLatch(8);
        CountDownLatch stopRequests = new CountDownLatch(9);
        List<NativeRun> nativeRuns = new ArrayList<>();
        List<LoadTask> tasks = new ArrayList<>();
        ExecutorService requests = Executors.newSingleThreadExecutor();
        CountDownLatch deleteSnapshot = new CountDownLatch(1);
        try {
            for (int i = 0; i < 9; i++) {
                LoadTask task = this.metadata(mapping);
                task.setStatus(LoadStatus.RUNNING);
                NativeRun nativeRun = this.nativeRun(task, active, stopRequests);
                nativeRuns.add(nativeRun);
                tasks.add(task);
                this.tasks.save(task);
                this.tasks.executeAfterCommit(task);
                if (i == 7) {
                    Assert.assertTrue(active.await(5, TimeUnit.SECONDS));
                }
            }
            LoadTask queued = tasks.get(8);
            Assert.assertFalse(queued.isFinished());
            Assert.assertEquals(1, this.executor.getThreadPoolExecutor().getQueue().size());
            this.tasks.queuedFirst(queued.getId());
            this.tasks.afterSnapshot(deleteSnapshot::countDown);
            Future<?> deletion = requests.submit(() -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertTrue(deleteSnapshot.await(5, TimeUnit.SECONDS));
            Assert.assertTrue("Every actual native stop intent must precede queued-first wait",
                              stopRequests.await(5, TimeUnit.SECONDS));
            deletion.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertEquals(0, this.count("job_manager"));
            for (int i = 0; i < tasks.size(); i++) {
                Assert.assertTrue(tasks.get(i).isFinished());
                nativeRuns.get(i).assertClosed();
            }
            Mockito.verify(nativeRuns.get(8).loader, Mockito.never()).load();
        } finally {
            for (NativeRun run : nativeRuns) {
                run.stop.countDown();
            }
            requests.shutdown();
            Assert.assertTrue(requests.awaitTermination(8, TimeUnit.SECONDS));
            for (NativeRun run : nativeRuns) {
                run.close();
            }
            this.executor.getThreadPoolExecutor().submit(() -> { }).get(5, TimeUnit.SECONDS);
            this.executor.setCorePoolSize(1);
            this.executor.setMaxPoolSize(1);
            this.tasks.queuedFirst(null);
        }
    }

    private NativeRun nativeRun(LoadTask task, CountDownLatch active, CountDownLatch stopRequests) throws Exception {
        LoadOptions options = task.getOptions();
        LoadContext context = Mockito.spy(LoadContext.forOffline(options, new SchemaCache(null)));
        TaskManager manager = new TaskManager(context);
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        HugeGraphLoader raw = (HugeGraphLoader) unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), HugeGraphLoader.class);
        ReflectionTestUtils.setField(raw, "context", context);
        ReflectionTestUtils.setField(raw, "manager", manager);
        ReflectionTestUtils.setField(raw, "options", options);
        HugeGraphLoader loader = Mockito.spy(raw);
        CountDownLatch stop = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean requested = new java.util.concurrent.atomic.AtomicBoolean();
        Mockito.doAnswer(call -> {
            Object value = call.callRealMethod();
            stop.countDown();
            if (stopRequests != null && requested.compareAndSet(false, true)) {
                stopRequests.countDown();
            }
            return value;
        }).when(context).stopLoading();
        Mockito.doAnswer(call -> {
            if (active != null) {
                active.countDown();
                await(stop);
                if (stopRequests != null) {
                    await(stopRequests);
                }
            }
            return true;
        }).when(loader).load();
        task.setLoader(loader);
        return new NativeRun(loader, context, manager, stop);
    }

    private static final class NativeRun implements AutoCloseable {
        private final HugeGraphLoader loader;
        private final LoadContext context;
        private final TaskManager manager;
        private final CountDownLatch stop;
        private NativeRun(HugeGraphLoader loader, LoadContext context, TaskManager manager, CountDownLatch stop) {
            this.loader = loader;
            this.context = context;
            this.manager = manager;
            this.stop = stop;
        }

        private void assertClosed() {
            Assert.assertTrue(this.context.closed());
            Assert.assertTrue(((ExecutorService) ReflectionTestUtils.getField(this.manager, "batchService"))
                    .isTerminated());
            Assert.assertTrue(((ExecutorService) ReflectionTestUtils.getField(this.manager, "singleService"))
                    .isTerminated());
            Mockito.verify(this.context).close(Mockito.anyBoolean());
        }

        @Override
        public void close() {
            this.stop.countDown();
            this.loader.shutdown();
        }
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "text/csv", "name\nperson\n".getBytes());
    }

    private MockMultipartFile blockedFile(String name, CountDownLatch writing, CountDownLatch finish) {
        return new MockMultipartFile("file", name, "text/csv", "name\nperson\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                writing.countDown();
                await(finish);
                super.transferTo(destination);
            }
        };
    }

    private JobManager seedJob() {
        JobManager job = JobManager.builder().graphSpace("DEFAULT").graph("hugegraph")
                .jobName(UUID.randomUUID().toString()).jobStatus(JobStatus.SETTING)
                .createTime(new Date()).updateTime(new Date()).build();
        this.jobs.save(job);
        return job;
    }

    private FileMappingController mappingController() {
        FileMappingController controller = new FileMappingController() {
            @Override
            protected HugeClient requireGraphSpaceWrite(String graphSpace) {
                return client();
            }
        };
        ReflectionTestUtils.setField(controller, "service", this.mappings);
        ReflectionTestUtils.setField(controller, "jobService", this.jobs);
        ReflectionTestUtils.setField(controller, "taskService", this.tasks);
        return controller;
    }

    @Test(timeout = 15000)
    public void testUploadCannotCreateMappingAfterDeleteSnapshot() throws Exception {
        FileMapping mapping = this.seed();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", mapping.getJobId(),
                                            List.of("later.csv")).get("later.csv");
        CountDownLatch snapshot = new CountDownLatch(1);
        CountDownLatch complete = new CountDownLatch(1);
        this.mappings.afterSnapshot(() -> {
            snapshot.countDown();
            await(complete);
        });
        AtomicInteger writes = new AtomicInteger();
        MockMultipartFile file = new MockMultipartFile("file", "later.csv", "text/csv", "name\nnew\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                writes.incrementAndGet();
                super.transferTo(destination);
            }
        };
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> deleting = requests.submit(() -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertTrue(snapshot.await(5, TimeUnit.SECONDS));
            CountDownLatch entered = new CountDownLatch(1);
            Future<?> uploading = requests.submit(() -> {
                entered.countDown();
                return controller.upload("DEFAULT", "hugegraph", mapping.getJobId(), file,
                                         "later.csv", null, token, 1, 0);
            });
            Assert.assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertBlocked(uploading);
            complete.countDown();
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertThrows(java.util.concurrent.ExecutionException.class,
                                () -> uploading.get(5, TimeUnit.SECONDS));
            Assert.assertEquals(0, writes.get());
            Assert.assertEquals(0, this.count("job_manager"));
            Assert.assertEquals(0, this.count("file_mapping"));
            try (var files = Files.walk(this.uploads)) {
                Assert.assertFalse(files.anyMatch(Files::isRegularFile));
            }
        } finally {
            complete.countDown();
            stop(requests);
        }
    }

    @Test(timeout = 15000)
    public void testDeleteWaitsForUploadingFileAndCleansCommittedMapping() throws Exception {
        FileMapping mapping = this.seed();
        FileUploadController controller = this.uploadController();
        String token = controller.fileToken("DEFAULT", "hugegraph", mapping.getJobId(),
                                            List.of("later.csv")).get("later.csv");
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch complete = new CountDownLatch(1);
        MockMultipartFile file = new MockMultipartFile("file", "later.csv", "text/csv", "name\nnew\n".getBytes()) {
            @Override
            public void transferTo(File destination) throws IOException {
                writing.countDown();
                await(complete);
                super.transferTo(destination);
            }
        };
        ExecutorService requests = Executors.newFixedThreadPool(2);
        try {
            Future<?> uploading = requests.submit(() -> controller.upload("DEFAULT", "hugegraph", mapping.getJobId(),
                                                                          file, "later.csv", null, token, 1, 0));
            Assert.assertTrue(writing.await(5, TimeUnit.SECONDS));
            CountDownLatch entered = new CountDownLatch(1);
            Future<?> deleting = requests.submit(() -> {
                entered.countDown();
                this.jobs.deleteJob(mapping.getJobId());
            });
            Assert.assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertBlocked(deleting);
            Assert.assertEquals(1, this.count("job_manager"));
            complete.countDown();
            uploading.get(5, TimeUnit.SECONDS);
            deleting.get(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, this.count("job_manager"));
            Assert.assertEquals(0, this.count("file_mapping"));
            try (var files = Files.walk(this.uploads)) {
                Assert.assertFalse(files.anyMatch(Files::isRegularFile));
            }
        } finally {
            complete.countDown();
            stop(requests);
        }
    }

    @Test
    public void testDetachedMappingCannotStartFromStaleControllerSnapshot() throws Exception {
        FileMapping mapping = this.seed();
        this.jobs.deleteMappings(mapping.getJobId(), List.of(mapping));
        Assert.assertTrue(this.mappings.get(mapping.getId()) == null ||
                          this.mappings.get(mapping.getId()).getJobId() == null);
        try (MockedConstruction<HugeGraphLoader> constructed = Mockito.mockConstruction(HugeGraphLoader.class)) {
            Assert.assertThrows(RuntimeException.class,
                                () -> this.tasks.start(this.connection(), mapping, this.client()));
            Assert.assertTrue(constructed.constructed().isEmpty());
            Assert.assertEquals(0, this.count("load_task"));
        }
    }

    @Test
    public void testRunningTaskPreventsMappingDetach() throws Exception {
        FileMapping mapping = this.seed();
        LoadTask task = this.metadata(mapping);
        task.setStatus(LoadStatus.RUNNING);
        this.tasks.save(task);
        Assert.assertThrows(RuntimeException.class,
                            () -> this.jobs.deleteMappings(mapping.getJobId(), List.of(mapping)));
        Assert.assertEquals(mapping.getJobId(), this.mappings.get(mapping.getId()).getJobId());
        Assert.assertTrue(Files.isRegularFile(Path.of(mapping.getPath())));
    }

    @Test
    public void testCleanupFailureBeforeGeneratedTaskIdProtectsParent() throws Exception {
        FileMapping mapping = this.seed();
        RuntimeException cleanup = new IllegalStateException("fixture native cleanup failed");
        String constraint = "reject_fixture_task";
        this.jdbc.execute("ALTER TABLE load_task ADD CONSTRAINT " + constraint +
                          " CHECK (job_id <> " + mapping.getJobId() + ")");
        try (MockedConstruction<HugeGraphLoader> constructed = Mockito.mockConstruction(HugeGraphLoader.class,
                (loader, context) -> Mockito.doThrow(cleanup).when(loader).shutdown())) {
            Throwable insert = Assert.assertThrows(RuntimeException.class,
                    () -> this.tasks.start(this.connection(), mapping, this.client()));
            Assert.assertTrue(insert instanceof org.springframework.dao.DataIntegrityViolationException);
            Assert.assertEquals(1, constructed.constructed().size());
            Mockito.verify(constructed.constructed().get(0)).shutdown();
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertThrows(RuntimeException.class, () -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertEquals(1, this.count("job_manager"));
            Assert.assertEquals(1, this.count("file_mapping"));
            Assert.assertTrue(Files.isRegularFile(Path.of(mapping.getPath())));
            Assert.assertTrue(java.util.Arrays.asList(insert.getSuppressed()).contains(cleanup));
        } finally {
            this.jdbc.execute("ALTER TABLE load_task DROP CONSTRAINT " + constraint);
        }
    }

    @Test
    public void testRollbackCleanupFailureProtectsParentWithoutTaskRow() throws Exception {
        FileMapping mapping = this.seed();
        RuntimeException cleanup = new IllegalStateException("fixture rollback cleanup failed");
        AtomicReference<LoadTask> created = new AtomicReference<>();
        try (MockedConstruction<HugeGraphLoader> constructed = Mockito.mockConstruction(HugeGraphLoader.class,
                (loader, context) -> Mockito.doThrow(cleanup).when(loader).shutdown())) {
            new TransactionTemplate(this.transactionManager).execute(status -> {
                created.set(this.tasks.start(this.connection(), mapping, this.client()));
                status.setRollbackOnly();
                return null;
            });
            Assert.assertNotNull(created.get().getId());
            Assert.assertTrue(created.get().isCleanupFailed());
            Assert.assertTrue(created.get().isFinished());
            Mockito.verify(constructed.constructed().get(0)).shutdown();
            Assert.assertEquals(0, this.count("load_task"));
            Assert.assertThrows(RuntimeException.class, () -> this.jobs.deleteJob(mapping.getJobId()));
            Assert.assertEquals(1, this.count("job_manager"));
            Assert.assertEquals(1, this.count("file_mapping"));
        }
    }

    private FileMapping seed() throws Exception {
        JobManager job = JobManager.builder().graphSpace("DEFAULT").graph("hugegraph")
                .jobName(UUID.randomUUID().toString()).jobStatus(JobStatus.SETTING)
                .createTime(new Date()).updateTime(new Date()).build();
        this.jobs.save(job);
        Path dir = Files.createDirectories(this.uploads.resolve(UUID.randomUUID().toString()));
        Path input = dir.resolve("people.csv");
        Files.writeString(input, "name\nperson\n");
        FileMapping mapping = new FileMapping();
        mapping.setGraphSpace("DEFAULT");
        mapping.setGraph("hugegraph");
        mapping.setJobId(job.getId());
        mapping.setName("people.csv");
        mapping.setPath(input.toString());
        mapping.setTotalLines(2L);
        mapping.setTotalSize(12L);
        mapping.setFileStatus(FileMappingStatus.COMPLETED);
        mapping.setFileSetting(FileSetting.builder().columnNames(List.of("name")).format("CSV").delimiter(",")
                .charset("UTF-8").dateFormat("yyyy-MM-dd HH:mm:ss").timeZone("GMT+8")
                .skippedLine("(^#|^//).*").listFormat(new ListFormat()).build());
        mapping.setVertexMappings(Collections.emptySet());
        mapping.setEdgeMappings(Collections.emptySet());
        mapping.setLoadParameter(new LoadParameter());
        mapping.setCreateTime(new Date());
        mapping.setUpdateTime(new Date());
        this.mappings.save(mapping);
        return mapping;
    }

    private LoadTask metadata(FileMapping mapping) {
        return LoadTask.builder().graphSpace("DEFAULT").graph("hugegraph").jobId(mapping.getJobId())
                .fileId(mapping.getId()).fileName(mapping.getName()).options(new LoadOptions())
                .vertices(Collections.emptySet()).edges(Collections.emptySet()).fileTotalLines(2L)
                .fileReadLines(0L).lastDuration(0L).currDuration(0L).createTime(new Date())
                .status(LoadStatus.STOPPED).build();
    }

    private LoadTaskController controller() {
        LoadTaskController controller = new LoadTaskController(this.tasks) {
            @Override
            protected HugeClient requireGraphSpaceWrite(String graphSpace) {
                return client();
            }

            @Override
            protected HugeClient authClient(String space, String graph) {
                return client();
            }

            @Override
            protected String getToken() {
                return "fixture-current-token";
            }

            @Override
            protected String getUser() {
                return "fixture-user";
            }
        };
        ReflectionTestUtils.setField(controller, "jobService", this.jobs);
        ReflectionTestUtils.setField(controller, "fmService", this.mappings);
        ReflectionTestUtils.setField(controller, "config", this.config);
        return controller;
    }

    private FileUploadController uploadController() {
        FileUploadController controller = new FileUploadController() {
            @Override
            protected HugeClient requireGraphSpaceWrite(String graphSpace) {
                return client();
            }
        };
        ReflectionTestUtils.setField(controller, "service", this.mappings);
        ReflectionTestUtils.setField(controller, "jobService", this.jobs);
        ReflectionTestUtils.setField(controller, "config", this.config);
        return controller;
    }

    private GraphConnection connection() {
        GraphConnection connection = new GraphConnection();
        connection.setGraphSpace("DEFAULT");
        connection.setGraph("hugegraph");
        return connection;
    }

    private HugeClient client() {
        return Mockito.mock(HugeClient.class);
    }

    private int count(String table) {
        return this.jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void clearRows() {
        this.jdbc.update("DELETE FROM load_task");
        this.jdbc.update("DELETE FROM file_mapping");
        this.jdbc.update("DELETE FROM job_manager");
    }

    private static void await(CountDownLatch latch) {
        try {
            Assert.assertTrue(latch.await(8, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void assertBlocked(Future<?> request) throws Exception {
        try {
            request.get(300, TimeUnit.MILLISECONDS);
            Assert.fail("request bypassed lifecycle reservation");
        } catch (TimeoutException expected) {
            // The owning transaction still holds the reservation.
        }
    }

    private static void stop(ExecutorService requests) throws Exception {
        requests.shutdownNow();
        Assert.assertTrue(requests.awaitTermination(5, TimeUnit.SECONDS));
    }

    public static class GateTaskService extends LoadTaskService {
        private Runnable afterSnapshot;
        private Integer queuedFirst;
        private java.util.function.Consumer<LoadTask> prepareRestart;
        private Runnable afterReservation;

        public void prepareRestart(java.util.function.Consumer<LoadTask> prepare) {
            this.prepareRestart = prepare;
        }

        public void afterReservation(Runnable action) {
            this.afterReservation = action;
        }

        @Override
        TaskReservation reserveTasks(List<Integer> ids) {
            TaskReservation reservation = super.reserveTasks(ids);
            Runnable action = this.afterReservation;
            if (action != null) {
                action.run();
            }
            return reservation;
        }

        @Override
        public LoadTask get(int id) {
            LoadTask task = super.get(id);
            java.util.function.Consumer<LoadTask> prepare = this.prepareRestart;
            if (task != null && prepare != null) {
                LoadTask controlled = Mockito.spy(task);
                Mockito.doAnswer(call -> {
                    prepare.accept(controlled);
                    return null;
                }).when(controlled).reconnect("fixture-session");
                return controlled;
            }
            return task;
        }

        public Throwable callbackFailure(int taskId) {
            java.util.Set<?> accepted = (java.util.Set<?>) ReflectionTestUtils.getField(this, "acceptedRuns");
            for (Object run : accepted) {
                LoadTask task = (LoadTask) ReflectionTestUtils.getField(run, "task");
                if (task.getId() != null && task.getId() == taskId) {
                    java.util.concurrent.CompletableFuture<?> completed = (java.util.concurrent.CompletableFuture<?>)
                            ReflectionTestUtils.getField(run, "completed");
                    try {
                        completed.join();
                    } catch (java.util.concurrent.CompletionException failure) {
                        return failure.getCause();
                    }
                }
            }
            return null;
        }

        public void afterSnapshot(Runnable action) {
            this.afterSnapshot = action;
        }

        public void queuedFirst(Integer id) {
            this.queuedFirst = id;
        }

        @Override
        public List<LoadTask> taskListByJob(int id) {
            List<LoadTask> snapshot = super.taskListByJob(id);
            if (this.queuedFirst != null) {
                snapshot.sort(Comparator.comparing(task -> !task.getId().equals(this.queuedFirst)));
            }
            Runnable action = this.afterSnapshot;
            this.afterSnapshot = null;
            if (action != null) {
                action.run();
            }
            return snapshot;
        }

        public LoadTask retained(int id) {
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, LoadTask> running = (java.util.Map<Integer, LoadTask>)
                    ReflectionTestUtils.getField(this, "runningTaskContainer");
            return running.get(id);
        }
    }

    public static class GateMappingService extends FileMappingService {
        private Runnable beforeCleanup;

        public void beforeCleanup(Runnable action) {
            this.beforeCleanup = action;
        }

        @Override
        public void cleanupMappings(List<FileMapping> mappings) {
            Runnable action = this.beforeCleanup;
            this.beforeCleanup = null;
            if (action != null) {
                action.run();
            }
            super.cleanupMappings(mappings);
        }

        private Runnable afterSnapshot;
        public void afterSnapshot(Runnable action) {
            this.afterSnapshot = action;
        }

        @Override
        public List<FileMapping> listByJob(int id) {
            List<FileMapping> snapshot = super.listByJob(id);
            Runnable action = this.afterSnapshot;
            this.afterSnapshot = null;
            if (action != null) {
                action.run();
            }
            return snapshot;
        }
    }

    public static class GateLoadTaskExecutor extends LoadTaskExecutor {
        private volatile Runnable callbackGate;
        private volatile CountDownLatch callbackFinished;

        public void callbackGate(Runnable action) {
            this.callbackGate = action;
        }

        public void callbackFinished(CountDownLatch signal) {
            this.callbackFinished = signal;
        }

        @Async
        @Override
        public void execute(LoadTask task, Runnable callback) {
            super.execute(task, () -> {
                Runnable action = this.callbackGate;
                if (action != null) {
                    action.run();
                }
                try {
                    callback.run();
                } finally {
                    CountDownLatch signal = this.callbackFinished;
                    if (signal != null) {
                        signal.countDown();
                    }
                }
            });
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EnableAsync
    @MapperScan("org.apache.hugegraph.mapper.load")
    public static class Configuration {
        @Bean
        public GateTaskService loadTaskService() {
            return new GateTaskService();
        }

        @Bean
        public JobManagerService jobManagerService() {
            return new JobManagerService();
        }

        @Bean
        public GateMappingService fileMappingService() {
            return new GateMappingService();
        }

        @Bean
        public GateLoadTaskExecutor loadTaskExecutor() {
            return new GateLoadTaskExecutor();
        }

        @Bean
        public HugeConfig hugeConfig() {
            return Mockito.mock(HugeConfig.class);
        }

        @Bean
        public ThreadPoolTaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
            pool.setCorePoolSize(1);
            pool.setMaxPoolSize(1);
            pool.setQueueCapacity(4);
            pool.setThreadNamePrefix("job-admission-fixture-");
            pool.setWaitForTasksToCompleteOnShutdown(true);
            pool.setAwaitTerminationSeconds(5);
            return pool;
        }
    }
}
