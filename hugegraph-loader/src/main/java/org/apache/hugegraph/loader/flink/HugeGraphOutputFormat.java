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

package org.apache.hugegraph.loader.flink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.flink.api.common.io.ParseException;
import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hugegraph.loader.builder.EdgeBuilder;
import org.apache.hugegraph.loader.builder.ElementBuilder;
import org.apache.hugegraph.loader.builder.VertexBuilder;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.EdgeMapping;
import org.apache.hugegraph.loader.mapping.ElementMapping;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.mapping.VertexMapping;
import org.slf4j.Logger;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.UpdateStrategy;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Log;

import io.debezium.data.Envelope;

public class HugeGraphOutputFormat<T> extends RichOutputFormat<T> {

    private static final Logger LOG = Log.logger(HugeGraphOutputFormat.class);
    private static final long serialVersionUID = -4514164348993670086L;
    private LoadContext loadContext;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient volatile boolean closed = false;

    private final LoadOptions loadOptions;
    private final InputStruct struct;
    private Map<ElementBuilder, List<String>> builders;

    public HugeGraphOutputFormat(InputStruct struct, String[] args) {
        this.struct = struct;
        this.loadOptions = LoadOptions.parseOptions(args);
    }

    private Map<ElementBuilder, List<String>> initBuilders() {
        LoadContext loadContext = new LoadContext(this.loadOptions);
        Map<ElementBuilder, List<String>> builders = new HashMap<>();
        for (VertexMapping vertexMapping : this.struct.vertices()) {
            builders.put(new VertexBuilder(loadContext, this.struct, vertexMapping),
                         new ArrayList<>());
        }
        for (EdgeMapping edgeMapping : this.struct.edges()) {
            builders.put(new EdgeBuilder(loadContext, this.struct, edgeMapping),
                         new ArrayList<>());
        }
        loadContext.updateSchemaCache();
        return builders;
    }

    @Override
    public void configure(Configuration configuration) {
        // pass
    }

    @Override
    public void open(int taskNumber, int numTasks) {
        this.builders = initBuilders();
        this.loadContext = new LoadContext(this.loadOptions);
        int flushIntervalMs = this.loadOptions.flushIntervalMs;
        if (flushIntervalMs > 0) {
            this.scheduler = new ScheduledThreadPoolExecutor(1, new ExecutorThreadFactory(
                                 "hugegraph-streamload-outputformat"));
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(
                    this::flushAll, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void flushAll() {
        if (this.closed) {
            return;
        }
        try {
            for (Map.Entry<ElementBuilder, List<String>> builder : this.builders.entrySet()) {
                List<String> graphElements = builder.getValue();
                if (graphElements.size() > 0) {
                    flush(builder.getKey(), graphElements);
                }
            }
        } catch (Exception e) {
            throw new LoadException("Failed to flush all data.", e);
        }
    }

    @Override
    public synchronized void writeRecord(T row) {
        for (Map.Entry<ElementBuilder, List<String>> builder :
                this.builders.entrySet()) {
            ElementMapping elementMapping = builder.getKey().mapping();
            if (elementMapping.skip()) {
                continue;
            }

            // Add batch
            List<String> graphElements = builder.getValue();
            graphElements.add(row.toString());
            if (graphElements.size() >= elementMapping.batchSize()) {
                flush(builder.getKey(), builder.getValue());
            }
        }
    }

    private Tuple2<String, List<GraphElement>> buildGraphData(ElementBuilder elementBuilder,
                                                              String row) {
        JsonNode node;
        try {
            node = new ObjectMapper().readTree(row);
        } catch (JsonProcessingException e) {
            throw new ParseException(row, e);
        }
        JsonNode data = node.get(Constants.CDC_DATA);
        String op = node.get(Constants.CDC_OP).asText();
        String[] fields = this.struct.input().header();
        String[] values = new String[data.size()];
        for (int i = 0; i < fields.length; i++) {
            values[i] = data.get(fields[i]).asText();
        }
        return Tuple2.of(op, elementBuilder.build(fields, values));
    }

    private void flush(ElementBuilder<GraphElement> elementBuilder, List<String> rows) {
        GraphManager g = this.loadContext.client().graph();
        ElementMapping elementMapping = elementBuilder.mapping();
        for (String row : rows) {
            Tuple2<String, List<GraphElement>> graphData = buildGraphData(elementBuilder, row);
            List<GraphElement> graphElements = graphData.f1;
            boolean isVertex = elementBuilder.mapping().type().isVertex();
            switch (Envelope.Operation.forCode(graphData.f0)) {
                case READ:
                case CREATE:
                    if (isVertex) {
                        g.addVertices((List<Vertex>) (Object) graphElements);
                    } else {
                        g.addEdges((List<Edge>) (Object) graphElements);
                    }
                    break;
                case UPDATE:
                    Map<String, UpdateStrategy> updateStrategyMap =
                            elementMapping.updateStrategies();
                    if (isVertex) {
                        BatchVertexRequest.Builder req = new BatchVertexRequest.Builder();
                        req.vertices((List<Vertex>) (Object) graphElements)
                           .updatingStrategies(updateStrategyMap)
                           .createIfNotExist(true);
                        g.updateVertices(req.build());
                    } else {
                        BatchEdgeRequest.Builder req = new BatchEdgeRequest.Builder();
                        req.edges((List<Edge>) (Object) graphElements)
                           .updatingStrategies(updateStrategyMap)
                           .checkVertex(this.loadOptions.checkVertex)
                           .createIfNotExist(true);
                        g.updateEdges(req.build());
                    }
                    break;
                case DELETE:
                    String id = graphElements.get(0).id().toString();
                    if (isVertex) {
                        g.removeVertex(id);
                    } else {
                        g.removeEdge(id);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                              "The type of `op` should be 'c' 'r' 'u' 'd' only");
            }
        }
        rows.clear();
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(false);
            this.scheduler.shutdown();
        }
    }
}
