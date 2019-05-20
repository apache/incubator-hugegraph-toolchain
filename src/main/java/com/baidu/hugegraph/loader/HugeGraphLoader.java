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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.progress.ElementProgress;
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.loader.source.graph.EdgeSource;
import com.baidu.hugegraph.loader.source.graph.GraphSource;
import com.baidu.hugegraph.loader.source.graph.VertexSource;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.beust.jcommander.JCommander;

public class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);
    private static final FailureLogger LOG_PARSE =
                         FailureLogger.logger("parseError");

    private final LoadOptions options;
    private final GraphSource graphSource;
    // The old progress just used to read
    private final LoadProgress oldProgress;
    private final LoadProgress newProgress;
    private final LoadMetrics loadMetrics;
    private final TaskManager taskManager;

    public static void main(String[] args) {
        HugeGraphLoader loader = new HugeGraphLoader(args);
        loader.load();
    }

    private HugeGraphLoader(String[] args) {
        this.options = parseAndCheckOptions(args);
        this.graphSource = GraphSource.of(this.options.file);
        this.oldProgress = parseLoadProgress(this.options);
        this.newProgress = new LoadProgress();
        this.loadMetrics = new LoadMetrics();
        this.taskManager = new TaskManager(this.options, this.loadMetrics);
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.newProgress.write(this.options.file);
            } catch (IOException e) {
                LOG.warn("Failed to write load progress", e);
            }
        }));
    }

    private static LoadOptions parseAndCheckOptions(String[] args) {
        LoadOptions options = new LoadOptions();
        JCommander commander = JCommander.newBuilder()
                                         .addObject(options)
                                         .build();
        commander.parse(args);
        // Print usage and exit
        if (options.help) {
            LoadUtil.exitWithUsage(commander, Constants.EXIT_CODE_NORM);
        }
        // Check options
        // Check option "-f"
        E.checkArgument(!StringUtils.isEmpty(options.file),
                        "Must specified entrance groovy file");
        File scriptFile = new File(options.file);
        if (!scriptFile.canRead()) {
            LOG.error("Script file must be readable: '{}'",
                      scriptFile.getAbsolutePath());
            LoadUtil.exitWithUsage(commander, Constants.EXIT_CODE_ERROR);
        }
        // Check option "-g"
        E.checkArgument(!StringUtils.isEmpty(options.graph),
                        "Must specified a graph");
        // Check option "-h"
        String httpPrefix = "http://";
        if (!options.host.startsWith(httpPrefix)) {
            options.host = httpPrefix + options.host;
        }
        return options;
    }

    private static LoadProgress parseLoadProgress(LoadOptions options) {
        if (!options.incrementalMode) {
            return new LoadProgress();
        }
        try {
            return LoadProgress.read(options.file);
        } catch (IOException e) {
            throw new LoadException("Failed to read progress file", e);
        }
    }

    private void load() {
        // Create schema
        this.createSchema();

        // Load vertices
        LoadSummary vertexSummary = this.loadWithMetric(ElemType.VERTEX);
        // Load edges
        LoadSummary edgeSummary = this.loadWithMetric(ElemType.EDGE);
        // Print load summary
        Printer.printSummary(vertexSummary, edgeSummary);

        this.stopLoading(Constants.EXIT_CODE_NORM);
    }

    private void stopLoading(int code) {
        // Shutdown task manager
        this.taskManager.shutdown(this.options.shutdownTimeout);
        LoadUtil.exit(code);
    }

    private void createSchema() {
        File schemaFile = FileUtils.getFile(this.options.schema);
        HugeClient client = HugeClientWrapper.get(this.options);
        GroovyExecutor groovyExecutor = new GroovyExecutor();
        groovyExecutor.bind("schema", client.schema());
        String script;
        try {
            script = FileUtils.readFileToString(schemaFile, Constants.CHARSET);
        } catch (IOException e) {
            throw new LoadException("Read schema file '%s' error",
                                    e, this.options.schema);
        }
        groovyExecutor.execute(script, client);
    }

    private LoadSummary loadWithMetric(ElemType type) {
        Printer.printElemType(type);
        Instant beginTime = Instant.now();

        if (type.isVertex()) {
            this.loadVertices();
        } else {
            assert type.isEdge();
            this.loadEdges();
        }

        Instant endTime = Instant.now();
        LoadSummary summary = new LoadSummary(type.string());
        Duration duration = Duration.between(beginTime, endTime);
        summary.parseFailure(this.loadMetrics.parseFailureNum());
        summary.insertSuccess(this.loadMetrics.insertSuccessNum());
        summary.insertFailure(this.loadMetrics.insertFailureNum());
        summary.loadTime(duration);

        Printer.print(summary.insertSuccess());
        // Reset counters
        this.loadMetrics.reset();
        return summary;
    }

    private void loadVertices() {
        // Used to get and update progress
        ElementProgress oldVertexProgress = this.oldProgress.vertex();
        ElementProgress newVertexProgress = this.newProgress.vertex();
        // Execute loading tasks
        List<VertexSource> vertexSources = this.graphSource.vertexSources();
        for (VertexSource source : vertexSources) {
            // Update loading element source
            newVertexProgress.addSource(source);
            LOG.info("Loading vertex source '{}'", source.label());
            try (VertexBuilder builder = new VertexBuilder(source, this.options)) {
                builder.progress(oldVertexProgress, newVertexProgress);
                builder.init();
                this.loadVertex(builder);
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(ElemType.VERTEX);
    }

    private long loadVertex(VertexBuilder builder) {
        int batchSize = this.options.batchSize;
        long parseSuccess = 0L;
        List<Vertex> batch = new ArrayList<>(batchSize);
        while (true) {
            try {
                if (!builder.hasNext()) {
                    break;
                }
                Vertex vertex = builder.next();
                batch.add(vertex);
            } catch (ParseException e) {
                if (this.options.testMode) {
                    throw e;
                }
                LOG.error("Vertex parse error", e);
                LOG_PARSE.error(e);
                long failureNum = this.loadMetrics.increaseParseFailureNum();
                if (failureNum >= this.options.maxParseErrors) {
                    Printer.printError("Error: More than %s vertices " +
                                       "parsing error ... stopping",
                                       this.options.maxParseErrors);
                    this.stopLoading(Constants.EXIT_CODE_ERROR);
                }
                continue;
            }
            if (batch.size() >= batchSize) {
                parseSuccess += batchSize;
                this.taskManager.submitVertexBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty()) {
            this.taskManager.submitVertexBatch(batch);
        }
        return parseSuccess;
    }

    private void loadEdges() {
        // Used to get and update progress
        ElementProgress oldEdgeProgress = this.oldProgress.edge();
        ElementProgress newEdgeProgress = this.newProgress.edge();
        // Execute loading tasks
        List<EdgeSource> edgeSources = this.graphSource.edgeSources();
        for (EdgeSource source : edgeSources) {
            // Update loading element source
            newEdgeProgress.addSource(source);
            LOG.info("Loading edge source '{}'", source.label());
            try (EdgeBuilder builder = new EdgeBuilder(source, this.options)) {
                builder.progress(oldEdgeProgress, newEdgeProgress);
                builder.init();
                this.loadEdge(builder);
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(ElemType.EDGE);
    }

    private long loadEdge(EdgeBuilder builder) {
        int batchSize = this.options.batchSize;
        long parseSuccess = 0L;
        List<Edge> batch = new ArrayList<>(batchSize);
        while (true) {
            try {
                if (!builder.hasNext()) {
                    break;
                }
                Edge edge = builder.next();
                batch.add(edge);
            } catch (ParseException e) {
                if (this.options.testMode) {
                    throw e;
                }
                LOG.error("Edge parse error", e);
                LOG_PARSE.error(e);
                long failureNum = this.loadMetrics.increaseParseFailureNum();
                if (failureNum >= this.options.maxParseErrors) {
                    Printer.printError("Error: More than %s edges " +
                                       "parsing error ... stopping",
                                       this.options.maxParseErrors);
                    this.stopLoading(Constants.EXIT_CODE_ERROR);
                }
                continue;
            }
            if (batch.size() >= batchSize) {
                parseSuccess += batchSize;
                this.taskManager.submitEdgeBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty()) {
            this.taskManager.submitEdgeBatch(batch);
        }
        return parseSuccess;
    }
}
