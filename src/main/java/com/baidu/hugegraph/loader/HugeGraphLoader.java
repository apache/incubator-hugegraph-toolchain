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

package com.baidu.hugegraph.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.struct.EdgeStruct;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.struct.GraphStruct;
import com.baidu.hugegraph.loader.struct.VertexStruct;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.Log;

public final class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);

    private final FailureLogger failureLogger = FailureLogger.parse();

    private final LoadContext context;
    private final GraphStruct graphStruct;
    private final TaskManager taskManager;

    public static void main(String[] args) {
        HugeGraphLoader loader = new HugeGraphLoader(args);
        loader.load();
    }

    private HugeGraphLoader(String[] args) {
        this.context = new LoadContext(args);
        this.graphStruct = GraphStruct.of(this.context);
        this.taskManager = new TaskManager(this.context);
        this.addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoadOptions options = this.context.options();
            try {
                this.context.newProgress().write(options.file);
            } catch (IOException e) {
                LOG.warn("Failed to write load progress", e);
            }
        }));
    }

    private void load() {
        try {
            // Create schema
            this.createSchema();
            // Load vertices
            this.loadVertices();
            // Load edges
            this.loadEdges();
        } catch (Exception e) {
            Printer.printError("Failed to load due to " + e.getMessage(), e);
            this.taskManager.shutdown();
            throw e;
        }

        // Print load summary
        Printer.printSummary(this.context);
        this.stopLoading(Constants.EXIT_CODE_NORM);
    }

    private void createSchema() {
        LoadOptions options = this.context.options();
        if (StringUtils.isEmpty(options.schema)) {
            return;
        }
        File schemaFile = FileUtils.getFile(options.schema);
        HugeClient client = HugeClientWrapper.get(options);
        GroovyExecutor groovyExecutor = new GroovyExecutor();
        groovyExecutor.bind("schema", client.schema());
        String script;
        try {
            script = FileUtils.readFileToString(schemaFile, Constants.CHARSET);
        } catch (IOException e) {
            throw new LoadException("Read schema file '%s' error",
                                    e, options.schema);
        }
        groovyExecutor.execute(script, client);
    }

    private void loadVertices() {
        Printer.printElemType(ElemType.VERTEX);
        StopWatch loadWatch = new StopWatch();

        LoadSummary summary = this.context.summary();
        InputProgressMap newProgress = this.context.newProgress().vertex();
        for (VertexStruct struct : this.graphStruct.vertexStructs()) {
            LoadMetrics metrics = summary.metrics(struct);
            loadWatch.start();

            // Update loading vertex struct
            newProgress.addStruct(struct);
            // Produce batch vertices and execute loading tasks
            try (VertexBuilder builder = new VertexBuilder(this.context,
                                                           struct)) {
                this.load(builder, this.context.options(), metrics);
            }

            loadWatch.stop();
            metrics.loadTime(loadWatch.getTime(TimeUnit.SECONDS));
            loadWatch.reset();
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(ElemType.VERTEX);

        Printer.print(summary.accumulateMetrics(ElemType.VERTEX).loadSuccess());
    }

    private void loadEdges() {
        Printer.printElemType(ElemType.EDGE);
        StopWatch loadWatch = new StopWatch();

        LoadSummary summary = this.context.summary();
        InputProgressMap newProgress = this.context.newProgress().edge();
        for (EdgeStruct struct : this.graphStruct.edgeStructs()) {
            LoadMetrics metrics = summary.metrics(struct);
            loadWatch.start();

            // Update loading edge struct
            newProgress.addStruct(struct);
            // Produce batch edges and execute loading tasks
            try (EdgeBuilder builder = new EdgeBuilder(this.context, struct)) {
                this.load(builder, this.context.options(), metrics);
            }

            loadWatch.stop();
            metrics.loadTime(loadWatch.getTime(TimeUnit.SECONDS));
            loadWatch.reset();
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(ElemType.EDGE);

        Printer.print(summary.accumulateMetrics(ElemType.EDGE).loadSuccess());
    }

    private <GE extends GraphElement> void load(ElementBuilder<GE> builder,
                                                LoadOptions options,
                                                LoadMetrics metrics) {
        ElementStruct struct = builder.struct();
        ElemType type = struct.type();
        LOG.info("Start parsing and loading data source for {} '{}'",
                 type, struct.label());
        StopWatch parseWatch = StopWatch.createStarted();

        List<GE> batch = new ArrayList<>(options.batchSize);
        while (true) {
            try {
                if (!builder.hasNext()) {
                    break;
                }
                GE element = builder.next();
                batch.add(element);
            } catch (ParseException e) {
                if (options.testMode) {
                    throw e;
                }
                LOG.error("Parse {} error", type, e);

                this.failureLogger.error(type, e);
                long failureNum = metrics.increaseParseFailure();
                if (failureNum >= options.maxParseErrors) {
                    Printer.printError("More than %s %s parsing error ... " +
                                       "stopping", options.maxParseErrors, type);
                    this.stopLoading(Constants.EXIT_CODE_ERROR);
                }
                continue;
            }
            if (batch.size() >= options.batchSize) {
                metrics.plusParseSuccess(options.batchSize);
                // Parse time doesn't include load time
                parseWatch.suspend();
                this.taskManager.submitBatch(struct, batch);
                parseWatch.resume();
                batch = new ArrayList<>(options.batchSize);
            }
        }
        if (!batch.isEmpty()) {
            metrics.plusParseSuccess(batch.size());
            parseWatch.suspend();
            this.taskManager.submitBatch(struct, batch);
            parseWatch.resume();
        }

        parseWatch.stop();
        metrics.parseTime(parseWatch.getTime(TimeUnit.SECONDS));
        LOG.info("Finish parsing {} '{}' {}, the average rate is {}/s",
                 metrics.parseSuccess(), struct.label(), type.string(),
                 metrics.parseRate());
    }

    private void stopLoading(int code) {
        // Shutdown task manager
        this.taskManager.shutdown();
        HugeClientWrapper.close();
        // Exit JVM if the code is not EXIT_CODE_NORM
        if (Constants.EXIT_CODE_NORM != code) {
            LoadUtil.exit(code);
        }
    }
}
