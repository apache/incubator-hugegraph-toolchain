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

package org.apache.hugegraph.loader.filter.util;

public class SegmentIdGenerator {

    private static final int SEGMENT_SIZE = 10000;

    private volatile int currentId = -1;

    public class Context {
        public int maxId = 0;
        public int lastId = 0;

        public int next() {
            return SegmentIdGenerator.this.next(this);
        }
    }

    public int next(Context context) {
        if (context.maxId == context.lastId) {
            allocatingSegment(context);
        }
        return ++context.lastId;
    }

    public synchronized void allocatingSegment(Context context) {
        context.lastId = currentId;
        currentId += SEGMENT_SIZE;
        context.maxId = currentId;
    }

    public Context genContext() {
        return new Context();
    }
}
