/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.service.load;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.entity.GraphConnection;
import com.baidu.hugegraph.entity.enums.LoadStatus;
import com.baidu.hugegraph.entity.load.EdgeMapping;
import com.baidu.hugegraph.entity.load.FileMapping;
import com.baidu.hugegraph.entity.load.FileSetting;
import com.baidu.hugegraph.entity.load.ListFormat;
import com.baidu.hugegraph.entity.load.LoadParameter;
import com.baidu.hugegraph.entity.load.LoadTask;
import com.baidu.hugegraph.entity.load.VertexMapping;
import com.baidu.hugegraph.entity.schema.EdgeLabelEntity;
import com.baidu.hugegraph.entity.schema.VertexLabelEntity;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.handler.LoadTaskExecutor;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.util.MappingUtil;
import com.baidu.hugegraph.mapper.load.LoadTaskMapper;
import com.baidu.hugegraph.service.SettingSSLService;
import com.baidu.hugegraph.service.schema.EdgeLabelService;
import com.baidu.hugegraph.service.schema.VertexLabelService;
import com.baidu.hugegraph.util.Ex;
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

    private Map<Integer, LoadTask> taskContainer;

    public LoadTaskService() {
        this.taskContainer = new ConcurrentHashMap<>();
    }

    public Map<Integer, LoadTask> getTaskContainer() {
        return this.taskContainer;
    }

    public LoadTask get(int id) {
        return this.mapper.selectById(id);
    }

    public List<LoadTask> listAll() {
        return this.mapper.selectList(null);
    }

    public IPage<LoadTask> list(int connId, int pageNo, int pageSize) {
        QueryWrapper<LoadTask> query = Wrappers.query();
        query.eq("conn_id", connId);
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int save(LoadTask entity) {
        return this.mapper.insert(entity);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int update(LoadTask entity) {
        return this.mapper.updateById(entity);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int remove(int id) {
        this.taskContainer.remove(id);
        return this.mapper.deleteById(id);
    }

    public LoadTask start(GraphConnection connection, FileMapping fileMapping) {
        connection = sslService.configSSL(this.config, connection);
        LoadTask task = this.buildLoadTask(connection, fileMapping);
        if (this.save(task) != 1) {
            throw new InternalException("entity.insert.failed", task);
        }
        this.taskExecutor.execute(task);

        if (this.update(task) != 1) {
            throw new InternalException("entity.update.failed", task);
        }
        // Save current load task
        this.taskContainer.put(task.getId(), task);
        return task;
    }

    public LoadTask pause(int taskId) {
        LoadTask task = this.taskContainer.get(taskId);
        Ex.check(task.getStatus() == LoadStatus.RUNNING,
                 "Can only pause the RUNNING task");
        LoadContext context = task.context();
        // Mark status as paused, should set before context.stopLoading()
        task.setStatus(LoadStatus.PAUSED);
        // Let HugeGraphLoader stop
        context.stopLoading();

        task.setFileReadLines(context.newProgress().totalInputReaded());
        task.setDuration(context.summary().totalTime());
        if (update(task) != 1) {
            throw new InternalException("entity.update.failed", task);
        }
        this.taskContainer.remove(taskId);
        return task;
    }

    public LoadTask resume(int taskId) {
        LoadTask task = this.get(taskId);
        Ex.check(task.getStatus() == LoadStatus.PAUSED ||
                 task.getStatus() == LoadStatus.FAILED,
                 "Can only resume the PAUSED or FAILED task");
        task.restoreContext();
        task.setStatus(LoadStatus.RUNNING);
        // Set work mode in incrental mode, load from last breakpoint
        task.context().options().incrementalMode = true;
        this.taskExecutor.execute(task);

        if (this.update(task) != 1) {
            throw new InternalException("entity.update.failed", task);
        }
        this.taskContainer.put(taskId, task);
        return task;
    }

    public LoadTask stop(int taskId) {
        LoadTask task = this.taskContainer.get(taskId);
        if (task == null) {
            task = this.get(taskId);
            task.restoreContext();
        }
        Ex.check(task.getStatus() == LoadStatus.RUNNING ||
                 task.getStatus() == LoadStatus.PAUSED,
                 "Can only stop the RUNNING or PAUSED task");
        LoadContext context = task.context();
        // Mark status as stopped
        task.setStatus(LoadStatus.STOPPED);
        context.stopLoading();

        task.setFileReadLines(context.newProgress().totalInputReaded());
        task.setDuration(context.summary().totalTime());
        if (update(task) != 1) {
            throw new InternalException("entity.update.failed", task);
        }
        this.taskContainer.remove(taskId);
        return task;
    }

    public LoadTask retry(int taskId) {
        LoadTask task = this.get(taskId);
        Ex.check(task.getStatus() == LoadStatus.FAILED ||
                 task.getStatus() == LoadStatus.STOPPED,
                 "Can only retry the FAILED or STOPPED task");
        task.restoreContext();

        task.setStatus(LoadStatus.RUNNING);
        // Set work mode in normal mode, load from begin
        task.context().options().incrementalMode = false;
        this.taskExecutor.execute(task);

        if (this.update(task) != 1) {
            throw new InternalException("entity.update.failed", task);
        }
        this.taskContainer.put(taskId, task);
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
        Ex.check(errorFiles != null && errorFiles.length == 1,
                 "There should exist one error file, actual is %s",
                 Arrays.toString(errorFiles));
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
    @Scheduled(fixedDelay = 5 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateLoadTaskProgress() {
        Map<Integer, LoadTask> taskContainer = this.getTaskContainer();
        Iterator<Map.Entry<Integer, LoadTask>> iter = taskContainer.entrySet()
                                                                   .iterator();
        iter.forEachRemaining(entry -> {
            LoadTask task = entry.getValue();
            LoadContext context = task.context();
            task.setFileReadLines(context.newProgress().totalInputReaded());
            task.setDuration(context.summary().totalTime());
            if (this.update(task) != 1) {
                throw new InternalException("entity.update.failed", task);
            }
        });
    }

    private LoadTask buildLoadTask(GraphConnection connection,
                                   FileMapping fileMapping) {
        LoadOptions options = this.buildLoadOptions(connection, fileMapping);
        // NOTE: For simplicity, one file corresponds to one import task
        LoadMapping mapping = this.buildLoadMapping(connection, fileMapping);
        this.bindMappingToOptions(options, mapping, fileMapping.getPath());
        return new LoadTask(options, connection, fileMapping);
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
        options.token = connection.getPassword();
        options.protocol = connection.getProtocol();
        options.trustStoreFile = connection.getTrustStoreFile();
        options.trustStorePassword = connection.getTrustStorePassword();
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

        List<com.baidu.hugegraph.loader.mapping.VertexMapping> vMappings;
        vMappings = this.buildVertexMappings(connection, fileMapping);
        List<com.baidu.hugegraph.loader.mapping.EdgeMapping> eMappings;
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
        source.listFormat(new com.baidu.hugegraph.loader.source.file.ListFormat());
        ListFormat listFormat = setting.getListFormat();
        source.listFormat().startSymbol(listFormat.getStartSymbol());
        source.listFormat().endSymbol(listFormat.getEndSymbol());
        source.listFormat().elemDelimiter(listFormat.getElemDelimiter());
        return source;
    }

    private List<com.baidu.hugegraph.loader.mapping.VertexMapping>
            buildVertexMappings(GraphConnection connection,
                                FileMapping fileMapping) {
        int connId = connection.getId();
        List<com.baidu.hugegraph.loader.mapping.VertexMapping> vMappings =
                new ArrayList<>();
        for (VertexMapping mapping : fileMapping.getVertexMappings()) {
            VertexLabelEntity vl = this.vlService.get(mapping.getLabel(), connId);
            List<String> idFields = mapping.getIdFields();
            Map<String, String> fieldMappings = mapping.fieldMappingToMap();
            com.baidu.hugegraph.loader.mapping.VertexMapping vMapping;
            if (vl.getIdStrategy().isCustomize()) {
                Ex.check(idFields.size() == 1,
                         "When the ID strategy is CUSTOMIZED, you must " +
                         "select a column in the file as the id");
                vMapping = new com.baidu.hugegraph.loader.mapping
                                  .VertexMapping(idFields.get(0), true);
            } else {
                assert vl.getIdStrategy().isPrimaryKey();
                List<String> primaryKeys = vl.getPrimaryKeys();
                Ex.check(idFields.size() >= 1 &&
                         idFields.size() == primaryKeys.size(),
                         "When the ID strategy is PRIMARY_KEY, you must " +
                         "select at least one column in the file as the " +
                         "primary keys");
                vMapping = new com.baidu.hugegraph.loader.mapping
                                  .VertexMapping(null, true);
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

    private List<com.baidu.hugegraph.loader.mapping.EdgeMapping>
            buildEdgeMappings(GraphConnection connection,
                              FileMapping fileMapping) {
        int connId = connection.getId();
        List<com.baidu.hugegraph.loader.mapping.EdgeMapping> eMappings =
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
            }
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
            }

            com.baidu.hugegraph.loader.mapping.EdgeMapping eMapping;
            eMapping = new com.baidu.hugegraph.loader.mapping.EdgeMapping(
                       sourceFields, true, targetFields, true);
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
