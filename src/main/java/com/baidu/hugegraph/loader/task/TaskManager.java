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
import static com.baidu.hugegraph.loader.constant.Constants.PARSE_WORKER;
import static com.baidu.hugegraph.loader.constant.Constants.SINGLE_WORKER;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.util.ExecutorUtil;
import com.baidu.hugegraph.util.Log;

public final class TaskManager {

    private static final Logger LOG = Log.logger(TaskManager.class);

    private final LoadContext context;
    private final LoadOptions options;

    private final Semaphore parseSemaphore;
    private final Semaphore batchSemaphore;
    private final Semaphore singleSemaphore;
    private final ExecutorService parseService;
    private final ExecutorService batchService;
    private final ExecutorService singleService;

    private volatile boolean stopped;

    public TaskManager(LoadContext context) {
        this.context = context;
        this.options = context.options();
        this.parseSemaphore = new Semaphore(this.parseSemaphoreNum());
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
        this.parseService = ExecutorUtil.newFixedThreadPool(
                            this.options.parseThreads, PARSE_WORKER);
        this.batchService = ExecutorUtil.newFixedThreadPool(
                            this.options.batchInsertThreads, BATCH_WORKER);
        this.singleService = ExecutorUtil.newFixedThreadPool(
                             this.options.singleInsertThreads, SINGLE_WORKER);
        this.stopped = false;
    }

    private int parseSemaphoreNum() {
        return 1 + this.options.parseThreads;
    }

    private int batchSemaphoreNum() {
        return 1 + this.options.batchInsertThreads;
    }

    private int singleSemaphoreNum() {
        return 2 * this.options.singleInsertThreads;
    }

    public void waitFinished() {
        if (this.stopped) {
            return;
        }

        LOG.info("Waiting for the parse tasks finish");
        try {
            // Wait batch mode task finished
            this.parseSemaphore.acquire(this.parseSemaphoreNum());
            LOG.info("The parse tasks finished");
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting parse tasks");
        } finally {
            this.parseSemaphore.release(this.parseSemaphoreNum());
        }

        LOG.info("Waiting for the insert tasks finish");
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
        long timeout = this.options.shutdownTimeout;

        LOG.debug("Ready to shutdown parse tasks executor");
        try {
            this.parseService.shutdown();
            this.parseService.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("The parse tasks are interrupted");
        } finally {
            if (!this.parseService.isTerminated()) {
                LOG.error("The unfinished parse tasks will be cancelled");
            }
            this.parseService.shutdownNow();
        }

        LOG.debug("Ready to shutdown batch-mode tasks executor");
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

    public void submitParseTask(InputStruct struct, ElementMapping mapping,
                                ParseTaskBuilder.ParseTask task) {
        try {
            this.parseSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "parse task", e);
        }

        CompletableFuture.supplyAsync(task, this.parseService).exceptionally(e -> {
            LOG.error("Failed to execute parse task", e);
            return null;
        }).thenAccept(batch -> {
            if (CollectionUtils.isEmpty(batch) ||
                this.context.options().dryRun) {
                return;
            }
            this.submitBatch(struct, mapping, batch);
        }).whenComplete((r ,e) -> this.parseSemaphore.release());
    }

    private void submitBatch(InputStruct struct, ElementMapping mapping,
                             List<Record> batch) {
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in batch mode", e, mapping.type());
        }

        InsertTask task = new BatchInsertTask(this.context, struct,
                                              mapping, batch);
        CompletableFuture.runAsync(task, this.batchService).exceptionally(e -> {
            LOG.warn("Batch insert {} error, try single insert",
                     mapping.type(), e);
            this.submitInSingle(struct, mapping, batch);
            return null;
        }).whenComplete((r, e) -> {
            this.batchSemaphore.release();
        });
    }

    private void submitInSingle(InputStruct struct, ElementMapping mapping,
                                List<Record> batch) {
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in single mode", e, mapping.type());
        }

        InsertTask task = new SingleInsertTask(this.context, struct,
                                               mapping, batch);
        CompletableFuture.runAsync(task, this.singleService)
                         .whenComplete((r, e) -> this.singleSemaphore.release());
    }
}
