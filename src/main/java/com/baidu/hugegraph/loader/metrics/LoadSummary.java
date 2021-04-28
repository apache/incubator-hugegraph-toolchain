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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.lang3.time.StopWatch;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.util.InsertionOrderUtil;

public final class LoadSummary {

    private final LongAdder vertexLoaded;
    private final LongAdder edgeLoaded;
    private final StopWatch totalTimer;
    private final AtomicLong vertexTime;
    private final AtomicLong edgeTime;
    private final RangesTimer vertexRangesTimer;
    private final RangesTimer edgeRangesTimer;
    // Every input struct has a metric
    private final Map<String, LoadMetrics> inputMetricsMap;

    public LoadSummary() {
        this.vertexLoaded = new LongAdder();
        this.edgeLoaded = new LongAdder();
        this.totalTimer = new StopWatch();
        this.vertexTime = new AtomicLong();
        this.edgeTime = new AtomicLong();
        this.vertexRangesTimer = new RangesTimer(10000);
        this.edgeRangesTimer = new RangesTimer(10000);
        this.inputMetricsMap = InsertionOrderUtil.newMap();
    }

    public Map<String, LoadMetrics> inputMetricsMap() {
        return this.inputMetricsMap;
    }

    public LoadMetrics metrics(InputStruct struct) {
        return this.inputMetricsMap.computeIfAbsent(struct.id(), k -> {
            return new LoadMetrics(struct);
        });
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
        RangesTimer timer = type.isVertex() ? this.vertexRangesTimer :
                                              this.edgeRangesTimer;
        timer.addTimeRange(start, end);
    }

    public void calculateTotalTime(ElemType type) {
        RangesTimer timer = type.isVertex() ? this.vertexRangesTimer :
                                              this.edgeRangesTimer;
        AtomicLong elemTime = type.isVertex() ? this.vertexTime : this.edgeTime;
        elemTime.set(timer.totalTime());
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
        // Ensure vetex time and edge time has been set
        this.calculateTotalTime(type);

        boolean isVertex = type.isVertex();
        long totalTime = isVertex ? this.vertexTime() : this.edgeTime();
        if (totalTime == 0) {
            return -1;
        }
        long success = isVertex ? this.vertexLoaded() : this.edgeLoaded();
        return success * 1000 / totalTime;
    }
}
