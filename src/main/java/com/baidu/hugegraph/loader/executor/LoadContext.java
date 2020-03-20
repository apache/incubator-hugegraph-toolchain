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

import com.baidu.hugegraph.loader.builder.SchemaCache;
import com.baidu.hugegraph.loader.failure.FailLogger;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.metrics.LoadSummary;
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.loader.util.DateUtil;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public final class LoadContext {

    private static final Logger LOG = Log.logger(LoadContext.class);

    private static volatile LoadContext instance;

    // The time at the beginning of loading, accurate to seconds
    private final String timestamp;

    private volatile boolean stopped;
    private final LoadOptions options;
    private final LoadSummary summary;
    // The old progress just used to read
    private final LoadProgress oldProgress;
    private final LoadProgress newProgress;
    // Each input mapping corresponds to a FailLogger
    private final Map<String, FailLogger> loggers;

    private SchemaCache schemaCache;

    public static synchronized LoadContext init(LoadOptions options) {
        if (instance == null) {
            instance = new LoadContext(options);
        }
        return instance;
    }

    public static synchronized void destroy() {
        if (instance != null) {
            try {
                instance.close();
            } finally {
                instance = null;
            }
        }
    }

    public static LoadContext get() {
        E.checkState(instance != null,
                     "LoadContext must be initialized firstly");
        return instance;
    }

    private LoadContext(LoadOptions options) {
        this.timestamp = DateUtil.now("yyyyMMdd-HHmmss");
        this.stopped = false;
        this.options = options;
        this.summary = new LoadSummary();
        this.oldProgress = LoadProgress.parse(this.options);
        this.newProgress = new LoadProgress();
        this.loggers = new ConcurrentHashMap<>();
        this.schemaCache = null;
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

    public void schemaCache(SchemaCache cache) {
        this.schemaCache = cache;
    }

    public SchemaCache schemaCache() {
        return this.schemaCache;
    }

    public void close() {
        LOG.info("Ready to close failure loggers");
        for (FailLogger logger : this.loggers.values()) {
            logger.close();
        }
        LOG.info("Successfully close all failure loggers");

        LOG.info("Ready to write load progress");
        try {
            this.newProgress().write(this);
        } catch (IOException e) {
            LOG.error("Failed to write load progress", e);
        }
        LOG.info("Successfully write load progress");

        LOG.info("Ready to close HugeClient");
        HugeClientHolder.close();
        LOG.info("Successfully close HugeClient");
    }
}
