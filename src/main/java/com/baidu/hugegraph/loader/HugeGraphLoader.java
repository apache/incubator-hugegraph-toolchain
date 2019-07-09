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
import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.failure.FailureLogger;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.struct.GraphStruct;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.Log;

public final class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);

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
            this.shutdown();
        }));
    }

    private void load() {
        try {
            // Create schema
            this.createSchema();
            // Load vertices
            this.load(ElemType.VERTEX);
            // Load edges
            this.load(ElemType.EDGE);
        } catch (Exception e) {
            Printer.printError("Failed to load due to " + e.getMessage(), e);
            this.shutdown();
            throw e;
        }
        // Print load summary
        Printer.printSummary(this.context);
        this.shutdown();
    }

    private void createSchema() {
        LoadOptions options = this.context.options();
        if (StringUtils.isEmpty(options.schema)) {
            return;
        }
        File schemaFile = FileUtils.getFile(options.schema);
        HugeClient client = HugeClientHolder.get(options);
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
        LOG.info("Start loading {}", type.string());
        Printer.printRealTimeProgress(type, this.lastLoadedCount(type));

        this.context.loadingType(type);
        LoadSummary summary = this.context.summary();
        InputProgressMap newProgress = this.context.newProgress().get(type);
        StopWatch totalTime = StopWatch.createStarted();
        for (ElementStruct struct : this.graphStruct.structs(type)) {
            StopWatch loadTime = StopWatch.createStarted();
            LoadMetrics metrics = summary.metrics(struct);
            if (!this.context.stopped()) {
                // Update loading vertex/edge struct
                newProgress.addStruct(struct);
                // Produce batch of vertices/edges and execute loading tasks
                try (ElementBuilder<?> builder = ElementBuilder.of(this.context,
                                                                   struct)) {
                    this.load(this.context, builder);
                }
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

    private <GE extends GraphElement> void load(LoadContext context,
                                                ElementBuilder<GE> builder) {
        ElementStruct struct = builder.struct();
        LOG.info("Start parsing and loading '{}'", struct);
        LoadOptions options = context.options();
        LoadMetrics metrics = context.summary().metrics(struct);
        FailureLogger logger = context.failureLogger(struct);

        StopWatch parseTime = StopWatch.createStarted();
        List<Record<GE>> batch = new ArrayList<>(options.batchSize);
        for (boolean finished = false; !finished;) {
            try {
                if (builder.hasNext()) {
                    Record<GE> element = builder.next();
                    batch.add(element);
                } else {
                    finished = true;
                }
            } catch (ParseException e) {
                if (options.testMode) {
                    throw e;
                }
                LOG.error("Parse {} error", struct.type(), e);
                // Write to current struct's parse failure log
                logger.error(e);
                long failureNum = metrics.increaseParseFailure();
                if (failureNum >= options.maxParseErrors) {
                    Printer.printError("More than %s %s parsing error, stop " +
                                       "parsing and waiting all insert tasks " +
                                       "finished",
                                       options.maxParseErrors,
                                       struct.type().string());
                    this.context.stopLoading();
                }
                continue;
            }
            if (batch.size() >= options.batchSize ||
                (finished && !batch.isEmpty())) {
                this.submit(struct, batch, options, metrics, parseTime);
                builder.confirmOffset();
                if (!finished) {
                    batch = new ArrayList<>(options.batchSize);
                }
            }
        }

        parseTime.stop();
        metrics.parseTime(parseTime.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Parsing {} '{}' with average rate: {}/s",
                 metrics.parseSuccess(), struct, metrics.parseRate());
    }

    private <GE extends GraphElement> void submit(ElementStruct struct,
                                                  List<Record<GE>> batch,
                                                  LoadOptions options,
                                                  LoadMetrics metrics,
                                                  StopWatch parseTime) {
        metrics.plusParseSuccess(batch.size());
        if (!options.dryRun) {
            // Parse time doesn't include submit time, it's accurate
            parseTime.suspend();
            this.taskManager.submitBatch(struct, batch);
            parseTime.resume();
        }
    }

    private void shutdown() {
        LOG.info("Shutdown HugeGraphLoader");
        this.context.stopLoading();
        // Wait all insert tasks finished before exit
        this.taskManager.waitFinished(this.context.loadingType());
        this.taskManager.shutdown();
        this.context.close();
    }
}
