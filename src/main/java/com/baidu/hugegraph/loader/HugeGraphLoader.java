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
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.executor.LoadSummary;
import com.baidu.hugegraph.loader.source.EdgeSource;
import com.baidu.hugegraph.loader.source.GraphSource;
import com.baidu.hugegraph.loader.source.VertexSource;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoaderUtil;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.beust.jcommander.JCommander;

public class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);
    private static final LoadLogger LOG_PARSE = LoadLogger.logger("parseError");

    private final LoadOptions options;
    private final TaskManager taskManager;
    private final GraphSource graphSource;

    private long parseFailureNum = 0L;

    public static void main(String[] args) {
        HugeGraphLoader loader = new HugeGraphLoader(args);
        loader.load();
    }

    private HugeGraphLoader(String[] args) {
        this.options = new LoadOptions();
        this.parseAndCheckOptions(args);
        this.taskManager = new TaskManager(this.options);
        this.graphSource = GraphSource.of(this.options.file);
    }

    private void parseAndCheckOptions(String[] args) {
        JCommander commander = JCommander.newBuilder()
                                         .addObject(this.options)
                                         .build();
        commander.parse(args);
        // Print usage and exit
        if (this.options.help) {
            LoaderUtil.exitWithUsage(commander, 0);
        }
        // Check options
        // Check option "-f"
        E.checkArgument(!StringUtils.isEmpty(this.options.file),
                        "Must specified entrance groovy file");
        File scriptFile = new File(this.options.file);
        if (!scriptFile.canRead()) {
            LOG.error("Script file must be readable: '{}'",
                      scriptFile.getAbsolutePath());
            LoaderUtil.exitWithUsage(commander, -1);
        }
        // Check option "-g"
        E.checkArgument(!StringUtils.isEmpty(this.options.graph),
                        "Must specified a graph");
        // Check option "-h"
        if (!this.options.host.startsWith("http://")) {
            this.options.host = "http://" + this.options.host;
        }
    }

    private void load() {
        // Create schema
        this.createSchema();

        // Load vertices
        System.out.print("Vertices has been imported: 0\b");
        LoadSummary vertexSummary = this.loadVertices();
        System.out.println(vertexSummary.insertSuccess());
        // Reset counters
        this.resetCounters();

        // Load edges
        System.out.print("Edges has been imported: 0\b");
        LoadSummary edgeSummary = this.loadEdges();
        System.out.println(edgeSummary.insertSuccess());
        // Reset counters
        this.resetCounters();

        // Print load summary
        LoaderUtil.printSummary(vertexSummary, edgeSummary);

        // Shutdown task manager
        this.shutdown(this.options.shutdownTimeout);
    }

    private void resetCounters() {
        this.taskManager.cleanup();
        this.parseFailureNum = 0L;
    }

    private void shutdown(int seconds) {
        this.taskManager.shutdown(seconds);
    }

    private void createSchema() {
        File schemaFile = FileUtils.getFile(this.options.schema);
        HugeClient client = HugeClientWrapper.get(this.options);
        GroovyExecutor groovyExecutor = new GroovyExecutor();
        groovyExecutor.bind("schema", client.schema());
        String script;
        try {
            script = FileUtils.readFileToString(schemaFile, "UTF-8");
        } catch (IOException e) {
            throw new LoadException("Read schema file '%s' error",
                                    e, this.options.schema);
        }
        groovyExecutor.execute(script, client);
    }

    private LoadSummary loadVertices() {
        Instant beginTime = Instant.now();

        // Execute loading tasks
        List<VertexSource> vertexSources = this.graphSource.vertexSources();
        for (VertexSource source : vertexSources) {
            LOG.info("Loading vertex source '{}'", source.label());
            VertexBuilder builder = new VertexBuilder(source, this.options);
            try {
                this.loadVertex(builder);
            } finally {
                try {
                    builder.close();
                } catch (Throwable e) {
                    LOG.warn("Failed to close builder for {} with exception {}",
                             source, e);
                }
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished("vertices");

        Instant endTime = Instant.now();

        LoadSummary summary = new LoadSummary("vertices");
        Duration duration = Duration.between(beginTime, endTime);
        summary.parseFailure(this.parseFailureNum);
        summary.insertFailure(this.taskManager.failureNum());
        summary.insertSuccess(this.taskManager.successNum());
        summary.loadTime(duration);

        return summary;
    }

    private void loadVertex(VertexBuilder builder) {
        int batchSize = this.options.batchSize;
        List<Vertex> batch = new ArrayList<>(batchSize);
        while (builder.hasNext()) {
            try {
                Vertex vertex = builder.next();
                batch.add(vertex);
            } catch (ParseException e) {
                if (this.options.testMode) {
                    throw e;
                }
                LOG.error("Vertex parse error", e);
                LOG_PARSE.error(e);
                if (++this.parseFailureNum >= this.options.maxParseErrors) {
                    LoaderUtil.printError("Error: More than %s vertices " +
                                          "parsing error ... Stopping",
                                          this.options.maxParseErrors);
                    // TODO: replace with a more graceful way
                    System.exit(-1);
                }
                continue;
            }
            if (batch.size() >= batchSize) {
                this.taskManager.submitVertexBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (batch.size() > 0) {
            this.taskManager.submitVertexBatch(batch);
        }
    }

    private LoadSummary loadEdges() {
        Instant beginTime = Instant.now();

        List<EdgeSource> edgeSources = this.graphSource.edgeSources();
        for (EdgeSource source : edgeSources) {
            LOG.info("Loading edge source '{}'", source.label());
            EdgeBuilder builder = new EdgeBuilder(source, this.options);
            try {
                this.loadEdge(builder);
            } finally {
                try {
                    builder.close();
                } catch (Throwable e) {
                    LOG.warn("Failed to close builder for {} with exception {}",
                             source, e);
                }
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished("edges");

        Instant endTime = Instant.now();

        LoadSummary summary = new LoadSummary("edges");
        Duration duration = Duration.between(beginTime, endTime);
        summary.parseFailure(this.parseFailureNum);
        summary.insertFailure(this.taskManager.failureNum());
        summary.insertSuccess(this.taskManager.successNum());
        summary.loadTime(duration);

        return summary;
    }

    private void loadEdge(EdgeBuilder builder) {
        int batchSize = this.options.batchSize;
        List<Edge> batch = new ArrayList<>(batchSize);
        while (builder.hasNext()) {
            try {
                Edge edge = builder.next();
                batch.add(edge);
            } catch (ParseException e) {
                if (this.options.testMode) {
                    throw e;
                }
                LOG.error("Edge parse error", e);
                LOG_PARSE.error(e);
                if (++this.parseFailureNum >= this.options.maxParseErrors) {
                    LoaderUtil.printError("Error: More than %s edges " +
                                          "parsing error ... Stopping",
                                          this.options.maxParseErrors);
                    System.exit(-1);
                }
                continue;
            }
            if (batch.size() >= batchSize) {
                this.taskManager.submitEdgeBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (batch.size() > 0) {
            this.taskManager.submitEdgeBatch(batch);
        }
    }
}
