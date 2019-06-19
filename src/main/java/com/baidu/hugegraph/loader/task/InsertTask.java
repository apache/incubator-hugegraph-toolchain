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

package com.baidu.hugegraph.loader.task;

import java.util.List;

import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.util.E;

public abstract class InsertTask<GE extends GraphElement> implements Runnable {

    private final LoadContext context;
    private final ElemType type;
    private final List<GE> batch;

    public InsertTask(LoadContext context, ElemType type, List<GE> batch) {
        E.checkArgument(batch != null && !batch.isEmpty(),
                        "The batch can't be null or empty");
        this.context = context;
        this.type = type;
        this.batch = batch;
    }

    public LoadContext context() {
        return this.context;
    }

    public ElemType type() {
        return this.type;
    }

    public List<GE> batch() {
        return this.batch;
    }
}
