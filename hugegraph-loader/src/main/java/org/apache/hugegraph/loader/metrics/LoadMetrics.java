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

package org.apache.hugegraph.loader.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;

public final class LoadMetrics {

    private final InputStruct struct;
    private long readSuccess;
    private long readFailure;
    private boolean inFlight;
    // It has been parsed and is in the loading state
    private final LongAdder flightingNums;
    // The key is vertexlabel or edgelabel
    private final Map<String, Metrics> vertexMetrics;
    private final Map<String, Metrics> edgeMetrics;

    public LoadMetrics(InputStruct struct) {
        this.struct = struct;
        this.readSuccess = 0L;
        this.readFailure = 0L;
        this.inFlight = false;
        this.flightingNums = new LongAdder();

        this.vertexMetrics = new HashMap<>();
        this.edgeMetrics = new HashMap<>();
        for (VertexMapping mapping : struct.vertices()) {
            this.vertexMetrics.put(mapping.label(), new Metrics());
        }
        for (EdgeMapping mapping : struct.edges()) {
            this.edgeMetrics.put(mapping.label(), new Metrics());
        }
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

    public void plusReadSuccess(long count) {
        this.readSuccess += count;
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

    public void startInFlight() {
        this.inFlight = true;
    }

    public void stopInFlight() {
        this.inFlight = false;
    }

    public void plusFlighting(int num) {
        this.flightingNums.add(num);
    }

    public void minusFlighting(int num) {
        this.flightingNums.add(-num);
    }

    public long parseSuccess(ElementMapping mapping) {
        return this.metrics(mapping).parseSuccess.longValue();
    }

    public void plusParseSuccess(ElementMapping mapping, long count) {
        this.metrics(mapping).parseSuccess.add(count);
    }

    public long parseFailure(ElementMapping mapping) {
        return this.metrics(mapping).parseFailure.longValue();
    }

    public void increaseParseFailure(ElementMapping mapping) {
        this.metrics(mapping).parseFailure.increment();
    }

    public long insertSuccess(ElementMapping mapping) {
        return this.metrics(mapping).insertSuccess.longValue();
    }

    public void plusInsertSuccess(ElementMapping mapping, long count) {
        this.metrics(mapping).insertSuccess.add(count);
    }

    public long insertFailure(ElementMapping mapping) {
        return this.metrics(mapping).insertFailure.longValue();
    }

    public void increaseInsertFailure(ElementMapping mapping) {
        this.metrics(mapping).insertFailure.increment();
    }

    public Map<String, Metrics> vertexMetrics() {
        return this.vertexMetrics;
    }

    public Map<String, Metrics> edgeMetrics() {
        return this.edgeMetrics;
    }

    public long totalParseFailures() {
        long total = 0L;
        for (Metrics counter : this.vertexMetrics.values()) {
            total += counter.parseFailure.longValue();
        }
        for (Metrics counter : this.edgeMetrics.values()) {
            total += counter.parseFailure.longValue();
        }
        return total;
    }

    public long totalInsertFailures() {
        long total = 0L;
        for (Metrics counter : this.vertexMetrics.values()) {
            total += counter.insertFailure.longValue();
        }
        for (Metrics counter : this.edgeMetrics.values()) {
            total += counter.insertFailure.longValue();
        }
        return total;
    }

    private Metrics metrics(ElementMapping mapping) {
        if (mapping.type().isVertex()) {
            return this.vertexMetrics.get(mapping.label());
        } else {
            return this.edgeMetrics.get(mapping.label());
        }
    }

    public static class Metrics {

        private final LongAdder parseSuccess;
        private final LongAdder parseFailure;
        private final LongAdder insertSuccess;
        private final LongAdder insertFailure;

        public Metrics() {
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
    }
}
