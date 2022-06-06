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

package com.baidu.hugegraph.loader.flink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.EdgeMapping;
import com.baidu.hugegraph.loader.mapping.ElementMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.VertexMapping;
import com.baidu.hugegraph.structure.GraphElement;
import com.baidu.hugegraph.structure.graph.BatchEdgeRequest;
import com.baidu.hugegraph.structure.graph.BatchVertexRequest;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.UpdateStrategy;
import com.baidu.hugegraph.structure.graph.Vertex;

import io.debezium.data.Envelope;

public class HugeGraphOutputFormat<T> extends RichOutputFormat<T> {
    private static final Logger LOG = LoggerFactory.getLogger(HugeGraphOutputFormat.class);
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
        LoadContext loadContext = new LoadContext(loadOptions);
        Map<ElementBuilder, List<String>> builders = new HashMap<>();
        for (VertexMapping vertexMapping : struct.vertices()) {
            builders.put(new VertexBuilder(loadContext, struct, vertexMapping),
                         new ArrayList<>());
        }
        for (EdgeMapping edgeMapping : struct.edges()) {
            builders.put(new EdgeBuilder(loadContext, struct, edgeMapping),
                         new ArrayList<>());
        }
        return builders;
    }

    @Override
    public void configure(Configuration configuration) {
    }

    @Override
    public void open(int taskNumber, int numTasks) {
        this.builders = initBuilders();
        this.loadContext = new LoadContext(this.loadOptions);
        int flushIntervalMs = this.loadOptions.flushIntervalMs;
        if (flushIntervalMs > 0) {
            this.scheduler = new ScheduledThreadPoolExecutor(1, new ExecutorThreadFactory(
                    "hugegraph-streamload-outputformat"));
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(() -> {
                synchronized (HugeGraphOutputFormat.this) {
                    if (!closed) {
                        try {
                            flushAll();
                        } catch (Exception e) {
                            LOG.error("Failed to flush all data.", e);
                        }
                    }
                }
            }, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    private void flushAll() {
        for (Map.Entry<ElementBuilder, List<String>> builder : this.builders.entrySet()) {
            List<String> graphElements = builder.getValue();
            if (graphElements.size() > 0) {
                flush(builder);
            }
        }
    }

    @Override
    public synchronized void writeRecord(T row) throws IOException {
        for (Map.Entry<ElementBuilder, List<String>> builder :
                this.builders.entrySet()) {
            ElementMapping elementMapping = builder.getKey().mapping();
            if (elementMapping.skip()) {
                continue;
            }

            // Add batch
            List<String> graphElements = builder.getValue();
            graphElements.add(row.toString());
            if (graphElements.size() > elementMapping.batchSize()) {
                flush(builder);
            }
        }
    }

    private void flush(Map.Entry<ElementBuilder, List<String>> builder) {
        GraphManager g = loadContext.client().graph();
        ElementBuilder elementBuilder = builder.getKey();
        ElementMapping elementMapping = elementBuilder.mapping();
        for (String row : builder.getValue()) {
            JsonNode node;
            try {
                node = new ObjectMapper().readTree(row);
            } catch (JsonProcessingException e) {
                LOG.error("Failed to parse json {}", row, e);
                continue;
            }

            JsonNode data = node.get(Constants.CDC_DATA);
            String op = node.get(Constants.CDC_OP).asText();

            String[] fields = struct.input().header();
            String[] values = new String[data.size()];
            for (int i = 0; i < fields.length; i++) {
                values[i] = data.get(fields[i]).asText();
            }
            List<GraphElement> graphElements = builder.getKey().build(fields, values);
            boolean isVertex = elementBuilder.mapping().type().isVertex();
            switch (Envelope.Operation.forCode(op)) {
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
                        BatchVertexRequest.Builder req =
                                new BatchVertexRequest.Builder();
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
                    throw new RuntimeException("The type of `op` should be 'c' 'r' 'u' 'd' only");
            }
        }
        builder.getValue().clear();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            if (this.scheduledFuture != null) {
                scheduledFuture.cancel(false);
                this.scheduler.shutdown();
            }
        }
    }
}
