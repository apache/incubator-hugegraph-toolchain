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

import java.util.ArrayList;
import java.util.List;

/**
 * It's thread safe
 */
public class FlowRangeTimer {

    private final int capacity;
    private final List<TimeRange> queue;
    private long lastEnd;
    private long totalTime;

    public FlowRangeTimer(int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayList<>(capacity);
        this.lastEnd = 0L;
        this.totalTime = 0L;
    }

    public synchronized long totalTime() {
        if (!this.queue.isEmpty()) {
            long time = this.caculate();
            this.totalTime += time;
            this.queue.clear();
        }
        return this.totalTime;
    }

    public synchronized void addTimeRange(long start, long end) {
        if (this.queue.size() >= this.capacity) {
            long time = this.caculate();
            this.totalTime += time;
            this.queue.clear();
        }
        this.queue.add(new TimeRange(start, end));
    }

    private long caculate() {
        assert !this.queue.isEmpty();
        this.queue.sort((o1, o2) -> (int) (o1.start() - o2.start()));
        long time = 0L;
        long start = this.lastEnd, end = this.lastEnd;
        for (TimeRange range : this.queue) {
            if (range.start() <= end) {
                // There is overlap
                end = Math.max(end, range.end());
            } else {
                time += (end - start);
                start = range.start();
                end = range.end();
            }
            this.lastEnd = Math.max(this.lastEnd, end);
        }
        time += (end - start);
        return time;
    }

    private static class TimeRange {

        private final long start;
        private final long end;

        public TimeRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long start() {
            return this.start;
        }

        public long end() {
            return this.end;
        }
    }
}
