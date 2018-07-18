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

package com.baidu.hugegraph.structure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

public class Task {

    @JsonProperty
    private long id;

    @JsonProperty("task_type")
    private String type;

    @JsonProperty("task_name")
    private String name;

    @JsonProperty("task_status")
    private String status;

    @JsonProperty("task_callable")
    private String callable;

    @JsonProperty("task_create")
    private long create;

    @JsonProperty("task_update")
    private long update;

    @JsonProperty("task_progress")
    private long progress;

    @JsonProperty("task_retries")
    private long retries;

    @JsonProperty("task_input")
    private String input;

    @JsonProperty("task_result")
    private Object result;

    @JsonProperty("task_description")
    private String description;

    public long id() {
        return this.id;
    }

    public String type() {
        return this.type;
    }

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    public String callable() {
        return callable;
    }

    public long createTime() {
        return create;
    }

    public long updateTime() {
        return update;
    }

    public long progress() {
        return progress;
    }

    public long retries() {
        return retries;
    }

    public String input() {
        return input;
    }

    public Object result() {
        return result;
    }

    public String description() {
        return description;
    }

    public boolean completed() {
        return ImmutableSet.of("success", "failed", "cancelled")
                           .contains(this.status);
    }

    public boolean success() {
        return "success".equals(this.status);
    }
}