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
import com.baidu.hugegraph.driver.SchemaManager;
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
import com.baidu.hugegraph.loader.util.LoadUtil;
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

    public HugeGraphLoader(String[] args) {
        this.context = new LoadContext(args);
        this.graphStruct = GraphStruct.of(this.context);
        this.taskManager = new TaskManager(this.context);
        this.addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.stopThenShutdown();
        }));
    }

    public void load() {
        try {
            // Clear schema if needed
            this.clearSchemaIfNeeded();
            // Create schema
            this.createSchema();
            // Move failure files from current to history directory
            LoadUtil.moveFailureFiles(this.context);
            // Load vertices
            this.load(ElemType.VERTEX);
            // Load edges
            this.load(ElemType.EDGE);
        } catch (Exception e) {
            Printer.printError("Failed to load", e);
            this.stopThenShutdown();
            throw e;
        }
        // Print load summary
        Printer.printSummary(this.context);
        this.stopThenShutdown();
    }

    private void clearSchemaIfNeeded() {
        LoadOptions options = this.context.options();
        if (!options.clearOldSchema) {
            return;
        }

        HugeClient client = HugeClientHolder.get(options);
        SchemaManager schema = client.schema();
        com.baidu.hugegraph.driver.TaskManager task = client.task();
        int timeout = options.clearSchemaTimeout;
        // Clear schema
        List<Long> taskIds = new ArrayList<>();
        schema.getIndexLabels().forEach(il -> {
            taskIds.add(schema.removeIndexLabelAsync(il.name()));
        });
        taskIds.forEach(taskId -> task.waitUntilTaskCompleted(taskId, timeout));

        taskIds.clear();
        schema.getEdgeLabels().forEach(el -> {
            taskIds.add(schema.removeEdgeLabelAsync(el.name()));
        });
        taskIds.forEach(taskId -> task.waitUntilTaskCompleted(taskId, timeout));

        taskIds.clear();
        schema.getVertexLabels().forEach(vl -> {
            taskIds.add(schema.removeVertexLabelAsync(vl.name()));
        });
        taskIds.forEach(taskId -> task.waitUntilTaskCompleted(taskId, timeout));

        schema.getPropertyKeys().forEach(pk -> {
            schema.removePropertyKey(pk.name());
        });
    }

    private void createSchema() {
        LoadOptions options = this.context.options();
        if (StringUtils.isEmpty(options.schema)) {
            return;
        }
        File schemaFile = FileUtils.getFile(options.schema);
        HugeClient client = HugeClientHolder.get(options);
        GroovyExecutor groovyExecutor = new GroovyExecutor();
        groovyExecutor.bind(Constants.GROOVY_SCHEMA, client.schema());
        String script;
        try {
            script = FileUtils.readFileToString(schemaFile, Constants.CHARSET);
        } catch (IOException e) {
            throw new LoadException("Read schema file '%s' error",
                                    e, options.schema);
        }
        groovyExecutor.execute(script, client);
    }

    private void load(ElemType type) {
        LOG.info("Start loading {}", type.string());
        this.context.loadingType(type);

        Printer.printRealTimeProgress(type, LoadUtil.lastLoaded(this.context,
                                                                type));
        LoadOptions options = this.context.options();
        LoadSummary summary = this.context.summary();
        StopWatch totalTimer = StopWatch.createStarted();

        // Load normal data
        this.load(type, this.graphStruct.structs(type));
        // Load failure data
        if (options.incrementalMode && options.reloadFailure) {
            this.load(type, this.graphStruct.structsForFailure(type, options));
        }

        // Waiting async worker threads finish
        this.taskManager.waitFinished(type);
        totalTimer.stop();
        summary.totalTime(type, totalTimer.getTime(TimeUnit.MILLISECONDS));

        Printer.print(summary.accumulateMetrics(type).loadSuccess());
    }

    private void load(ElemType type, List<ElementStruct> structs) {
        LoadSummary summary = this.context.summary();
        InputProgressMap newProgress = this.context.newProgress().type(type);
        for (ElementStruct struct : structs) {
            if (struct.skip()) {
                continue;
            }
            StopWatch loadTimer = StopWatch.createStarted();
            LoadMetrics metrics = summary.metrics(struct);
            if (!this.context.stopped()) {
                // Update loading vertex/edge struct
                newProgress.addStruct(struct);
                // Produce batch of vertices/edges and execute loading tasks
                try (ElementBuilder<?> builder = ElementBuilder.of(this.context,
                                                                   struct)) {
                    this.load(builder);
                }
            }
            /*
             * NOTE: the load task of this struct may not be completed,
             * so the load rate of each struct is an inaccurate value.
             */
            loadTimer.stop();
            metrics.loadTime(loadTimer.getTime(TimeUnit.MILLISECONDS));
            LOG.info("Loading {} '{}' with average rate: {}/s",
                     metrics.loadSuccess(), struct, metrics.averageLoadRate());
        }
    }

    private <GE extends GraphElement> void load(ElementBuilder<GE> builder) {
        ElementStruct struct = builder.struct();
        LOG.info("Start parsing and loading '{}'", struct);
        final int batchSize = this.context.options().batchSize;
        LoadMetrics metrics = this.context.summary().metrics(struct);

        StopWatch parseTimer = StopWatch.createStarted();
        List<Record<GE>> batch = new ArrayList<>(batchSize);
        for (boolean finished = false; !this.context.stopped() && !finished;) {
            try {
                if (builder.hasNext()) {
                    Record<GE> record = builder.next();
                    batch.add(record);
                } else {
                    finished = true;
                }
            } catch (ParseException e) {
                builder.confirmOffset();
                metrics.increaseParseFailure();

                this.handleParseFailure(struct, e);
            }

            boolean stopped = this.context.stopped() || finished;
            if (batch.size() >= batchSize || stopped) {
                this.submit(struct, batch, parseTimer);
                metrics.plusParseSuccess(batch.size());
                batch = new ArrayList<>(batchSize);

                // Confirm offset to avoid lost records
                builder.confirmOffset();
                if (stopped) {
                    this.context.newProgress().markLoaded(struct, finished);
                }
            }
        }

        parseTimer.stop();
        metrics.parseTime(parseTimer.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Parsing {} '{}' with average rate: {}/s",
                 metrics.parseSuccess(), struct, metrics.parseRate());
    }

    private void handleParseFailure(ElementStruct struct, ParseException e) {
        if (this.context.options().testMode) {
            throw e;
        }

        LOG.error("Parse {} error", struct.type(), e);
        // Write to current struct's parse failure log
        FailureLogger logger = this.context.failureLogger(struct);
        logger.write(e);

        LoadOptions options = this.context.options();
        long failures = this.context.summary().totalParseFailures();
        if (failures >= options.maxParseErrors) {
            Printer.printError("More than %s %s parsing error, stop parsing " +
                               "and waiting all insert tasks finished",
                               options.maxParseErrors, struct.type().string());
            this.context.stopLoading();
        }
    }

    private <GE extends GraphElement> void submit(ElementStruct struct,
                                                  List<Record<GE>> batch,
                                                  StopWatch parseTimer) {
        if (!this.context.options().dryRun && !batch.isEmpty()) {
            // Parse time doesn't include submit time, it's accurate
            parseTimer.suspend();
            try {
                this.taskManager.submitBatch(struct, batch);
            } finally {
                parseTimer.resume();
            }
        }
    }

    private void stopThenShutdown() {
        LOG.info("Stop loading then shutdown HugeGraphLoader");
        this.context.stopLoading();
        // Wait all insert tasks finished before exit
        this.taskManager.waitFinished(this.context.loadingType());
        this.taskManager.shutdown();
        this.context.close();
    }
}
