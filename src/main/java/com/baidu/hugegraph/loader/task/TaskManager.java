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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.executor.HugeClients;
import com.baidu.hugegraph.loader.executor.LoadLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.LoaderUtil;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
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

    private final int futureNum;
    private final Semaphore available;
    private final ListeningExecutorService batchService;
    private final ExecutorService singleExecutor;
    private final CompletionService<Void> singleService;
    private final LongAdder singleTasks;

    private final LongAdder successNum;
    private final LongAdder failureNum;

    public TaskManager(LoadOptions options) {
        this.futureNum = options.numThreads;
        this.available = new Semaphore(this.futureNum);
        ExecutorService pool = Executors.newFixedThreadPool(this.futureNum);
        this.batchService = MoreExecutors.listeningDecorator(pool);
        this.singleExecutor = Executors.newFixedThreadPool(1);
        this.singleService = new ExecutorCompletionService<>(singleExecutor);
        this.singleTasks = new LongAdder();
        this.successNum = new LongAdder();
        this.failureNum = new LongAdder();
    }

    public long successNum() {
        return this.successNum.longValue();
    }

    public long failureNum() {
        return this.failureNum.longValue();
    }

    public boolean waitFinished(int timeout) {
        try {
            // Wait batch task finished
            this.available.acquire(this.futureNum);
            // Wait single task finished
            this.tryConsume(timeout);
            if (this.singleTasks.longValue() != 0L) {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        } finally {
            this.available.release(this.futureNum);
        }
        return true;
    }

    public void cleanup() {
        this.successNum.reset();
        this.failureNum.reset();
    }

    public void shutdown(int seconds) {
        try {
            LOG.debug("Attempt to shutdown executor.");
            this.batchService.shutdown();
            this.batchService.awaitTermination(seconds, TimeUnit.SECONDS);
            this.singleExecutor.shutdown();
            this.singleExecutor.awaitTermination(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Tasks is interrupted.");
        } finally {
            if (!this.batchService.isTerminated()) {
                LOG.error("Cancel unfinished tasks.");
            }
            this.batchService.shutdownNow();
            LOG.debug("Shutdown is completed.");
        }
    }

    public void submitVertexBatch(List<Vertex> batch) {
        this.ensurePoolAvailable();
        try {
            this.available.acquire();
        } catch (InterruptedException ignored) {
        }

        InsertVertexTask task = new InsertVertexTask(batch);
        ListenableFuture<Integer> future = this.batchService.submit(task);

        Futures.addCallback(future, new FutureCallback<Integer>() {

            @Override
            public void onSuccess(Integer size) {
                available.release();
                successNum.add(size);
                printProgress("Vertices", BATCH_PRINT_FREQUENCY, size);
            }

            @Override
            public void onFailure(Throwable t) {
                available.release();
                submitVerticesInSingleMode(batch);
            }
        });
    }

    public void submitEdgeBatch(List<Edge> batch) {
        this.ensurePoolAvailable();
        try {
            this.available.acquire();
        } catch (InterruptedException ignored) {
        }

        InsertEdgeTask task = new InsertEdgeTask(batch);
        ListenableFuture<Integer> future = this.batchService.submit(task);

        Futures.addCallback(future, new FutureCallback<Integer>() {

            @Override
            public void onSuccess(Integer size) {
                available.release();
                successNum.add(size);
                printProgress("Edges", BATCH_PRINT_FREQUENCY, size);
            }

            @Override
            public void onFailure(Throwable t) {
                available.release();
                submitEdgesInSingleMode(batch);
            }
        });
    }

    private void submitVerticesInSingleMode(List<Vertex> vertices) {
        LoadOptions options = LoadOptions.instance();
        int maxInsertErrors = options.maxInsertErrors;
        int shutdownTimeout = options.shutdownTimeout;
        GraphManager graph = HugeClients.get(options).graph();
        this.singleService.submit(() -> {
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

                    if (failureNum.longValue() >= maxInsertErrors) {
                        LOG.error("Too many vertices insert error... Stopping");
                        // Print an empty line.
                        System.out.println();
                        System.out.println(String.format(
                                           "Error: More than %s vertices " +
                                           "insert error... Stopping",
                                           maxInsertErrors));
                        this.shutdown(shutdownTimeout);
                        System.exit(-1);
                    }
                }
            }
            printProgress("Vertices", SINGLE_PRINT_FREQUENCY, vertices.size());
            return null;
        });
        this.singleTasks.increment();

        try {
            this.tryConsume(0L);
        } catch (InterruptedException ignored) {}
    }

    private void submitEdgesInSingleMode(List<Edge> edges) {
        LoadOptions options = LoadOptions.instance();
        int maxInsertErrors = options.maxInsertErrors;
        int shutdownTimeout = options.shutdownTimeout;
        GraphManager graph = HugeClients.get(options).graph();
        this.singleService.submit(() -> {
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

                    if (failureNum.longValue() >= maxInsertErrors) {
                        LOG.error("Too many edges insert error... Stopping");
                        // Print an empty line.
                        System.out.println();
                        System.out.println(String.format(
                                           "Error: More than %s edges " +
                                           "insert error... Stopping",
                                           maxInsertErrors));
                        this.shutdown(shutdownTimeout);
                        System.exit(-1);
                    }
                }
            }
            printProgress("Edges", SINGLE_PRINT_FREQUENCY, edges.size());
            return null;
        });
        this.singleTasks.increment();

        try {
            this.tryConsume(0L);
        } catch (InterruptedException ignored) {}
    }

    private void tryConsume(long timeout) throws InterruptedException {
        long total = this.singleTasks.longValue();
        for (long i = 0; i < total; i++) {
            Future<?> future = this.singleService.poll(timeout, TimeUnit.SECONDS);
            // The future is null if timeout
            if (future != null) {
                this.singleTasks.decrement();
            }
        }
    }

    private void printProgress(String type, long frequency, int batchSize) {
        long inserted = this.successNum.longValue();
        System.out.print(String.format(" %d%s",
                         inserted, LoaderUtil.backward(inserted)));
        if (inserted % frequency < batchSize) {
            LOG.info("{} has been imported: {}", type, inserted);
        }
    }

    private void ensurePoolAvailable() {
        while (this.batchService.isShutdown()){
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {
                // That's fine, just continue.
            }
        }
    }
}
