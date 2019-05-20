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

import static com.baidu.hugegraph.loader.constant.Constants.BATCH_PRINT_FREQUENCY;
import static com.baidu.hugegraph.loader.constant.Constants.BATCH_WORKER;
import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_PRINT_FREQUENCY;
import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_WORKER;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.ExecutorUtil;
import com.baidu.hugegraph.util.Log;

public final class TaskManager {

    private static final Logger LOG = Log.logger(TaskManager.class);
    private static final FailureLogger LOG_VERTEX_INSERT =
                         FailureLogger.logger("vertexInsertError");
    private static final FailureLogger LOG_EDGE_INSERT =
                         FailureLogger.logger("edgeInsertError");

    private final LoadOptions options;
    private final LoadMetrics counter;

    private final Semaphore batchSemaphore;
    private final Semaphore singleSemaphore;
    private final ExecutorService batchService;
    private final ExecutorService singleService;

    public TaskManager(LoadOptions options, LoadMetrics counter) {
        this.options = options;
        this.counter = counter;
        this.batchSemaphore = new Semaphore(options.numThreads);
        this.singleSemaphore = new Semaphore(options.numThreads);
        this.batchService = ExecutorUtil.newFixedThreadPool(options.numThreads,
                                                            BATCH_WORKER);
        this.singleService = ExecutorUtil.newFixedThreadPool(options.numThreads,
                                                             SINGLE_WORKER);
    }

    public void waitFinished(ElemType type) {
        try {
            // Wait batch mode task finished
            this.batchSemaphore.acquire(this.options.numThreads);
            LOG.info("Batch-mode tasks of {} finished", type.string());
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting batch-mode tasks");
        } finally {
            this.batchSemaphore.release(this.options.numThreads);
        }

        try {
            // Wait single mode task finished
            this.singleSemaphore.acquire(options.numThreads);
            LOG.info("Single-mode tasks of {} finished", type.string());
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting batch-mode tasks");
        } finally {
            this.singleSemaphore.release(this.options.numThreads);
        }
    }

    public void shutdown(int seconds) {
        LOG.debug("Attempt to shutdown batch-mode tasks executor");
        try {
            this.batchService.shutdown();
            this.batchService.awaitTermination(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The batch-mode tasks are interrupted");
        } finally {
            if (!this.batchService.isTerminated()) {
                LOG.error("Cancel unfinished batch-mode tasks");
            }
            this.batchService.shutdownNow();
        }

        LOG.debug("Attempt to shutdown single-mode tasks executor");
        try {
            this.singleService.shutdown();
            this.singleService.awaitTermination(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The single-mode task are interrupted");
        } finally {
            if (!this.singleService.isTerminated()) {
                LOG.error("Cancel unfinished single-mode tasks");
            }
            this.singleService.shutdownNow();
        }
    }

    public void submitVertexBatch(List<Vertex> batch) {
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit " +
                                    "vertex batch in batch mode", e);
        }

        InsertionTask<Vertex> task = new VertexInsertionTask(batch, this.options);
        CompletableFuture<Integer> future;
        future = CompletableFuture.supplyAsync(task, this.batchService);
        future.exceptionally(error -> {
            LOG.error("Insert vertex failed", error);
            this.submitVerticesInSingleMode(batch);
            return 0;
        }).whenComplete((size, error) -> {
            if (error == null) {
                this.counter.addInsertSuccessNum(size);
                printProgress(ElemType.VERTEX, BATCH_PRINT_FREQUENCY, size);
            }
            this.batchSemaphore.release();
        });
    }

    public void submitEdgeBatch(List<Edge> batch) {
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit " +
                                    "edge batch in batch mode", e);
        }

        InsertionTask<Edge> task = new EdgeInsertionTask(batch, this.options);
        CompletableFuture<Integer> future;
        future = CompletableFuture.supplyAsync(task, this.batchService);
        future.exceptionally(error -> {
            LOG.error("Insert edge failed", error);
            this.submitEdgesInSingleMode(batch);
            return 0;
        }).whenComplete((size, error) -> {
            if (error == null) {
                this.counter.addInsertSuccessNum(size);
                this.printProgress(ElemType.EDGE, BATCH_PRINT_FREQUENCY, size);
            }
            this.batchSemaphore.release();
        });
    }

    private void submitVerticesInSingleMode(List<Vertex> vertices) {
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit " +
                                    "vertex batch in single mode", e);
        }

        GraphManager graph = HugeClientWrapper.get(this.options).graph();
        this.submitInSingleMode(() -> {
            for (Vertex vertex : vertices) {
                try {
                    graph.addVertex(vertex);
                    this.counter.increaseInsertSuccessNum();
                } catch (Exception e) {
                    this.counter.increaseInsertFailureNum();
                    LOG.error("Vertex insert error", e);
                    if (this.options.testMode) {
                        throw e;
                    }
                    LOG_VERTEX_INSERT.error(new InsertException(vertex,
                                            e.getMessage()));
                    if (this.counter.insertFailureNum() >=
                        options.maxInsertErrors) {
                        Printer.printError("Error: More than %s vertices " +
                                           "insert error... stopping",
                                           options.maxInsertErrors);
                        LoadUtil.exit(Constants.EXIT_CODE_ERROR);
                    }
                }
            }
            printProgress(ElemType.VERTEX, SINGLE_PRINT_FREQUENCY,
                          vertices.size());
        });
    }

    private void submitEdgesInSingleMode(List<Edge> edges) {
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit " +
                                    "edge batch in single mode", e);
        }

        GraphManager graph = HugeClientWrapper.get(this.options).graph();
        this.submitInSingleMode(() -> {
            for (Edge edge : edges) {
                try {
                    graph.addEdge(edge);
                    this.counter.increaseInsertSuccessNum();
                } catch (Exception e) {
                    this.counter.increaseInsertFailureNum();
                    LOG.error("Edge insert error", e);
                    if (this.options.testMode) {
                        throw e;
                    }
                    LOG_EDGE_INSERT.error(new InsertException(edge,
                                          e.getMessage()));

                    if (this.counter.insertFailureNum() >=
                        options.maxInsertErrors) {
                        Printer.printError("Error: More than %s edges " +
                                           "insert error... stopping",
                                           options.maxInsertErrors);
                        LoadUtil.exit(Constants.EXIT_CODE_ERROR);
                    }
                }
            }
            printProgress(ElemType.EDGE, SINGLE_PRINT_FREQUENCY, edges.size());
        });
    }

    private void submitInSingleMode(Runnable runnable) {
        CompletableFuture<Void> future;
        future = CompletableFuture.runAsync(runnable, this.singleService);
        future.whenComplete((r, error) -> {
            this.singleSemaphore.release();
        });
    }

    private void printProgress(ElemType type, long frequency, int batchSize) {
        long loaded = this.counter.insertSuccessNum();
        Printer.printInBackward(loaded);
        if (loaded % frequency < batchSize) {
            LOG.info("{} has been loaded: {}", type.string(), loaded);
        }
    }
}
