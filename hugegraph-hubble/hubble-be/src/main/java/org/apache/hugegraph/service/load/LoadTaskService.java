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

package org.apache.hugegraph.service.load;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Striped;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.JobStatus;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.EdgeMapping;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.JobManager;
import org.apache.hugegraph.entity.load.ListFormat;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.entity.load.VertexMapping;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.util.MappingUtil;
import org.apache.hugegraph.loader.util.Printer;
import org.apache.hugegraph.mapper.load.JobManagerMapper;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import org.apache.hugegraph.util.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.StringUtils;

@Log4j2
@Service
public class LoadTaskService {

    public static final String NO_ERROR_FILE_REASON =
            "For some reason, the error file was not generated. " +
            "Please check the log for details";

    @Autowired
    private LoadTaskMapper mapper;
    @Autowired
    private JobManagerMapper jobManagerMapper;
    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;
    @Autowired
    private LoadTaskExecutor taskExecutor;
    @Autowired
    private HugeConfig config;
    @Autowired
    private PlatformTransactionManager transactionManager;


    private Map<Integer, LoadTask> runningTaskContainer;
    // Request admission precedes task locks. Workers and progress polling never take this lock.
    private final Striped<Lock> taskControls = Striped.lazyWeakLock(1024);

    public LoadTaskService() {
        this.runningTaskContainer = new ConcurrentHashMap<>();
    }

    public LoadTask get(int id) {
        return this.mapper.selectById(id);
    }

    public LoadTask get(String graphSpace, String graph, int jobId, int id) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("id", id)
             .eq("graphspace", graphSpace)
             .eq("graph", graph)
             .eq("job_id", jobId);
        return this.mapper.selectOne(query);
    }

    public List<LoadTask> listAll() {
        return this.mapper.selectList(null);
    }

    public IPage<LoadTask> list(String graphSpace, String graph, int jobId,
                                int pageNo, int pageSize) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("graphspace", graphSpace);
        query.eq("graph", graph);
        query.eq("job_id", jobId);
        query.orderByDesc("create_time");
        Page<LoadTask> page = new Page<>(pageNo,
                                         PageUtil.boundedSize(pageSize));
        return this.mapper.selectPage(page, query);
    }

    public List<LoadTask> list(String graphSpace, String graph, int jobId,
                               List<Integer> taskIds) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("graphspace", graphSpace)
             .eq("graph", graph)
             .eq("job_id", jobId)
             .in("id", taskIds);
        return this.mapper.selectList(query);
    }

    public int count() {
        return Math.toIntExact(this.mapper.selectCount(null));
    }

    public int taskCountByJob(int jobId) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("job_id", jobId);
        return Math.toIntExact(this.mapper.selectCount(query));
    }

    public List<LoadTask> taskListByJob(int jobId) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("job_id", jobId);
        return this.mapper.selectList(query);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(LoadTask entity) {
        LoadOptions runtimeOptions = entity.getOptions();
        entity.setOptions(withoutCredentials(runtimeOptions));
        try {
            if (this.mapper.insert(entity) != 1) {
                throw new InternalException("entity.insert.failed");
            }
        } finally {
            entity.setOptions(runtimeOptions);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void update(LoadTask entity) {
        this.update(entity, null);
    }

    private void update(LoadTask entity, LoadStatus previousStatus) {
        LoadOptions runtimeOptions = entity.getOptions();
        entity.setOptions(withoutCredentials(runtimeOptions));
        try {
            if (previousStatus == null) {
                if (this.mapper.updateById(entity) != 1) {
                    throw new InternalException("entity.update.failed");
                }
            } else {
                int updated = this.mapper.update(entity, Wrappers.<LoadTask>update()
                                                           .eq("id", entity.getId())
                                                           .eq("load_status", previousStatus.getValue()));
                Ex.check(updated == 1, "The load task state has changed; retry the request");
            }
        } finally {
            entity.setOptions(runtimeOptions);
        }
    }

    private static LoadOptions withoutCredentials(LoadOptions options) {
        if (options == null) {
            return null;
        }
        try {
            LoadOptions persisted = (LoadOptions) options.clone();
            persisted.password = null;
            persisted.token = null;
            persisted.pdToken = null;
            persisted.trustStoreToken = null;
            return persisted;
        } catch (CloneNotSupportedException e) {
            throw new InternalException("Failed to prepare load task options",
                                        e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(int id) {
        Lock control = this.taskControls.get(id);
        control.lock();
        try {
            LoadTask persisted = this.get(id);
            Ex.check(persisted != null, "load.task.not-exist.id", id);
            Ex.check(!persisted.getStatus().inRunning(), "load.task.delete.task-in-running", id);
            LoadTask owner = this.runningTaskContainer.get(id);
            if (owner != null) {
                owner.lock();
            }
            try {
                Ex.check(owner == null || (owner.isFinished() &&
                         (owner.getLoader() == null || owner.context().closed())),
                         "load.task.delete.task-in-running", id);
                int deleted = this.mapper.delete(Wrappers.<LoadTask>query().eq("id", id)
                                                       .eq("load_status", persisted.getStatus().getValue()));
                Ex.check(deleted == 1, "The load task state has changed; retry the request");
                if (owner != null) {
                    this.removeOwnerAfterCommit(owner);
                }
            } finally {
                if (owner != null) {
                    owner.unlock();
                }
            }
        } finally {
            control.unlock();
        }
    }

    public List<LoadTask> batchTasks(int jobId) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("job_id", jobId);
        return this.mapper.selectList(query);
    }

    public LoadTask start(GraphConnection connection, FileMapping fileMapping,
                          HugeClient client) {
        LoadTask task = this.buildLoadTask(connection, fileMapping, client);
        try {
            this.save(task);
            this.executeAfterCommit(task);
            return task;
        } catch (RuntimeException | Error failure) {
            closeUnscheduled(task, failure);
            this.removeClosedOwner(task);
            throw failure;
        }
    }

    protected void executeAfterCommit(LoadTask task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            this.executeSafely(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        executeSafely(task);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            closeUnscheduled(task, null);
                        }
                    }
                });
    }

    private void executeSafely(LoadTask task) {
        Lock control = this.taskControls.get(task.getId());
        control.lock();
        try {
            this.executeControlled(task);
        } finally {
            control.unlock();
        }
    }

    private void executeControlled(LoadTask task) {
        LoadTask current = this.runningTaskContainer.get(task.getId());
        if (current == task) {
            // The same owner was already scheduled; do not run or close it twice.
            return;
        }
        if (current != null) {
            this.cancelInitial(task, LoadStatus.FAILED, null);
            return;
        }
        LoadTask persisted;
        try {
            persisted = this.get(task.getId());
        } catch (RuntimeException failure) {
            log.error("Failed to read metadata for initial load task {}", task.getId(), failure);
            task.setInitialStateReadFailed(true);
            this.cancelInitial(task, LoadStatus.FAILED, failure);
            // Keep even a closed owner until the existing poll can conditionally persist this failure.
            this.runningTaskContainer.putIfAbsent(task.getId(), task);
            return;
        }
        HugeGraphLoader loader = task.getLoader();
        if (persisted == null || persisted.getStatus() != LoadStatus.RUNNING || current != null ||
            task.isFinished() || loader == null || loader.context().closed() || loader.context().stopped()) {
            LoadStatus status = persisted == null || persisted.getStatus() == LoadStatus.RUNNING ?
                                LoadStatus.FAILED : persisted.getStatus();
            this.cancelInitial(task, status, null);
            return;
        }
        if (this.runningTaskContainer.putIfAbsent(task.getId(), task) != null) {
            this.cancelInitial(task, LoadStatus.FAILED, null);
            return;
        }
        try {
            this.dispatch(task);
        } catch (RuntimeException failure) {
            this.markDispatchFailed(task, failure);
        }
    }

    private void cancelInitial(LoadTask task, LoadStatus status, Throwable failure) {
        // A delayed callback owns only its unused Loader, never a newer task's metadata.
        closeUnscheduled(task, failure, status);
        HugeGraphLoader owner = task.getLoader();
        if (owner != null && !owner.context().closed()) {
            task.setStatus(LoadStatus.FAILED);
            this.runningTaskContainer.putIfAbsent(task.getId(), task);
        }
    }

    private void markDispatchFailed(LoadTask task, RuntimeException cause) {
        if (this.runningTaskContainer.get(task.getId()) != task) {
            this.cancelInitial(task, LoadStatus.FAILED, cause);
            return;
        }
        log.error("Failed to dispatch load task {}, marking task and job as FAILED", task.getId(), cause);
        closeUnscheduled(task, cause);
        try {
            TransactionTemplate transaction = new TransactionTemplate(this.transactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.execute(status -> {
                if (this.runningTaskContainer.get(task.getId()) != task) {
                    return null;
                }
                int updated = this.mapper.update(null, Wrappers.<LoadTask>update()
                                                               .eq("id", task.getId())
                                                               .eq("load_status", LoadStatus.RUNNING.getValue())
                                                               .set("load_status", LoadStatus.FAILED.getValue()));
                if (updated == 0) {
                    return null;
                }
                if (updated != 1) {
                    throw new InternalException("entity.update.failed", task);
                }
                if (task.getJobId() != null) {
                    JobManager job = this.jobManagerMapper.selectById(task.getJobId());
                    if (job != null) {
                        job.setJobStatus(JobStatus.FAILED);
                        job.setUpdateTime(HubbleUtil.nowDate());
                        if (this.jobManagerMapper.updateById(job) != 1) {
                            throw new InternalException("entity.update.failed", job);
                        }
                    }
                }
                return null;
            });
        } catch (RuntimeException failure) {
            // The ingest transaction is committed. Do not report a retryable dispatch failure.
            log.error("Failed to persist dispatch failure for load task {}", task.getId(), failure);
        } finally {
            this.removeClosedOwner(task);
        }
    }

    private static void closeUnscheduled(LoadTask task, Throwable failure) {
        closeUnscheduled(task, failure, LoadStatus.FAILED);
    }

    private static void closeUnscheduled(LoadTask task, Throwable failure, LoadStatus status) {
        task.lock();
        try {
            task.setStatus(status);
            if (task.getLoader() != null) {
                try {
                    task.getLoader().shutdown();
                } catch (Throwable cleanup) {
                    task.setStatus(LoadStatus.FAILED);
                    if (failure != null && failure != cleanup) {
                        failure.addSuppressed(cleanup);
                    }
                    log.error("Failed to close unscheduled load task {}", task.getId(), cleanup);
                }
            }
        } finally {
            // No runner was scheduled to publish completion for this owner.
            task.setFinished(true);
            task.unlock();
        }
    }

    private void dispatch(LoadTask task) {
        this.taskExecutor.execute(task, () -> {
            this.update(task);
            // Pause/stop retain ownership until their caller has also persisted
            // its terminal request. A failed callback can be retried there.
            if (task.getStatus() != LoadStatus.PAUSED && task.getStatus() != LoadStatus.STOPPED) {
                this.removeClosedOwner(task);
            }
        });
    }

    private void persistStopped(LoadTask task, boolean loading, LoadStatus previousStatus) {
        task.lock();
        try {
            // The runner already persisted its final state before finishing.
            // Never let an old pause/stop callback overwrite a new owner.
            if (!loading || this.runningTaskContainer.get(task.getId()) == task) {
                this.update(task, loading ? null : previousStatus);
                this.removeClosedOwner(task);
            }
        } finally {
            task.unlock();
        }
    }

    private void removeClosedOwner(LoadTask task) {
        HugeGraphLoader owner = task.getLoader();
        if (task.getId() != null && (owner == null || owner.context().closed())) {
            this.removeOwner(task);
        }
    }

    private void removeOwner(LoadTask expected) {
        this.runningTaskContainer.computeIfPresent(expected.getId(),
                (id, current) -> current == expected ? null : current);
    }

    private void removeOwnerAfterCommit(LoadTask task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            this.removeOwner(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    removeOwner(task);
                }
            }
        });
    }

    private void releasePreviousOwner(int taskId) {
        LoadTask previous = this.runningTaskContainer.get(taskId);
        if (previous == null) {
            return;
        }
        Ex.check(previous.isFinished(), "The previous load task is still finishing");
        previous.lock();
        try {
            HugeGraphLoader owner = previous.getLoader();
            Ex.check(owner == null || owner.context().closed(),
                     "The previous Loader has not closed; finish its cleanup or restart Hubble before retrying");
            this.removeOwner(previous);
        } finally {
            previous.unlock();
        }
    }

    public LoadTask pause(int taskId) {
        Lock control = this.taskControls.get(taskId);
        control.lock();
        try {
            return this.pauseControlled(taskId);
        } finally {
            control.unlock();
        }
    }

    private LoadTask pauseControlled(int taskId) {
        /*
         * The in-memory container is not repopulated on startup, so a task
         * that was RUNNING before a restart is only visible in the database
         */
        LoadTask task = this.runningTaskContainer.get(taskId);
        boolean loading = task != null;
        LoadStatus readStatus = null;
        if (!loading) {
            task = this.get(taskId);
            readStatus = task == null ? null : task.getStatus();
            // A restart may have published an owner during the database read.
            LoadTask owner = this.runningTaskContainer.get(taskId);
            if (owner != null) {
                task = owner;
                loading = true;
            }
        }
        Ex.check(task != null, "load.task.not-exist.id", taskId);
        task.lock();
        try {
            Ex.check(!loading || readStatus == null || task.getStatus() == readStatus,
                     "The load task state has changed; retry the request");
            Ex.check(task.getStatus() == LoadStatus.RUNNING,
                     "Can only pause the RUNNING task");
            if (loading) {
                LoadTask owner = this.runningTaskContainer.putIfAbsent(taskId, task);
                Ex.check(owner == null || owner == task, "The load task has already restarted");
            }
            task.setStatus(LoadStatus.PAUSED);
        } finally {
            task.unlock();
        }
        if (loading) {
            // Let HugeGraphLoader stop
            task.stop();
        }

        this.persistStopped(task, loading, LoadStatus.RUNNING);
        return task;
    }

    public LoadTask resume(int taskId, String token) {
        return this.restart(taskId, token, true);
    }

    public LoadTask stop(int taskId) {
        Lock control = this.taskControls.get(taskId);
        control.lock();
        try {
            return this.stopControlled(taskId);
        } finally {
            control.unlock();
        }
    }

    private LoadTask stopControlled(int taskId) {
        LoadTask task = this.runningTaskContainer.get(taskId);
        boolean loading = task != null;
        LoadStatus readStatus = null;
        if (!loading) {
            task = this.get(taskId);
            readStatus = task == null ? null : task.getStatus();
            // A restart may have published an owner during the database read.
            LoadTask owner = this.runningTaskContainer.get(taskId);
            if (owner != null) {
                task = owner;
                loading = true;
            }
        }
        Ex.check(task != null, "load.task.not-exist.id", taskId);
        LoadStatus status;
        task.lock();
        try {
            status = task.getStatus();
            Ex.check(!loading || readStatus == null || status == readStatus,
                     "The load task state has changed; retry the request");
            Ex.check(status == LoadStatus.RUNNING || status == LoadStatus.PAUSED,
                     "Can only stop the RUNNING or PAUSED task");
            if (loading) {
                LoadTask owner = this.runningTaskContainer.putIfAbsent(taskId, task);
                Ex.check(owner == null || owner == task, "The load task has already restarted");
            }
            task.setStatus(LoadStatus.STOPPED);
        } finally {
            task.unlock();
        }
        /*
         * Only a task taken from the in-memory container owns a loader, one
         * restored from the database has nothing running to interrupt, so
         * just persist the terminal status for it
         */
        if (loading) {
            task.stop();
        }

        this.persistStopped(task, loading, status);
        return task;
    }

    public LoadTask retry(int taskId, String token) {
        return this.restart(taskId, token, false);
    }

    private LoadTask restart(int taskId, String token, boolean incremental) {
        Lock control = this.taskControls.get(taskId);
        control.lock();
        try {
            return this.restartControlled(taskId, token, incremental);
        } finally {
            control.unlock();
        }
    }

    private LoadTask restartControlled(int taskId, String token, boolean incremental) {
        LoadTask task = this.get(taskId);
        Ex.check(task != null, "load.task.not-exist.id", taskId);
        LoadStatus previousStatus = task.getStatus();
        Ex.check(previousStatus == LoadStatus.FAILED ||
                 previousStatus == (incremental ? LoadStatus.PAUSED : LoadStatus.STOPPED),
                 incremental ? "Can only resume the PAUSED or FAILED task" :
                               "Can only retry the FAILED or STOPPED task");
        this.releasePreviousOwner(taskId);
        task.lock();
        try {
            Ex.check(this.runningTaskContainer.putIfAbsent(taskId, task) == null,
                     "The previous load task is still finishing");
            task.getOptions().incrementalMode = incremental;
            task.reconnect(token);
            task.setStatus(LoadStatus.RUNNING);
            if (!incremental) {
                task.setLastDuration(0L);
                task.setCurrDuration(0L);
            }
            this.update(task, previousStatus);
            try {
                this.dispatch(task);
            } catch (RuntimeException failure) {
                this.markDispatchFailed(task, failure);
            }
        } catch (RuntimeException | Error failure) {
            closeUnscheduled(task, failure);
            this.removeClosedOwner(task);
            throw failure;
        } finally {
            task.unlock();
        }
        return task;
    }

    public String readLoadFailedReason(FileMapping mapping) {
        if (mapping == null) {
            return NO_ERROR_FILE_REASON;
        }
        String path = mapping.getPath();
        File parentDir = FileUtils.getFile(path).getParentFile();
        File failureDataDir = FileUtils.getFile(parentDir, "mapping",
                                                "failure-data");
        // list error data file
        File[] errorFiles = failureDataDir.listFiles((dir, name) -> {
            return name.endsWith("error");
        });
        if (errorFiles == null || errorFiles.length == 0) {
            return NO_ERROR_FILE_REASON;
        }
        /*
         * A single run can legitimately produce both a parse error file and
         * an insert error file, report the contents of all of them
         */
        Arrays.sort(errorFiles, Comparator.comparing(File::getName));
        StringBuilder reason = new StringBuilder();
        for (File errorFile : errorFiles) {
            if (reason.length() > 0) {
                reason.append(System.lineSeparator());
            }
            try {
                reason.append(FileUtils.readFileToString(
                              errorFile, Charset.defaultCharset()));
            } catch (IOException e) {
                throw new InternalException("Failed to read error file %s",
                                            e, errorFile);
            }
        }
        return reason.toString();
    }

    public void pauseAllTasks() {
        List<LoadTask> tasks = this.listAll();
        for (LoadTask task : tasks) {
            if (task.getStatus().inRunning()) {
                this.pause(task.getId());
            }
        }
    }

    /**
     * Update progress periodically
     */
    @Async
    @Scheduled(fixedDelay = 1 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateLoadTaskProgress() {
        for (LoadTask task : this.runningTaskContainer.values()) {
            task.lock();
            try {
                if (this.runningTaskContainer.get(task.getId()) != task) {
                    continue;
                }
                HugeGraphLoader owner = task.getLoader();
                if (task.isFinished() && (owner == null || owner.context().closed())) {
                    if (task.isInitialStateReadFailed()) {
                        // Unknown initial metadata must not overwrite a later terminal state
                        // or recreate a missing row.
                        int updated = this.mapper.update(null,
                                Wrappers.<LoadTask>update().eq("id", task.getId())
                                        .eq("load_status", LoadStatus.RUNNING.getValue())
                                        .set("load_status", LoadStatus.FAILED.getValue()));
                        if (updated != 0 && updated != 1) {
                            throw new InternalException("entity.update.failed", task);
                        }
                    } else {
                        // Retry a final write that failed in the runner or pause/stop.
                        this.update(task);
                    }
                    this.removeOwnerAfterCommit(task);
                } else if (task.getStatus().inRunning()) {
                    LoadContext context = task.context();
                    long readLines = context.newProgress().totalInputRead();
                    if (readLines == 0L) {
                        readLines = context.oldProgress().totalInputRead();
                    }
                    task.setFileReadLines(readLines);
                    task.setCurrDuration(context.summary().totalTime());
                    this.update(task);
                }
            } finally {
                task.unlock();
            }
        }
    }

    private LoadTask buildLoadTask(GraphConnection connection,
                                   FileMapping fileMapping, HugeClient client) {
        try {
            LoadOptions options = this.buildLoadOptions(connection, fileMapping);
            // NOTE: For simplicity, one file corresponds to one import task
            LoadMapping mapping = this.buildLoadMapping(connection, fileMapping,
                                                        client);
            this.bindMappingToOptions(options, mapping, fileMapping.getPath());
            return new LoadTask(options, connection, fileMapping);
        } catch (Exception e) {
            Throwable rootCause = Ex.rootCause(e);
            throw new ExternalException("load.build-task.failed", rootCause);
        }
    }

    private void bindMappingToOptions(LoadOptions options, LoadMapping mapping,
                                      String fileMappingPath) {
        String parentDir = new File(fileMappingPath).getParentFile()
                                                    .getAbsolutePath();
        String mappingPath = Paths.get(parentDir, Constant.MAPPING_FILE_NAME)
                                  .toString();
        MappingUtil.write(mapping, mappingPath);
        log.info("Convert mapping file successfuly, stored at {}", mappingPath);
        // NOTE: HugeGraphLoader need specified mapping file(on disk)
        options.file = mappingPath;
    }

    private LoadOptions buildLoadOptions(GraphConnection connection,
                                         FileMapping fileMapping) {
        LoadOptions options = new LoadOptions();
        // Connection params
        options.file = fileMapping.getPath();
        boolean directServer = StringUtils.isNotEmpty(connection.getHost()) ||
                               connection.getPort() != null;
        if (StringUtils.isNotEmpty(connection.getGraphSpace())) {
            options.graphSpace = connection.getGraphSpace();
        }
        if (StringUtils.isNotEmpty(connection.getGraph())) {
            options.graph = connection.getGraph();
        }
        if (!directServer && StringUtils.isNotEmpty(connection.getPdPeers())) {
            options.pdPeers = connection.getPdPeers();
        }
        if (StringUtils.isNotEmpty(connection.getCluster())) {
            options.cluster = connection.getCluster();
        }
        if (StringUtils.isNotEmpty(connection.getRouteType())) {
            options.routeType = connection.getRouteType();
        }
        if (StringUtils.isNotEmpty(connection.getHost())) {
            options.host = connection.getHost();
        }
        if (connection.getPort() != null) {
            options.port = connection.getPort();
        }
        options.username = connection.getUsername();
        options.password = null;
        options.token = connection.getToken();
        options.protocol = StringUtils.isNotEmpty(connection.getProtocol()) ?
                           connection.getProtocol() : "http";
        // Load parameters
        LoadParameter parameter = fileMapping.getLoadParameter();
        options.checkVertex = parameter.isCheckVertex();
        options.timeout = parameter.getInsertTimeout();
        options.maxReadErrors = parameter.getMaxParseErrors();
        options.maxParseErrors = parameter.getMaxParseErrors();
        options.maxInsertErrors = parameter.getMaxInsertErrors();
        options.retryTimes = parameter.getRetryTimes();
        options.retryInterval = parameter.getRetryInterval();
        // Optimized for hubble (conservative defaults)
        options.batchInsertThreads = 4;
        options.singleInsertThreads = 4;
        options.batchSize = 100;
        return options;
    }

    private LoadMapping buildLoadMapping(GraphConnection connection,
                                         FileMapping fileMapping,
                                         HugeClient client) {
        FileSource source = this.buildFileSource(fileMapping);
        log.info("Building load mapping for file: {}, vertices: {}, edges: {}",
                 fileMapping.getName(),
                 fileMapping.getVertexMappings().size(),
                 fileMapping.getEdgeMappings().size());

        List<org.apache.hugegraph.loader.mapping.VertexMapping> vMappings;
        vMappings = this.buildVertexMappings(connection, fileMapping, client);
        List<org.apache.hugegraph.loader.mapping.EdgeMapping> eMappings;
        eMappings = this.buildEdgeMappings(connection, fileMapping, client);

        InputStruct inputStruct = new InputStruct(vMappings, eMappings);
        inputStruct.id("1");
        inputStruct.input(source);
        log.info("Built InputStruct id={}, vertices={}, edges={}",
                 inputStruct.id(), inputStruct.vertices().size(), inputStruct.edges().size());
        return new LoadMapping(ImmutableList.of(inputStruct));
    }

    private FileSource buildFileSource(FileMapping fileMapping) {
        // Set input source
        FileSource source = new FileSource();
        source.path(fileMapping.getPath());

        FileSetting setting = fileMapping.getFileSetting();
        Ex.check(setting.getColumnNames() != null,
                 "Must do file setting firstly");
        source.header(setting.getColumnNames().toArray(new String[]{}));
        source.hasHeader(setting.isHasHeader());
        // NOTE: format and delimiter must be CSV and "," temporarily
        source.format(FileFormat.valueOf(setting.getFormat()));
        source.delimiter(setting.getDelimiter());
        source.charset(setting.getCharset());
        source.dateFormat(setting.getDateFormat());
        source.timeZone(setting.getTimeZone());
        source.skippedLine().regex(setting.getSkippedLine());
        // Set list format
        source.listFormat(new org.apache.hugegraph.loader.source.file.ListFormat());
        ListFormat listFormat = setting.getListFormat();
        source.listFormat().startSymbol(listFormat.getStartSymbol());
        source.listFormat().endSymbol(listFormat.getEndSymbol());
        source.listFormat().elemDelimiter(listFormat.getElemDelimiter());
        return source;
    }

    private List<org.apache.hugegraph.loader.mapping.VertexMapping>
            buildVertexMappings(GraphConnection connection,
                                FileMapping fileMapping, HugeClient client) {
        List<org.apache.hugegraph.loader.mapping.VertexMapping> vMappings =
                new ArrayList<>();
        for (VertexMapping mapping : fileMapping.getVertexMappings()) {
            VertexLabelEntity vl = this.vlService.get(mapping.getLabel(),
                                                      client);
            List<String> idFields = mapping.getIdFields();
            Map<String, String> fieldMappings = mapping.fieldMappingToMap();

            org.apache.hugegraph.loader.mapping.VertexMapping vMapping;
            if (vl.getIdStrategy().isCustomize()) {
                Ex.check(idFields.size() == 1,
                         "When the ID strategy is CUSTOMIZED, you must " +
                         "select a column in the file as the id");
                vMapping = new org.apache.hugegraph.loader.mapping.VertexMapping(
                        idFields.get(0), true);
            } else {
                Ex.check(vl.getIdStrategy().isPrimaryKey(),
                         "Unsupported vertex id strategy: %s",
                         vl.getIdStrategy());
                List<String> primaryKeys = vl.getPrimaryKeys();
                if (idFields == null || idFields.isEmpty()) {
                    idFields = this.inferPrimaryKeyFields(fieldMappings,
                                                          primaryKeys);
                }
                Ex.check(idFields.size() >= 1 &&
                         idFields.size() == primaryKeys.size(),
                         "When the ID strategy is PRIMARY_KEY, you must " +
                         "select at least one column in the file as the " +
                         "primary keys");
                boolean unfold = idFields.size() == 1;
                vMapping = new org.apache.hugegraph.loader.mapping.VertexMapping(
                        null, unfold);
                for (int i = 0; i < primaryKeys.size(); i++) {
                    fieldMappings.put(idFields.get(i), primaryKeys.get(i));
                }
            }
            // set label
            vMapping.label(mapping.getLabel());
            // set field_mapping
            vMapping.mappingFields(fieldMappings);
            // set value_mapping
            vMapping.mappingValues(mapping.valueMappingToMap());
            // set selected fields
            vMapping.selectedFields().addAll(idFields);
            vMapping.selectedFields().addAll(fieldMappings.keySet());
            // set null_values
            Set<Object> nullValues = new HashSet<>();
            nullValues.addAll(mapping.getNullValues().getChecked());
            nullValues.addAll(mapping.getNullValues().getCustomized());
            vMapping.nullValues(nullValues);

            vMappings.add(vMapping);
        }
        return vMappings;
    }

    private List<String> inferPrimaryKeyFields(Map<String, String> fieldMappings,
                                               List<String> primaryKeys) {
        List<String> idFields = new ArrayList<>();
        for (String primaryKey : primaryKeys) {
            for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                if (primaryKey.equals(entry.getValue())) {
                    idFields.add(entry.getKey());
                    break;
                }
            }
        }
        return idFields;
    }

    private List<org.apache.hugegraph.loader.mapping.EdgeMapping>
            buildEdgeMappings(GraphConnection connection,
                              FileMapping fileMapping, HugeClient client) {
        List<org.apache.hugegraph.loader.mapping.EdgeMapping> eMappings =
                new ArrayList<>();
        for (EdgeMapping mapping : fileMapping.getEdgeMappings()) {
            List<String> sourceFields = mapping.getSourceFields();
            List<String> targetFields = mapping.getTargetFields();
            EdgeLabelEntity el = this.elService.get(mapping.getLabel(), client);
            VertexLabelEntity svl = this.vlService.get(el.getSourceLabel(), client);
            VertexLabelEntity tvl = this.vlService.get(el.getTargetLabel(), client);
            Map<String, String> fieldMappings = mapping.fieldMappingToMap();

            boolean unfoldSource = true;
            if (svl.getIdStrategy().isPrimaryKey()) {
                List<String> primaryKeys = svl.getPrimaryKeys();
                Ex.check(sourceFields.size() >= 1 &&
                         sourceFields.size() == primaryKeys.size(),
                         "When the source vertex ID strategy is PRIMARY_KEY, " +
                         "you must select at least one column in the file " +
                         "as the id");
                for (int i = 0; i < primaryKeys.size(); i++) {
                    fieldMappings.put(sourceFields.get(i), primaryKeys.get(i));
                }
                if (sourceFields.size() > 1) {
                    unfoldSource = false;
                }
            }
            boolean unfoldTarget = true;
            if (tvl.getIdStrategy().isPrimaryKey()) {
                List<String> primaryKeys = tvl.getPrimaryKeys();
                Ex.check(targetFields.size() >= 1 &&
                         targetFields.size() == primaryKeys.size(),
                         "When the target vertex ID strategy is PRIMARY_KEY, " +
                         "you must select at least one column in the file " +
                         "as the id");
                for (int i = 0; i < primaryKeys.size(); i++) {
                    fieldMappings.put(targetFields.get(i), primaryKeys.get(i));
                }
                if (targetFields.size() > 1) {
                    unfoldTarget = false;
                }
            }

            org.apache.hugegraph.loader.mapping.EdgeMapping eMapping =
                    new org.apache.hugegraph.loader.mapping.EdgeMapping(
                            sourceFields, unfoldSource, targetFields, unfoldTarget);
            // set label
            eMapping.label(mapping.getLabel());
            // set field_mapping
            eMapping.mappingFields(fieldMappings);
            // set value_mapping
            eMapping.mappingValues(mapping.valueMappingToMap());
            // set selected fields
            eMapping.selectedFields().addAll(sourceFields);
            eMapping.selectedFields().addAll(targetFields);
            eMapping.selectedFields().addAll(fieldMappings.keySet());
            // set null_values
            Set<Object> nullValues = new HashSet<>();
            nullValues.addAll(mapping.getNullValues().getChecked());
            nullValues.addAll(mapping.getNullValues().getCustomized());
            eMapping.nullValues(nullValues);

            eMappings.add(eMapping);
        }
        return eMappings;
    }

    public void startCovid19(GraphConnection connection,
                                 String graphSpace, String graph,
                                 HugeClient client) {
        FileMapping fileMapping =
                new FileMapping(graphSpace, graph, "covid19",
                                "example/covid19/struct.json");
        LoadParameter loadParameter = new LoadParameter();
        fileMapping.setLoadParameter(loadParameter);

        LoadOptions options = this.buildLoadOptions(connection, fileMapping);
        // options.direct = true;
        // options.pdPeers = connection.getPdPeers();
        options.schema = "example/covid19/schema.groovy";
        options.host = connection.getHost();
        options.port = connection.getPort();
        options.protocol = connection.getProtocol();
        loader(options);
    }

    public void loader(LoadOptions options) {
        HugeGraphLoader loader = null;
        Throwable failure = null;
        try {
            loader = new HugeGraphLoader(options);
            loader.load();
        } catch (Throwable e) {
            failure = e;
        } finally {
            if (loader != null) {
                try {
                    loader.shutdown();
                } catch (Throwable e) {
                    if (failure == null) {
                        failure = e;
                    } else if (failure != e) {
                        failure.addSuppressed(e);
                    }
                }
            }
        }
        if (failure != null) {
            Printer.printError("Failed to start loading", failure);
        }
    }
}
