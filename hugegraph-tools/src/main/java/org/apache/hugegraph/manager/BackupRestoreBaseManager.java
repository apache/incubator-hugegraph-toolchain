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

package org.apache.hugegraph.manager;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.base.Directory;
import org.apache.hugegraph.base.HdfsDirectory;
import org.apache.hugegraph.base.LocalDirectory;
import org.apache.hugegraph.base.Printer;
import org.apache.hugegraph.base.RetryManager;
import org.apache.hugegraph.base.ToolClient;
import org.apache.hugegraph.cmd.SubCommands;
import org.apache.hugegraph.concurrent.KeyLock;
import org.apache.hugegraph.exception.ToolsException;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableMap;

public class BackupRestoreBaseManager extends RetryManager {

    public static final int BATCH = 500;
    protected static final int LBUF_SIZE = 1024;

    protected AtomicLong propertyKeyCounter = new AtomicLong(0);
    protected AtomicLong vertexLabelCounter = new AtomicLong(0);
    protected AtomicLong edgeLabelCounter = new AtomicLong(0);
    protected AtomicLong indexLabelCounter = new AtomicLong(0);
    protected AtomicLong vertexCounter = new AtomicLong(0);
    protected AtomicLong edgeCounter = new AtomicLong(0);

    private long startTime = 0L;
    protected static KeyLock locks = new KeyLock();
    private String logDir;
    protected Directory directory;
    private Map<String, OutputStream> outputStreams;
    private Map<String, InputStream> inputStreams;

    public BackupRestoreBaseManager(ToolClient.ConnectionInfo info,
                                    String type) {
        super(info, type);
        this.outputStreams = new ConcurrentHashMap<>();
        this.inputStreams = new ConcurrentHashMap<>();
    }

    public void init(SubCommands.BackupRestore cmd) {
        this.threadsNum(cmd.threadsNum());
        assert cmd.retry() > 0;
        this.retry(cmd.retry());
        LocalDirectory.ensureDirectoryExist(cmd.logDir());
        this.logDir(cmd.logDir());
        this.directory(cmd.directory(), cmd.hdfsConf());
        this.initExecutors();
    }

    public void logDir(String logDir) {
        this.logDir = logDir;
    }

    public String logDir() {
        return this.logDir;
    }

    public Directory directory() {
        return this.directory;
    }

    protected void ensureDirectoryExist(boolean create) {
        this.directory.ensureDirectoryExist(create);
    }

    protected void removeDirectory() {
        this.directory.removeDirectory();
    }

    protected long write(String path, HugeType type,
                         List<?> list, boolean compress) {
        OutputStream os = this.outputStream(path, compress);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(LBUF_SIZE);
        try {
            String key = String.format("{\"%s\": ", type.string());
            baos.write(key.getBytes(API.CHARSET));
            this.client.mapper().writeValue(baos, list);
            baos.write("}\n".getBytes(API.CHARSET));
            os.write(baos.toByteArray());
        } catch (Throwable e) {
            throw new ToolsException("Failed to serialize %s to %s",
                                     e, type, path);
        }
        return list.size();
    }

    protected long write(String path, HugeType type, List<?> list,
                         boolean compress, String format,
                         String label, boolean allProperties,
                         List<String> properties) {
        // format == null when called by dump
        if (format == null || format.equals("json")) {
            return this.write(path, type, list, compress);
        }

        assert format.equals("text");
        return this.writeText(path, type, list, compress, label,
                              allProperties, properties);
    }

    protected long writeText(String path, HugeType type, List<?> list,
                             boolean compress, String label,
                             boolean allProperties, List<String> properties) {
        OutputStream os = this.outputStream(path, compress);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(LBUF_SIZE);
        StringBuilder builder = new StringBuilder(LBUF_SIZE);
        long count = 0L;
        try {
            for (Object e : list) {
                GraphElement element = (GraphElement) e;
                if (label != null && !label.equals(element.label())) {
                    continue;
                }
                count++;
                if (type == HugeType.VERTEX) {
                    builder.append(element.id()).append("\t");
                } else {
                    Edge edge = (Edge) e;
                    builder.append(edge.sourceId()).append("\t")
                           .append(edge.targetId()).append("\t");
                }
                if (allProperties) {
                    for (Object value : element.properties().values()) {
                        builder.append(value).append(",");
                    }
                } else {
                    for (String property : properties) {
                        builder.append(element.property(property)).append(",");
                    }
                }
                builder.setCharAt(builder.length() - 1, '\n');
            }
            baos.write(builder.toString().getBytes(API.CHARSET));
            os.write(baos.toByteArray());
        } catch (Throwable e) {
            throw new ToolsException("Failed to serialize %s to %s",
                                     e, type, path);
        }
        return count;
    }

    protected void read(String file, HugeType type,
                        BiConsumer<String, String> consumer) {
        InputStream is = this.inputStream(file);
        try (InputStreamReader isr = new InputStreamReader(is, API.CHARSET);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(type.string(), line);
            }
        } catch (IOException e) {
            throw new ToolsException("Failed to deserialize %s from %s",
                                     e, type, file);
        }
    }

    protected OutputStream outputStream(String file, boolean compress) {
        OutputStream os = this.outputStreams.get(file);
        if (os != null) {
            return os;
        }
        os = this.directory.outputStream(file, compress, true);
        OutputStream prev = this.outputStreams.putIfAbsent(file, os);
        if (prev != null) {
            Directory.closeAndIgnoreException(os);
            os = prev;
        }
        return os;
    }

    protected InputStream inputStream(String file) {
        InputStream is = this.inputStreams.get(file);
        if (is != null) {
            return is;
        }
        is = this.directory.inputStream(file);
        InputStream prev = this.inputStreams.putIfAbsent(file, is);
        if (prev != null) {
            Directory.closeAndIgnoreException(is);
            is = prev;
        }
        return is;
    }

    protected void directory(String dir, Map<String, String> hdfsConf) {
        if (hdfsConf == null || hdfsConf.isEmpty()) {
            // Local FS directory
            this.directory = LocalDirectory.constructDir(dir, this.graph());
        } else {
            // HDFS directory
            this.directory = HdfsDirectory.constructDir(dir, this.graph(),
                                                        hdfsConf);
        }
    }

    protected String fileWithPrefix(HugeType type) {
        List<String> files = this.filesWithPrefix(type);
        E.checkState(files.size() == 1,
                     "There should be only one file of '%s', but got '%s'",
                     type, files.size());
        return files.get(0);
    }

    protected List<String> filesWithPrefix(HugeType type) {
        List<String> files = new ArrayList<>();
        for (String file : this.directory.files()) {
            if (file.startsWith(type.string())) {
                files.add(file);
            }
        }
        return files;
    }

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public long elapseSeconds() {
        E.checkState(this.startTime != 0,
                     "Must call startTimer() to set start time, " +
                     "before call elapse()");
        return (System.currentTimeMillis() - this.startTime) / 1000;
    }

    protected void printSummary() {
        this.printSummary(this.type());
    }

    protected void printSummary(String type) {
        Printer.print("===============================================");
        Map<String, Long> summary = ImmutableMap.<String, Long>builder()
                .put("property key number", this.propertyKeyCounter.longValue())
                .put("vertex label number", this.vertexLabelCounter.longValue())
                .put("edge label number", this.edgeLabelCounter.longValue())
                .put("index label number", this.indexLabelCounter.longValue())
                .put("vertex number", this.vertexCounter.longValue())
                .put("edge number", this.edgeCounter.longValue()).build();
        Printer.printMap(type + " summary", summary);

        Printer.printKV("cost time(s)", this.elapseSeconds());
    }

    @Override
    public void shutdown(String taskType) {
        super.shutdown(taskType);
        for (Map.Entry<String, OutputStream> e : this.outputStreams.entrySet()) {
            try {
                OutputStream os = e.getValue();
                os.close();
            } catch (IOException exception) {
                Printer.print("Failed to close file '%s'", e.getKey());
            }
        }
        for (Map.Entry<String, InputStream> e : this.inputStreams.entrySet()) {
            try {
                InputStream is = e.getValue();
                is.close();
            } catch (IOException exception) {
                Printer.print("Failed to close file '%s'", e.getKey());
            }
        }
    }
}
