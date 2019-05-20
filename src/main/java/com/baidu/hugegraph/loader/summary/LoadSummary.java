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

package com.baidu.hugegraph.loader.summary;

import java.time.Duration;

public final class LoadSummary {

    private final String type;

    private long parseFailure;
    private long parseSuccess;
    private long insertFailure;
    private long insertSuccess;
    private long averageSpeed;
    private Duration loadTime;

//    private final LoadMetrics vertexMetrics;
//    private final LoadMetrics edgeMetrics;

    public LoadSummary(String type) {
        this.type = type;
        this.parseFailure = 0L;
        this.parseSuccess = 0L;
        this.insertFailure = 0L;
        this.insertSuccess = 0L;
        this.averageSpeed = 0L;
        this.loadTime = Duration.ZERO;
    }

    public String type() {
        return this.type;
    }

    public long parseFailure() {
        return this.parseFailure;
    }

    public void parseFailure(long count) {
        this.parseFailure = count;
    }

    public long parseSuccess() {
        return parseSuccess;
    }

    public void parseSuccess(long count) {
        this.parseSuccess = count;
    }

    public long insertFailure() {
        return this.insertFailure;
    }

    public void insertFailure(long count) {
        this.insertFailure = count;
    }

    public long insertSuccess() {
        return this.insertSuccess;
    }

    public void insertSuccess(long count) {
        this.insertSuccess = count;
    }

    public Duration loadTime() {
        return this.loadTime;
    }

    public void loadTime(Duration duration) {
        this.loadTime = duration;
    }

    public long averageSpeed() {
        return averageSpeed;
    }

    public void averageSpeed(long insertSuccess, Duration loadTime) {
        if (loadTime.getSeconds() != 0) {
            this.averageSpeed = insertSuccess / loadTime.getSeconds();
        } else {
            this.averageSpeed = 0L;
        }
    }
}
