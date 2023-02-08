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

package org.apache.hugegraph.base;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.hugegraph.exception.ToolsException;

public class RetryManager extends ToolManager {

    private int cpus = Runtime.getRuntime().availableProcessors();
    private int threadsNum = Math.min(10, Math.max(4, cpus / 2));
    private ExecutorService pool;
    private final Queue<Future<?>> futures = new ConcurrentLinkedQueue<>();
    private int retry = 0;

    public RetryManager(ToolClient.ConnectionInfo info, String type) {
        super(info, type);
    }

    public void initExecutors() {
        Printer.print("Init %s executors", this.threadsNum);
        this.pool = Executors.newFixedThreadPool(this.threadsNum);
    }

    public <R> R retry(Supplier<R> supplier, String description) {
        int retries = 0;
        R r = null;
        do {
            try {
                r = supplier.get();
            } catch (Exception e) {
                if (retries == this.retry) {
                    throw new ToolsException(
                              "Exception occurred while %s(after %s retries)",
                              e, description, this.retry);
                }
                // Ignore exception and retry
                continue;
            }
            break;
        } while (retries++ < this.retry);
        return r;
    }

    public void submit(Runnable task) {
        this.futures.add(this.pool.submit(task));
    }

    public void awaitTasks() {
        Future<?> future;
        while ((future = this.futures.poll()) != null) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown(String taskType) {
        if (this.pool == null) {
            return;
        }
        this.pool.shutdown();
        try {
            this.pool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new ToolsException(
                      "Exception appears in %s threads", e, taskType);
        }
    }

    public int retry() {
        return this.retry;
    }

    public void retry(int retry) {
        this.retry = retry;
    }

    public int threadsNum() {
        return this.threadsNum;
    }

    public void threadsNum(int threadsNum) {
        if (threadsNum > 0) {
            this.threadsNum = threadsNum;
        }
    }
}
