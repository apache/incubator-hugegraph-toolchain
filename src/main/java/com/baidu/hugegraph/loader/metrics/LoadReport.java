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

public final class LoadReport {

    private long totalTime;
    private long parseSuccess;
    private long parseFailure;
    private long loadSuccess;
    private long loadFailure;

    public LoadReport() {
        this(0L, 0L, 0L, 0L, 0L);
    }

    public LoadReport(long totalTime, long parseSuccess, long parseFailure,
                      long loadSuccess, long loadFailure) {
        this.totalTime = totalTime;
        this.parseSuccess = parseSuccess;
        this.parseFailure = parseFailure;
        this.loadSuccess = loadSuccess;
        this.loadFailure = loadFailure;
    }

    public long totalTime() {
        return this.totalTime;
    }

    public void totalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long parseSuccess() {
        return this.parseSuccess;
    }

    public void parseSuccess(long parseSuccess) {
        this.parseSuccess = parseSuccess;
    }

    public long parseFailure() {
        return this.parseFailure;
    }

    public void parseFailure(long parseFailure) {
        this.parseFailure = parseFailure;
    }

    public long parseRate() {
        return this.totalTime == 0 ? -1L :
               this.parseSuccess * 1000 / this.totalTime;
    }

    public long loadSuccess() {
        return this.loadSuccess;
    }

    public void loadSuccess(long loadSuccess) {
        this.loadSuccess = loadSuccess;
    }

    public long loadFailure() {
        return this.loadFailure;
    }

    public void loadFailure(long loadFailure) {
        this.loadFailure = loadFailure;
    }

    public long loadRate() {
        return this.totalTime == 0 ? -1L :
               this.loadSuccess * 1000 / this.totalTime;
    }
}
