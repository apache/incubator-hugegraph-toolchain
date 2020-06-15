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

package com.baidu.hugegraph.loader.metrics;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.lang3.time.StopWatch;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.util.InsertionOrderUtil;

public final class LoadSummary {

    private final LongAdder vertexLoaded;
    private final LongAdder edgeLoaded;
    private final StopWatch totalTimer;
    // Every input struct has a metric
    private final Map<String, LoadMetrics> inputMetricsMap;

    public LoadSummary() {
        this.vertexLoaded = new LongAdder();
        this.edgeLoaded = new LongAdder();
        this.totalTimer = new StopWatch();
        this.inputMetricsMap = InsertionOrderUtil.newMap();
    }

    public Map<String, LoadMetrics> inputMetricsMap() {
        return this.inputMetricsMap;
    }

    public LoadMetrics metrics(InputStruct struct) {
        String id = struct.id();
        return this.inputMetricsMap.computeIfAbsent(id, k -> new LoadMetrics());
    }

    public long vertexLoaded() {
        return this.vertexLoaded.longValue();
    }

    public long edgeLoaded() {
        return this.edgeLoaded.longValue();
    }

    public void plusLoaded(ElemType type, int count) {
        if (type.isVertex()) {
            this.vertexLoaded.add(count);
        } else {
            this.edgeLoaded.add(count);
        }
    }

    public long totalReadFailures() {
        return this.inputMetricsMap.values().stream()
                                   .map(LoadMetrics::readFailure)
                                   .reduce(0L, Long::sum);
    }

    public long totalParseFailures() {
        return this.inputMetricsMap.values().stream()
                                   .map(LoadMetrics::totalParseFailures)
                                   .reduce(0L, Long::sum);
    }

    public long totalInsertFailures() {
        return this.inputMetricsMap.values().stream()
                                   .map(LoadMetrics::totalInsertFailures)
                                   .reduce(0L, Long::sum);
    }

    public long totalTime() {
        return this.totalTimer.getTime();
    }

    public void startTimer() {
        if (!this.totalTimer.isStarted()) {
            this.totalTimer.start();
        }
    }

    public void stopTimer() {
        if (!this.totalTimer.isStopped()) {
            this.totalTimer.stop();
        }
    }

    public long loadRate(ElemType type) {
        long totalTime = this.totalTime();
        if (totalTime == 0) {
            return -1;
        }
        long success = type.isVertex() ? this.vertexLoaded.longValue() :
                       this.edgeLoaded.longValue();
        return success * 1000 / totalTime;
    }
}
