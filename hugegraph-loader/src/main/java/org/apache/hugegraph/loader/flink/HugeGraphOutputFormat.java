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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.flink.api.common.io.ParseException;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;
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
import org.apache.hugegraph.loader.util.JsonUtil;
import org.slf4j.Logger;

import org.apache.hugegraph.api.graph.GraphAPI;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.structure.constant.Direction;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.graph.BatchEdgeRequest;
import org.apache.hugegraph.structure.graph.BatchVertexRequest;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.UpdateStrategy;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Log;
import org.apache.hugegraph.util.E;

import io.debezium.data.Envelope;

public class HugeGraphOutputFormat<T> implements OutputFormat<T> {

    private static final Logger LOG = Log.logger(HugeGraphOutputFormat.class);
    private static final long serialVersionUID = -4514164348993670086L;
    private transient LoadContext loadContext;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient volatile boolean closed = false;

    // Ship validated configuration, not submitter-side files or live client objects.
    private final String optionsJson;
    private final String structJson;
    private transient LoadOptions loadOptions;
    private transient InputStruct struct;
    private transient Map<ElementBuilder, List<String>> builders;

    public HugeGraphOutputFormat(InputStruct struct, String[] args) {
        this.struct = struct;
        this.loadOptions = LoadOptions.parseOptions(args);
        validateOptions(this.loadOptions);
        this.optionsJson = JsonUtil.toJson(this.loadOptions);
        this.structJson = JsonUtil.toJson(struct);
    }

    static void validateOptions(LoadOptions options) {
        E.checkArgument(!options.dryRun, "CDC does not support --dry-run true");
        E.checkArgument(!options.usePrefilter, "CDC does not support --use-prefilter true");
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        // Parsing CLI options here would require the submitter's mapping file on every worker.
        this.loadOptions = JsonUtil.fromJson(this.optionsJson, LoadOptions.class);
        this.struct = JsonUtil.fromJson(this.structJson, InputStruct.class);
    }

    private Map<ElementBuilder, List<String>> initBuilders() {
        LoadContext loadContext = this.loadContext;
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
    public void open(InitializationContext context) {
        validateOptions(this.loadOptions);
        this.loadContext = new LoadContext(this.loadOptions);
        this.builders = initBuilders();
        int flushIntervalMs = this.loadOptions.flushIntervalMs;
        if (flushIntervalMs > 0) {
            this.scheduler = new ScheduledThreadPoolExecutor(1, new ExecutorThreadFactory(
                                 "hugegraph-streamload-outputformat"));
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(
                    this::flushAll, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    synchronized void flushAll() {
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
        String record = row.toString();
        // Validate every mapping before buffering or writing any part of this event.
        validateUpdateIdentities(record);
        for (Map.Entry<ElementBuilder, List<String>> builder :
                this.builders.entrySet()) {
            ElementMapping elementMapping = builder.getKey().mapping();
            if (elementMapping.skip()) {
                continue;
            }

            // Add batch
            List<String> graphElements = builder.getValue();
            graphElements.add(record);
            //if (graphElements.size() >= elementMapping.batchSize()) {
            //    flush(builder.getKey(), builder.getValue());
            //}
            flush(builder.getKey(), builder.getValue());
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
        String op = node.get(Constants.CDC_OP).asText();
        return Tuple2.of(op, buildElements(elementBuilder, node.get(Constants.CDC_DATA),
                                         Envelope.Operation.DELETE.code().equals(op)));
    }

    private List<GraphElement> buildElements(ElementBuilder elementBuilder, JsonNode data,
                                            boolean identityOnly) {
        if (data == null || !data.isObject()) {
            throw new LoadException("CDC event requires a full row image");
        }
        ElementMapping mapping = elementBuilder.mapping();
        Set<String> nullableKeys = mapping.type().isVertex() ?
                                   this.loadContext.schemaCache().getVertexLabel(mapping.label()).nullableKeys() :
                                   this.loadContext.schemaCache().getEdgeLabel(mapping.label()).nullableKeys();
        List<String> fields = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (String name : this.struct.input().header()) {
            JsonNode value = data.get(name);
            if (value == null) {
                throw new LoadException("CDC event is missing mapped field '%s'", name);
            }
            // SQL NULL is structural CDC data, independent of textual null_values sentinels.
            String property = mapping.mappingField(name, this.struct.input().headerCaseSensitive());
            if (value.isNull() && nullableKeys.contains(property)) {
                continue;
            }
            fields.add(name);
            values.add(value.isNull() ? null : value.asText());
        }
        String[] names = fields.toArray(new String[0]);
        String[] row = values.toArray(new String[0]);
        if (identityOnly && elementBuilder instanceof VertexBuilder) {
            return (List<GraphElement>) (List<?>) ((VertexBuilder) elementBuilder).buildIdentity(names, row);
        }
        return elementBuilder.build(names, row);
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
                    Set<String> writtenProperties = new HashSet<>();
                    graphElements.forEach(element -> writtenProperties.addAll(element.properties().keySet()));
                    Map<String, UpdateStrategy> updateStrategyMap =
                            elementMapping.updateStrategies();
                    if (isVertex) {
                        BatchVertexRequest.Builder req = new BatchVertexRequest.Builder();
                        req.vertices((List<Vertex>) (Object) graphElements)
                            .updatingStrategies(updateStrategyMap)
                            .createIfNotExist(true);
                        BatchVertexRequest request = req.build();
                        removeNullProperties(g.updateVertices(request), elementMapping, row, writtenProperties);
                    } else {
                        BatchEdgeRequest.Builder req = new BatchEdgeRequest.Builder();
                        req.edges((List<Edge>) (Object) graphElements)
                            .updatingStrategies(updateStrategyMap)
                            .checkVertex(this.loadOptions.checkVertex)
                            .createIfNotExist(true);
                        BatchEdgeRequest request = req.build();
                        removeNullProperties(g.updateEdges(request), elementMapping, row, writtenProperties);
                    }
                    break;
                case DELETE:
                    for (GraphElement element : graphElements) {
                        removeElement(g, elementBuilder, element);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                              "The type of `op` should be 'c' 'r' 'u' 'd' only");
            }
        }
        rows.clear();
    }

    private void validateUpdateIdentities(String row) {
        JsonNode node;
        try {
            node = new ObjectMapper().readTree(row);
        } catch (JsonProcessingException e) {
            throw new ParseException(row, e);
        }
        if (!Envelope.Operation.UPDATE.code().equals(node.path(Constants.CDC_OP).asText())) {
            return;
        }
        for (ElementBuilder builder : this.builders.keySet()) {
            if (builder.mapping().skip()) {
                continue;
            }
            List<GraphElement> before = buildElements(builder, node.get("before"), true);
            List<GraphElement> unmatched = new ArrayList<>(
                    buildElements(builder, node.get(Constants.CDC_DATA), true));
            boolean retained = before.size() == unmatched.size();
            for (GraphElement old : before) {
                int matching = -1;
                for (int i = 0; i < unmatched.size(); i++) {
                    if (sameIdentity(builder, old, unmatched.get(i))) {
                        matching = i;
                        break;
                    }
                }
                if (matching < 0) {
                    retained = false;
                    break;
                }
                unmatched.remove(matching);
            }
            E.checkArgument(retained && unmatched.isEmpty(),
                            "CDC UPDATE cannot change identity for label '%s'; " +
                            "vertex IDs, edge endpoints and sort keys must remain unchanged",
                            builder.mapping().label());
        }
    }

    private static boolean sameIdentity(ElementBuilder builder, GraphElement left, GraphElement right) {
        if (builder instanceof VertexBuilder) {
            return GraphAPI.formatVertexId(left.id()).equals(GraphAPI.formatVertexId(right.id()));
        }
        Edge old = (Edge) left;
        Edge current = (Edge) right;
        EdgeBuilder edges = (EdgeBuilder) builder;
        return GraphAPI.formatVertexId(old.sourceId()).equals(GraphAPI.formatVertexId(current.sourceId())) &&
               GraphAPI.formatVertexId(old.targetId()).equals(GraphAPI.formatVertexId(current.targetId())) &&
               edges.identityProperties(old).equals(edges.identityProperties(current));
    }

    private static void removeElement(GraphManager graph, ElementBuilder builder, GraphElement element) {
        try {
            if (builder instanceof VertexBuilder) {
                graph.getVertex(element.id());
                graph.removeVertex(element.id());
            } else {
                Edge edge = (Edge) element;
                Map<String, Object> properties = ((EdgeBuilder) builder).identityProperties(edge);
                // Resolve the server-owned ID from endpoints, label and sort keys. Keep literal
                // strings such as "P.gt(1)" instead of interpreting them as query predicates.
                int offset = 0;
                int pageSize = 100;
                while (true) {
                    List<Edge> candidates = graph.getEdges(edge.sourceId(), Direction.OUT, edge.label(),
                                                          properties, true, offset, pageSize);
                    for (Edge candidate : candidates) {
                        if (sameReturnedVertexId(edge.targetId(), candidate.targetId())) {
                            graph.removeEdge(candidate.id());
                            return;
                        }
                    }
                    if (candidates.size() < pageSize) {
                        return;
                    }
                    offset += pageSize;
                }
            }
        } catch (ServerException e) {
            // A delete can be replayed after restoring an earlier checkpoint.
            if (e.status() != 404) {
                throw e;
            }
        }
    }

    private static boolean sameReturnedVertexId(Object expected, Object actual) {
        // Graph API JSON returns UUID endpoint IDs as strings; the builder knows their schema type.
        if (expected instanceof UUID && actual instanceof String) {
            actual = UUID.fromString((String) actual);
        }
        return GraphAPI.formatVertexId(expected).equals(GraphAPI.formatVertexId(actual));
    }

    private void removeNullProperties(List<? extends GraphElement> elements,
                                      ElementMapping mapping, String row, Set<String> writtenProperties) {
        JsonNode data;
        try {
            data = new ObjectMapper().readTree(row).get(Constants.CDC_DATA);
        } catch (JsonProcessingException e) {
            throw new ParseException(row, e);
        }
        boolean caseSensitive = this.struct.input().headerCaseSensitive();
        for (String name : this.struct.input().header()) {
            JsonNode value = data.get(name);
            if (value == null || (!value.isNull() && !mapping.nullValues().contains(value.asText())) ||
                containsField(mapping.ignoredFields(), name, caseSensitive) ||
                (!mapping.selectedFields().isEmpty() &&
                 !containsField(mapping.selectedFields(), name, caseSensitive))) {
                continue;
            }
            String property = mapping.mappingField(name, caseSensitive);
            // Another source field may have written the same target, or a non-nullable
            // property may retain a configured null sentinel. Trust the actual builder output.
            if (writtenProperties.contains(property)) {
                continue;
            }
            for (GraphElement element : elements) {
                // Batch update omits null properties; explicitly remove the previous value.
                // Returned elements have their GraphManager attached by the client.
                if (element.properties().containsKey(property)) {
                    element.removeProperty(property);
                }
            }
        }
    }

    private static boolean containsField(Set<String> fields, String name, boolean caseSensitive) {
        return fields.stream().anyMatch(field -> caseSensitive ? field.equals(name) : field.equalsIgnoreCase(name));
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
        if (this.loadContext != null) {
            this.loadContext.client().close();
        }
    }
}
