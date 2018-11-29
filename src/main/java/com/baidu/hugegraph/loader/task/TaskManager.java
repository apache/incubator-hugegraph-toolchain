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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoaderUtil;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.ExecutorUtil;
import com.baidu.hugegraph.util.Log;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class TaskManager {

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

    private final Semaphore batchSemaphore;
    private final ListeningExecutorService batchService;
    private final Semaphore singleSemaphore;
    private final ListeningExecutorService singleService;

    private final LongAdder successNum;
    private final LongAdder failureNum;

    public TaskManager(LoadOptions options) {
        this.options = options;
        this.batchSemaphore = new Semaphore(options.numThreads);
        this.singleSemaphore = new Semaphore(options.numThreads);
        this.batchService = MoreExecutors.listeningDecorator(
                            ExecutorUtil.newFixedThreadPool(options.numThreads,
                                                            BATCH_WORKER));
        this.singleService = MoreExecutors.listeningDecorator(
                             ExecutorUtil.newFixedThreadPool(options.numThreads,
                                                             SINGLE_WORKER));
        this.successNum = new LongAdder();
        this.failureNum = new LongAdder();
    }

    public long successNum() {
        return this.successNum.longValue();
    }

    public long failureNum() {
        return this.failureNum.longValue();
    }

    public boolean waitFinished(String type) {
        try {
            // Wait batch mode task finished
            this.batchSemaphore.acquire(options.numThreads);
            LOG.info("Batch-mode tasks of {} finished", type);
            // Wait single mode task finished
            this.singleSemaphore.acquire(options.numThreads);
            LOG.info("Single-mode tasks of {} finished", type);
        } catch (InterruptedException e) {
            return false;
        } finally {
            this.batchSemaphore.release(options.numThreads);
            this.singleSemaphore.release(options.numThreads);
        }
        return true;
    }

    public void cleanup() {
        this.successNum.reset();
        this.failureNum.reset();
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
        this.ensurePoolAvailable();

        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted", e);
        }

        InsertionTask<Vertex> task = new VertexInsertionTask(batch, this.options);
        ListenableFuture<Integer> future = this.batchService.submit(task);

        Futures.addCallback(future, new FutureCallback<Integer>() {

            @Override
            public void onSuccess(Integer size) {
                successNum.add(size);
                batchSemaphore.release();
                printProgress("Vertices", BATCH_PRINT_FREQUENCY, size);
            }

            @Override
            public void onFailure(Throwable t) {
                submitVerticesInSingleMode(batch);
                batchSemaphore.release();
            }
        });
    }

    public void submitEdgeBatch(List<Edge> batch) {
        this.ensurePoolAvailable();

        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted", e);
        }

        InsertionTask<Edge> task = new EdgeInsertionTask(batch, this.options);
        ListenableFuture<Integer> future = this.batchService.submit(task);

        Futures.addCallback(future, new FutureCallback<Integer>() {

            @Override
            public void onSuccess(Integer size) {
                successNum.add(size);
                batchSemaphore.release();
                printProgress("Edges", BATCH_PRINT_FREQUENCY, size);
            }

            @Override
            public void onFailure(Throwable t) {
                submitEdgesInSingleMode(batch);
                batchSemaphore.release();
            }
        });
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
            return null;
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
            return null;
        });
    }

    private void submitInSingleMode(Callable<Void> callable) {
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException ignored) {}

        ListenableFuture<Void> future = this.singleService.submit(callable);

        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                singleSemaphore.release();
            }

            @Override
            public void onFailure(Throwable t) {
                singleSemaphore.release();
            }
        });
    }

    private void printProgress(String type, long frequency, int batchSize) {
        long inserted = this.successNum.longValue();
        LoaderUtil.printInBackward(inserted);
        if (inserted % frequency < batchSize) {
            LOG.info("{} has been imported: {}", type, inserted);
        }
    }

    private void ensurePoolAvailable() {
        if (this.batchService.isShutdown()) {
            throw new LoadException("The batch-mode thread pool " +
                                    "has been closed");
        }
        if (this.singleService.isShutdown()) {
            throw new LoadException("The single-mode thread pool " +
                                    "has been closed");
        }
    }
}
