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

import static com.baidu.hugegraph.loader.metrics.LoadMetrics.MetricsCounter;

public final class LoadReport {

    // input struct related
    private long totalTime;
    private long readSuccess;
    private long readFailure;
    // vertex
    private long vertexParseSuccess;
    private long vertexParseFailure;
    private long vertexInsertSuccess;
    private long vertexInsertFailure;
    // edge
    private long edgeParseSuccess;
    private long edgeParseFailure;
    private long edgeInsertSuccess;
    private long edgeInsertFailure;

    public long totalTime() {
        return this.totalTime;
    }

    public long readSuccess() {
        return this.readSuccess;
    }

    public long readFailure() {
        return this.readFailure;
    }

    public long vertexParseSuccess() {
        return this.vertexParseSuccess;
    }

    public long vertexParseFailure() {
        return this.vertexParseFailure;
    }

    public long vertexInsertSuccess() {
        return this.vertexInsertSuccess;
    }

    public long vertexInsertFailure() {
        return this.vertexInsertFailure;
    }

    public long edgeParseSuccess() {
        return this.edgeParseSuccess;
    }

    public long edgeParseFailure() {
        return this.edgeParseFailure;
    }

    public long edgeInsertSuccess() {
        return this.edgeInsertSuccess;
    }

    public long edgeInsertFailure() {
        return this.edgeInsertFailure;
    }

    public static LoadReport collect(LoadSummary summary) {
        LoadReport report = new LoadReport();
        report.totalTime = summary.totalTime();
        for (LoadMetrics metrics : summary.inputMetricsMap().values()) {
            report.readSuccess += metrics.readSuccess();
            report.readFailure += metrics.readFailure();
            for (MetricsCounter counter : metrics.vertexCounters().values()) {
                report.vertexParseSuccess += counter.parseSuccess();
                report.vertexParseFailure += counter.parseFailure();
                report.vertexInsertSuccess += counter.insertSuccess();
                report.vertexInsertFailure += counter.insertFailure();
            }
            for (MetricsCounter counter : metrics.edgeCounters().values()) {
                report.edgeParseSuccess += counter.parseSuccess();
                report.edgeParseFailure += counter.parseFailure();
                report.edgeInsertSuccess += counter.insertSuccess();
                report.edgeInsertFailure += counter.insertFailure();
            }
        }
        return report;
    }
}
