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
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.HugeClients;
import com.baidu.hugegraph.loader.executor.LoadLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.executor.LoadSummary;
import com.baidu.hugegraph.loader.parser.EdgeParser;
import com.baidu.hugegraph.loader.parser.VertexParser;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.InputReaderFactory;
import com.baidu.hugegraph.loader.source.EdgeSource;
import com.baidu.hugegraph.loader.source.GraphSource;
import com.baidu.hugegraph.loader.source.VertexSource;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.beust.jcommander.JCommander;

public class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);
    private static final LoadLogger LOG_PARSE = LoadLogger.logger("parseError");

    private final JCommander commander;
    private final TaskManager taskManager;
    private final GraphSource graphSource;

    private long parseFailureNum = 0L;

    public static void main(String[] args) {
        HugeGraphLoader loader = new HugeGraphLoader(args);
        LoadSummary summary = loader.load();
        summary.print();
    }

    private HugeGraphLoader(String[] args) {
        LoadOptions options = LoadOptions.instance();
        this.commander = JCommander.newBuilder().addObject(options).build();
        this.parseAndCheckOptions(args);
        this.taskManager = new TaskManager(options);
        this.graphSource = GraphSource.of(options.file);
    }

    private void parseAndCheckOptions(String[] args) {
        this.commander.parse(args);
        // Check options
        LoadOptions options = LoadOptions.instance();
        // Check option "-f"
        E.checkArgument(!StringUtils.isEmpty(options.file),
                        "Must specified entrance groovy file");
        File scriptFile = new File(options.file);
        if (!scriptFile.canRead()) {
            LOG.error("Script file must be readable: '{}'",
                      scriptFile.getAbsolutePath());
            this.exitWithUsage(-1);
        }
        // Check option "-g"
        E.checkArgument(!StringUtils.isEmpty(options.graph),
                        "Must specified a graph");
        // Check option "-h"
        if (!options.host.startsWith("http://")) {
            options.host = "http://" + options.host;
        }
    }

    private LoadSummary load() {
        // Create schema
        this.createSchema();

        LoadSummary summary = new LoadSummary();
        // Prepare to load vertices
        Instant begTime = Instant.now();
        System.out.print("Vertices has been imported: 0\b\b");
        // Load vertices
        this.loadVertices();
        Instant endTime = Instant.now();
        Duration duration = Duration.between(begTime, endTime);
        summary.parseFailureVertices(this.parseFailureNum);
        summary.insertFailureVertices(this.taskManager.failureNum());
        summary.insertSuccessVertices(this.taskManager.successNum());
        summary.vertexLoadTime(duration);
        System.out.println(" " + summary.insertSuccessVertices());
        // Reset counters
        this.resetCounters();

        // Prepare to load edges ...
        begTime = Instant.now();
        System.out.print("Edges has been imported: 0\b\b");
        // Load edges
        this.loadEdges();
        endTime = Instant.now();
        duration = Duration.between(begTime, endTime);
        summary.parseFailureEdges(this.parseFailureNum);
        summary.insertFailureEdges(this.taskManager.failureNum());
        summary.insertSuccessEdges(this.taskManager.successNum());
        summary.edgeLoadTime(duration);
        System.out.println(" " + summary.insertSuccessEdges());
        // Reset counters
        this.resetCounters();

        LoadOptions options = LoadOptions.instance();
        // Shutdown task manager
        this.taskManager.shutdown(options.shutdownTimeout);
        return summary;
    }

    private void resetCounters() {
        this.taskManager.cleanup();
        this.parseFailureNum = 0L;
    }

    private void createSchema() {
        LoadOptions options = LoadOptions.instance();
        File schemaFile = FileUtils.getFile(options.schema);
        HugeClient client = HugeClients.get(options);
        GroovyExecutor groovyExecutor = new GroovyExecutor();
        groovyExecutor.bind("schema", client.schema());
        String script;
        try {
            script = FileUtils.readFileToString(schemaFile, "UTF-8");
        } catch (IOException e) {
            throw new LoadException("Read schema file '%s' error",
                                    e, options.schema);
        }
        groovyExecutor.execute(script, client);
    }

    private void loadVertices() {
        LoadOptions options = LoadOptions.instance();
        List<VertexSource> vertexSources = this.graphSource.vertexSources();
        for (VertexSource source : vertexSources) {
            InputReader reader = InputReaderFactory.create(source.input());
            VertexParser parser = new VertexParser(source, reader);
            this.loadVertex(parser);
            try {
                parser.close();
            } catch (Exception e) {
                LOG.warn("Failed to close parser for vertex source {}", source);
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(options.timeout);
    }

    private void loadVertex(VertexParser parser) {
        LoadOptions options = LoadOptions.instance();
        int batchSize = options.batchSize;
        List<Vertex> batch = new ArrayList<>(batchSize);
        while (parser.hasNext()) {
            try {
                Vertex vertex = parser.next();
                batch.add(vertex);
            } catch (ParseException e) {
                if (options.testMode) {
                    throw e;
                }
                LOG.error("Vertex parse error", e);
                LOG_PARSE.error(e);
                if (++this.parseFailureNum >= options.maxParseErrors) {
                    exitWithInfo("vertices", options.maxParseErrors);
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

    private void loadEdges() {
        LoadOptions options = LoadOptions.instance();
        List<EdgeSource> edgeSources = this.graphSource.edgeSources();
        for (EdgeSource source : edgeSources) {
            InputReader reader = InputReaderFactory.create(source.input());
            EdgeParser parser = new EdgeParser(source, reader);
            this.loadEdge(parser);
            try {
                parser.close();
            } catch (Exception e) {
                LOG.warn("Failed to close parser for edge source {}", source);
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(options.timeout);
    }

    private void loadEdge(EdgeParser parser) {
        LoadOptions options = LoadOptions.instance();
        int batchSize = options.batchSize;
        List<Edge> batch = new ArrayList<>(batchSize);
        while (parser.hasNext()) {
            try {
                Edge edge = parser.next();
                batch.add(edge);
            } catch (ParseException e) {
                if (options.testMode) {
                    throw e;
                }
                LOG.error("Edge parse error", e);
                LOG_PARSE.error(e);
                if (++this.parseFailureNum >= options.maxParseErrors) {
                    exitWithInfo("edges", options.maxParseErrors);
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

    private void exitWithUsage(int status) {
        this.commander.usage();
        System.exit(status);
    }

    private static void exitWithInfo(String type, int parseErrors) {
        LOG.error("Too many {} parse error ... Stopping", type);
        // Print an empty line.
        System.out.println();
        System.out.println(String.format(
                           "Error: More than %s %s parsing error ... Stopping",
                           parseErrors, type));
        System.exit(0);
    }
}
