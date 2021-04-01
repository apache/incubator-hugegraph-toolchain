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

import java.util.Collection;
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
    private final LongAdder vertexTime;
    private final LongAdder edgeTime;
    private final FlowRangeTimer vertexFlowRangeTimer;
    private final FlowRangeTimer edgeFlowRangeTimer;
    // Every input struct has a metric
    private final Map<String, LoadMetrics> inputMetricsMap;

    public LoadSummary() {
        this.vertexLoaded = new LongAdder();
        this.edgeLoaded = new LongAdder();
        this.totalTimer = new StopWatch();
        this.vertexTime = new LongAdder();
        this.edgeTime = new LongAdder();
        this.vertexFlowRangeTimer = new FlowRangeTimer(10000);
        this.edgeFlowRangeTimer = new FlowRangeTimer(10000);
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

    public long totalReadLines() {
        Collection<LoadMetrics> metricsList = this.inputMetricsMap.values();
        long lines = 0L;
        for (LoadMetrics metrics : metricsList) {
            lines += metrics.readSuccess();
            lines += metrics.readFailure();
        }
        return lines;
    }

    public long totalReadSuccess() {
        return this.inputMetricsMap.values().stream()
                                   .map(LoadMetrics::readSuccess)
                                   .reduce(0L, Long::sum);
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

    public void addTimeRange(ElemType type, long start, long end) {
        FlowRangeTimer timer = type.isVertex() ? this.vertexFlowRangeTimer :
                                                 this.edgeFlowRangeTimer;
        timer.addTimeRange(start, end);
    }

    public void stopFlowRangeTimer(ElemType type) {
        FlowRangeTimer timer = type.isVertex() ? this.vertexFlowRangeTimer :
                                                 this.edgeFlowRangeTimer;
        LongAdder elemTime = type.isVertex() ? this.vertexTime : this.edgeTime;
        elemTime.add(timer.totalTime());
    }

    public long totalTime() {
        return this.totalTimer.getTime();
    }

    public long vertexTime() {
        return this.vertexTime.longValue();
    }

    public long edgeTime() {
        return this.edgeTime.longValue();
    }

    public void startTotalTimer() {
        if (!this.totalTimer.isStarted()) {
            this.totalTimer.start();
        }
    }

    public void stopTotalTimer() {
        if (!this.totalTimer.isStopped()) {
            this.totalTimer.stop();
        }
    }

    public long loadRate(ElemType type) {
        boolean isVertex = type.isVertex();
        long totalTime = isVertex ? this.vertexTime() : this.edgeTime();
        if (totalTime == 0) {
            return -1;
        }
        long success = isVertex ? this.vertexLoaded() : this.edgeLoaded();
        return success * 1000 / totalTime;
    }
}
