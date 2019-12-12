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
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.summary.LoadMetrics;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.ImmutableSet;

public abstract class InsertTask<GE extends GraphElement> implements Runnable {

    public static final Set<String> UNACCEPTABLE_EXCEPTIONS = ImmutableSet.of(
            "class java.lang.IllegalArgumentException"
    );

    private final LoadContext context;
    private final ElementStruct struct;
    private final List<Record<GE>> batch;

    public InsertTask(LoadContext context, ElementStruct struct,
                      List<Record<GE>> batch) {
        E.checkArgument(batch != null && !batch.isEmpty(),
                        "The batch can't be null or empty");
        this.context = context;
        this.struct = struct;
        this.batch = batch;
    }

    public LoadContext context() {
        return this.context;
    }

    public ElementStruct struct() {
        return this.struct;
    }

    public ElemType type() {
        return this.struct.type();
    }

    public LoadOptions options() {
        return this.context.options();
    }

    public LoadMetrics metrics() {
        return this.context.summary().metrics(this.struct);
    }

    public List<Record<GE>> batch() {
        return this.batch;
    }

    @SuppressWarnings("unchecked")
    protected void addBatch(ElemType type, List<Record<GE>> batch,
                            boolean checkVertex) {
        HugeClient client = HugeClientHolder.get(this.context().options());
        List<GE> elements = new ArrayList<>(batch.size());
        batch.forEach(r -> elements.add(r.element()));
        if (type.isVertex()) {
            client.graph().addVertices((List<Vertex>) elements);
        } else {
            assert type.isEdge();
            client.graph().addEdges((List<Edge>) elements, checkVertex);
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateBatch(ElemType type, List<Record<GE>> batch,
                               boolean checkVertex) {
        HugeClient client = HugeClientHolder.get(this.context().options());
        List<GE> elements = new ArrayList<>(batch.size());
        batch.forEach(r -> elements.add(r.element()));
        // CreateIfNotExist dose not support false now
        if (type.isVertex()) {
            BatchVertexRequest.Builder req = new BatchVertexRequest.Builder();
            req.vertices((List<Vertex>) elements)
               .updatingStrategies(this.struct().updateStrategies())
               .createIfNotExist(true);

            client.graph().updateVertices(req.build());
        } else {
            assert type.isEdge();
            BatchEdgeRequest.Builder req = new BatchEdgeRequest.Builder();
            req.edges((List<Edge>) elements)
               .updatingStrategies(this.struct().updateStrategies())
               .checkVertex(checkVertex)
               .createIfNotExist(true);

            client.graph().updateEdges(req.build());
        }
    }
}
