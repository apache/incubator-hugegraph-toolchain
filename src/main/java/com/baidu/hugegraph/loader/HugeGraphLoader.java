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
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.task.ParseTaskBuilder;
import com.baidu.hugegraph.loader.task.ParseTaskBuilder.ParseTask;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.util.Log;

public final class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);

    /*
     * TODO: use a more graceful way to express loader end succeed or failed
     */
    private boolean succeed;
    private LoadContext context;
    private LoadMapping mapping;
    private TaskManager manager;

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
        this.succeed = true;
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
            // Clear schema if needed
            this.clearAllDataIfNeeded();
            // Create schema
            this.createSchema();
            this.loadInputs();
            // Print load summary
            Printer.printSummary(this.context);
        } catch (Throwable e) {
            Printer.printError("Failed to load", e);
            if (this.context.options().testMode) {
                throw e;
            }
        } finally {
            this.stopThenShutdown();
        }
        return this.succeed;
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

        LOG.info("Prepare to clear the data of graph {}", options.graph);
        client.graphs().clear(options.graph, message);
        LOG.info("The graph {} has been cleared successfully", options.graph);

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
        LOG.info("Start loading");
        Printer.printRealtimeProgress(this.context);
        LoadOptions options = this.context.options();

        this.context.summary().startTimer();
        try {
            if (!options.failureMode) {
                // Load normal data from user supplied input structs
                this.load(this.mapping.structs());
            } else {
                // Load failure data from generated input structs
                this.load(this.mapping.structsForFailure(options));
            }
            // Waiting for async worker threads finish
            this.manager.waitFinished();
        } finally {
            this.context.summary().stopTimer();
        }
        Printer.printFinalProgress(this.context);
    }

    private void load(List<InputStruct> structs) {
        // Load input structs one by one
        for (InputStruct struct : structs) {
            if (this.context.stopped()) {
                this.succeed = false;
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
                this.load(struct, reader);
            } catch (InitException e) {
                throw new LoadException("Failed to init input reader", e);
            }
        }
    }

    /**
     * TODO: Seperate classes: ReadHandler -> ParseHandler -> InsertHandler
     * Let load task worked in pipeline mode
     */
    private void load(InputStruct struct, InputReader reader) {
        LOG.info("Start parsing and loading '{}'", struct);
        LoadMetrics metrics = this.context.summary().metrics(struct);
        ParseTaskBuilder taskBuilder = new ParseTaskBuilder(this.context, struct);
        int batchSize = this.context.options().batchSize;
        List<Line> lines = new ArrayList<>(batchSize);
        for (boolean finished = false; !finished;) {
            if (this.context.stopped()) {
                return;
            }
            try {
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
            if (this.context.stopped()) {
                LOG.warn("Read errors exceed limit, load task stopped");
                return;
            }

            if (lines.size() >= batchSize || finished) {
                List<ParseTask> tasks = taskBuilder.build(lines);
                for (ParseTask task : tasks) {
                    this.executeParseTask(struct, task.mapping(), task);
                }
                // Confirm offset to avoid lost records
                reader.confirmOffset();
                this.context.newProgress().markLoaded(struct, finished);

                this.markStopIfNeeded();
                if (this.context.stopped()) {
                    LOG.warn("Parse errors exceed limit, stopped loading tasks");
                    return;
                }

                lines = new ArrayList<>(batchSize);
            }
        }
    }

    /**
     * Execute parse task sync
     */
    private void executeParseTask(InputStruct struct, ElementMapping mapping,
                                  ParseTaskBuilder.ParseTask task) {
        List<List<Record>> batches = task.get();
        if (this.context.stopped() || this.context.options().dryRun ||
            CollectionUtils.isEmpty(batches)) {
            return;
        }
        for (List<Record> batch : batches) {
            this.manager.submitBatch(struct, mapping, batch);
        }
    }

    private void handleReadFailure(InputStruct struct, ReadException e) {
        LOG.error("Read {} error", struct, e);
        LoadOptions options = this.context.options();
        if (options.testMode) {
            throw e;
        }

        long failures = this.context.summary().totalReadFailures();
        if (options.maxReadErrors != Constants.NO_LIMIT &&
            failures >= options.maxReadErrors) {
            Printer.printError("More than %s reading error, stop loading " +
                               "and waiting all load tasks stopped",
                               options.maxReadErrors);
            this.context.stopLoading();
        } else {
            // Write to current mapping's parse failure log
            this.context.failureLogger(struct).write(e);
        }
    }

    private void markStopIfNeeded() {
        LoadOptions options = this.context.options();
        long failures = this.context.summary().totalParseFailures();
        if (options.maxParseErrors != Constants.NO_LIMIT &&
            failures >= options.maxParseErrors) {
            if (this.context.stopped()) {
                return;
            }
            synchronized (this.context) {
                if (!this.context.stopped()) {
                    Printer.printError("More than %s parse errors, stop " +
                                       "parsing and waiting all insert tasks " +
                                       "stopped", options.maxParseErrors);
                    this.context.stopLoading();
                }
            }
        }
    }

    private void stopThenShutdown() {
        LOG.info("Stop loading then shutdown HugeGraphLoader");
        if (this.context == null) {
            return;
        }
        try {
            this.context.stopLoading();
            if (this.manager != null) {
                // Wait all insert tasks stopped before exit
                this.manager.waitFinished();
                this.manager.shutdown();
            }
        } finally {
            this.context.close();
        }
    }
}
