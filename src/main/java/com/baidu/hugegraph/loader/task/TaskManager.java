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

import static com.baidu.hugegraph.loader.constant.Constants.BATCH_WORKER;
import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_WORKER;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.ExecutorUtil;
import com.baidu.hugegraph.util.Log;

public final class TaskManager {

    private static final Logger LOG = Log.logger(TaskManager.class);

    private final LoadContext context;
    private final LoadOptions options;

    private final Semaphore batchSemaphore;
    private final Semaphore singleSemaphore;
    private final ExecutorService batchService;
    private final ExecutorService singleService;

    private volatile boolean stopped;

    public TaskManager(LoadContext context) {
        this.context = context;
        this.options = context.options();
        // Try to make all batch threads running and don't wait for producer
        this.batchSemaphore = new Semaphore(this.batchSemaphoreNum());
        /*
         * Let batch threads go forward as far as possible and don't wait for
         * single thread pool
         */
        this.singleSemaphore = new Semaphore(this.singleSemaphoreNum());
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
        this.stopped = false;
    }

    private int batchSemaphoreNum() {
        return 1 + this.options.numThreads;
    }

    private int singleSemaphoreNum() {
        return 2 * this.options.numThreads;
    }

    public void waitFinished(ElemType type) {
        if (type == null || this.stopped) {
            return;
        }

        LOG.info("Waiting for the insert tasks of {} finish", type.string());
        try {
            // Wait batch mode task finished
            this.batchSemaphore.acquire(this.batchSemaphoreNum());
            LOG.info("The batch-mode tasks finished");
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting batch-mode tasks");
        } finally {
            this.batchSemaphore.release(this.batchSemaphoreNum());
        }

        try {
            // Wait single mode task finished
            this.singleSemaphore.acquire(this.singleSemaphoreNum());
            LOG.info("The single-mode tasks finished");
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting single-mode tasks");
        } finally {
            this.singleSemaphore.release(this.singleSemaphoreNum());
        }
    }

    public void shutdown() {
        this.stopped = true;

        LOG.debug("Ready to shutdown batch-mode tasks executor");
        long timeout = this.options.shutdownTimeout;
        try {
            this.batchService.shutdown();
            this.batchService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The batch-mode tasks are interrupted");
        } finally {
            if (!this.batchService.isTerminated()) {
                LOG.error("The unfinished batch-mode tasks will be cancelled");
            }
            this.batchService.shutdownNow();
        }

        LOG.debug("Ready to shutdown single-mode tasks executor");
        try {
            this.singleService.shutdown();
            this.singleService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The single-mode tasks are interrupted");
        } finally {
            if (!this.singleService.isTerminated()) {
                LOG.error("The unfinished single-mode tasks will be cancelled");
            }
            this.singleService.shutdownNow();
        }
    }

    public <GE extends GraphElement> void submitBatch(ElementStruct struct,
                                                      List<Record<GE>> batch) {
        ElemType type = struct.type();
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in batch mode", e, type);
        }

        InsertTask<GE> task = new BatchInsertTask<>(this.context, struct,
                                                    batch);
        CompletableFuture.runAsync(task, this.batchService).exceptionally(e -> {
            LOG.warn("Batch insert {} error, try single insert", type, e);
            this.submitInSingle(struct, batch);
            return null;
        }).whenComplete((r, e) -> this.batchSemaphore.release());
    }

    private <GE extends GraphElement> void submitInSingle(ElementStruct struct,
                                                          List<Record<GE>> batch) {
        ElemType type = struct.type();
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in single mode", e, type);
        }

        InsertTask<GE> task = new SingleInsertTask<>(this.context, struct,
                                                     batch);
        CompletableFuture.runAsync(task, this.singleService)
                         .whenComplete((r, e) -> this.singleSemaphore.release());
    }
}
