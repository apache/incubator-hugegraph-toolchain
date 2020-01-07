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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import com.baidu.hugegraph.loader.mapping.ElementMapping;

public final class LoadMetrics {

    private long readSuccess;
    private long readFailure;
    // The key is vertexlabel or edgelabel
    private final Map<String, MetricsCounter> vertexCounters;
    private final Map<String, MetricsCounter> edgeCounters;

    public LoadMetrics() {
        this(0L, 0L, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public LoadMetrics(long readSuccess, long readFailure,
                       Map<String, MetricsCounter> vertexCounters,
                       Map<String, MetricsCounter> edgeCounters) {
        this.readSuccess = readSuccess;
        this.readFailure = readFailure;
        this.vertexCounters = vertexCounters;
        this.edgeCounters = edgeCounters;
    }

    public long readSuccess() {
        return this.readSuccess;
    }

    public void readSuccess(long count) {
        this.readSuccess = count;
    }

    public void increaseReadSuccess() {
        this.readSuccess++;
    }

    public long readFailure() {
        return this.readFailure;
    }

    public void readFailure(long count) {
        this.readFailure = count;
    }

    public void increaseReadFailure() {
        this.readFailure++;
    }

    public long parseSuccess(ElementMapping mapping) {
        return this.getCounter(mapping).parseSuccess.longValue();
    }

    public void plusParseSuccess(ElementMapping mapping, long count) {
        this.getCounter(mapping).parseSuccess.add(count);
    }

    public long parseFailure(ElementMapping mapping) {
        return this.getCounter(mapping).parseFailure.longValue();
    }

    public void increaseParseFailure(ElementMapping mapping) {
        this.getCounter(mapping).parseFailure.increment();
    }

    public long insertSuccess(ElementMapping mapping) {
        return this.getCounter(mapping).insertSuccess.longValue();
    }

    public void plusInsertSuccess(ElementMapping mapping, long count) {
        this.getCounter(mapping).insertSuccess.add(count);
    }

    public long insertFailure(ElementMapping mapping) {
        return this.getCounter(mapping).insertFailure.longValue();
    }

    public void increaseInsertFailure(ElementMapping mapping) {
        this.getCounter(mapping).insertFailure.increment();
    }

    public Map<String, MetricsCounter> vertexCounters() {
        return this.vertexCounters;
    }

    public Map<String, MetricsCounter> edgeCounters() {
        return this.edgeCounters;
    }

    public long totalParseFailures() {
        long total = 0L;
        for (MetricsCounter counter : this.vertexCounters.values()) {
            total += counter.parseFailure.longValue();
        }
        for (MetricsCounter counter : this.edgeCounters.values()) {
            total += counter.parseFailure.longValue();
        }
        return total;
    }

    public long totalInsertFailures() {
        long total = 0L;
        for (MetricsCounter counter : this.vertexCounters.values()) {
            total += counter.insertFailure.longValue();
        }
        for (MetricsCounter counter : this.edgeCounters.values()) {
            total += counter.insertFailure.longValue();
        }
        return total;
    }

    private MetricsCounter getCounter(ElementMapping mapping) {
        Map<String, MetricsCounter> counters;
        if (mapping.type().isVertex()) {
            counters = this.vertexCounters;
        } else {
            counters = this.edgeCounters;
        }
        // TODO: does it thread safe
        return counters.computeIfAbsent(mapping.label(),
                                        k -> new MetricsCounter());
    }

    public static class MetricsCounter {

        private final LongAdder parseSuccess;
        private final LongAdder parseFailure;
        private final LongAdder insertSuccess;
        private final LongAdder insertFailure;

        public MetricsCounter() {
            this.parseSuccess = new LongAdder();
            this.parseFailure = new LongAdder();
            this.insertSuccess = new LongAdder();
            this.insertFailure = new LongAdder();
        }

        public long parseSuccess() {
            return this.parseSuccess.longValue();
        }

        public long parseFailure() {
            return this.parseFailure.longValue();
        }

        public long insertSuccess() {
            return this.insertSuccess.longValue();
        }

        public long insertFailure() {
            return this.insertFailure.longValue();
        }

        public void accumulate(MetricsCounter counter) {
            this.parseSuccess.add(counter.parseSuccess.longValue());
            this.parseFailure.add(counter.parseFailure.longValue());
            this.insertSuccess.add(counter.insertSuccess.longValue());
            this.insertFailure.add(counter.insertFailure.longValue());
        }
    }
}
