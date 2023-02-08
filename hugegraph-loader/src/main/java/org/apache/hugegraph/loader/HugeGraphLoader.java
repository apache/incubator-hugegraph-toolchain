/*
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

package org.apache.hugegraph.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.loader.task.ParseTaskBuilder;
import org.apache.hugegraph.loader.task.TaskManager;
import org.apache.hugegraph.loader.util.HugeClientHolder;
import org.apache.hugegraph.loader.util.LoadUtil;
import org.apache.hugegraph.loader.util.Printer;
import org.slf4j.Logger;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.constant.ElemType;
import org.apache.hugegraph.loader.exception.InitException;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.exception.ReadException;
import org.apache.hugegraph.loader.executor.GroovyExecutor;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.LoadMapping;
import org.apache.hugegraph.loader.metrics.LoadMetrics;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.loader.reader.InputReader;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.util.Log;

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
            Printer.printError("Failed to start loading", LoadUtil.targetRuntimeException(e));
            throw e;
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
        client.graphs().clearGraph(options.graph, message);
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
            summary.calculateTotalTime(ElemType.VERTEX);
            summary.calculateTotalTime(ElemType.EDGE);
            summary.stopTotalTimer();
        }
        Printer.printFinalProgress(this.context);
    }

    private void loadInputs(List<InputStruct> structs) {
        if (this.context.options().checkVertex) {
            LOG.info("Forced to load vertices before edges since set " +
                     "option check-vertex=true");
            SplitInputStructs split = this.splitStructs(structs);
            // Load all vertex structs
            this.loadStructs(split.vertexInputStructs);
            // Wait all vertex load tasks finished
            this.manager.waitFinished("vertex insert tasks");
            // Load all edge structs
            this.loadStructs(split.edgeInputStructs);
        } else {
            // Load vertex and edge structs concurrent in the same input
            this.loadStructs(structs);
        }
    }

    private void loadStructs(List<InputStruct> structs) {
        // Load input structs one by one
        for (InputStruct struct : structs) {
            if (this.context.stopped()) {
                break;
            }
            if (struct.skip()) {
                continue;
            }
            // Create and init InputReader, fetch next batch lines
            try (InputReader reader = InputReader.create(struct.input())) {
                // Init reader
                reader.init(this.context, struct);
                // Load data from current input mapping
                this.loadStruct(struct, reader);
            } catch (InitException e) {
                throw new LoadException("Failed to init input reader", e);
            }
        }
    }

    /**
     * TODO: Separate classes: ReadHandler -> ParseHandler -> InsertHandler
     * Let load task worked in pipeline mode
     */
    private void loadStruct(InputStruct struct, InputReader reader) {
        LOG.info("Start loading '{}'", struct);
        LoadMetrics metrics = this.context.summary().metrics(struct);
        metrics.startInFlight();

        ParseTaskBuilder taskBuilder = new ParseTaskBuilder(this.context, struct);
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
            // If read max allowed lines, stop loading
            boolean reachedMaxReadLines = this.reachedMaxReadLines();
            if (reachedMaxReadLines) {
                finished = true;
            }
            if (lines.size() >= batchSize || finished) {
                List<ParseTaskBuilder.ParseTask> tasks = taskBuilder.build(lines);
                for (ParseTaskBuilder.ParseTask task : tasks) {
                    this.executeParseTask(struct, task.mapping(), task);
                }
                // Confirm offset to avoid lost records
                reader.confirmOffset();
                this.context.newProgress().markLoaded(struct, finished);

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
        this.context.occurredError();
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

    private SplitInputStructs splitStructs(List<InputStruct> structs) {
        SplitInputStructs split = new SplitInputStructs();
        for (InputStruct struct : structs) {
            InputStruct result = struct.extractVertexStruct();
            if (result != InputStruct.EMPTY) {
                split.vertexInputStructs.add(result);
            }
        }
        for (InputStruct struct : structs) {
            InputStruct result = struct.extractEdgeStruct();
            if (result != InputStruct.EMPTY) {
                split.edgeInputStructs.add(result);
            }
        }
        return split;
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

    private static class SplitInputStructs {

        private final List<InputStruct> vertexInputStructs;
        private final List<InputStruct> edgeInputStructs;

        public SplitInputStructs() {
            this.vertexInputStructs = new ArrayList<>();
            this.edgeInputStructs = new ArrayList<>();
        }
    }
}
