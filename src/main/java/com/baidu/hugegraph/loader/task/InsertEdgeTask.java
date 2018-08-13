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

import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.structure.graph.Edge;

public class InsertEdgeTask extends InsertTask<Edge> {

    InsertEdgeTask(List<Edge> batch) {
        super(batch);
    }

    @Override
    protected void execute() {
        LoadOptions options = LoadOptions.instance();
        this.client().graph().addEdges(this.batch(), options.checkVertex);
    }
}
