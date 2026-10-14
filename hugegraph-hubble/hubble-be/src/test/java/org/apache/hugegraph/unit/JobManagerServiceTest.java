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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.exception.ExternalException;

import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;

import org.junit.Test;
import org.mockito.Mockito;

import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.mapper.load.JobManagerMapper;
import org.apache.hugegraph.service.load.JobManagerService;
import org.apache.hugegraph.service.load.FileMappingService;
import org.apache.hugegraph.service.load.LoadTaskService;
import org.apache.hugegraph.testutil.Assert;

public class JobManagerServiceTest {

    @Test
    public void testListByIdsAppliesGraphScope() throws Exception {
        JobManagerService service = this.service();
        JobManagerMapper mapper = Mockito.mock(JobManagerMapper.class);
        this.setField(service, "mapper", mapper);

        service.list("space-a", "graph-a", Arrays.asList(1, 2));

        Mockito.verify(mapper).selectList(Mockito.any());
        Mockito.verify(mapper, Mockito.never()).selectBatchIds(Mockito.any());
    }

    @Test
    public void testRefreshStatusMarksSucceededWhenAllLoadTasksSucceed()
           throws Exception {
        JobManagerService service = this.service();
        LoadTaskService taskService = Mockito.mock(LoadTaskService.class);
        JobManagerMapper mapper = Mockito.mock(JobManagerMapper.class);
        this.setField(service, "taskService", taskService);
        this.setField(service, "mapper", mapper);

        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.LOADING)
                                   .build();
        LoadTask task = LoadTask.builder().status(LoadStatus.SUCCEED).build();
        Mockito.when(taskService.taskListByJob(1))
               .thenReturn(Collections.singletonList(task));
        Mockito.when(mapper.updateById(job)).thenReturn(1);

        service.refreshStatus(job);

        Assert.assertEquals(JobStatus.SUCCESS, job.getJobStatus());
        Mockito.verify(mapper).updateById(job);
    }

    @Test
    public void testRefreshStatusMarksFailedWhenAnyLoadTaskFails()
           throws Exception {
        JobManagerService service = this.service();
        LoadTaskService taskService = Mockito.mock(LoadTaskService.class);
        JobManagerMapper mapper = Mockito.mock(JobManagerMapper.class);
        this.setField(service, "taskService", taskService);
        this.setField(service, "mapper", mapper);

        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.LOADING)
                                   .build();
        LoadTask task = LoadTask.builder().status(LoadStatus.FAILED).build();
        Mockito.when(taskService.taskListByJob(1))
               .thenReturn(Collections.singletonList(task));
        Mockito.when(mapper.updateById(job)).thenReturn(1);

        service.refreshStatus(job);

        Assert.assertEquals(JobStatus.FAILED, job.getJobStatus());
        Mockito.verify(mapper).updateById(job);
    }

    @Test
    public void testRefreshStatusKeepsLoadingWhenAnyLoadTaskRuns()
           throws Exception {
        JobManagerService service = this.service();
        LoadTaskService taskService = Mockito.mock(LoadTaskService.class);
        JobManagerMapper mapper = Mockito.mock(JobManagerMapper.class);
        this.setField(service, "taskService", taskService);
        this.setField(service, "mapper", mapper);

        JobManager job = JobManager.builder()
                                   .id(1)
                                   .jobStatus(JobStatus.LOADING)
                                   .build();
        LoadTask task = LoadTask.builder().status(LoadStatus.RUNNING).build();
        Mockito.when(taskService.taskListByJob(1))
               .thenReturn(Collections.singletonList(task));

        service.refreshStatus(job);

        Assert.assertEquals(JobStatus.LOADING, job.getJobStatus());
        Mockito.verify(mapper, Mockito.never()).updateById(Mockito.any(JobManager.class));
    }

    @Test
    public void testCreateIngestTaskPersistsOneAtomicUnit() throws Exception {
        JobManagerService service = this.service();
        JobManagerMapper mapper = Mockito.mock(JobManagerMapper.class);
        FileMappingService mappingService = Mockito.mock(FileMappingService.class);
        LoadTaskService taskService = Mockito.mock(LoadTaskService.class);
        this.setField(service, "mapper", mapper);
        this.setField(service, "fileMappingService", mappingService);
        this.setField(service, "taskService", taskService);

        JobManager job = JobManager.builder().id(7).build();
        FileMapping mapping = new FileMapping();
        GraphConnection connection = new GraphConnection();
        HugeClient client = Mockito.mock(HugeClient.class);
        LoadTask task = LoadTask.builder().id(9).build();
        Mockito.when(mapper.insert(job)).thenReturn(1);
        Mockito.when(taskService.start(connection, mapping, client))
               .thenReturn(task);

        Assert.assertSame(task, service.createIngestTask(job, mapping,
                                                        connection, client));
        Assert.assertEquals(7, mapping.getJobId().intValue());
        Mockito.verify(mapper).insert(job);
        Mockito.verify(mappingService).save(mapping);
        Mockito.verify(taskService).start(connection, mapping, client);
    }

    @Test
    public void testCreateIngestTaskPropagatesLoadBuildFailure()
           throws Exception {
        JobManagerService service = this.service();
        JobManagerMapper mapper = Mockito.mock(JobManagerMapper.class);
        FileMappingService mappingService = Mockito.mock(FileMappingService.class);
        LoadTaskService taskService = Mockito.mock(LoadTaskService.class);
        this.setField(service, "mapper", mapper);
        this.setField(service, "fileMappingService", mappingService);
        this.setField(service, "taskService", taskService);

        JobManager job = JobManager.builder().id(7).build();
        FileMapping mapping = new FileMapping();
        GraphConnection connection = new GraphConnection();
        HugeClient client = Mockito.mock(HugeClient.class);
        Mockito.when(mapper.insert(job)).thenReturn(1);
        Mockito.when(taskService.start(connection, mapping, client))
               .thenThrow(new IllegalArgumentException("invalid mapping"));

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            service.createIngestTask(job, mapping, connection, client);
        });
        Mockito.verify(mappingService).save(mapping);
        Mockito.verify(taskService).start(connection, mapping, client);
    }

    @Test
    public void testDeleteJobAllowsSucceededTaskAfterRunningSnapshot() throws Exception {
        this.deleteCompletedTask(LoadStatus.SUCCEED);
    }

    @Test
    public void testDeleteJobAllowsFailedLoadAfterRunningSnapshot() throws Exception {
        this.deleteCompletedTask(LoadStatus.FAILED);
    }

    @Test
    public void testDeleteJobAllowsStoppedTaskAfterRunningSnapshot() throws Exception {
        this.deleteCompletedTask(LoadStatus.STOPPED);
    }

    private void deleteCompletedTask(LoadStatus terminal) throws Exception {
        DeleteFixture fixture = this.deleteFixture(terminal, null);

        fixture.jobs.deleteJob(7);

        Assert.assertEquals(terminal, fixture.task.getStatus());
        Assert.assertTrue(fixture.task.isFinished());
        Mockito.verify(fixture.loader).shutdown();
        Mockito.verify(fixture.tasks).deleteById(9);
        Mockito.verify(fixture.mapper).deleteById(7);
        Assert.assertTrue(fixture.running.isEmpty());
    }

    @Test(timeout = 10000)
    public void testDeleteJobPreservesTaskWhenCleanupFailsDuringStopWait() throws Exception {
        DeleteFixture fixture = this.deleteFixture(LoadStatus.FAILED,
                                                    new IllegalStateException("release failed"));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        Mockito.when(fixture.loader.load()).thenAnswer(invocation -> {
            entered.countDown();
            Assert.assertTrue(stopped.await(5, TimeUnit.SECONDS));
            return false;
        });
        Mockito.doAnswer(invocation -> {
            stopped.countDown();
            return null;
        }).when(fixture.context).stopLoading();
        LoadTask snapshot = LoadTask.builder().id(9).status(LoadStatus.RUNNING).build();
        Mockito.doReturn(Collections.singletonList(snapshot)).when(fixture.service).taskListByJob(7);
        Mockito.when(fixture.tasks.updateById(Mockito.any(LoadTask.class))).thenReturn(1);
        Thread runner = new Thread(fixture.task, "delete-job-cleanup-race-test");
        runner.start();
        try {
            Assert.assertTrue(entered.await(5, TimeUnit.SECONDS));

            Assert.assertThrows(RuntimeException.class, () -> fixture.jobs.deleteJob(7));

            Mockito.verify(fixture.tasks, Mockito.never()).deleteById(Mockito.anyInt());
            Mockito.verify(fixture.mapper, Mockito.never()).deleteById(Mockito.anyInt());
            Assert.assertSame(fixture.task, fixture.running.get(9));
            Assert.assertEquals(LoadStatus.FAILED, fixture.task.getStatus());
        } finally {
            stopped.countDown();
            runner.join(5000);
            Assert.assertFalse(runner.isAlive());
        }
    }

    @Test
    public void testDeleteJobPreservesTaskWhenCleanupFailedAfterSnapshot() throws Exception {
        DeleteFixture fixture = this.deleteFixture(LoadStatus.FAILED,
                                                    new IllegalStateException("release failed"));

        Assert.assertThrows(RuntimeException.class, () -> fixture.jobs.deleteJob(7));

        Mockito.verify(fixture.tasks, Mockito.never()).deleteById(Mockito.anyInt());
        Mockito.verify(fixture.mapper, Mockito.never()).deleteById(Mockito.anyInt());
        Mockito.verify(fixture.mappings, Mockito.never()).listByJob(Mockito.anyInt());
        Assert.assertEquals(LoadStatus.FAILED, fixture.task.getStatus());
        Assert.assertSame(fixture.task, fixture.running.get(9));
    }

    @Test
    public void testDeleteJobDoesNotAssumeDatabaseTerminalCleanupSucceeded() throws Exception {
        DeleteFixture fixture = this.deleteFixture(LoadStatus.FAILED, null);
        fixture.running.clear();
        LoadTask snapshot = LoadTask.builder().id(9).status(LoadStatus.RUNNING).build();
        Mockito.doReturn(Collections.singletonList(snapshot)).when(fixture.service).taskListByJob(7);
        Mockito.when(fixture.tasks.selectById(9))
               .thenReturn(LoadTask.builder().id(9).status(LoadStatus.FAILED).build());

        Assert.assertThrows(ExternalException.class, () -> fixture.jobs.deleteJob(7));

        Mockito.verify(fixture.tasks, Mockito.never()).deleteById(Mockito.anyInt());
        Mockito.verify(fixture.mapper, Mockito.never()).deleteById(Mockito.anyInt());
    }

    @Test
    public void testOrdinaryStopStillRejectsCompletedTasks() throws Exception {
        for (LoadStatus terminal : new LoadStatus[]{LoadStatus.SUCCEED, LoadStatus.FAILED, LoadStatus.STOPPED}) {
            DeleteFixture fixture = this.deleteFixture(terminal, null);
            fixture.task.run();

            Assert.assertThrows(ExternalException.class, () -> fixture.service.stop(9));

            Assert.assertEquals(terminal, fixture.task.getStatus());
            Mockito.verify(fixture.tasks, Mockito.never()).updateById(Mockito.any(LoadTask.class));
        }
    }

    @Test
    public void testDeleteJobDoesNotRemoveMetadataWhenStopPersistenceFails() throws Exception {
        DeleteFixture fixture = this.deleteFixture(LoadStatus.PAUSED, null);
        fixture.running.clear();
        LoadTask paused = LoadTask.builder().id(9).status(LoadStatus.PAUSED).build();
        Mockito.doReturn(Collections.singletonList(paused)).when(fixture.service).taskListByJob(7);
        Mockito.when(fixture.tasks.selectById(9)).thenReturn(paused);
        IllegalStateException failure = new IllegalStateException("persist stop failed");
        Mockito.when(fixture.tasks.updateById(Mockito.any(LoadTask.class))).thenThrow(failure);

        Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class,
                                                      () -> fixture.jobs.deleteJob(7)));

        Mockito.verify(fixture.tasks, Mockito.never()).deleteById(Mockito.anyInt());
        Mockito.verify(fixture.mapper, Mockito.never()).deleteById(Mockito.anyInt());
        Mockito.verify(fixture.mappings, Mockito.never()).listByJob(Mockito.anyInt());
    }

    private DeleteFixture deleteFixture(LoadStatus terminal, RuntimeException cleanup) throws Exception {
        DeleteFixture fixture = new DeleteFixture();
        fixture.jobs = this.service();
        fixture.service = Mockito.spy(new LoadTaskService());
        org.springframework.transaction.PlatformTransactionManager transactions =
                Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        Mockito.when(transactions.getTransaction(Mockito.any()))
               .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        this.setField(fixture.service, "transactionManager", transactions);
        fixture.mapper = Mockito.mock(JobManagerMapper.class);
        fixture.tasks = Mockito.mock(LoadTaskMapper.class);
        fixture.mappings = Mockito.mock(FileMappingService.class);
        fixture.loader = Mockito.mock(HugeGraphLoader.class);
        fixture.context = Mockito.mock(LoadContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(fixture.loader.context()).thenReturn(fixture.context);
        fixture.task = LoadTask.builder().id(9).jobId(7).loader(fixture.loader)
                               .options(new LoadOptions()).status(LoadStatus.RUNNING).lastDuration(0L).build();
        Mockito.when(fixture.loader.load()).thenAnswer(invocation -> {
            if (terminal == LoadStatus.STOPPED) {
                fixture.task.setStatus(LoadStatus.STOPPED);
            }
            return terminal == LoadStatus.SUCCEED;
        });
        if (cleanup != null) {
            Mockito.doThrow(cleanup).when(fixture.loader).shutdown();
        }
        fixture.running = new ConcurrentHashMap<>();
        fixture.running.put(9, fixture.task);
        this.setField(fixture.service, "runningTaskContainer", fixture.running);
        this.setField(fixture.service, "mapper", fixture.tasks);
        this.setField(fixture.jobs, "mapper", fixture.mapper);
        this.setField(fixture.jobs, "taskService", fixture.service);
        this.setField(fixture.jobs, "fileMappingService", fixture.mappings);
        Mockito.when(fixture.mapper.selectById(7)).thenReturn(JobManager.builder().id(7).build());
        Mockito.when(fixture.mapper.deleteById(7)).thenReturn(1);
        Mockito.when(fixture.tasks.deleteById(9)).thenReturn(1);
        Mockito.when(fixture.mappings.listByJob(7)).thenReturn(Collections.emptyList());
        LoadTask snapshot = LoadTask.builder().id(9).status(LoadStatus.RUNNING).build();
        Mockito.when(fixture.tasks.selectById(9)).thenAnswer(invocation ->
                LoadTask.builder().id(9).status(fixture.task.getStatus()).build());
        Mockito.doAnswer(invocation -> {
            // The real runner finishes cleanup after the database snapshot,
            // before deleteJob reaches the real stop implementation.
            fixture.task.run();
            return Collections.singletonList(snapshot);
        }).when(fixture.service).taskListByJob(7);
        return fixture;
    }

    private static class DeleteFixture {
        private JobManagerService jobs;
        private LoadTaskService service;
        private JobManagerMapper mapper;
        private LoadTaskMapper tasks;
        private FileMappingService mappings;
        private HugeGraphLoader loader;
        private LoadContext context;
        private LoadTask task;
        private ConcurrentHashMap<Integer, LoadTask> running;
    }

    private JobManagerService service() throws Exception {
        JobManagerService service = new JobManagerService();
        org.springframework.transaction.PlatformTransactionManager transactions =
                Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        Mockito.when(transactions.getTransaction(Mockito.any()))
               .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        this.setField(service, "transactionManager", transactions);
        return service;
    }

    private void setField(Object object, String name, Object value)
                          throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
