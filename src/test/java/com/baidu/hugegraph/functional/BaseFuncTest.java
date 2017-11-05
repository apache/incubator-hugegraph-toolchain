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

package com.baidu.hugegraph.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.baidu.hugegraph.client.BaseClientTest;
import com.baidu.hugegraph.util.TimeUtil;

public class BaseFuncTest extends BaseClientTest {

    @BeforeClass
    public static void init() {
        BaseClientTest.init();
        BaseFuncTest.clearData();
    }

    @AfterClass
    public static void clear() throws Exception {
        BaseFuncTest.clearData();
        BaseClientTest.clear();
    }

    protected static void clearData() {
        // Clear edge
        graph().listEdges().forEach(e -> graph().removeEdge(e.id()));
        // Clear vertex
        graph().listVertices().forEach(v -> graph().removeVertex(v.id()));

        // Clear schema
        schema().getIndexLabels().forEach(il -> {
            schema().removeIndexLabel(il.name());
        });
        schema().getEdgeLabels().forEach(el -> {
            schema().removeEdgeLabel(el.name());
        });
        schema().getVertexLabels().forEach(vl -> {
            schema().removeVertexLabel(vl.name());
        });
        schema().getPropertyKeys().forEach(pk -> {
            schema().removePropertyKey(pk.name());
        });
    }

    protected static final void runWithThreads(int threads, Runnable task) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(task));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static final void waitTillNext(long seconds) {
        TimeUtil.tillNextMillis(TimeUtil.timeGen() + seconds * 1000);
    }
}
