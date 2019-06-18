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

package com.baidu.hugegraph.loader.source.jdbc;

import com.baidu.hugegraph.loader.source.AbstractSource;
import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JDBCSource extends AbstractSource {

    @JsonProperty(value = "vendor")
    private JDBCVendor vendor;
    @JsonProperty("driver")
    private String driver;
    @JsonProperty(value = "url")
    private String url;
    @JsonProperty("database")
    private String database;
    @JsonProperty("schema")
    private String schema;
    @JsonProperty("table")
    private String table;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("reconnect_max_times")
    private int reconnectMaxTimes = 3;
    @JsonProperty("reconnect_interval")
    private int reconnectInterval = 3;
    @JsonProperty("batch_size")
    private int batchSize = 500;

    @Override
    public SourceType type() {
        return SourceType.JDBC;
    }

    @Override
    public void check() throws IllegalArgumentException {
        E.checkArgument(this.vendor != null, "The vendor can't be null");
        E.checkArgument(this.url != null, "The url can't be null");
        E.checkArgument(this.database != null, "The database can't be null");
        E.checkArgument(this.table != null, "The table can't be null");
        E.checkArgument(this.username != null, "The username can't be null");
        E.checkArgument(this.password != null, "The password can't be null");

        switch (this.vendor) {
            case MYSQL:
                if (this.schema != null) {
                    E.checkArgument(this.schema.equals(this.database),
                                    "The schema(%s) is allowed to not " +
                                    "specified in %s vendor, if specified, " +
                                    "it must be same as the database(%s)",
                                    this.schema, this.vendor, this.database);
                } else {
                    this.schema = this.vendor.defaultSchema(this);
                }
                break;
            case POSTGRESQL:
                // The default schema is "public"
                if (this.schema == null) {
                    this.schema = this.vendor.defaultSchema(this);
                }
                break;
            case ORACLE:
                // The default schema is uppercase of username
                if (this.schema == null) {
                    this.schema = this.vendor.defaultSchema(this);
                }
                break;
            case SQLSERVER:
                E.checkArgument(this.schema != null,
                                "The schema must be specified in %s vendor",
                                this.vendor);
                break;
            default:
                throw new AssertionError(String.format(
                          "Unsupported database vendor '%s'", vendor));
        }
        if (this.driver == null) {
            this.driver = this.vendor.defaultDriver();
        }
    }

    public JDBCVendor vendor() {
        return this.vendor;
    }

    public String driver() {
        return this.driver;
    }

    public String url() {
        return this.url;
    }

    public String database() {
        return this.database;
    }

    public String schema() {
        return this.schema;
    }

    public String table() {
        return this.table;
    }

    public String username() {
        return this.username;
    }

    public String password() {
        return this.password;
    }

    public int reconnectMaxTimes() {
        return this.reconnectMaxTimes;
    }

    public int reconnectInterval() {
        return this.reconnectInterval;
    }

    public int batchSize() {
        return this.batchSize;
    }
}
