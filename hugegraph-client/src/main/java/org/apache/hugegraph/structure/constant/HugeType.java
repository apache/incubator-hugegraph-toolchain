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

public enum HugeType {

    // Schema
    VERTEX_LABEL(1, "vertexlabels"),
    EDGE_LABEL(2, "edgelabels"),
    PROPERTY_KEY(3, "propertykeys"),
    INDEX_LABEL(4, "indexlabels"),

    // Auth
    TARGET(50, "targets"),
    GROUP(51, "groups"),
    USER(52, "users"),
    ACCESS(53, "accesses"),
    BELONG(54, "belongs"),
    PROJECT(55, "projects"),
    LOGIN(56, "login"),
    LOGOUT(57, "logout"),
    TOKEN_VERIFY(58, "verify"),

    // Data
    VERTEX(101, "vertices"),
    EDGE(120, "edges"),

    // Variables
    VARIABLES(130, "variables"),

    // Task
    TASK(140, "tasks"),

    // Job
    JOB(150, "jobs"),

    // Gremlin
    GREMLIN(201, "gremlin"),

    // Cypher
    CYPHER(202, "cypher"),

    GRAPHS(220, "graphs"),

    // Version
    VERSION(230, "versions"),

    // Metrics
    METRICS(240, "metrics");

    private final int code;
    private final String name;

    HugeType(int code, String name) {
        assert code < 256;
        this.code = code;
        this.name = name;
    }

    public int code() {
        return this.code;
    }

    public String string() {
        return this.name;
    }
}
