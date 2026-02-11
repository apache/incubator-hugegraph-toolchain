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

import static org.apache.hugegraph.loader.constant.Constants.BATCH_WORKER_PREFIX;
import static org.apache.hugegraph.loader.constant.Constants.SINGLE_WORKER_PREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.hugegraph.util.ExecutorUtil;
import org.apache.hugegraph.util.Log;
import org.parboiled.common.Preconditions;
import org.slf4j.Logger;

public class GlobalExecutorManager {

    private static final Logger LOG = Log.logger(GlobalExecutorManager.class);

    private static final int CPUS = Runtime.getRuntime().availableProcessors();
    private static int batchThreadCount = CPUS;
    private static int singleThreadCount = CPUS;

    private static final Map<String, ExecutorService> EXECUTORS = new HashMap();

    public static ExecutorService getExecutor(int parallel, String name) {
        Preconditions.checkArgNotNull(name, "executor name");
        Preconditions.checkArgument(parallel > 0,
                                    "executor pool size must > 0");

        synchronized (EXECUTORS) {
            if (!EXECUTORS.containsKey(name)) {
                String patternName = name + "-%d";
                ExecutorService executor =
                        ExecutorUtil.newFixedThreadPool(parallel, patternName);
                EXECUTORS.put(name, executor);
            }
            return EXECUTORS.get(name);
        }
    }

    public static void shutdown(int timeout) {
        EXECUTORS.forEach((name, executor) -> {
            if (executor.isShutdown()) {
                return;
            }

            try {
                executor.shutdown();
                executor.awaitTermination(timeout, TimeUnit.SECONDS);
                LOG.info(String.format("The %s executor shutdown", name));
            } catch (InterruptedException e) {
                LOG.error("The batch-mode tasks are interrupted", e);
            } finally {
                if (!executor.isTerminated()) {
                    LOG.error(String.format("The unfinished tasks will be " +
                                            "cancelled in executor (%s)", name));
                }
                executor.shutdownNow();
            }
        });
    }

    public static void setBatchThreadCount(int count) {
        batchThreadCount = count;
    }

    public static void setSingleThreadCount(int count) {
        singleThreadCount = count;
    }

    public static synchronized ExecutorService getBatchInsertExecutor() {
        return GlobalExecutorManager.getExecutor(batchThreadCount,
                                                 BATCH_WORKER_PREFIX);
    }

    public static synchronized ExecutorService getSingleInsertExecutor() {

        return GlobalExecutorManager.getExecutor(singleThreadCount,
                                                 SINGLE_WORKER_PREFIX);
    }
}
