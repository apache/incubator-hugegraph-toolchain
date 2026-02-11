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

package org.apache.hugegraph.structure.constant;

/**
 * GraphMode defines the operational modes of a HugeGraph instance.
 * Different modes have different permissions for schema and vertex ID creation.
 */
public enum GraphMode {

    /**
     * NONE mode is the default regular mode for normal graph operations.
     * Restrictions:
     * 1. Not allowed to create schema with specified ID
     * 2. Not allowed to create vertex with custom ID for AUTOMATIC ID strategy
     * Use case: Daily graph database operations
     */
    NONE(1, "none"),

    /**
     * RESTORING mode is used to restore schema and graph data to a new graph.
     * This mode allows full control over IDs during restoration.
     * Permissions:
     * 1. Allowed to create schema with specified ID
     * 2. Allowed to create vertex with custom ID for AUTOMATIC ID strategy
     * Use case: Database backup recovery, graph migration
     */
    RESTORING(2, "restoring"),

    /**
     * MERGING mode is used to merge schema and graph data into an existing graph.
     * This mode allows vertex ID control but not schema ID control to avoid conflicts.
     * Permissions:
     * 1. Not allowed to create schema with specified ID (to prevent conflicts)
     * 2. Allowed to create vertex with custom ID for AUTOMATIC ID strategy
     * Use case: Data merging, incremental data import
     */
    MERGING(3, "merging"),

    /**
     * LOADING mode is used for bulk data loading via hugegraph-loader.
     * This mode is optimized for high-throughput data ingestion.
     * Use case: Bulk data import operations
     */
    LOADING(4, "loading");

    private final byte code;
    private final String name;

    GraphMode(int code, String name) {
        assert code < 256;
        this.code = (byte) code;
        this.name = name;
    }

    public byte code() {
        return this.code;
    }

    public String string() {
        return this.name;
    }

    /**
     * Check if the graph is in maintenance mode (RESTORING or MERGING).
     * In maintenance mode, the graph allows creating vertices with custom IDs.
     *
     * @return true if mode is RESTORING or MERGING
     */
    public boolean maintaining() {
        return this == RESTORING || this == MERGING;
    }

    /**
     * Check if the graph is in loading mode.
     * Loading mode is optimized for bulk data import operations.
     *
     * @return true if mode is LOADING
     */
    public boolean loading() {
        return this == LOADING;
    }
}
