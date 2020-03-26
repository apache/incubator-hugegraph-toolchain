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
import com.baidu.hugegraph.loader.builder.SchemaCache;
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

    private final LoadContext context;
    private final LoadMapping mapping;
    private final TaskManager manager;

    public static void main(String[] args) {
        HugeGraphLoader loader = new HugeGraphLoader(args);
        loader.load();
    }

    public HugeGraphLoader(String[] args) {
        LoadOptions options = LoadOptions.parseOptions(args);
        try {
            this.context = LoadContext.init(options);
            this.mapping = LoadMapping.of(options.file);
            this.manager = new TaskManager();
        } catch (Throwable e) {
            LoadContext.destroy();
            throw e;
        }
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
            this.clearAllDataIfNeeded();
            // Create schema
            this.createSchema();
            // Init schema cache
            this.initSchemaCache();
            this.loadInputs();
            // Print load summary
            Printer.printSummary();
        } catch (Throwable e) {
            Printer.printError("Failed to load", e);
            if (this.context.options().testMode) {
                throw e;
            } else {
                System.exit(-1);
            }
        } finally {
            this.stopThenShutdown();
        }
    }

    private void clearAllDataIfNeeded() {
        LoadOptions options = this.context.options();
        if (!options.clearAllData) {
            return;
        }

        int requestTimeout = options.timeout;
        options.timeout = options.clearTimeout;
        HugeClient client = HugeClientHolder.get(options);
        String message = "I'm sure to delete all data";

        LOG.info("Prepare to clear the data of graph {}", options.graph);
        client.graphs().clear(options.graph, message);
        LOG.info("The graph {} has been cleared successfully", options.graph);

        // Close HugeClient and set the original timeout back if needed
        if (requestTimeout != options.clearTimeout) {
            HugeClientHolder.close();
            options.timeout = requestTimeout;
        }
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

    private void initSchemaCache() {
        HugeClient client = HugeClientHolder.get(this.context.options());
        SchemaCache cache = new SchemaCache(client);
        if (cache.isEmpty()) {
            throw new InitException("There is no schema in HugeGraphServer, " +
                                    "please create it at first");
        }
        this.context.schemaCache(cache);
    }

    private void loadInputs() {
        LOG.info("Start loading");
        Printer.printRealtimeProgress();
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
        Printer.printFinalProgress();
    }

    private void load(List<InputStruct> structs) {
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
        ParseTaskBuilder taskBuilder = new ParseTaskBuilder(struct);
        int batchSize = this.context.options().batchSize;
        List<Line> lines = new ArrayList<>(batchSize);
        for (boolean finished = false; !finished;) {
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
                               "and waiting all load tasks finished",
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
                                       "finished", options.maxParseErrors);
                    this.context.stopLoading();
                }
            }
        }
    }

    private void stopThenShutdown() {
        LOG.info("Stop loading then shutdown HugeGraphLoader");
        try {
            this.context.stopLoading();
            // Wait all insert tasks finished before exit
            this.manager.waitFinished();
            this.manager.shutdown();
        } finally {
            LoadContext.destroy();
        }
    }
}
