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
    private long parseFailureNum;
    private final LongAdder insertSuccessNum;
    private final LongAdder insertFailureNum;

    public LoadMetrics() {
        this.parseFailureNum = 0L;
        this.insertSuccessNum = new LongAdder();
        this.insertFailureNum = new LongAdder();
    }

    public long parseFailureNum() {
        return this.parseFailureNum;
    }

    public long increaseParseFailureNum() {
        return ++this.parseFailureNum;
    }

    public long insertSuccessNum() {
        return this.insertSuccessNum.longValue();
    }

    public void addInsertSuccessNum(long count) {
        this.insertSuccessNum.add(count);
    }

    public void increaseInsertSuccessNum() {
        this.insertSuccessNum.increment();
    }

    public long insertFailureNum() {
        return this.insertFailureNum.longValue();
    }

    public void increaseInsertFailureNum() {
        this.insertFailureNum.increment();
    }

    public void reset() {
        this.parseFailureNum = 0L;
        this.insertSuccessNum.reset();
        this.insertFailureNum.reset();
    }
}
