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
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.struct.GraphStruct;
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

    private static final FailureLogger FAILURE_LOGGER = FailureLogger.parse();

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
        LOG.info("Start loading");
        try {
            // Create schema
            this.createSchema();
            // Load vertices
            this.load(ElemType.VERTEX);
            // Load edges
            this.load(ElemType.EDGE);
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

    private long lastLoadedCount(ElemType type) {
        if (this.context.options().incrementalMode) {
            return this.context.oldProgress().totalLoaded(type);
        } else {
            return 0L;
        }
    }

    private void load(ElemType type) {
        Printer.printRealTimeProgress(type, this.lastLoadedCount(type));

        LoadSummary summary = this.context.summary();
        InputProgressMap newProgress = this.context.newProgress().get(type);
        StopWatch totalTime = StopWatch.createStarted();
        for (ElementStruct struct : this.graphStruct.structs(type)) {
            StopWatch loadTime = StopWatch.createStarted();

            LoadMetrics metrics = summary.metrics(struct);
            // Update loading vertex/edge struct
            newProgress.addStruct(struct);
            // Produce batch of vertices/edges and execute loading tasks
            try (ElementBuilder<?> builder = ElementBuilder.of(this.context,
                                                               struct)) {
                this.load(builder, this.context.options(), metrics);
            }

            /*
             * NOTE: the load task of this struct may not be completed,
             * so the load rate of each struct is an inaccurate value.
             */
            loadTime.stop();
            metrics.loadTime(loadTime.getTime(TimeUnit.MILLISECONDS));
            LOG.info("Loading {} '{}' with average rate: {}/s",
                     metrics.loadSuccess(), struct, metrics.averageLoadRate());
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(type);
        totalTime.stop();
        summary.totalTime(type, totalTime.getTime(TimeUnit.MILLISECONDS));

        Printer.print(summary.accumulateMetrics(type).loadSuccess());
    }

    private <GE extends GraphElement> void load(ElementBuilder<GE> builder,
                                                LoadOptions options,
                                                LoadMetrics metrics) {
        ElementStruct struct = builder.struct();
        LOG.info("Start parsing and loading '{}'", struct);
        StopWatch parseWatch = StopWatch.createStarted();

        ElemType type = struct.type();
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

                FAILURE_LOGGER.error(type, e);
                long failureNum = metrics.increaseParseFailure();
                if (failureNum >= options.maxParseErrors) {
                    Printer.printError("Exceed %s %s parsing error... stopping",
                                       options.maxParseErrors, type);
                    this.stopLoading(Constants.EXIT_CODE_ERROR);
                }
                continue;
            }
            if (batch.size() >= options.batchSize) {
                metrics.plusParseSuccess(batch.size());
                if (!options.dryRun) {
                    /*
                     * NOTE: parse time doesn't include submit batch time,
                     * it's accurate
                     */
                    parseWatch.suspend();
                    this.taskManager.submitBatch(struct, batch);
                    parseWatch.resume();
                }
                batch = new ArrayList<>(options.batchSize);
            }
        }
        parseWatch.stop();
        if (!batch.isEmpty()) {
            metrics.plusParseSuccess(batch.size());
            if (!options.dryRun) {
                parseWatch.suspend();
                this.taskManager.submitBatch(struct, batch);
                parseWatch.resume();
            }
        }

        metrics.parseTime(parseWatch.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Parsing {} '{}' with average rate: {}/s",
                 metrics.parseSuccess(), struct, metrics.parseRate());
    }

    private void stopLoading(int code) {
        LOG.info("Stop loading");
        // Shutdown task manager
        this.taskManager.shutdown();
        HugeClientWrapper.close();
        // Exit JVM if the code is not EXIT_CODE_NORM
        if (Constants.EXIT_CODE_NORM != code) {
            LoadUtil.exit(code);
        }
    }
}
