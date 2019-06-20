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

import java.util.concurrent.atomic.LongAdder;

public final class LoadMetrics {

    // Modified in only one thread
    private long parseTime;
    private long parseSuccess;
    private long parseFailure;
    // These metrics are user’s focus
    private long loadTime;
    private final LongAdder loadSuccess;
    private final LongAdder loadFailure;

    public LoadMetrics() {
        this.parseTime = 0L;
        this.parseSuccess = 0L;
        this.parseFailure = 0L;
        this.loadTime = 0L;
        this.loadSuccess = new LongAdder();
        this.loadFailure = new LongAdder();
    }

    public long parseTime() {
        return this.parseTime;
    }

    public void parseTime(long time) {
        this.parseTime = time;
    }

    public void plusParseTime(long time) {
        this.parseTime += time;
    }

    public long parseSuccess() {
        return this.parseSuccess;
    }

    public void plusParseSuccess(long count) {
        this.parseSuccess += count;
    }

    public long parseFailure() {
        return this.parseFailure;
    }

    public void plusParseFailure(long count) {
        this.parseFailure += count;
    }

    public long increaseParseFailure() {
        return ++this.parseFailure;
    }

    public long parseRate() {
        return this.parseTime != 0 ? this.parseSuccess() / this.parseTime : -1L;
    }

    public long loadTime() {
        return this.loadTime;
    }

    public void loadTime(long time) {
        this.loadTime = time;
    }

    public void plusLoadTime(long time) {
        this.loadTime += time;
    }

    public long loadSuccess() {
        return this.loadSuccess.longValue();
    }

    public void plusLoadSuccess(long count) {
        this.loadSuccess.add(count);
    }

    public void increaseLoadSuccess() {
        this.loadSuccess.increment();
    }

    public long loadFailure() {
        return this.loadFailure.longValue();
    }

    public void plusLoadFailure(long count) {
        this.loadFailure.add(count);
    }

    public void increaseLoadFailure() {
        this.loadFailure.increment();
    }

    public long loadRate() {
        return this.loadTime != 0 ? this.loadSuccess() / this.loadTime : -1L;
    }
}
