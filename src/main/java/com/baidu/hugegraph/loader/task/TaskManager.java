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

import static com.baidu.hugegraph.loader.constant.Constants.BATCH_PRINT_FREQ;
import static com.baidu.hugegraph.loader.constant.Constants.BATCH_WORKER;
import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_PRINT_FREQ;
import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_WORKER;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.LoadContext;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.FailureLogger;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.Printer;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.ExecutorUtil;
import com.baidu.hugegraph.util.Log;

public final class TaskManager {

    private static final Logger LOG = Log.logger(TaskManager.class);

    private final FailureLogger failureLogger = FailureLogger.insert();

    private final LoadOptions options;
    private final LoadSummary summary;

    private final Semaphore batchSemaphore;
    private final Semaphore singleSemaphore;
    private final ExecutorService batchService;
    private final ExecutorService singleService;

    public TaskManager(LoadContext context) {
        this.options = context.options();
        this.summary = context.summary();
        this.batchSemaphore = new Semaphore(options.numThreads);
        this.singleSemaphore = new Semaphore(options.numThreads);
        /*
         * In principle, unbounded synchronization queue(which may lead to OOM)
         * should not be used, but there the task manager uses semaphores to
         * limit the number of tasks added. When there are no idle threads in
         * the thread pool, the producer will be blocked, so OOM will not occur.
         */
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

    public void shutdown() {
        long timeout = this.options.shutdownTimeout;
        LOG.debug("Attempt to shutdown batch-mode tasks executor");
        try {
            this.batchService.shutdown();
            this.batchService.awaitTermination(timeout, TimeUnit.SECONDS);
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
            this.singleService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The single-mode task are interrupted");
        } finally {
            if (!this.singleService.isTerminated()) {
                LOG.error("Cancel unfinished single-mode tasks");
            }
            this.singleService.shutdownNow();
        }
    }

    public <GE extends GraphElement> void submitBatch(ElemType type,
                                                      List<GE> batch) {
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in batch mode", e, type);
        }

        BatchInsertTask<GE> task = new BatchInsertTask<>(type, batch,
                                                         this.options);
        CompletableFuture<Integer> future;
        future = CompletableFuture.supplyAsync(task, this.batchService);
        future.exceptionally(error -> {
            LOG.warn("Batch insert {} error, try single insert", type, error);
            this.submitInSingleMode(type, batch);
            return 0;
        }).whenComplete((size, error) -> {
            if (error == null) {
                LoadMetrics metrics = this.summary.metrics(type);
                metrics.addInsertSuccess(size);
                Printer.printProgress(metrics, BATCH_PRINT_FREQ, size);
            }
            this.batchSemaphore.release();
        });
    }

    private <GE extends GraphElement> void submitInSingleMode(ElemType type,
                                                              List<GE> batch) {
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in single mode", e, type);
        }

        Runnable singleInsertTask = () -> {
            LoadMetrics metrics = this.summary.metrics(type);
            for (GE element : batch) {
                try {
                    HugeClientWrapper.addSingle(type, element);
                    metrics.increaseInsertSuccess();
                } catch (Exception e) {
                    metrics.increaseInsertFailure();
                    LOG.error("Single insert {} error", type, e);
                    if (this.options.testMode) {
                        throw e;
                    }
                    this.failureLogger.error(type,
                                             new InsertException(element, e));

                    if (metrics.insertFailure() >=
                        this.options.maxInsertErrors) {
                        Printer.printError("More than %s %s insert error... " +
                                           "stopping",
                                           this.options.maxInsertErrors, type);
                        LoadUtil.exit(Constants.EXIT_CODE_ERROR);
                    }
                }
            }
            Printer.printProgress(metrics, SINGLE_PRINT_FREQ, batch.size());
        };

        CompletableFuture<Void> future;
        future = CompletableFuture.runAsync(singleInsertTask, this.singleService);
        future.whenComplete((r, error) -> this.singleSemaphore.release());
    }
}
