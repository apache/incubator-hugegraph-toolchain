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
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.util.E;

public final class LoadMetrics {

    private final ElemType type;

    // Modified in only one thread
    private long parseFailure;
    private final LongAdder insertSuccess;
    private final LongAdder insertFailure;

    private Instant begTime;
    private Instant endTime;

    public LoadMetrics(ElemType type) {
        this.type = type;
        this.parseFailure = 0L;
        this.insertSuccess = new LongAdder();
        this.insertFailure = new LongAdder();
        this.begTime = null;
        this.begTime = null;
    }

    public ElemType type() {
        return this.type;
    }

    public void startTimer() {
        this.begTime = Instant.now();
    }

    public void stopTimer() {
        this.endTime = Instant.now();
    }

    public Duration duration() {
        E.checkNotNull(this.begTime, "begTime");
        E.checkNotNull(this.endTime, "endTime");
        return Duration.between(begTime, endTime);
    }

    public long parseFailure() {
        return this.parseFailure;
    }

    public long increaseParseFailure() {
        return ++this.parseFailure;
    }

    public long insertSuccess() {
        return this.insertSuccess.longValue();
    }

    public void addInsertSuccess(long count) {
        this.insertSuccess.add(count);
    }

    public void increaseInsertSuccess() {
        this.insertSuccess.increment();
    }

    public long insertFailure() {
        return this.insertFailure.longValue();
    }

    public void increaseInsertFailure() {
        this.insertFailure.increment();
    }
}
