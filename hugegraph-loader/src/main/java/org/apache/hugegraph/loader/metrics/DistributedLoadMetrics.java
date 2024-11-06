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

import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.apache.spark.SparkContext;
import org.apache.spark.util.LongAccumulator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public final class DistributedLoadMetrics implements Serializable {
    private final InputStruct struct;

    private long readSuccess;

    private long readFailure;

    private boolean inFlight;

    private final LongAdder flightingNums;

    private final Map<String, DistributedMetrics> vertexMetrics;

    private final Map<String, DistributedMetrics> edgeMetrics;

    public DistributedLoadMetrics(InputStruct struct, SparkContext sc) {
        this.struct = struct;
        this.readSuccess = 0L;
        this.readFailure = 0L;
        this.inFlight = false;
        this.flightingNums = new LongAdder();
        this.vertexMetrics = new HashMap<>();
        this.edgeMetrics = new HashMap<>();
        for (VertexMapping mapping : struct.vertices()) {
            DistributedMetrics distributedMetrics = new DistributedMetrics();
            distributedMetrics.init(sc, mapping.toString());
            this.vertexMetrics.put(mapping.label(), distributedMetrics);
        }
        for (EdgeMapping mapping : struct.edges()) {
            DistributedMetrics distributedMetrics = new DistributedMetrics();
            distributedMetrics.init(sc, mapping.toString());
            this.edgeMetrics.put(mapping.label(), distributedMetrics);
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
        return (metrics(mapping)).parseSuccess.value().longValue();
    }

    public void plusParseSuccess(ElementMapping mapping, long count) {
        (metrics(mapping)).parseSuccess.add(count);
    }

    public long parseFailure(ElementMapping mapping) {
        return
                (metrics(mapping)).parseFailure.value().longValue();
    }

    public void increaseParseFailure(ElementMapping mapping) {
        (metrics(mapping)).parseFailure.add(1L);
    }

    public long insertSuccess(ElementMapping mapping) {
        return (metrics(mapping)).insertSuccess.value().longValue();
    }

    public void plusInsertSuccess(ElementMapping mapping, long count) {
        (metrics(mapping)).insertSuccess.add(count);
    }

    public long insertFailure(ElementMapping mapping) {
        return (metrics(mapping)).insertFailure.value().longValue();
    }

    public void increaseInsertFailure(ElementMapping mapping) {
        (metrics(mapping)).insertFailure.add(1L);
    }

    public Map<String, DistributedMetrics> vertexMetrics() {
        return this.vertexMetrics;
    }

    public Map<String, DistributedMetrics> edgeMetrics() {
        return this.edgeMetrics;
    }

    public long totalParseFailures() {
        long total = 0L;
        for (DistributedMetrics counter : this.vertexMetrics.values())
            total += counter.parseFailure.value().longValue();
        for (DistributedMetrics counter : this.edgeMetrics.values())
            total += counter.parseFailure.value().longValue();
        return total;
    }

    public long totalInsertFailures() {
        long total = 0L;
        for (DistributedMetrics counter : this.vertexMetrics.values())
            total += counter.insertFailure.value().longValue();
        for (DistributedMetrics counter : this.edgeMetrics.values())
            total += counter.insertFailure.value().longValue();
        return total;
    }

    private DistributedMetrics metrics(ElementMapping mapping) {
        if (mapping.type().isVertex())
            return this.vertexMetrics.get(mapping.label());
        return this.edgeMetrics.get(mapping.label());
    }

    public static class DistributedMetrics implements Serializable {
        private LongAccumulator parseSuccess;

        private LongAccumulator parseFailure;

        private LongAccumulator insertSuccess;

        private LongAccumulator insertFailure;

        public void init(SparkContext sc, String label) {
            this.parseSuccess = sc.longAccumulator(label + "_" + "parseSuccess");
            this.parseFailure = sc.longAccumulator(label + "_" + "parseFailure");
            this.insertSuccess = sc.longAccumulator(label + "_" + "insertSuccess");
            this.insertFailure = sc.longAccumulator(label + "_" + "parseFailure");
        }

        public void plusDisInsertSuccess(Long count) {
            this.insertSuccess.add(count);
        }

        public void plusDisParseSuccess(Long count) {
            this.parseSuccess.add(count);
        }

        public long parseSuccess() {
            return this.parseSuccess.value().longValue();
        }

        public long parseFailure() {
            return this.parseFailure.value().longValue();
        }

        public long insertSuccess() {
            return this.insertSuccess.value().longValue();
        }

        public long insertFailure() {
            return this.insertFailure.value().longValue();
        }

        @Override
        public String toString() {
            return
                    "parseSuccess=" + parseSuccess.value() +
                    ", parseFailure=" + parseFailure.value() +
                    ", insertSuccess=" + insertSuccess.value() +
                    ", insertFailure=" + insertFailure.value()
                   ;
        }
    }
}
