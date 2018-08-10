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

package com.baidu.hugegraph.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.base.RetryManager;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.rest.SerializeException;
import com.baidu.hugegraph.structure.constant.GraphMode;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IdStrategy;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class RestoreManager extends RetryManager {

    private static final int BATCH_SIZE = 500;
    private GraphMode mode = null;

    private Map<String, Long> primaryKeyVLs = null;

    public RestoreManager(String url, String graph) {
        super(url, graph, "restore");
    }

    public RestoreManager(String url, String graph,
                          String username, String password) {
        super(url, graph, username, password, "restore");
    }

    public void mode(GraphMode mode) {
        this.mode = mode;
    }

    public void restore(List<HugeType> types, String inputDir) {
        E.checkNotNull(this.mode, "mode");
        this.startTimer();
        for (HugeType type : types) {
            switch (type) {
                case VERTEX:
                    this.restoreVertices(type, inputDir);
                    break;
                case EDGE:
                    this.restoreEdges(type, inputDir);
                    break;
                case PROPERTY_KEY:
                    this.restorePropertyKeys(type, inputDir);
                    break;
                case VERTEX_LABEL:
                    this.restoreVertexLabels(type, inputDir);
                    break;
                case EDGE_LABEL:
                    this.restoreEdgeLabels(type, inputDir);
                    break;
                case INDEX_LABEL:
                    this.restoreIndexLabels(type, inputDir);
                    break;
                default:
                    throw new AssertionError(String.format(
                              "Bad restore type: %s", type));
            }
        }
        this.shutdown(this.type());
        this.printSummary();
    }

    private void restoreVertices(HugeType type, String dir) {
        this.initPrimaryKeyVLs();
        String filePrefix = type.string();
        List<File> files = filesWithPrefix(dir, filePrefix);
        BiConsumer<String, String> consumer = (t, l) -> {
            List<Vertex> vertices = this.readList(t, Vertex.class, l);
            int size = vertices.size();
            for (int i = 0; i < size; i += BATCH_SIZE) {
                int toIndex = Math.min(i + BATCH_SIZE, size);
                List<Vertex> subVertices = vertices.subList(i, toIndex);
                for (Vertex vertex : subVertices) {
                    if (this.primaryKeyVLs.containsKey(vertex.label())) {
                        vertex.id(null);
                    }
                }
                this.retry(() -> this.client.graph().addVertices(subVertices),
                           "restoring vertices");
                this.vertexCounter.getAndAdd(toIndex - i);
            }
        };
        for (File file : files) {
            this.submit(() -> {
                this.restore(type, file, consumer);
            });
        }
        this.awaitTasks();
    }

    private void restoreEdges(HugeType type, String dir) {
        this.initPrimaryKeyVLs();
        String filePrefix = type.string();
        List<File> files = filesWithPrefix(dir, filePrefix);
        BiConsumer<String, String> consumer = (t, l) -> {
            List<Edge> edges = this.readList(t, Edge.class, l);
            int size = edges.size();
            for (int i = 0; i < size; i += BATCH_SIZE) {
                int toIndex = Math.min(i + BATCH_SIZE, size);
                List<Edge> subEdges = edges.subList(i, toIndex);
                /*
                 * Edge id is concat using source and target vertex id and
                 * vertices of primary key id strategy might have changed
                 * their id
                 */
                this.updateVertexIdInEdge(subEdges);
                this.retry(() -> this.client.graph().addEdges(subEdges, false),
                           "restoring edges");
                this.edgeCounter.getAndAdd(toIndex - i);
            }
        };
        for (File file : files) {
            this.submit(() -> {
                this.restore(type, file, consumer);
            });
        }
        this.awaitTasks();
    }

    private void restorePropertyKeys(HugeType type, String dir) {
        String fileName = type.string();
        BiConsumer<String, String> consumer = (t, l) -> {
            for (PropertyKey pk : this.readList(t, PropertyKey.class, l)) {
                if (this.mode == GraphMode.MERGING) {
                    pk.resetId();
                    pk.checkExist(false);
                }
                this.client.schema().addPropertyKey(pk);
                this.propertyKeyCounter.getAndIncrement();
            }
        };
        this.restore(type, Paths.get(dir, fileName).toFile(), consumer);
    }

    private void restoreVertexLabels(HugeType type, String dir) {
        String fileName = type.string();
        BiConsumer<String, String> consumer = (t, l) -> {
            for (VertexLabel vl : this.readList(t, VertexLabel.class, l)) {
                if (this.mode == GraphMode.MERGING) {
                    vl.resetId();
                    vl.checkExist(false);
                }
                this.client.schema().addVertexLabel(vl);
                this.vertexLabelCounter.getAndIncrement();
            }
        };
        this.restore(type, Paths.get(dir, fileName).toFile(), consumer);
    }

    private void restoreEdgeLabels(HugeType type, String dir) {
        String fileName = type.string();
        BiConsumer<String, String> consumer = (t, l) -> {
            for (EdgeLabel el : this.readList(t, EdgeLabel.class, l)) {
                if (this.mode == GraphMode.MERGING) {
                    el.resetId();
                    el.checkExist(false);
                }
                this.client.schema().addEdgeLabel(el);
                this.edgeLabelCounter.getAndIncrement();
            }
        };
        this.restore(type, Paths.get(dir, fileName).toFile(), consumer);
    }

    private void restoreIndexLabels(HugeType type, String dir) {
        String fileName = type.string();
        BiConsumer<String, String> consumer = (t, l) -> {
            for (IndexLabel il : this.readList(t, IndexLabel.class, l)) {
                if (this.mode == GraphMode.MERGING) {
                    il.resetId();
                    il.checkExist(false);
                }
                this.client.schema().addIndexLabel(il);
                this.indexLabelCounter.getAndIncrement();
            }
        };
        this.restore(type, Paths.get(dir, fileName).toFile(), consumer);
    }

    private void restore(HugeType type, File file,
                         BiConsumer<String, String> consumer) {
        E.checkArgument(
                file.exists() && file.isFile() && file.canRead(),
                "Need to specify a readable filter file rather than: %s",
                file.toString());

        try (InputStream is = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(is, API.CHARSET);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(type.string(), line);
            }
        } catch (IOException e) {
            throw new ClientException("IOException occur while reading %s",
                                      e, file.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> readList(String key, Class<T> clazz, String content) {
        ObjectMapper mapper = this.client.mapper();
        try {
            JsonNode root = mapper.readTree(content);
            JsonNode element = root.get(key);
            if(element == null) {
                throw new SerializeException(
                          "Can't find value of the key: %s in json.", key);
            } else {
                JavaType t = mapper.getTypeFactory()
                                   .constructParametricType(List.class, clazz);
                return (List<T>) mapper.readValue(element.toString(), t);
            }
        } catch (IOException e) {
            throw new SerializeException(
                      "Failed to deserialize %s", e, content);
        }
    }

    private static List<File> filesWithPrefix(String directory, String prefix) {
        List<File> inputFiles = new ArrayList<>(8);
        File dir = new File(directory);
        String[] files = dir.list();
        if (files == null) {
            return ImmutableList.of();
        }
        for (int i = 0; i < files.length; i++) {
            boolean matchPrefix = files[i].startsWith(prefix);
            File file = Paths.get(dir.getAbsolutePath(), files[i]).toFile();
            if (matchPrefix && file.isFile()) {
                inputFiles.add(file);
            }
        }
        return inputFiles;
    }

    private void initPrimaryKeyVLs() {
        if (this.primaryKeyVLs != null) {
            return;
        }
        this.primaryKeyVLs = new HashMap<>();
        List<VertexLabel> vertexLabels = this.client.schema().getVertexLabels();
        for (VertexLabel vl : vertexLabels) {
            if (vl.idStrategy() == IdStrategy.PRIMARY_KEY) {
                this.primaryKeyVLs.put(vl.name(), vl.id());
            }
        }
    }

    private void updateVertexIdInEdge(List<Edge> edges) {
        for (Edge edge : edges) {
            edge.source(this.updateVid(edge.sourceLabel(), edge.source()));
            edge.target(this.updateVid(edge.targetLabel(), edge.target()));
        }
    }

    private Object updateVid(String label, Object id) {
        if (this.primaryKeyVLs.containsKey(label)) {
            String sid = (String) id;
            return this.primaryKeyVLs.get(label) +
                   sid.substring(sid.indexOf(':'));
        }
        return id;
    }
}
