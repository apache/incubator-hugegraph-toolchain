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

import java.util.concurrent.atomic.LongAdder;

/**
 * Records parse and load statistics for each vertexlabel and edgelabel
 */
public final class LoadMetrics {

    private final LongAdder parseSuccess;
    private final LongAdder parseFailure;
    private final LongAdder loadSuccess;
    private final LongAdder loadFailure;

    public LoadMetrics() {
        this.parseSuccess = new LongAdder();
        this.parseFailure = new LongAdder();
        this.loadSuccess = new LongAdder();
        this.loadFailure = new LongAdder();
    }

    public long parseSuccess() {
        return this.parseSuccess.longValue();
    }

    public void plusParseSuccess(long count) {
        this.parseSuccess.add(count);
    }

    public void increaseParseSuccess() {
        this.parseSuccess.increment();
    }

    public long parseFailure() {
        return this.parseFailure.longValue();
    }

    public void plusParseFailure(long count) {
        this.parseFailure.add(count);
    }

    public void increaseParseFailure() {
        this.parseFailure.increment();
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

    public LoadReport buildReport() {
        return new LoadReport(0L, this.parseSuccess(), this.parseFailure(),
                              this.loadSuccess(), this.loadFailure());
    }
}
