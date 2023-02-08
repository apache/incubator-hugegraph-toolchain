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

package org.apache.hugegraph.loader.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.builder.Record;
import org.apache.hugegraph.loader.constant.ElemType;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.LoadMetrics;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;

import com.google.common.collect.ImmutableSet;

public abstract class InsertTask implements Runnable {

    public static final Set<String> UNACCEPTABLE_EXCEPTIONS = ImmutableSet.of(
            "class java.lang.IllegalArgumentException"
    );

    public static final String[] UNACCEPTABLE_MESSAGES = {
            // org.apache.http.conn.HttpHostConnectException
            "Connection refused",
            "The server is being shutting down",
            "not allowed to insert, because already exist a vertex " +
            "with same id and different label"
    };

    protected final LoadContext context;
    protected final InputStruct struct;
    protected final ElementMapping mapping;
    protected final List<Record> batch;

    public InsertTask(LoadContext context, InputStruct struct,
                      ElementMapping mapping, List<Record> batch) {
        assert batch != null;
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
        return this.summary().metrics(this.struct);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        try {
            this.execute();
        } finally {
            long end = System.currentTimeMillis();
            this.context.summary().addTimeRange(this.type(), start, end);
        }
    }

    public abstract void execute();

    protected void plusLoadSuccess(int count) {
        LoadMetrics metrics = this.summary().metrics(this.struct);
        metrics.plusInsertSuccess(this.mapping, count);
        this.summary().plusLoaded(this.type(), count);
    }

    protected void increaseLoadSuccess() {
        this.plusLoadSuccess(1);
    }

    @SuppressWarnings("unchecked")
    protected void insertBatch(List<Record> batch, boolean checkVertex) {
        HugeClient client = this.context.client();
        List<GraphElement> elements = new ArrayList<>(batch.size());
        batch.forEach(r -> elements.add(r.element()));
        if (this.type().isVertex()) {
            client.graph().addVertices((List<Vertex>) (Object) elements);
        } else {
            client.graph().addEdges((List<Edge>) (Object) elements,
                                    checkVertex);
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateBatch(List<Record> batch, boolean checkVertex) {
        HugeClient client = this.context.client();
        List<GraphElement> elements = new ArrayList<>(batch.size());
        batch.forEach(r -> elements.add(r.element()));
        // CreateIfNotExist does not support false now
        if (this.type().isVertex()) {
            BatchVertexRequest.Builder req = new BatchVertexRequest.Builder();
            req.vertices((List<Vertex>) (Object) elements)
               .updatingStrategies(this.mapping.updateStrategies())
               .createIfNotExist(true);

            client.graph().updateVertices(req.build());
        } else {
            BatchEdgeRequest.Builder req = new BatchEdgeRequest.Builder();
            req.edges((List<Edge>) (Object) elements)
               .updatingStrategies(this.mapping.updateStrategies())
               .checkVertex(checkVertex)
               .createIfNotExist(true);

            client.graph().updateEdges(req.build());
        }
    }
}
