/*
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

package org.apache.hugegraph.loader.task;

import static org.apache.hugegraph.loader.constant.Constants.BATCH_WORKER;
import static org.apache.hugegraph.loader.constant.Constants.SINGLE_WORKER;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.util.ExecutorUtil;
import org.apache.hugegraph.util.Log;

public final class TaskManager {

    private static final Logger LOG = Log.logger(TaskManager.class);

    private final LoadContext context;
    private final LoadOptions options;

    private final Semaphore batchSemaphore;
    private final Semaphore singleSemaphore;
    private final ExecutorService batchService;
    private final ExecutorService singleService;

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
        this.batchService = ExecutorUtil.newFixedThreadPool(
                            this.options.batchInsertThreads, BATCH_WORKER);
        this.singleService = ExecutorUtil.newFixedThreadPool(
                             this.options.singleInsertThreads, SINGLE_WORKER);
    }

    private int batchSemaphoreNum() {
        return 1 + this.options.batchInsertThreads;
    }

    private int singleSemaphoreNum() {
        return 2 * this.options.singleInsertThreads;
    }

    public void waitFinished() {
        this.waitFinished("insert tasks");
    }

    public void waitFinished(String tasksName) {
        LOG.info("Waiting for the {} to finish", tasksName);
        try {
            // Wait batch mode task stopped
            this.batchSemaphore.acquire(this.batchSemaphoreNum());
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting batch-mode tasks");
        } finally {
            this.batchSemaphore.release(this.batchSemaphoreNum());
        }

        try {
            // Wait single mode task stopped
            this.singleSemaphore.acquire(this.singleSemaphoreNum());
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting single-mode tasks");
        } finally {
            this.singleSemaphore.release(this.singleSemaphoreNum());
        }
        LOG.info("All the {} finished", tasksName);
    }

    public void shutdown() {
        long timeout = this.options.shutdownTimeout;
        try {
            this.batchService.shutdown();
            this.batchService.awaitTermination(timeout, TimeUnit.SECONDS);
            LOG.info("The batch-mode tasks service executor shutdown");
        } catch (InterruptedException e) {
            LOG.error("The batch-mode tasks are interrupted");
        } finally {
            if (!this.batchService.isTerminated()) {
                LOG.error("The unfinished batch-mode tasks will be cancelled");
            }
            this.batchService.shutdownNow();
        }

        try {
            this.singleService.shutdown();
            this.singleService.awaitTermination(timeout, TimeUnit.SECONDS);
            LOG.info("The single-mode tasks service executor shutdown");
        } catch (InterruptedException e) {
            LOG.error("The single-mode tasks are interrupted");
        } finally {
            if (!this.singleService.isTerminated()) {
                LOG.error("The unfinished single-mode tasks will be cancelled");
            }
            this.singleService.shutdownNow();
        }
    }

    public void submitBatch(InputStruct struct, ElementMapping mapping,
                            List<Record> batch) {
        long start = System.currentTimeMillis();
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in batch mode", e, mapping.type());
        }
        LoadSummary summary = this.context.summary();
        summary.metrics(struct).plusFlighting(batch.size());

        InsertTask task = new BatchInsertTask(this.context, struct,
                                              mapping, batch);
        CompletableFuture.runAsync(task, this.batchService).whenComplete(
            (r, e) -> {
                if (e != null) {
                    LOG.warn("Batch insert {} error, try single insert",
                             mapping.type(), e);
                    // The time of single insert is counted separately
                    this.submitInSingle(struct, mapping, batch);
                } else {
                    summary.metrics(struct).minusFlighting(batch.size());
                }

                this.batchSemaphore.release();
                long end = System.currentTimeMillis();
                this.context.summary().addTimeRange(mapping.type(), start, end);
            });
    }

    private void submitInSingle(InputStruct struct, ElementMapping mapping,
                                List<Record> batch) {
        long start = System.currentTimeMillis();
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in single mode", e, mapping.type());
        }
        LoadSummary summary = this.context.summary();

        InsertTask task = new SingleInsertTask(this.context, struct,
                                               mapping, batch);
        CompletableFuture.runAsync(task, this.singleService).whenComplete(
            (r, e) -> {
                summary.metrics(struct).minusFlighting(batch.size());
                this.singleSemaphore.release();

                long end = System.currentTimeMillis();
                this.context.summary().addTimeRange(mapping.type(), start, end);
            });
    }
}
