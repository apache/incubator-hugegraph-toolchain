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

package com.baidu.hugegraph.loader.progress;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class InputItemProgress {

    /**
     * NOTE: this offset refers to the parsed part in input instead of loaded.
     * So if a file is completely parsed, but some lines fail to be inserted,
     * the file will be marked as loaded, and the offset will be the last line
     * of the file. This file will be skipped when load at next time.
     */
    @JsonProperty("offset")
    private long offset;

    public InputItemProgress() {
        this.offset = 0L;
    }

    public long offset() {
        return this.offset;
    }

    public void offset(long offset) {
        this.offset = offset;
    }

    public void increaseOffset() {
        this.offset++;
    }

    public void addOffset(long count) {
        this.offset += count;
    }
}
