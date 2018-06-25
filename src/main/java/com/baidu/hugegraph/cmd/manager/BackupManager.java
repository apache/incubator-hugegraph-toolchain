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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.concurrent.KeyLock;
import com.baidu.hugegraph.rest.ClientException;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.type.Shard;

public class BackupManager extends RetryManager {

    private static KeyLock locks = new KeyLock();

    public BackupManager(String url, String graph) {
        super(url, graph, "backup");
    }

    public BackupManager(String url, String graph,
                         String username, String password) {
        super(url, graph, username, password, "backup");
    }

    public void backup(List<HugeType> types, String outputDir) {
        this.startTimer();
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
        shutdown(this.type());
        this.printSummary();
    }

    private void backupVertices(String prefix) {
        List<Shard> shards = this.client.traverser()
                                        .vertexShards(SPLIT_SIZE);
        int i = 0;
        for (Shard shard : shards) {
            final int j = ++i;
            this.submit(() -> {
                List<Vertex> vertices = retry(
                             () -> this.client.traverser().vertices(shard),
                             "backing up vertices");
                if (vertices == null || vertices.isEmpty()) {
                    return;
                }
                this.vertexCounter.getAndAdd(vertices.size());
                String filename = prefix + (j % threadsNum());
                this.write(filename, HugeType.VERTEX, vertices);
            });
        }
        this.awaitTasks();
    }

    private void backupEdges(String prefix) {
        List<Shard> shards = this.client.traverser().edgeShards(SPLIT_SIZE);
        int i = 0;
        for (Shard shard : shards) {
            final int j = ++i;
            this.submit(() -> {
                List<Edge> edges = retry(
                        () -> this.client.traverser().edges(shard),
                        "backing up edges");
                if (edges == null || edges.isEmpty()) {
                    return;
                }
                this.edgeCounter.getAndAdd(edges.size());
                String filename = prefix + (j % threadsNum());
                this.write(filename, HugeType.EDGE, edges);
            });
        }
        this.awaitTasks();
    }

    private void backupPropertyKeys(String filename) {
        List<PropertyKey> pks = this.client.schema().getPropertyKeys();
        this.propertyKeyCounter.getAndAdd(pks.size());
        this.write(filename, HugeType.PROPERTY_KEY, pks);
    }

    private void backupVertexLabels(String filename) {
        List<VertexLabel> vls = this.client.schema().getVertexLabels();
        this.vertexLabelCounter.getAndAdd(vls.size());
        this.write(filename, HugeType.VERTEX_LABEL, vls);
    }

    private void backupEdgeLabels(String filename) {
        List<EdgeLabel> els = this.client.schema().getEdgeLabels();
        this.edgeLabelCounter.getAndAdd(els.size());
        this.write(filename, HugeType.EDGE_LABEL, els);
    }

    private void backupIndexLabels(String filename) {
        List<IndexLabel> ils = this.client.schema().getIndexLabels();
        this.indexLabelCounter.getAndAdd(ils.size());
        this.write(filename, HugeType.INDEX_LABEL, ils);
    }

    private void write(String file, HugeType type, List<?> list) {
        Lock lock = locks.lock(file);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(LBUF_SIZE);
             FileOutputStream fos = new FileOutputStream(file, true)) {
            String key = String.format("{\"%s\": ", type.string());
            baos.write(key.getBytes(API.CHARSET));
            this.client.mapper().writeValue(baos, list);
            baos.write("}\n".getBytes(API.CHARSET));
            fos.write(baos.toByteArray());
        } catch (Exception e) {
            throw new ClientException("Failed to serialize %s",
                                      e, type.string());
        } finally {
            lock.unlock();
        }
    }
}
