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

package com.baidu.hugegraph.loader.executor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.exception.ServerException;
import com.baidu.hugegraph.loader.builder.SchemaCache;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.failure.FailLogger;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.loader.util.DateUtil;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.structure.constant.GraphMode;
import com.baidu.hugegraph.util.Log;

public final class LoadContext {

    private static final Logger LOG = Log.logger(LoadContext.class);

    // The time at the beginning of loading, accurate to seconds
    private final String timestamp;

    private volatile boolean closed;
    private volatile boolean stopped;
    private volatile boolean noError;
    private final LoadOptions options;
    private final LoadSummary summary;
    // The old progress just used to read
    private final LoadProgress oldProgress;
    private final LoadProgress newProgress;
    // Each input mapping corresponds to a FailLogger
    private final Map<String, FailLogger> loggers;

    private final HugeClient client;
    private final SchemaCache schemaCache;

    public LoadContext(LoadOptions options) {
        this.timestamp = DateUtil.now("yyyyMMdd-HHmmss");
        this.closed = false;
        this.stopped = false;
        this.noError = true;
        this.options = options;
        this.summary = new LoadSummary();
        this.oldProgress = LoadProgress.parse(options);
        this.newProgress = new LoadProgress();
        this.loggers = new ConcurrentHashMap<>();
        this.client = HugeClientHolder.create(options);
        this.schemaCache = new SchemaCache(this.client);
    }

    public LoadContext(ComputerLoadOptions options) {
        this.timestamp = DateUtil.now("yyyyMMdd-HHmmss");
        this.closed = false;
        this.stopped = false;
        this.noError = true;
        this.options = options;
        this.summary = new LoadSummary();
        this.oldProgress = LoadProgress.parse(options);
        this.newProgress = new LoadProgress();
        this.loggers = new ConcurrentHashMap<>();
        this.client = null;
        this.schemaCache = options.schemaCache();
    }

    public String timestamp() {
        return this.timestamp;
    }

    public boolean closed() {
        return this.closed;
    }

    public boolean stopped() {
        return this.stopped;
    }

    public void stopLoading() {
        this.stopped = true;
    }

    public boolean noError() {
        return this.noError;
    }

    public void occuredError() {
        this.noError = false;
    }

    public LoadOptions options() {
        return this.options;
    }

    public LoadSummary summary() {
        return this.summary;
    }

    public LoadProgress oldProgress() {
        return this.oldProgress;
    }

    public LoadProgress newProgress() {
        return this.newProgress;
    }

    public FailLogger failureLogger(InputStruct struct) {
        return this.loggers.computeIfAbsent(struct.id(), k -> {
            LOG.info("Create failure logger for mapping '{}'", struct);
            return new FailLogger(this, struct);
        });
    }

    public HugeClient client() {
        return this.client;
    }

    public SchemaCache schemaCache() {
        return this.schemaCache;
    }

    public void updateSchemaCache() {
        assert this.client != null;
        this.schemaCache.updateAll();
    }

    public void setLoadingMode() {
        String graph = this.client.graph().graph();
        try {
            this.client.graphs().mode(graph, GraphMode.LOADING);
        } catch (ServerException e) {
            if (e.getMessage().contains("Can not deserialize value of type")) {
                LOG.warn("HugeGraphServer doesn't support loading mode");
            } else {
                throw e;
            }
        }
    }

    public void unsetLoadingMode() {
        try {
            String graph = this.client.graph().graph();
            GraphMode mode = this.client.graphs().mode(graph);
            if (mode.loading()) {
                this.client.graphs().mode(graph, GraphMode.NONE);
            }
        } catch (Exception e) {
            throw new LoadException("Failed to unset mode %s for server",
                                    e, GraphMode.LOADING);
        }
    }

    public void close() {
        if (this.closed) {
            return;
        }
        for (FailLogger logger : this.loggers.values()) {
            logger.close();
        }
        LOG.info("Close all failure loggers successfully");

        this.newProgress.plusVertexLoaded(this.summary.vertexLoaded());
        this.newProgress.plusEdgeLoaded(this.summary.edgeLoaded());
        try {
            this.newProgress.write(this);
        } catch (IOException e) {
            LOG.error("Failed to write load progress", e);
        }
        LOG.info("Write load progress successfully");

        this.client.close();
        LOG.info("Close HugeClient successfully");
        this.closed = true;
    }
}
