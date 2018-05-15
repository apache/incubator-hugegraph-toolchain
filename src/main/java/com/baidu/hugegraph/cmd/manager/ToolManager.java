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

package com.baidu.hugegraph.cmd.manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.baidu.hugegraph.rest.ClientException;

public class ToolManager {

    private static int threadsNum =
            Math.max(4, Runtime.getRuntime().availableProcessors() / 2);
    private static ExecutorService pool =
            Executors.newFixedThreadPool(threadsNum);

    protected static final long SPLIT_SIZE = 64 * 1024 * 1024;
    protected static final int LBUF_SIZE = 1024;

    protected final ToolClient client;
    private final String type;

    public ToolManager(String url, String graph, String type) {
        this.client = new ToolClient(url, graph);
        this.type = type;
    }

    public ToolManager(String url, String graph,
                       String username, String password, String type) {
        this.client = new ToolClient(url, graph, username, password);
        this.type = type;
    }

    protected String type() {
        return this.type;
    }

    public static void shutdown(String taskType) {
        pool.shutdown();
        try {
            pool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new ClientException(
                      "Exception appears in %s threads", e, taskType);
        }
    }

    public static void submit(Runnable task) {
        pool.submit(task);
    }

    public static int threadsNum() {
        return threadsNum;
    }
}
