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

package com.baidu.hugegraph.loader.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.executor.LoadLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoaderUtil;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.Log;

public final class TaskManager {

    private static final Logger LOG = Log.logger(TaskManager.class);
    private static final LoadLogger LOG_VERTEX_INSERT =
                         LoadLogger.logger("vertexInsertError");
    private static final LoadLogger LOG_EDGE_INSERT =
                         LoadLogger.logger("edgeInsertError");

    private static final long BATCH_PRINT_FREQUENCY = 10_000_000L;
    private static final long SINGLE_PRINT_FREQUENCY = 10_000L;

    private static final String BATCH_WORKER = "batch-worker-%d";
    private static final String SINGLE_WORKER = "single-worker-%d";

    private final LoadOptions options;

    private final BlockingExecutor batchExecutor;
    private final BlockingExecutor singleExecutor;
    private final List<CompletableFuture<Integer>> batchFutures;
    private final List<CompletableFuture<Void>> singleFutures;

    private final LongAdder successNum;
    private final LongAdder failureNum;

    public TaskManager(LoadOptions options) {
        this.options = options;
        // Let queue size same as thread nums
        this.batchExecutor = new BlockingExecutor(options.numThreads,
                                                  options.numThreads,
                                                  BATCH_WORKER);
        this.singleExecutor = new BlockingExecutor(options.numThreads,
                                                   options.numThreads,
                                                   SINGLE_WORKER);
        this.batchFutures = new ArrayList<>(options.numThreads);
        this.singleFutures = new ArrayList<>(options.numThreads);
        this.successNum = new LongAdder();
        this.failureNum = new LongAdder();
    }

    public long successNum() {
        return this.successNum.longValue();
    }

    public long failureNum() {
        return this.failureNum.longValue();
    }

    public void waitFinished(String type) {
        try {
            // Wait batch mode task finished
            for (CompletableFuture<Integer> future : this.batchFutures) {
                future.get();
            }
            LOG.info("Batch-mode tasks of {} finished", type);
        } catch (InterruptedException e) {
            // TODO: deal with it
        } catch (ExecutionException e) {
            // pass
        }

        try {
            // Wait single mode task finished
            for (CompletableFuture<Void> future : this.singleFutures) {
                future.get();
            }
            LOG.info("Single-mode tasks of {} finished", type);
        } catch (InterruptedException e) {
            // TODO: deal with it
        } catch (ExecutionException e) {
            // pass
        }
    }

    public void cleanup() {
        this.successNum.reset();
        this.failureNum.reset();
    }

    public void shutdown(int seconds) {
        LOG.debug("Attempt to shutdown batch-mode tasks executor");
        try {
            this.batchExecutor.shutdown();
            this.batchExecutor.awaitTermination(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The batch-mode tasks are interrupted");
        } finally {
            if (!this.batchExecutor.isTerminated()) {
                LOG.error("Cancel unfinished batch-mode tasks");
            }
            this.batchExecutor.shutdownNow();
        }

        LOG.debug("Attempt to shutdown single-mode tasks executor");
        try {
            this.singleExecutor.shutdown();
            this.singleExecutor.awaitTermination(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The single-mode task are interrupted");
        } finally {
            if (!this.singleExecutor.isTerminated()) {
                LOG.error("Cancel unfinished single-mode tasks");
            }
            this.singleExecutor.shutdownNow();
        }
    }

    public void submitVertexBatch(List<Vertex> batch) {
        InsertionTask<Vertex> task = new VertexInsertionTask(batch, this.options);
        CompletableFuture<Integer> future;
        future = CompletableFuture.supplyAsync(task, this.batchExecutor);
        future.exceptionally(t -> {
            LOG.error("Insert vertex failed", t);
            submitVerticesInSingleMode(batch);
            return 0;
        }).whenComplete((size, t) -> {
            if (t == null) {
                successNum.add(size);
                printProgress("Vertices", BATCH_PRINT_FREQUENCY, size);
            }
        });
        this.batchFutures.add(future);
    }

    public void submitEdgeBatch(List<Edge> batch) {
        InsertionTask<Edge> task = new EdgeInsertionTask(batch, this.options);
        CompletableFuture<Integer> future;
        future = CompletableFuture.supplyAsync(task, this.batchExecutor);
        future.exceptionally(t -> {
            LOG.error("Insert edge failed", t);
            submitEdgesInSingleMode(batch);
            return 0;
        }).whenComplete((size, t) -> {
            if (t == null) {
                successNum.add(size);
                printProgress("Edges", BATCH_PRINT_FREQUENCY, size);
            }
        });
        this.batchFutures.add(future);
    }

    private void submitVerticesInSingleMode(List<Vertex> vertices) {
        GraphManager graph = HugeClientWrapper.get(options).graph();

        this.submitInSingleMode(() -> {
            for (Vertex vertex : vertices) {
                try {
                    graph.addVertex(vertex);
                    successNum.add(1);
                } catch (Exception e) {
                    failureNum.add(1);
                    LOG.error("Vertex insert error", e);
                    if (options.testMode) {
                        throw e;
                    }
                    LOG_VERTEX_INSERT.error(new InsertException(vertex,
                                            e.getMessage()));

                    if (failureNum.longValue() >= options.maxInsertErrors) {
                        LoaderUtil.printError("Error: More than %s vertices " +
                                              "insert error... Stopping",
                                              options.maxInsertErrors);
                        System.exit(-1);
                    }
                }
            }
            printProgress("Vertices", SINGLE_PRINT_FREQUENCY, vertices.size());
        });
    }

    private void submitEdgesInSingleMode(List<Edge> edges) {
        GraphManager graph = HugeClientWrapper.get(options).graph();

        this.submitInSingleMode(() -> {
            for (Edge edge : edges) {
                try {
                    graph.addEdge(edge);
                    successNum.add(1);
                } catch (Exception e) {
                    failureNum.add(1);
                    LOG.error("Edge insert error", e);
                    if (options.testMode) {
                        throw e;
                    }
                    LOG_EDGE_INSERT.error(new InsertException(edge,
                                          e.getMessage()));

                    if (failureNum.longValue() >= options.maxInsertErrors) {
                        LoaderUtil.printError("Error: More than %s edges " +
                                              "insert error... Stopping",
                                              options.maxInsertErrors);
                        System.exit(-1);
                    }
                }
            }
            printProgress("Edges", SINGLE_PRINT_FREQUENCY, edges.size());
        });
    }

    private void submitInSingleMode(Runnable runnable) {
        CompletableFuture<Void> future;
        future = CompletableFuture.runAsync(runnable, this.singleExecutor);
        this.singleFutures.add(future);
    }

    private void printProgress(String type, long frequency, int batchSize) {
        long inserted = this.successNum.longValue();
        LoaderUtil.printInBackward(inserted);
        if (inserted % frequency < batchSize) {
            LOG.info("{} has been imported: {}", type, inserted);
        }
    }
}
