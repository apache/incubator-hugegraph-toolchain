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

package org.apache.hugegraph.loader.executor;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.progress.LoadProgress;
import org.apache.hugegraph.loader.util.DateUtil;
import org.apache.hugegraph.loader.util.HugeClientHolder;
import org.slf4j.Logger;

import lombok.SneakyThrows;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.failure.FailLogger;
import org.apache.hugegraph.loader.filter.ElementParseGroup;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.metrics.LoadSummary;
import org.apache.hugegraph.structure.LoadingModeOwner;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.apache.hugegraph.util.Log;

public final class LoadContext implements Cloneable {

    private static final Logger LOG = Log.logger(LoadContext.class);

    private enum StopReason {
        READ_LIMIT,
        OTHER
    }

    // The time at the beginning of loading, accurate to seconds
    private final String timestamp;

    private volatile boolean closed;
    // Only a successful LOADING request permits this context to reset it.
    private boolean loadingModeSet;
    // Retain the tuple before acquire, including an unknown acquire outcome.
    private LoadingModeOwner loadingOwner;
    private boolean loadingOwnerReleased;
    private volatile StopReason stopReason;
    private volatile boolean noError;
    private volatile Throwable failure;
    private final LoadOptions options;
    private final LoadSummary summary;
    // The old progress just used to read
    private final LoadProgress oldProgress;
    private final LoadProgress newProgress;
    // Each input mapping corresponds to a FailLogger
    private final Map<String, FailLogger> loggers;

    private final HugeClient client;
    // Non-direct mode client
    private final HugeClient indirectClient;
    private final SchemaCache schemaCache;
    private final ElementParseGroup parseGroup;

    public LoadContext(LoadOptions options) {
        this(options, null);
    }

    /**
     * Create a file parser context without creating server clients.
     * Supply a complete schema cache with no server client, such as one parsed from JSON.
     * Server clients are null and close() does not save loader progress.
     */
    public static LoadContext forOffline(LoadOptions options, SchemaCache schemaCache) {
        return new LoadContext(Objects.requireNonNull(options, "options"),
                               Objects.requireNonNull(schemaCache, "schemaCache"));
    }

    @SneakyThrows
    private LoadContext(LoadOptions options, SchemaCache schemaCache) {
        this.timestamp = DateUtil.now("yyyyMMdd-HHmmss");
        this.closed = false;
        this.stopReason = null;
        this.noError = true;
        this.options = options;
        this.summary = new LoadSummary();
        this.oldProgress = LoadProgress.parse(options);
        this.newProgress = new LoadProgress();
        this.loggers = new ConcurrentHashMap<>();
        this.client = schemaCache == null ? HugeClientHolder.create(options) : null;
        if (this.client != null && this.options.direct) {
            // options implements ShallowClone
            LoadOptions indirectOptions = (LoadOptions) options.clone();
            indirectOptions.direct = false;
            this.indirectClient = HugeClientHolder.create(indirectOptions);
        } else {
            this.indirectClient = this.client;
        }
        this.schemaCache = schemaCache == null ? new SchemaCache(this.client) : schemaCache;
        this.parseGroup = ElementParseGroup.create(options);
    }

    public String timestamp() {
        return this.timestamp;
    }

    public boolean closed() {
        return this.closed;
    }

    public boolean stopped() {
        return this.stopReason != null;
    }

    public synchronized void stopLoading() {
        this.stopReason = StopReason.OTHER;
    }

    public synchronized void stopLoadingAtReadLimit() {
        if (this.stopReason == null) {
            this.stopReason = this.noError ? StopReason.READ_LIMIT : StopReason.OTHER;
        }
    }

    public boolean stoppedAtReadLimit() {
        return this.stopReason == StopReason.READ_LIMIT;
    }

    public boolean noError() {
        return this.noError;
    }

    public synchronized void occurredError() {
        this.noError = false;
        if (this.stopReason == StopReason.READ_LIMIT) {
            this.stopReason = StopReason.OTHER;
        }
    }

    public synchronized void failLoading(Throwable failure) {
        this.occurredError();
        this.stopLoading();
        if (this.failure == null) {
            this.failure = failure;
        }
    }

    public void throwIfFailed() {
        if (this.failure instanceof RuntimeException) {
            throw (RuntimeException) this.failure;
        }
        if (this.failure != null) {
            throw new LoadException("Loading failed without recoverable progress", this.failure);
        }
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

    public HugeClient indirectClient() {
        return this.indirectClient;
    }

    public ElementParseGroup filterGroup() {
        return parseGroup;
    }

    public SchemaCache schemaCache() {
        return this.schemaCache;
    }

    public void updateSchemaCache() {
        assert this.client != null;
        this.schemaCache.updateAll();
    }

    @SneakyThrows
    public synchronized void setLoadingMode() {
        String graph = this.client.graph().graph();
        if (this.loadingOwnerReleased) {
            throw new LoadException("Loading owner has already been released; create a new context");
        }
        if (this.loadingOwner == null) {
            try {
                this.loadingOwner = this.client.graphs().newLoadingOwner(graph);
                LOG.info("Preparing LOADING owner {} for graph '{}'; after an unclean exit, stop related imports " +
                         "and use the Server loading-owner recovery procedure", this.loadingOwner, graph);
            } catch (ServerException e) {
                if (e.status() != 404 || !"HTTP 404 Not Found".equals(e.getMessage()) ||
                    !("jakarta.ws.rs.NotFoundException".equals(e.exception()) ||
                      "javax.ws.rs.NotFoundException".equals(e.exception()) ||
                      "class jakarta.ws.rs.NotFoundException".equals(e.exception()) ||
                      "class javax.ws.rs.NotFoundException".equals(e.exception()))) {
                    throw e;
                }
                LOG.warn("Server has no loading-owner endpoint; use legacy mode without concurrent-import protection");
            }
        }
        if (this.loadingOwner != null) {
            this.client.graphs().acquireLoadingOwner(graph, this.loadingOwner);
            return;
        }
        try {
            this.client.graphs().mode(graph, GraphMode.LOADING);
            this.loadingModeSet = true;
        } catch (ServerException e) {
            if (e.getMessage().contains("Can not deserialize value of type")) {
                LOG.warn("HugeGraphServer doesn't support loading mode");
            } else {
                throw e;
            }
        } catch (Exception e) {
            LOG.error("Failed to confirm LOADING mode for graph '{}'; it may still be in LOADING mode. " +
                      "Confirm no imports are active, inspect the graph mode, and manually reset it to NONE if needed.",
                      graph);
            throw e;
        }
    }

    public void setRestoreMode() {
        String graph = this.client.graph().graph();
        try {
            this.client.graphs().mode(graph, GraphMode.RESTORING);
        } catch (ServerException e) {
            if (e.getMessage().contains("Can not deserialize value of type")) {
                LOG.warn("HugeGraphServer doesn't support loading mode");
            } else {
                throw e;
            }
        }
    }

    public synchronized void unsetLoadingMode() {
        if (this.loadingOwner != null) {
            if (!this.loadingOwnerReleased) {
                try {
                    this.client.graphs().releaseLoadingOwner(this.client.graph().graph(), this.loadingOwner);
                    this.loadingOwnerReleased = true;
                } catch (Exception e) {
                    throw new LoadException("Failed to release LOADING owner %s; stop and drain related imports " +
                                            "before administrator recovery on the Server", e, this.loadingOwner);
                }
            }
            return;
        }
        if (!this.loadingModeSet) {
            return;
        }
        try {
            String graph = this.client.graph().graph();
            GraphMode mode = this.client.graphs().mode(graph);
            if (mode.loading()) {
                this.client.graphs().mode(graph, GraphMode.NONE);
            }
            this.loadingModeSet = false;
        } catch (Exception e) {
            throw new LoadException("Failed to unset mode %s for server",
                                    e, GraphMode.LOADING);
        }
    }

    public void close() {
        this.close(true);
    }

    public void close(boolean saveProgress) {
        if (this.closed) {
            return;
        }
        try (HugeClient secondary = this.indirectClient != this.client ? this.indirectClient : null;
             HugeClient primary = this.client) {
            RuntimeException loggerFailure = null;
            for (FailLogger logger : this.loggers.values()) {
                try {
                    logger.close();
                } catch (RuntimeException e) {
                    if (loggerFailure == null) {
                        loggerFailure = e;
                    } else {
                        loggerFailure.addSuppressed(e);
                    }
                }
            }
            if (loggerFailure != null) {
                throw loggerFailure;
            }
            LOG.info("Close all failure loggers successfully");

            this.throwIfFailed();
            this.newProgress.plusVertexLoaded(this.summary.vertexLoaded());
            this.newProgress.plusEdgeLoaded(this.summary.edgeLoaded());
            if (this.client != null && saveProgress) {
                try {
                    this.newProgress.write(this);
                } catch (IOException e) {
                    this.occurredError();
                    throw new LoadException("Failed to write load progress", e);
                }
                LOG.info("Write load progress successfully");
            }
        } catch (RuntimeException e) {
            this.occurredError();
            throw e;
        } finally {
            this.closed = true;
        }
        LOG.info("Close loader clients successfully");
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        LoadContext context = (LoadContext) super.clone();
        context.loadingModeSet = false;
        context.loadingOwner = null;
        context.loadingOwnerReleased = false;
        return context;
    }
}
