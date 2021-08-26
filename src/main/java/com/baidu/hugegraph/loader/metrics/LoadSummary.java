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

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.LoadMapping;
import com.baidu.hugegraph.util.InsertionOrderUtil;

public final class LoadSummary {
    public final class LoadRater {
        private final LongAdder count;
        private final AtomicLong cost;
        private final RangesTimer timer;

        private final AtomicLong prevCount;
        private final AtomicLong prevCost;

        public LoadRater() {
            this.timer = new RangesTimer(Constants.TIME_RANGE_CAPACITY);

            this.count = new LongAdder();
            this.cost = new AtomicLong();
            this.prevCount = new AtomicLong();
            this.prevCost = new AtomicLong();
        }

        public void addCount(long value) {
            this.count.add(value);
        }

        public void addTimeRange(long start, long end) {
            this.timer.addTimeRange(start, end);
        }

        public void calculateTotalTime() {
            this.cost.set(this.timer.totalTime());
        }

        public long getTime() {
            return this.cost.get();
        }

        public long getCount() {
            return this.count.longValue();
        }

        public long avgRate() {
            this.calculateTotalTime();

            long totalTime = this.cost.get();
            if (totalTime == 0) {
                return -1;
            }
            return this.count.longValue() * 1000 / totalTime;
        }

        public long curRate() {
            long period = this.cost.get() -
                    this.prevCost.getAndSet(this.cost.get());
            long count = this.count.longValue() -
                    this.prevCount.getAndSet(this.count.longValue());
            if (period == 0)
                return 0;

            return count * 1000 / period;
        }
    }

    private final LoadRater vertexRater = new LoadRater();
    private final LoadRater edgeRater = new LoadRater();
    private final StopWatch totalTimer;
    private final AtomicLong loadTime;
    private final RangesTimer loadRangesTimer;
    // Every input struct has a metric
    private final Map<String, LoadMetrics> inputMetricsMap;

    private final AtomicLong taskQueueLen = new AtomicLong(0);
    private final AtomicLong taskQueueLenSum = new AtomicLong(0);
    private final AtomicLong taskQueueLenCount = new AtomicLong(0);

    public LoadSummary() {
        this.totalTimer = new StopWatch();
        this.loadTime = new AtomicLong();
        this.loadRangesTimer = new RangesTimer(Constants.TIME_RANGE_CAPACITY);
        this.inputMetricsMap = InsertionOrderUtil.newMap();
    }

    public void increaseTaskQueueLen() {
        this.taskQueueLen.incrementAndGet();
    }

    public void decreaseTaskQueueLen() {
        taskQueueLenSum.addAndGet(this.taskQueueLen.decrementAndGet());
        taskQueueLenCount.incrementAndGet();
    }

    public long getTaskQueueLen() {
        return this.taskQueueLen.get();
    }

    public long getAvgTaskQueueLen() {
        long count = taskQueueLenCount.getAndSet(0);
        long sum = this.taskQueueLenSum.getAndSet(0);
        if (count > 0) {
            return sum / count;
        }
        return 0;
    }

    public void initMetrics(LoadMapping mapping) {
        for (InputStruct struct : mapping.structs()) {
            this.inputMetricsMap.put(struct.id(), new LoadMetrics(struct));
        }
    }

    public Map<String, LoadMetrics> inputMetricsMap() {
        return this.inputMetricsMap;
    }

    public LoadMetrics metrics(InputStruct struct) {
        return this.inputMetricsMap.get(struct.id());
    }

    public LoadRater vertex() {
        return this.vertexRater;
    }

    public LoadRater edge() {
        return this.edgeRater;
    }

    public void plusLoaded(ElemType type, int count) {
        if (type.isVertex()) {
            this.vertexRater.addCount(count);
        } else {
            this.edgeRater.addCount(count);
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
        if (type.isVertex()) {
            this.vertexRater.addTimeRange(start, end);
        } else {
            this.edgeRater.addTimeRange(start, end);
        }
        this.loadRangesTimer.addTimeRange(start, end);
    }

    public void calculateTotalTime() {
        this.vertexRater.calculateTotalTime();
        this.edgeRater.calculateTotalTime();

        loadTime.set(this.loadRangesTimer.totalTime());
    }

    public long totalTime() {
        return this.totalTimer.getTime();
    }

    public long loadTime() {
        return this.loadTime.longValue();
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
}
