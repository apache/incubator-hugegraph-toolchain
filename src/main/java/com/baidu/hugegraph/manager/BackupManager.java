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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.base.Printer;
import com.baidu.hugegraph.base.ToolClient;
import com.baidu.hugegraph.cmd.SubCommands;
import com.baidu.hugegraph.driver.TraverserManager;
import com.baidu.hugegraph.exception.ToolsException;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Edges;
import com.baidu.hugegraph.structure.graph.Shard;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.structure.graph.Vertices;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.baidu.hugegraph.util.E;

import jersey.repackaged.com.google.common.collect.ImmutableList;

public class BackupManager extends BackupRestoreBaseManager {

    private static final String ALL_SHARDS = "_all_shards";
    private static final String TIMEOUT_SHARDS = "_timeout_shards";
    private static final String LIMIT_EXCEED_SHARDS = "_limit_exceed_shards";
    private static final String FAILED_SHARDS = "_failed_shards";

    public static final int BACKUP_DEFAULT_TIMEOUT = 120;

    private static final AtomicInteger nextId = new AtomicInteger(0);
    private static final ThreadLocal<Integer> suffix =
            ThreadLocal.withInitial(nextId::getAndIncrement);

    private long splitSize;

    public BackupManager(ToolClient.ConnectionInfo info) {
        super(info, "backup");
    }

    public void init(SubCommands.Backup backup) {
        super.init(backup);
        this.ensureDirectoryExist(true);
        long splitSize = backup.splitSize();
        E.checkArgument(splitSize >= 1024 * 1024,
                        "Split size must >= 1M, but got %s", splitSize);
        this.splitSize(splitSize);
    }

    public void splitSize(long splitSize) {
        this.splitSize = splitSize;
    }

    public long splitSize() {
        return this.splitSize;
    }

    public void backup(List<HugeType> types) {
        this.startTimer();
        for (HugeType type : types) {
            switch (type) {
                case VERTEX:
                    this.backupVertices();
                    break;
                case EDGE:
                    this.backupEdges();
                    break;
                case PROPERTY_KEY:
                    this.backupPropertyKeys();
                    break;
                case VERTEX_LABEL:
                    this.backupVertexLabels();
                    break;
                case EDGE_LABEL:
                    this.backupEdgeLabels();
                    break;
                case INDEX_LABEL:
                    this.backupIndexLabels();
                    break;
                default:
                    throw new AssertionError(String.format(
                              "Bad backup type: %s", type));
            }
        }
        this.shutdown(this.type());
        this.printSummary();
    }

    protected void backupVertices() {
        Printer.print("Vertices backup started");
        Printer.printInBackward("Vertices has been backup: ");
        List<Shard> shards = retry(() ->
                             this.client.traverser().vertexShards(splitSize()),
                             "querying shards of vertices");
        this.writeShards(this.allShardsLog(HugeType.VERTEX), shards);
        for (Shard shard : shards) {
            this.backupVertexShardAsync(shard);
        }
        this.awaitTasks();
        this.postProcessFailedShard(HugeType.VERTEX);
        Printer.print("%d", this.vertexCounter.get());
        Printer.print("Vertices backup finished: %d",
                      this.vertexCounter.get());
    }

    protected void backupEdges() {
        Printer.print("Edges backup started");
        Printer.printInBackward("Edges has been backup: ");
        List<Shard> shards = retry(() ->
                             this.client.traverser().edgeShards(splitSize()),
                             "querying shards of edges");
        this.writeShards(this.allShardsLog(HugeType.EDGE), shards);
        for (Shard shard : shards) {
            this.backupEdgeShardAsync(shard);
        }
        this.awaitTasks();
        this.postProcessFailedShard(HugeType.EDGE);
        Printer.print("%d", this.edgeCounter.get());
        Printer.print("Edges backup finished: %d", this.edgeCounter.get());
    }

    protected void backupPropertyKeys() {
        Printer.print("Property key backup started");
        List<PropertyKey> pks = this.client.schema().getPropertyKeys();
        this.propertyKeyCounter.getAndAdd(pks.size());
        this.backup(HugeType.PROPERTY_KEY, pks);
        Printer.print("Property key backup finished: %d",
                      this.propertyKeyCounter.get());
    }

    protected void backupVertexLabels() {
        Printer.print("Vertex label backup started");
        List<VertexLabel> vls = this.client.schema().getVertexLabels();
        this.vertexLabelCounter.getAndAdd(vls.size());
        this.backup(HugeType.VERTEX_LABEL, vls);
        Printer.print("Vertex label backup finished: %d",
                      this.vertexLabelCounter.get());
    }

    protected void backupEdgeLabels() {
        Printer.print("Edge label backup started");
        List<EdgeLabel> els = this.client.schema().getEdgeLabels();
        this.edgeLabelCounter.getAndAdd(els.size());
        this.backup(HugeType.EDGE_LABEL, els);
        Printer.print("Edge label backup finished: %d",
                      this.edgeLabelCounter.get());
    }

    protected void backupIndexLabels() {
        Printer.print("Index label backup started");
        List<IndexLabel> ils = this.client.schema().getIndexLabels();
        this.indexLabelCounter.getAndAdd(ils.size());
        this.backup(HugeType.INDEX_LABEL, ils);
        Printer.print("Index label backup finished: %d",
                      this.indexLabelCounter.get());
    }

    private void backupVertexShardAsync(Shard shard) {
        this.submit(() -> {
            try {
                backupVertexShard(shard);
            } catch (Throwable e) {
                this.logExceptionWithShard(e, HugeType.VERTEX, shard);
            }
        });
    }

    private void backupEdgeShardAsync(Shard shard) {
        this.submit(() -> {
            try {
                backupEdgeShard(shard);
            } catch (Throwable e) {
                this.logExceptionWithShard(e, HugeType.EDGE, shard);
            }
        });
    }

    private void backupVertexShard(Shard shard) {
        String desc = String.format("backing up vertices[shard:%s]", shard);
        Vertices vertices = null;
        String page = "";
        TraverserManager g = client.traverser();
        do {
            String finalPage = page;
            try {
                vertices = retry(() -> g.vertices(shard, finalPage), desc);
            } catch (ToolsException e) {
                this.exceptionHandler(e, HugeType.VERTEX, shard);
            }
            if (vertices == null) {
                return;
            }
            List<Vertex> vertexList = vertices.results();
            if (vertexList == null || vertexList.isEmpty()) {
                return;
            }
            this.backup(HugeType.VERTEX, suffix.get(), vertexList);

            this.vertexCounter.getAndAdd(vertexList.size());
            Printer.printInBackward(this.vertexCounter.get());
        } while ((page = vertices.page()) != null);
    }

    private void backupEdgeShard(Shard shard) {
        String desc = String.format("backing up edges[shard %s]", shard);
        Edges edges = null;
        List<Edge> edgeList;
        String page = "";
        do {
            try {
                String p = page;
                edges = retry(() -> client.traverser().edges(shard, p), desc);
            } catch (ToolsException e) {
                this.exceptionHandler(e, HugeType.EDGE, shard);
            }
            if (edges == null) {
                return;
            }
            edgeList = edges.results();
            if (edgeList == null || edgeList.isEmpty()) {
                return;
            }
            this.backup(HugeType.EDGE, suffix.get(), edgeList);

            this.edgeCounter.getAndAdd(edgeList.size());
            Printer.printInBackward(this.edgeCounter.get());
        } while ((page = edges.page()) != null);
    }

    private void backup(HugeType type, List<?> list) {
        String file = type.string();
        this.write(file, type, list);
    }

    private void backup(HugeType type, int number, List<?> list) {
        String file = type.string() + number;
        int size = list.size();
        for (int start = 0; start < size; start += BATCH) {
            int end = Math.min(start + BATCH, size);
            this.write(file, type, list.subList(start, end));
        }
    }

    private void exceptionHandler(ToolsException e, HugeType type,
                                  Shard shard) {
        String message = e.getMessage();
        switch (type) {
            case VERTEX:
                E.checkState(message.contains("backing up vertices"),
                             "Unexpected exception %s", e);
                break;
            case EDGE:
                E.checkState(message.contains("backing up edges"),
                             "Unexpected exception %s", e);
                break;
            default:
                throw new AssertionError(String.format(
                          "Only VERTEX or EDGE exception is expected, " +
                          "but got '%s' exception", type));
        }
        if (isLimitExceedException(e)) {
            this.logLimitExceedShard(type, shard);
        } else if (isTimeoutException(e)) {
            this.logTimeoutShard(type, shard);
        } else {
            this.logExceptionWithShard(e, type, shard);
        }
    }

    private void logTimeoutShard(HugeType type, Shard shard) {
        String file = type.string() + TIMEOUT_SHARDS;
        this.writeShard(Paths.get(this.logDir(), file).toString(), shard);
    }

    private void logLimitExceedShard(HugeType type, Shard shard) {
        String file = type.string() + LIMIT_EXCEED_SHARDS;
        this.writeShard(Paths.get(this.logDir(), file).toString(), shard);
    }

    private void logExceptionWithShard(Object e, HugeType type, Shard shard) {
        String fileName = type.string() + FAILED_SHARDS;
        String filePath = Paths.get(this.logDir(), fileName).toString();
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(shard.toString() + "\n");
            writer.write(exceptionStackTrace(e) + "\n");
        } catch (IOException e1) {
            Printer.print("Failed to write shard '%s' with exception '%s'",
                          shard, e);
        }
    }

    private void postProcessFailedShard(HugeType type) {
        this.processTimeoutShards(type);
        this.processLimitExceedShards(type);
    }

    private void processTimeoutShards(HugeType type) {
        Path path = Paths.get(this.logDir(), type.string() + TIMEOUT_SHARDS);
        File shardFile = path.toFile();
        if (!shardFile.exists() || shardFile.isDirectory()) {
            return;
        }
        Printer.print("Timeout occurs when backup %s shards in file '%s', " +
                      "try to use global option --timeout to increase " +
                      "connection timeout(default is 120s for backup) or use " +
                      "option --split-size to decrease split size",
                      type, shardFile);
    }

    private void processLimitExceedShards(HugeType type) {
        Path path = Paths.get(this.logDir(),
                              type.string() + LIMIT_EXCEED_SHARDS);
        File shardFile = path.toFile();
        if (!shardFile.exists() || shardFile.isDirectory()) {
            return;
        }
        Printer.print("Limit exceed occurs when backup %s shards in file '%s'",
                      type, shardFile);
    }

    private List<Shard> readShards(File file) {
        E.checkArgument(file.exists() && file.isFile() && file.canRead(),
                        "Need to specify a readable filter file rather than:" +
                        " %s", file.toString());
        List<Shard> shards = new ArrayList<>();
        try (InputStream is = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(is, API.CHARSET);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                shards.addAll(this.readList("shards", Shard.class, line));
            }
        } catch (IOException e) {
            throw new ToolsException("IOException occur while reading %s",
                                     e, file.getName());
        }
        return shards;
    }

    private void writeShard(String file, Shard shard) {
        this.writeShards(file, ImmutableList.of(shard));
    }

    private void writeShards(String file, List<Shard> shards) {
        this.writeLog(file, "shards", shards);
    }

    private void writeLog(String file, String type, List<?> list) {
        Lock lock = locks.lock(file);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(LBUF_SIZE);
             FileOutputStream fos = new FileOutputStream(file, false)) {
            String key = String.format("{\"%s\": ", type);
            baos.write(key.getBytes(API.CHARSET));
            this.client.mapper().writeValue(baos, list);
            baos.write("}\n".getBytes(API.CHARSET));
            fos.write(baos.toByteArray());
        } catch (Exception e) {
            Printer.print("Failed to serialize %s: %s", type, e);
        } finally {
            lock.unlock();
        }
    }

    private String allShardsLog(HugeType type) {
        String shardsFile = type.string() + ALL_SHARDS;
        return Paths.get(this.logDir(), shardsFile).toString();
    }

    private static boolean isTimeoutException(ToolsException e) {
        return e.getCause() != null && e.getCause().getCause() != null &&
               e.getCause().getCause().getMessage().contains("Read timed out");
    }

    private static boolean isLimitExceedException(ToolsException e) {
        return e.getCause() != null &&
               e.getCause().getMessage().contains("Too many records");
    }

    private static String exceptionStackTrace(Object e) {
        if (!(e instanceof Throwable)) {
            return e.toString();
        }
        Throwable t = (Throwable) e;
        StringBuilder sb = new StringBuilder();
        sb.append(t.getMessage()).append("\n");
        if (t.getCause() != null) {
            sb.append(t.getCause().toString()).append("\n");
        }
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append(element).append("\n");
        }
        return sb.toString();
    }
}
