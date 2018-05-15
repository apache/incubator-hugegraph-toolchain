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

package com.baidu.hugegraph.cmd.manager;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.type.Shard;

public class BackupManager extends ToolManager {

    public BackupManager(String url, String graph) {
        super(url, graph, "backup");
    }

    public BackupManager(String url, String graph,
                         String username, String password) {
        super(url, graph, username, password, "backup");
    }

    public void backup(List<HugeType> types, String outputDir) {
        for (HugeType type : types) {
            String prefix = outputDir + type.string();
            switch (type) {
                case VERTEX:
                    this.backupVertices(prefix);
                    break;
                case EDGE:
                    this.backupEdges(prefix);
                    break;
                case PROPERTY_KEY:
                    this.backupPropertyKeys(prefix);
                    break;
                case VERTEX_LABEL:
                    this.backupVertexLabels(prefix);
                    break;
                case EDGE_LABEL:
                    this.backupEdgeLabels(prefix);
                    break;
                case INDEX_LABEL:
                    this.backupIndexLabels(prefix);
                    break;
                default:
                    throw new AssertionError(String.format(
                              "Bad backup type: %s", type));
            }
        }
    }

    private void backupVertices(String prefix) {
        List<Shard> shards = this.client.traverser()
                                        .vertexShards(SPLIT_SIZE);
        int i = 0;
        for (Shard shard : shards) {
            List<Vertex> vertices = this.client.traverser().vertices(shard);
            if (vertices.isEmpty()) {
                continue;
            }
            final int j = ++i;
            submit(() -> {
                write(prefix + (j % threadsNum()),
                      this.writeList(HugeType.VERTEX.string(), vertices));
            });
        }
    }

    private void backupEdges(String prefix) {
        List<Shard> shards = this.client.traverser().edgeShards(SPLIT_SIZE);
        int i = 0;
        for (Shard shard : shards) {
            List<Edge> edges = this.client.traverser().edges(shard);
            if (edges.isEmpty()) {
                continue;
            }
            final int j = ++i;
            submit(() -> {
                write(prefix + (j % threadsNum()),
                      this.writeList(HugeType.EDGE.string(), edges));
            });
        }
    }

    private void backupPropertyKeys(String filename) {
        write(filename, this.writeList(HugeType.PROPERTY_KEY.string(),
                                       this.client.schema().getPropertyKeys()));
    }

    private void backupVertexLabels(String filename) {
        write(filename, this.writeList(HugeType.VERTEX_LABEL.string(),
                                       this.client.schema().getVertexLabels()));
    }

    private void backupEdgeLabels(String filename) {
        write(filename, this.writeList(HugeType.EDGE_LABEL.string(),
                                       this.client.schema().getEdgeLabels()));
    }

    private void backupIndexLabels(String filename) {
        write(filename, this.writeList(HugeType.INDEX_LABEL.string(),
                                       this.client.schema().getIndexLabels()));
    }

    private static void write(String filename, String content) {
        File file = new File(filename);
        try (FileWriter fr = new FileWriter(file, true);
             BufferedWriter writer = new BufferedWriter(fr)) {
            writer.write(content);
        } catch (IOException e) {
            throw new ClientException("IO error occur", e);
        }
    }

    private String writeList(String label, List<?> list) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(LBUF_SIZE)) {
            out.write(String.format("{\"%s\": ", label).getBytes(API.CHARSET));
            this.client.mapper().writeValue(out, list);
            out.write("}\n".getBytes(API.CHARSET));
            return out.toString(API.CHARSET);
        } catch (Exception e) {
            throw new ClientException("Failed to serialize %s", e, label);
        }
    }
}
