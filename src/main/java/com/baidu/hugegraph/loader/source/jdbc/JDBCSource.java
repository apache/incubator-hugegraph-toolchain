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

import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.SourceType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JDBCSource implements InputSource {

    @JsonProperty("driver")
    private String driver;
    @JsonProperty("url")
    private String url;
    @JsonProperty("database")
    private String database;
    @JsonProperty("table")
    private String table;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("reconnect_max_times")
    private int reconnectMaxTimes;
    @JsonProperty("reconnect_interval")
    private int reconnectInterval;
    @JsonProperty("batch_size")
    private int batchSize = 500;

    public JDBCSource() {
    }

    @Override
    public SourceType type() {
        return SourceType.JDBC;
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
