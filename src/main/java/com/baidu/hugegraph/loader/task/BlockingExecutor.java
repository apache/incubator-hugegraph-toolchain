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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

public class BlockingExecutor extends ThreadPoolExecutor {

    public BlockingExecutor(int poolSize, int queueSize, String threadName) {
        super(poolSize, poolSize, 0L, TimeUnit.SECONDS,
              blockingQueue(queueSize), threadFactory(threadName),
              rejectHandler());
    }

    private static BlockingQueue<Runnable> blockingQueue(int capacity) {
        return new LinkedBlockingQueue<>(capacity);
    }

    private static ThreadFactory threadFactory(String name) {
        return new BasicThreadFactory.Builder().namingPattern(name).build();
    }

    private static RejectedExecutionHandler rejectHandler() {
        return new BlockHandler();
    }

    private static class BlockHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                // Block until queue is not full
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // TODO: Record unfinished batch to file
                throw new RejectedExecutionException("Producer thread interrupted", e);
            }
        }
    }
}
