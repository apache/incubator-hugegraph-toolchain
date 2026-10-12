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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.loader.util.Printer;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.exception.InsertException;
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
    private boolean accepting = true;
    private int submissions;

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
        this.waitForTasks(this.batchSemaphore, this.batchSemaphoreNum(), "batch-mode");
        this.waitForTasks(this.singleSemaphore, this.singleSemaphoreNum(), "single-mode");
        LOG.info("All the {} finished", tasksName);
        this.context.throwIfFailed();
    }

    private void waitForTasks(Semaphore semaphore, int permits, String mode) {
        try {
            semaphore.acquire(permits);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.context.failLoading(e);
            throw new LoadException("Interrupted while waiting %s tasks", e, mode);
        }
        semaphore.release(permits);
    }

    public void shutdown() {
        synchronized (this) {
            this.accepting = false;
        }
        boolean interrupted = Thread.interrupted();
        if (interrupted) {
            this.context.failLoading(new InterruptedException("Interrupted while shutting down insert tasks"));
        }
        try {
            interrupted |= this.shutdownExecutor(this.batchService, "batch-mode");
            interrupted |= this.shutdownExecutor(this.singleService, "single-mode");
            if (!this.batchService.isTerminated() || !this.singleService.isTerminated()) {
                LoadException failure = new LoadException("Insert tasks did not terminate within shutdown timeout");
                this.context.failLoading(failure);
                throw failure;
            }
            this.waitForHandlers(this.batchSemaphore, this.batchSemaphoreNum());
            this.waitForHandlers(this.singleSemaphore, this.singleSemaphoreNum());
            this.waitForSubmissions();
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void waitForSubmissions() {
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(this.options.shutdownTimeout);
        try {
            while (this.submissions != 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    LoadException failure = new LoadException("Batch submitters did not finish within shutdown timeout");
                    this.context.failLoading(failure);
                    throw failure;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(this, remaining);
                } catch (InterruptedException e) {
                    interrupted = true;
                    this.context.failLoading(e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void waitForHandlers(Semaphore semaphore, int permits) {
        boolean interrupted = false;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(this.options.shutdownTimeout);
        try {
            while (true) {
                try {
                    if (semaphore.tryAcquire(permits, deadline - System.nanoTime(), TimeUnit.NANOSECONDS)) {
                        semaphore.release(permits);
                        return;
                    }
                    LoadException failure = new LoadException("Insert handlers did not finish within shutdown timeout");
                    this.context.failLoading(failure);
                    throw failure;
                } catch (InterruptedException e) {
                    interrupted = true;
                    this.context.failLoading(e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean shutdownExecutor(ExecutorService service, String mode) {
        boolean interrupted = false;
        service.shutdown();
        // Bound both the graceful drain and the wait after cancellation.
        for (int attempt = 0; attempt < 2 && !service.isTerminated(); attempt++) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(this.options.shutdownTimeout);
            while (!service.isTerminated()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    break;
                }
                try {
                    service.awaitTermination(remaining, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                    this.context.failLoading(e);
                }
            }
            if (!service.isTerminated()) {
                LOG.error("The unfinished {} tasks will be cancelled", mode);
                this.context.failLoading(new LoadException("Cancelling unfinished %s tasks", mode));
                for (Runnable queued : service.shutdownNow()) {
                    if (queued instanceof InsertFuture) {
                        ((InsertFuture) queued).cancel(false);
                    }
                }
            }
        }
        return interrupted;
    }

    public void submitBatch(InputStruct struct, ElementMapping mapping,
                            List<Record> batch) {
        synchronized (this) {
            if (!this.accepting) {
                LoadException failure = new LoadException("Cannot submit batch after insert tasks shutdown");
                this.context.failLoading(failure);
                throw failure;
            }
            this.submissions++;
        }
        try {
            this.submitBatchInternal(struct, mapping, batch);
        } finally {
            synchronized (this) {
                this.submissions--;
                this.notifyAll();
            }
        }
    }

    private void submitBatchInternal(InputStruct struct, ElementMapping mapping,
                                     List<Record> batch) {
        long start = System.currentTimeMillis();
        this.checkAcceptingBatches();
        try {
            this.batchSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.context.failLoading(e);
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in batch mode", e, mapping.type());
        }
        try {
            this.checkAcceptingBatches();
        } catch (RuntimeException e) {
            this.batchSemaphore.release();
            throw e;
        }
        if (this.context.stopped()) {
            try {
                if (!this.context.stoppedAtReadLimit()) {
                    this.recordFailedBatch(struct, mapping, batch,
                                           new LoadException("Loading stopped before batch submission"));
                }
            } finally {
                this.batchSemaphore.release();
            }
            return;
        }
        LoadSummary summary = this.context.summary();
        summary.metrics(struct).plusFlighting(batch.size());
        InsertFuture task = new InsertFuture(new BatchInsertTask(this.context, struct, mapping, batch));
        task.whenComplete((r, e) -> {
            boolean singleSubmitted = false;
            try {
                if (e != null) {
                    if (e instanceof CancellationException) {
                        this.context.failLoading(e);
                    } else if (this.options.batchFailureFallback) {
                        LOG.warn("Batch insert {} error, try single insert", mapping.type(), e);
                        singleSubmitted = true;
                        this.submitInSingle(struct, mapping, batch);
                    } else {
                        this.context.occurredError();
                        this.context.stopLoading();
                        this.recordFailedBatch(struct, mapping, batch, e);
                        LOG.error("Batch insert {} error, interrupting import", mapping.type(), e);
                        Printer.printError("Batch insert %s failed, stop loading. " +
                                           "Please check the logs", mapping.type().string());
                    }
                }
            } catch (Throwable failure) {
                this.context.failLoading(failure);
            } finally {
                try {
                    if (!singleSubmitted) {
                        summary.metrics(struct).minusFlighting(batch.size());
                    }
                    this.context.summary().addTimeRange(mapping.type(), start,
                                                        System.currentTimeMillis());
                } catch (Throwable failure) {
                    this.context.failLoading(failure);
                } finally {
                    this.batchSemaphore.release();
                }
            }
        });
        this.execute(task, this.batchService);
    }

    private void checkAcceptingBatches() {
        if (this.batchService.isShutdown() || this.context.closed()) {
            LoadException failure = new LoadException("Cannot submit batch after insert tasks shutdown");
            this.context.failLoading(failure);
            throw failure;
        }
    }

    private void recordFailedBatch(InputStruct struct, ElementMapping mapping,
                                   List<Record> batch, Throwable failure) {
        this.context.occurredError();
        try {
            for (Record record : batch) {
                this.context.summary().metrics(struct).increaseInsertFailure(mapping);
                this.context.failureLogger(struct).write(new InsertException(record.rawLine(), failure));
            }
        } catch (RuntimeException e) {
            this.context.failLoading(e);
            throw e;
        }
    }

    private void submitInSingle(InputStruct struct, ElementMapping mapping,
                                List<Record> batch) {
        long start = System.currentTimeMillis();
        try {
            this.singleSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.context.summary().metrics(struct).minusFlighting(batch.size());
            throw new LoadException("Interrupted while waiting to submit %s " +
                                    "batch in single mode", e, mapping.type());
        }
        LoadSummary summary = this.context.summary();
        InsertFuture task = new InsertFuture(new SingleInsertTask(this.context, struct, mapping, batch));
        task.whenComplete((r, e) -> {
            try {
                if (e != null) {
                    this.context.failLoading(e);
                }
            } finally {
                try {
                    summary.metrics(struct).minusFlighting(batch.size());
                    this.context.summary().addTimeRange(mapping.type(), start,
                                                        System.currentTimeMillis());
                } catch (Throwable failure) {
                    this.context.failLoading(failure);
                } finally {
                    this.singleSemaphore.release();
                }
            }
        });
        this.execute(task, this.singleService);
    }

    private void execute(InsertFuture task, ExecutorService service) {
        try {
            service.execute(task);
        } catch (RuntimeException e) {
            this.context.failLoading(e);
            task.cancel(false);
            throw e;
        }
    }

    private static final class InsertFuture extends CompletableFuture<Void> implements Runnable {
        private final InsertTask task;

        private InsertFuture(InsertTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                this.task.run();
                this.complete(null);
            } catch (Throwable e) {
                this.completeExceptionally(e);
            }
        }
    }
}
