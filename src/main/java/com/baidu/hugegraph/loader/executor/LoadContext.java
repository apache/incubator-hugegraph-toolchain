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
import com.baidu.hugegraph.loader.builder.SchemaCache;
import com.baidu.hugegraph.loader.failure.FailLogger;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.loader.util.DateUtil;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.util.Log;

public final class LoadContext {

    private static final Logger LOG = Log.logger(LoadContext.class);

    // The time at the beginning of loading, accurate to seconds
    private final String timestamp;

    private volatile boolean closed;
    private volatile boolean stopped;
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
        this.options = options;
        this.summary = new LoadSummary();
        this.oldProgress = LoadProgress.parse(options);
        this.newProgress = new LoadProgress();
        this.loggers = new ConcurrentHashMap<>();
        this.client = HugeClientHolder.create(options);
        this.schemaCache = new SchemaCache();
    }

    public String timestamp() {
        return this.timestamp;
    }

    public boolean stopped() {
        return this.stopped;
    }

    public void stopLoading() {
        this.stopped = true;
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
        this.schemaCache.updateAll(this.client);
    }

    public void close() {
        if (this.closed) {
            return;
        }
        LOG.info("Ready to close failure loggers");
        for (FailLogger logger : this.loggers.values()) {
            logger.close();
        }
        LOG.info("Close all failure loggers successfully");

        LOG.info("Ready to write load progress");
        this.newProgress.plusVertexLoaded(summary.vertexLoaded());
        this.newProgress.plusEdgeLoaded(summary.edgeLoaded());
        try {
            this.newProgress.write(this);
        } catch (IOException e) {
            LOG.error("Failed to write load progress", e);
        }
        LOG.info("Write load progress successfully");

        LOG.info("Ready to close HugeClient");
        this.client.close();
        LOG.info("Close HugeClient successfully");
        this.closed = true;
    }
}
