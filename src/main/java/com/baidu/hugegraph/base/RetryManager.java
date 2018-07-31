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

package com.baidu.hugegraph.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.ImmutableMap;

public class RetryManager extends ToolManager {

    protected int retry = 0;

    protected AtomicLong propertyKeyCounter = new AtomicLong(0);
    protected AtomicLong vertexLabelCounter = new AtomicLong(0);
    protected AtomicLong edgeLabelCounter = new AtomicLong(0);
    protected AtomicLong indexLabelCounter = new AtomicLong(0);
    protected AtomicLong vertexCounter = new AtomicLong(0);
    protected AtomicLong edgeCounter = new AtomicLong(0);

    private long startTime = 0L;

    private static int threadsNum = Math.min(10,
            Math.max(4, Runtime.getRuntime().availableProcessors() / 2));
    private ExecutorService pool =
            Executors.newFixedThreadPool(threadsNum);
    private List<Future<?>> futures = new ArrayList<>();

    protected static final long SPLIT_SIZE = 1024 * 1024L;
    protected static final int LBUF_SIZE = 1024;

    public RetryManager(String url, String graph, String type) {
        super(url, graph, type);
    }

    public RetryManager(String url, String graph,
                        String username, String password, String type) {
        super(url, graph, username, password, type);
    }

    public <R> R retry(Supplier<R> supplier, String description) {
        int retries = 0;
        R r = null;
        do {
            try {
                r = supplier.get();
            } catch (Exception e) {
                if (retries == this.retry) {
                    throw new ClientException(
                              "Exception occurred while %s",
                              e, description);
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
        for (Future<?> future : this.futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        this.futures.clear();
    }

    public void shutdown(String taskType) {
        this.pool.shutdown();
        try {
            this.pool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new ClientException(
                      "Exception appears in %s threads", e, taskType);
        }
    }

    protected void printSummary() {
        this.printSummary(this.type());
    }

    protected void printSummary(String type) {
        Map<String, Long> summary = ImmutableMap.<String, Long>builder()
                .put("property key number", this.propertyKeyCounter.longValue())
                .put("vertex label number", this.vertexLabelCounter.longValue())
                .put("edge label number", this.edgeLabelCounter.longValue())
                .put("index label number", this.indexLabelCounter.longValue())
                .put("vertex number", this.vertexCounter.longValue())
                .put("edge number", this.edgeCounter.longValue()).build();
        Printer.printMap(type + " summary", summary);

        Printer.printKV("cost time(s)", this.elapseSeconds());
    }

    public int retry() {
        return this.retry;
    }

    public void retry(int retry) {
        this.retry = retry;
    }

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public long elapseSeconds() {
        E.checkState(this.startTime != 0,
                     "Must call startTimer() to set start time, " +
                     "before call elapse()");
        return (System.currentTimeMillis() - this.startTime) / 1000;
    }

    public static int threadsNum() {
        return threadsNum;
    }
}
