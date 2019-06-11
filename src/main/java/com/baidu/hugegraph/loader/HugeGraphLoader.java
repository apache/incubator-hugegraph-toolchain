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
import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.GroovyExecutor;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.struct.EdgeStruct;
import com.baidu.hugegraph.loader.struct.GraphStruct;
import com.baidu.hugegraph.loader.struct.VertexStruct;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.task.TaskManager;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;

public final class HugeGraphLoader {

    private static final Logger LOG = Log.logger(HugeGraphLoader.class);
    private static final FailureLogger LOG_PARSE =
                         FailureLogger.logger("parse-error");

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
            LoadOptions options = this.context.options();
            try {
                this.context.newProgress().write(options.file);
            } catch (IOException e) {
                LOG.warn("Failed to write load progress", e);
            }
        }));
    }

    private void load() {
        try {
            // Create schema
            this.createSchema();
            // Load vertices
            this.loadVertices();
            // Load edges
            this.loadEdges();
        } catch (Exception e) {
            Printer.printError("Failed to load due to " + e.getMessage(), e);
            this.taskManager.shutdown();
            throw e;
        }

        // Print load summary
        Printer.printSummary(this.context);

        this.stopLoading(Constants.EXIT_CODE_NORM);
    }

    private void createSchema() {
        LoadOptions options = this.context.options();
        if (StringUtils.isEmpty(options.schema)) {
            return;
        }
        File schemaFile = FileUtils.getFile(options.schema);
        HugeClient client = HugeClientWrapper.get(options);
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

    private void loadVertices() {
        LoadMetrics metrics = this.context.summary().vertexMetrics();
        Printer.printElemType(metrics.type());
        metrics.startTimer();

        InputProgressMap newProgress = this.context.newProgress().vertex();
        List<VertexStruct> vertexStructs = this.graphStruct.vertexStructs();
        for (VertexStruct struct : vertexStructs) {
            // Update loading vertex struct
            newProgress.addStruct(struct);
            LOG.info("Loading vertex struct with label {}", struct.label());
            // Produce batch vertices and execute loading tasks
            try (VertexBuilder builder = new VertexBuilder(this.context,
                                                           struct)) {
                this.loadVertex(builder, this.context.options(), metrics);
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(ElemType.VERTEX);

        metrics.stopTimer();
        Printer.print(metrics.insertSuccess());
    }

    private void loadVertex(VertexBuilder builder, LoadOptions options,
                            LoadMetrics metrics) {
        int batchSize = options.batchSize;
        List<Vertex> batch = new ArrayList<>(batchSize);
        while (true) {
            try {
                if (!builder.hasNext()) {
                    break;
                }
                Vertex vertex = builder.next();
                batch.add(vertex);
            } catch (ParseException e) {
                if (options.testMode) {
                    throw e;
                }
                LOG.error("Vertex parse error", e);
                LOG_PARSE.error(e);
                long failureNum = metrics.increaseParseFailure();
                if (failureNum >= options.maxParseErrors) {
                    Printer.printError("More than %s vertices " +
                                       "parsing error ... stopping",
                                       options.maxParseErrors);
                    this.stopLoading(Constants.EXIT_CODE_ERROR);
                }
                continue;
            }
            if (batch.size() >= batchSize) {
                this.taskManager.submitVertexBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty()) {
            this.taskManager.submitVertexBatch(batch);
        }
    }

    private void loadEdges() {
        LoadMetrics metrics = this.context.summary().edgeMetrics();
        Printer.printElemType(metrics.type());
        metrics.startTimer();

        InputProgressMap newProgress = this.context.newProgress().edge();
        List<EdgeStruct> edgeStructs = this.graphStruct.edgeStructs();
        for (EdgeStruct struct : edgeStructs) {
            // Update loading edge struct
            newProgress.addStruct(struct);
            LOG.info("Loading edge struct with label {}", struct.label());
            // Produce batch vertices and execute loading tasks
            try (EdgeBuilder builder = new EdgeBuilder(this.context, struct)) {
                this.loadEdge(builder, this.context.options(), metrics);
            }
        }
        // Waiting async worker threads finish
        this.taskManager.waitFinished(ElemType.EDGE);

        metrics.stopTimer();
        Printer.print(metrics.insertSuccess());
    }

    private void loadEdge(EdgeBuilder builder, LoadOptions options,
                          LoadMetrics metrics) {
        int batchSize = options.batchSize;
        List<Edge> batch = new ArrayList<>(batchSize);
        while (true) {
            try {
                if (!builder.hasNext()) {
                    break;
                }
                Edge edge = builder.next();
                batch.add(edge);
            } catch (ParseException e) {
                if (options.testMode) {
                    throw e;
                }
                LOG.error("Edge parse error", e);
                LOG_PARSE.error(e);
                long failureNum = metrics.increaseParseFailure();
                if (failureNum >= options.maxParseErrors) {
                    Printer.printError("More than %s edges " +
                                       "parsing error ... stopping",
                                       options.maxParseErrors);
                    this.stopLoading(Constants.EXIT_CODE_ERROR);
                }
                continue;
            }
            if (batch.size() >= batchSize) {
                this.taskManager.submitEdgeBatch(batch);
                batch = new ArrayList<>(batchSize);
            }
        }
        if (!batch.isEmpty()) {
            this.taskManager.submitEdgeBatch(batch);
        }
    }

    private void stopLoading(int code) {
        // Shutdown task manager
        this.taskManager.shutdown();
        // Exit JVM if the code is not EXIT_CODE_NORM
        if (Constants.EXIT_CODE_NORM != code) {
            LoadUtil.exit(code);
        }
    }
}
