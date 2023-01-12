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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.entity.enums.LoadStatus;
import org.apache.hugegraph.entity.load.EdgeMapping;
import org.apache.hugegraph.entity.load.FileMapping;
import org.apache.hugegraph.entity.load.FileSetting;
import org.apache.hugegraph.entity.load.ListFormat;
import org.apache.hugegraph.entity.load.LoadParameter;
import org.apache.hugegraph.entity.load.LoadTask;
import org.apache.hugegraph.entity.load.VertexMapping;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.handler.LoadTaskExecutor;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.source.file.FileFormat;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.util.MappingUtil;
import org.apache.hugegraph.mapper.load.LoadTaskMapper;
import org.apache.hugegraph.service.SettingSSLService;
import org.apache.hugegraph.util.Ex;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableList;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class LoadTaskService {

    @Autowired
    private LoadTaskMapper mapper;
    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;
    @Autowired
    private LoadTaskExecutor taskExecutor;
    @Autowired
    private SettingSSLService sslService;
    @Autowired
    private HugeConfig config;

    private Map<Integer, LoadTask> runningTaskContainer;

    public LoadTaskService() {
        this.runningTaskContainer = new ConcurrentHashMap<>();
    }

    public LoadTask get(int id) {
        return this.mapper.selectById(id);
    }

    public List<LoadTask> listAll() {
        return this.mapper.selectList(null);
    }

    public IPage<LoadTask> list(int connId, int jobId, int pageNo, int pageSize) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("conn_id", connId);
        query.eq("job_id", jobId);
        query.orderByDesc("create_time");
        Page<LoadTask> page = new Page<>(pageNo, pageSize);
        return this.mapper.selectPage(page, query);
    }

    public List<LoadTask> list(int connId, List<Integer> taskIds) {
        return this.mapper.selectBatchIds(taskIds);
    }

    public int count() {
        return this.mapper.selectCount(null);
    }

    public int taskCountByJob(int jobId) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("job_id", jobId);
        return this.mapper.selectCount(query);
    }

    public List<LoadTask> taskListByJob(int jobId) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("job_id", jobId);
        return this.mapper.selectList(query);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(LoadTask entity) {
        if (this.mapper.insert(entity) != 1) {
            throw new InternalException("entity.insert.failed", entity);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void update(LoadTask entity) {
        if (this.mapper.updateById(entity) != 1) {
            throw new InternalException("entity.update.failed", entity);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(int id) {
        this.runningTaskContainer.remove(id);
        if (this.mapper.deleteById(id) != 1) {
            throw new InternalException("entity.delete.failed", id);
        }
    }

    public List<LoadTask> batchTasks(int jobId) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("job_id", jobId);
        return this.mapper.selectList(query);
    }

    public LoadTask start(GraphConnection connection, FileMapping fileMapping) {
        this.sslService.configSSL(this.config, connection);
        LoadTask task = this.buildLoadTask(connection, fileMapping);
        this.save(task);
        // Executed in other threads
        this.taskExecutor.execute(task, () -> this.update(task));
        // Save current load task
        this.runningTaskContainer.put(task.getId(), task);
        return task;
    }

    public LoadTask pause(int taskId) {
        LoadTask task = this.runningTaskContainer.get(taskId);
        Ex.check(task.getStatus() == LoadStatus.RUNNING,
                 "Can only pause the RUNNING task");
        // Mark status as paused, should set before context.stopLoading()
        task.setStatus(LoadStatus.PAUSED);
        // Let HugeGraphLoader stop
        task.stop();

        task.lock();
        try {
            this.update(task);
            this.runningTaskContainer.remove(taskId);
        } finally {
            task.unlock();
        }
        return task;
    }

    public LoadTask resume(int taskId) {
        LoadTask task = this.get(taskId);
        Ex.check(task.getStatus() == LoadStatus.PAUSED ||
                 task.getStatus() == LoadStatus.FAILED,
                 "Can only resume the PAUSED or FAILED task");
        task.lock();
        try {
            // Set work mode in incrental mode, load from last breakpoint
            task.getOptions().incrementalMode = true;
            task.setStatus(LoadStatus.RUNNING);
            this.update(task);
            this.taskExecutor.execute(task, () -> this.update(task));
            this.runningTaskContainer.put(taskId, task);
        } finally {
            task.unlock();
        }
        return task;
    }

    public LoadTask stop(int taskId) {
        LoadTask task = this.runningTaskContainer.get(taskId);
        if (task == null) {
            task = this.get(taskId);
        }
        LoadStatus status = task.getStatus();
        Ex.check(status == LoadStatus.RUNNING || status == LoadStatus.PAUSED,
                 "Can only stop the RUNNING or PAUSED task");
        // Mark status as stopped
        task.setStatus(LoadStatus.STOPPED);
        if (status == LoadStatus.RUNNING) {
            task.stop();
        }

        task.lock();
        try {
            this.update(task);
            this.runningTaskContainer.remove(taskId);
        } finally {
            task.unlock();
        }
        return task;
    }

    public LoadTask retry(int taskId) {
        LoadTask task = this.get(taskId);
        Ex.check(task.getStatus() == LoadStatus.FAILED ||
                 task.getStatus() == LoadStatus.STOPPED,
                 "Can only retry the FAILED or STOPPED task");
        task.lock();
        try {
            // Set work mode in normal mode, load from begin
            task.getOptions().incrementalMode = false;
            task.setStatus(LoadStatus.RUNNING);
            task.setLastDuration(0L);
            task.setCurrDuration(0L);
            this.update(task);
            this.taskExecutor.execute(task, () -> this.update(task));
            this.runningTaskContainer.put(taskId, task);
        } finally {
            task.unlock();
        }
        return task;
    }

    public String readLoadFailedReason(FileMapping mapping) {
        String path = mapping.getPath();
        File parentDir = FileUtils.getFile(path).getParentFile();
        File failureDataDir = FileUtils.getFile(parentDir, "mapping",
                                                "failure-data");
        // list error data file
        File[] errorFiles = failureDataDir.listFiles((dir, name) -> {
            return name.endsWith("error");
        });
        if (errorFiles == null) {
            return "For some reason, the error file was not generated. " +
                   "Please check the log for details";
        }
        Ex.check(errorFiles.length == 1,
                 "There should exist only one error file, actual is %s",
                 errorFiles.length);
        File errorFile = errorFiles[0];
        try {
            return FileUtils.readFileToString(errorFile);
        } catch (IOException e) {
            throw new InternalException("Failed to read error file %s",
                                        e, errorFile);
        }
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
            if (!task.getStatus().inRunning()) {
                continue;
            }
            task.lock();
            try {
                if (task.getStatus().inRunning()) {
                    LoadContext context = task.context();
                    long readLines = context.newProgress().totalInputRead();
                    if (readLines == 0L) {
                        /*
                         * When the Context is just constructed, newProgress
                         * is empty. Only after parsing is started will use
                         * oldProgress and incrementally update newProgress,
                         * if get totalInputRead value during this process,
                         * it will return 0, so need read it from oldProgress
                         */
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
                                   FileMapping fileMapping) {
        try {
            LoadOptions options = this.buildLoadOptions(connection, fileMapping);
            // NOTE: For simplicity, one file corresponds to one import task
            LoadMapping mapping = this.buildLoadMapping(connection, fileMapping);
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
        // Fill with input and server params
        options.file = fileMapping.getPath();
        // No need to specify a schema file
        options.host = connection.getHost();
        options.port = connection.getPort();
        options.graph = connection.getGraph();
        options.username = connection.getUsername();
        options.token = connection.getPassword();
        options.protocol = connection.getProtocol();
        options.trustStoreFile = connection.getTrustStoreFile();
        options.trustStoreToken = connection.getTrustStorePassword();
        // Fill with load parameters
        LoadParameter parameter = fileMapping.getLoadParameter();
        options.checkVertex = parameter.isCheckVertex();
        options.timeout = parameter.getInsertTimeout();
        options.maxReadErrors = parameter.getMaxParseErrors();
        options.maxParseErrors = parameter.getMaxParseErrors();
        options.maxInsertErrors = parameter.getMaxInsertErrors();
        options.retryTimes = parameter.getRetryTimes();
        options.retryInterval = parameter.getRetryInterval();
        // Optimized for hubble
        options.batchInsertThreads = 4;
        options.singleInsertThreads = 4;
        options.batchSize = 100;
        return options;
    }

    private LoadMapping buildLoadMapping(GraphConnection connection,
                                         FileMapping fileMapping) {
        FileSource source = this.buildFileSource(fileMapping);

        List<org.apache.hugegraph.loader.mapping.VertexMapping> vMappings;
        vMappings = this.buildVertexMappings(connection, fileMapping);
        List<org.apache.hugegraph.loader.mapping.EdgeMapping> eMappings;
        eMappings = this.buildEdgeMappings(connection, fileMapping);

        InputStruct inputStruct = new InputStruct(vMappings, eMappings);
        inputStruct.id("1");
        inputStruct.input(source);
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
                                FileMapping fileMapping) {
        int connId = connection.getId();
        List<org.apache.hugegraph.loader.mapping.VertexMapping> vMappings =
                new ArrayList<>();
        for (VertexMapping mapping : fileMapping.getVertexMappings()) {
            VertexLabelEntity vl = this.vlService.get(mapping.getLabel(), connId);
            List<String> idFields = mapping.getIdFields();
            Map<String, String> fieldMappings = mapping.fieldMappingToMap();
            org.apache.hugegraph.loader.mapping.VertexMapping vMapping;
            if (vl.getIdStrategy().isCustomize()) {
                Ex.check(idFields.size() == 1,
                         "When the ID strategy is CUSTOMIZED, you must " +
                         "select a column in the file as the id");
                vMapping = new org.apache.hugegraph.loader.mapping.VertexMapping(idFields.get(0), true);
            } else {
                assert vl.getIdStrategy().isPrimaryKey();
                List<String> primaryKeys = vl.getPrimaryKeys();
                Ex.check(idFields.size() >= 1 &&
                         idFields.size() == primaryKeys.size(),
                         "When the ID strategy is PRIMARY_KEY, you must " +
                         "select at least one column in the file as the " +
                         "primary keys");
                /*
                 * The id column can be unfold into multi sub-ids only
                 * when primarykeys contains just one field
                 */
                boolean unfold = idFields.size() == 1;
                vMapping = new org.apache.hugegraph.loader.mapping.VertexMapping(null, unfold);
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
            // set selected
            vMapping.selectedFields().addAll(idFields);
            vMapping.selectedFields().addAll(fieldMappings.keySet());
            // set null_values
            Set<Object> nullValues = new HashSet<>();
            nullValues.addAll(mapping.getNullValues().getChecked());
            nullValues.addAll(mapping.getNullValues().getCustomized());
            vMapping.nullValues(nullValues);
            // TODO: Update strategies
            vMappings.add(vMapping);
        }
        return vMappings;
    }

    private List<org.apache.hugegraph.loader.mapping.EdgeMapping>
            buildEdgeMappings(GraphConnection connection,
                              FileMapping fileMapping) {
        int connId = connection.getId();
        List<org.apache.hugegraph.loader.mapping.EdgeMapping> eMappings =
                new ArrayList<>();
        for (EdgeMapping mapping : fileMapping.getEdgeMappings()) {
            List<String> sourceFields = mapping.getSourceFields();
            List<String> targetFields = mapping.getTargetFields();
            EdgeLabelEntity el = this.elService.get(mapping.getLabel(), connId);
            VertexLabelEntity svl = this.vlService.get(el.getSourceLabel(),
                                                       connId);
            VertexLabelEntity tvl = this.vlService.get(el.getTargetLabel(),
                                                       connId);
            Map<String, String> fieldMappings = mapping.fieldMappingToMap();
            /*
             * When id strategy is customize or primaryKeys contains
             * just one field, the param 'unfold' can be true
             */
            boolean unfoldSource = true;
            if (svl.getIdStrategy().isPrimaryKey()) {
                List<String> primaryKeys = svl.getPrimaryKeys();
                Ex.check(sourceFields.size() >= 1 &&
                         sourceFields.size() == primaryKeys.size(),
                         "When the source vertex ID strategy is CUSTOMIZED, " +
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
                         "When the target vertex ID strategy is CUSTOMIZED, " +
                         "you must select at least one column in the file " +
                         "as the id");
                for (int i = 0; i < primaryKeys.size(); i++) {
                    fieldMappings.put(targetFields.get(i), primaryKeys.get(i));
                }
                if (targetFields.size() > 1) {
                    unfoldTarget = false;
                }
            }

            org.apache.hugegraph.loader.mapping.EdgeMapping eMapping;
            eMapping = new org.apache.hugegraph.loader.mapping.EdgeMapping(
                       sourceFields, unfoldSource, targetFields, unfoldTarget);
            // set label
            eMapping.label(mapping.getLabel());
            // set field_mapping
            eMapping.mappingFields(fieldMappings);
            // set value_mapping
            eMapping.mappingValues(mapping.valueMappingToMap());
            // set selected
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
}
