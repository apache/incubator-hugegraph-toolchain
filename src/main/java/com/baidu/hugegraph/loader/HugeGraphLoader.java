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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.baidu.hugegraph.util.ExecutorUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.InitException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ReadException;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.metrics.LoadMetrics;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.task.ParseTaskBuilder;
import com.baidu.hugegraph.loader.task.ParseTaskBuilder.ParseTask;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.ImmutableList;

public final class HugeGraphLoader {

    public static final Logger LOG = Log.logger(HugeGraphLoader.class);

    private final LoadContext context;
    private final LoadMapping mapping;
    private final TaskManager manager;

    public static void main(String[] args) {
        HugeGraphLoader loader;
        try {
            loader = new HugeGraphLoader(args);
        } catch (Throwable e) {
            Printer.printError("Failed to start loading", e);
            return;
        }
        loader.load();
    }

    public HugeGraphLoader(String[] args) {
        this(LoadOptions.parseOptions(args));
    }

    public HugeGraphLoader(LoadOptions options) {
        this(options, LoadMapping.of(options.file));
    }

    public HugeGraphLoader(LoadOptions options, LoadMapping mapping) {
        this.context = new LoadContext(options);
        this.mapping = mapping;
        this.manager = new TaskManager(this.context);
        this.addShutdownHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown hook was triggered");
            this.stopThenShutdown();
        }));
    }

    public LoadContext context() {
        return this.context;
    }

    public boolean load() {
        this.context.options().dumpParams();

        try {
            // Switch to loading mode
            this.context.setLoadingMode();
            // Clear schema if needed
            this.clearAllDataIfNeeded();
            // Create schema
            this.createSchema();
            this.loadInputs();
            // Print load summary
            Printer.printSummary(this.context);
        } catch (Throwable t) {
            RuntimeException e = LoadUtil.targetRuntimeException(t);
            Printer.printError("Failed to load", e);
            if (this.context.options().testMode) {
                throw e;
            }
        } finally {
            this.stopThenShutdown();
        }
        return this.context.noError();
    }

    private void clearAllDataIfNeeded() {
        LoadOptions options = this.context.options();
        if (!options.clearAllData) {
            return;
        }

        int requestTimeout = options.timeout;
        options.timeout = options.clearTimeout;
        HugeClient client = HugeClientHolder.create(options);
        String message = "I'm sure to delete all data";

        LOG.info("Prepare to clear the data of graph '{}'", options.graph);
        client.graphs().clear(options.graph, message);
        LOG.info("The graph '{}' has been cleared successfully", options.graph);
        options.timeout = requestTimeout;
        client.close();
    }

    private void createSchema() {
        LoadOptions options = this.context.options();
        if (!StringUtils.isEmpty(options.schema)) {
            File file = FileUtils.getFile(options.schema);
            HugeClient client = this.context.client();
            GroovyExecutor groovyExecutor = new GroovyExecutor();
            groovyExecutor.bind(Constants.GROOVY_SCHEMA, client.schema());
            String script;
            try {
                script = FileUtils.readFileToString(file, Constants.CHARSET);
            } catch (IOException e) {
                throw new LoadException("Failed to read schema file '%s'", e,
                                        options.schema);
            }
            groovyExecutor.execute(script, client);
        }
        this.context.updateSchemaCache();
    }

    private void loadInputs() {
        Printer.printRealtimeProgress(this.context);
        LoadOptions options = this.context.options();
        LoadSummary summary = this.context.summary();
        summary.initMetrics(this.mapping);

        summary.startTotalTimer();
        try {
            if (!options.failureMode) {
                // Load normal data from user supplied input structs
                this.loadInputs(this.mapping.structs());
            } else {
                // Load failure data from generated input structs
                this.loadInputs(this.mapping.structsForFailure(options));
            }
            // Waiting for async worker threads finish
            this.manager.waitFinished();
        } finally {
            summary.calculateTotalTime();
            summary.stopTotalTimer();
        }
        Printer.printFinalProgress(this.context);
    }

    private void loadInputs(List<InputStruct> structs) {
        if (this.context.options().checkVertex) {
            LOG.info("Forced to load vertices before edges since set " +
                     "option check-vertex=true");
            SplittedInputStructs splitted = this.splitStructs(structs);
            // Load all vertex structs
            this.loadStructs(splitted.vertexInputStructs);
            // Wait all vertex load tasks finished
            this.manager.waitFinished("vertex insert tasks");
            // Load all edge structs
            this.loadStructs(splitted.edgeInputStructs);
        } else {
            // Load vertex and edge structs concurrent in the same input
            this.loadStructs(structs);
        }
    }

    private void loadStructs(List<InputStruct> structs) {
        int parallelCount = this.context.options().parallelCount;
        if (structs.size() == 0) {
            return;
        }
        if (parallelCount <= 0 ) {
            parallelCount = structs.size();
        }

        LOG.info("{} threads for loading {} structs",
                parallelCount, structs.size());

        ExecutorService service = ExecutorUtil.newFixedThreadPool(
                parallelCount, "loader");

        List<CompletableFuture<Void>> loadTasks = new ArrayList<>();
        List<InputReader> readers = new ArrayList<>();

        for (InputStruct struct : structs) {
            if (this.context.stopped()) {
                break;
            }
            if (struct.skip()) {
                continue;
            }

            // Create and init InputReader, fetch next batch lines
            try {
                InputReader reader = InputReader.create(struct.input());
                List<InputReader> readerList = reader.multiReaders() ?
                                               reader.split() :
                                               ImmutableList.of(reader);
                for (InputReader r : readerList) {
                    // Init reader
                    r.init(this.context, struct);
                    // Load data from current input mapping
                    loadTasks.add(this.asyncLoadStruct(struct, r, service));
                    readers.add(r);
                }
            } catch (InitException e) {
                throw new LoadException("Failed to init input reader", e);
            }
        }
        LOG.info("waiting for loading finish {}", loadTasks.size());
        // wait for finish
        CompletableFuture.allOf(loadTasks.toArray(new CompletableFuture[0]))
                .join();

        service.shutdown();

        for (InputReader reader : readers) {
            reader.close();
        }
        LOG.info("load finish");
    }

    private CompletableFuture<Void> asyncLoadStruct(
            InputStruct struct, InputReader reader, ExecutorService service) {
        return CompletableFuture.runAsync(() -> {
            this.loadStruct(struct, reader);
            }, service);
    }

    /**
     * TODO: Seperate classes: ReadHandler -> ParseHandler -> InsertHandler
     * Let load task worked in pipeline mode
     */
    private void loadStruct(InputStruct struct, InputReader reader) {
        LOG.info("Start loading '{}'", struct);
        LoadMetrics metrics = this.context.summary().metrics(struct);
        metrics.startInFlight();

        ParseTaskBuilder taskBuilder = new ParseTaskBuilder(this.context,
                                                            struct);
        final int batchSize = this.context.options().batchSize;
        List<Line> lines = new ArrayList<>(batchSize);
        for (boolean finished = false; !finished;) {
            if (this.context.stopped()) {
                break;
            }
            try {
                // Read next line from data source
                if (reader.hasNext()) {
                    lines.add(reader.next());
                    metrics.increaseReadSuccess();
                } else {
                    finished = true;
                }
            } catch (ReadException e) {
                metrics.increaseReadFailure();
                this.handleReadFailure(struct, e);
            }
            // If readed max allowed lines, stop loading
            boolean reachedMaxReadLines = this.reachedMaxReadLines();
            if (reachedMaxReadLines) {
                finished = true;
            }
            if (lines.size() >= batchSize || finished) {
                List<ParseTask> tasks = taskBuilder.build(lines);
                for (ParseTask task : tasks) {
                    this.executeParseTask(struct, task.mapping(), task);
                }
                // Confirm offset to avoid lost records
                reader.confirmOffset();
                this.context.newProgress().markLoaded(struct, reader, finished);

                this.handleParseFailure();
                if (reachedMaxReadLines) {
                    LOG.warn("Read lines exceed limit, stopped loading tasks");
                    this.context.stopLoading();
                }
                lines = new ArrayList<>(batchSize);
            }
        }

        metrics.stopInFlight();
        LOG.info("Finish loading '{}'", struct);
    }

    /**
     * Execute parse task sync
     */
    private void executeParseTask(InputStruct struct, ElementMapping mapping,
                                  ParseTaskBuilder.ParseTask task) {
        long start = System.currentTimeMillis();
        // Sync parse
        List<List<Record>> batches = task.get();
        long end = System.currentTimeMillis();
        this.context.summary().addTimeRange(mapping.type(), start, end);

        if (this.context.options().dryRun || CollectionUtils.isEmpty(batches)) {
            return;
        }
        // Async load
        for (List<Record> batch : batches) {
            this.manager.submitBatch(struct, mapping, batch);
        }
    }

    private void handleReadFailure(InputStruct struct, ReadException e) {
        LOG.error("Read {} error", struct, e);
        this.context.occuredError();
        LoadOptions options = this.context.options();
        if (options.testMode) {
            throw e;
        }
        // Write to current mapping's read failure log
        this.context.failureLogger(struct).write(e);

        long failures = this.context.summary().totalReadFailures();
        if (options.maxReadErrors != Constants.NO_LIMIT &&
            failures >= options.maxReadErrors) {
            Printer.printError("More than %s read error, stop reading and " +
                               "waiting all parse/insert tasks stopped",
                               options.maxReadErrors);
            this.context.stopLoading();
        }
    }

    private void handleParseFailure() {
        LoadOptions options = this.context.options();
        long failures = this.context.summary().totalParseFailures();
        if (options.maxParseErrors != Constants.NO_LIMIT &&
            failures >= options.maxParseErrors) {
            if (this.context.stopped()) {
                return;
            }
            synchronized (this.context) {
                if (!this.context.stopped()) {
                    Printer.printError("More than %s parse error, stop " +
                                       "parsing and waiting all insert tasks " +
                                       "stopped", options.maxParseErrors);
                    this.context.stopLoading();
                }
            }
        }
    }

    private SplittedInputStructs splitStructs(List<InputStruct> structs) {
        SplittedInputStructs splitted = new SplittedInputStructs();
        for (InputStruct struct : structs) {
            InputStruct result = struct.extractVertexStruct();
            if (result != InputStruct.EMPTY) {
                splitted.vertexInputStructs.add(result);
            }
        }
        for (InputStruct struct : structs) {
            InputStruct result = struct.extractEdgeStruct();
            if (result != InputStruct.EMPTY) {
                splitted.edgeInputStructs.add(result);
            }
        }
        return splitted;
    }

    private boolean reachedMaxReadLines() {
        final long maxReadLines = this.context.options().maxReadLines;
        if (maxReadLines == -1L) {
            return false;
        }
        return this.context.summary().totalReadLines() >= maxReadLines;
    }

    /**
     * TODO: How to distinguish load task finished normally or abnormally
     */
    private synchronized void stopThenShutdown() {
        if (this.context.closed()) {
            return;
        }
        LOG.info("Stop loading then shutdown HugeGraphLoader");
        try {
            this.context.stopLoading();
            if (this.manager != null) {
                // Wait all insert tasks stopped before exit
                this.manager.waitFinished();
                this.manager.shutdown();
            }
        } finally {
            try {
                this.context.unsetLoadingMode();
            } finally {
                this.context.close();
            }
        }
    }

    private static class SplittedInputStructs {

        private final List<InputStruct> vertexInputStructs;
        private final List<InputStruct> edgeInputStructs;

        public SplittedInputStructs() {
            this.vertexInputStructs = new ArrayList<>();
            this.edgeInputStructs = new ArrayList<>();
        }
    }
}
