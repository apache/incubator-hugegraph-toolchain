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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.baidu.hugegraph.api.graph.structure.BatchEdgeRequest;
import com.baidu.hugegraph.api.graph.structure.BatchVertexRequest;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.builder.Record;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.metrics.LoadMetrics;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.ImmutableSet;

public abstract class InsertTask implements Runnable {

    public static final Set<String> UNACCEPTABLE_EXCEPTIONS = ImmutableSet.of(
            "class java.lang.IllegalArgumentException"
    );

    protected final LoadContext context;
    protected final InputStruct struct;
    protected final ElementMapping mapping;
    protected final List<Record> batch;

    public InsertTask(LoadContext context, InputStruct struct,
                      ElementMapping mapping, List<Record> batch) {
        E.checkArgument(batch != null && !batch.isEmpty(),
                        "The batch can't be null or empty");
        this.context = context;
        this.struct = struct;
        this.mapping = mapping;
        this.batch = batch;
    }

    public ElemType type() {
        return this.mapping.type();
    }

    public LoadOptions options() {
        return this.context.options();
    }

    public LoadSummary summary() {
        return this.context.summary();
    }

    public LoadMetrics metrics() {
        return this.summary().metrics(this.mapping);
    }

    protected void plusLoadSuccess(int count) {
        LoadMetrics metrics = this.summary().metrics(this.mapping);
        metrics.plusLoadSuccess(count);
        this.summary().plusTotalLoaded(this.type(), count);
    }

    protected void increaseLoadSuccess() {
        LoadMetrics metrics = this.summary().metrics(this.mapping);
        metrics.increaseLoadSuccess();
        this.summary().plusTotalLoaded(this.type(), 1);
    }

    @SuppressWarnings("unchecked")
    protected void addBatch(ElemType type, List<Record> batch,
                            boolean checkVertex) {
        HugeClient client = HugeClientHolder.get(this.options());
        List<GraphElement> elements = new ArrayList<>(batch.size());
        batch.forEach(r -> elements.add(r.element()));
        if (type.isVertex()) {
            client.graph().addVertices((List<Vertex>) (Object) elements);
        } else {
            assert type.isEdge();
            client.graph().addEdges((List<Edge>) (Object) elements, checkVertex);
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateBatch(ElemType type, List<Record> batch,
                               boolean checkVertex) {
        HugeClient client = HugeClientHolder.get(this.options());
        List<GraphElement> elements = new ArrayList<>(batch.size());
        batch.forEach(r -> elements.add(r.element()));
        // CreateIfNotExist dose not support false now
        if (type.isVertex()) {
            BatchVertexRequest.Builder req = new BatchVertexRequest.Builder();
            req.vertices((List<Vertex>) (Object) elements)
               .updatingStrategies(this.mapping.updateStrategies())
               .createIfNotExist(true);

            client.graph().updateVertices(req.build());
        } else {
            assert type.isEdge();
            BatchEdgeRequest.Builder req = new BatchEdgeRequest.Builder();
            req.edges((List<Edge>) (Object) elements)
               .updatingStrategies(this.mapping.updateStrategies())
               .checkVertex(checkVertex)
               .createIfNotExist(true);

            client.graph().updateEdges(req.build());
        }
    }
}
