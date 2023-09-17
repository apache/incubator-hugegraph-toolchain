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

package org.apache.hugegraph.spark.connector.client;

import java.io.Serializable;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.spark.connector.exception.LoadException;
import org.apache.hugegraph.spark.connector.options.HGOptions;
import org.apache.hugegraph.structure.constant.GraphMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HGLoadContext implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(HGLoadContext.class);

    private final HGOptions options;

    private final HugeClient client;

    private final SchemaCache schemaCache;

    public HGLoadContext(HGOptions options) {
        this.options = options;
        this.client = HGClientHolder.create(options);
        this.schemaCache = new SchemaCache(this.client);
    }

    public HGOptions options() {
        return this.options;
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
            throw new LoadException(String.format("Failed to unset mode %s for server",
                                                  GraphMode.LOADING), e);
        }
    }

    public void close() {
        LOG.info("Write load progress successfully");
        this.client.close();
        LOG.info("Close HugeClient successfully");
    }
}
