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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.builder.SchemaCache;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.InitException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.task.ParseTaskBuilder;
import com.baidu.hugegraph.loader.task.ParseTaskBuilder.ParseTask;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.LoadUtil;
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
            // Move failure files from current to history directory
            LoadUtil.moveFailureFiles(this.context);
            this.loadInputs();
        } catch (Exception e) {
            Printer.printError("Failed to load", e);
            this.stopThenShutdown();
            throw e;
        }
        // Print load summary
        Printer.printSummary();
        this.stopThenShutdown();
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
                                    "please init it at first");
        }
        this.context.schemaCache(cache);
    }

    private void loadInputs() {
        LOG.info("Start loading");
        Printer.printRealtimeProgress();
        LoadOptions options = this.context.options();

        this.context.summary().startTimer();
        try {
            // Load normal data from user supplied input structs
            this.load(this.mapping.structs());
            // Load failure data from generated input structs
            if (options.incrementalMode && options.reloadFailure) {
                this.load(this.mapping.structsForFailure(options));
            }
            // Waiting async worker threads finish
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

    private void load(InputStruct struct, InputReader reader) {
        LOG.info("Start parsing and loading '{}'", struct);
        ParseTaskBuilder taskBuilder = new ParseTaskBuilder(struct);
        int batchSize = this.context.options().batchSize;
        List<Line> lines = new ArrayList<>(batchSize);
        for (boolean finished = false; !this.context.stopped() && !finished;) {
            if (reader.hasNext()) {
                lines.add(reader.next());
            } else {
                finished = true;
            }

            boolean stopped = this.context.stopped() || finished;
            if ((!lines.isEmpty() && lines.size() >= batchSize) || stopped) {
                List<ParseTask> tasks = taskBuilder.build(lines);
                for (ParseTask task : tasks) {
                    this.manager.submitParseTask(struct, task.mapping(), task);
                }
                lines = new ArrayList<>(batchSize);
            }
            if (stopped) {
                this.context.newProgress().markLoaded(struct, finished);
            }
            // Confirm offset to avoid lost records
            reader.confirmOffset();
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
