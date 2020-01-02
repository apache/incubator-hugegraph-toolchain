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
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.util.InsertionOrderUtil;

public final class LoadSummary {

    private final Map<String, LoadMetrics> vertexMetricsMap;
    private final Map<String, LoadMetrics> edgeMetricsMap;

    private final LongAdder vertexParsed;
    private final LongAdder vertexLoaded;
    private final LongAdder edgeParsed;
    private final LongAdder edgeLoaded;
    private final StopWatch totalTimer;

    public LoadSummary() {
        this.vertexMetricsMap = InsertionOrderUtil.newMap();
        this.edgeMetricsMap = InsertionOrderUtil.newMap();
        this.vertexParsed = new LongAdder();
        this.vertexLoaded = new LongAdder();
        this.edgeParsed = new LongAdder();
        this.edgeLoaded = new LongAdder();
        this.totalTimer = new StopWatch();
    }

    public LoadMetrics metrics(ElementMapping mapping) {
        return this.metrics(mapping.type(), mapping.label());
    }

    public LoadMetrics metrics(ElemType type, String label) {
        Map<String, LoadMetrics> metricsMap;
        if (type.isVertex()) {
            metricsMap = this.vertexMetricsMap;
        } else {
            assert type.isEdge();
            metricsMap = this.edgeMetricsMap;
        }
        return metricsMap.computeIfAbsent(label, k -> new LoadMetrics());
    }

    public Map<String, LoadMetrics> vertexMetrics() {
        return this.vertexMetricsMap;
    }

    public Map<String, LoadMetrics> edgeMetrics() {
        return this.edgeMetricsMap;
    }

    public long vertexParsed() {
        return this.vertexParsed.longValue();
    }

    public long edgeParsed() {
        return this.edgeParsed.longValue();
    }

    public void plusParsed(ElemType type, int count) {
        if (type.isVertex()) {
            this.vertexParsed.add(count);
        } else {
            this.edgeParsed.add(count);
        }
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

    public long totalParseFailures() {
        long total = 0L;
        for (LoadMetrics metrics : this.vertexMetricsMap.values()) {
            total += metrics.parseFailure();
        }
        for (LoadMetrics metrics : this.edgeMetricsMap.values()) {
            total += metrics.parseFailure();
        }
        return total;
    }

    public long totalInsertFailures() {
        long total = 0L;
        for (LoadMetrics metrics : this.vertexMetricsMap.values()) {
            total += metrics.loadFailure();
        }
        for (LoadMetrics metrics : this.edgeMetricsMap.values()) {
            total += metrics.loadFailure();
        }
        return total;
    }

    public long totalTime() {
        return this.totalTimer.getTime();
    }

    public void startTimer() {
        this.totalTimer.start();
    }

    public void stopTimer() {
        this.totalTimer.stop();
    }

    public long parseRate(ElemType type) {
        long totalTime = this.totalTime();
        if (totalTime == 0) {
            return -1;
        }
        long success = type.isVertex() ? this.vertexParsed.longValue() :
                       this.edgeParsed.longValue();
        return success * 1000 / totalTime;
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

    public LoadReport buildReport(ElemType type) {
        if (type.isVertex()) {
            return this.accumulateMetrics(this.vertexMetricsMap.values());
        } else {
            assert type.isEdge();
            return this.accumulateMetrics(this.edgeMetricsMap.values());
        }
    }

    private LoadReport accumulateMetrics(Collection<LoadMetrics> metricsList) {
        long parseSuccess = 0L, parseFailure = 0L;
        long loadSuccess = 0L, loadFailure = 0L;
        for (LoadMetrics metrics : metricsList) {
            parseSuccess += metrics.parseSuccess();
            parseFailure += metrics.parseFailure();
            loadSuccess += metrics.loadSuccess();
            loadFailure += metrics.loadFailure();
        }
        return new LoadReport(this.totalTime(), parseSuccess, parseFailure,
                              loadSuccess, loadFailure);
    }
}
