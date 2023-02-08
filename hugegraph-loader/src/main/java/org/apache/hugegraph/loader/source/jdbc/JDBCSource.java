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

package org.apache.hugegraph.loader.source.jdbc;

import org.apache.hugegraph.loader.source.AbstractSource;
import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"type", "vendor"})
public class JDBCSource extends AbstractSource {

    @JsonProperty("custom_sql")
    private String customSQL;
    @JsonProperty("vendor")
    private JDBCVendor vendor;
    @JsonProperty("driver")
    private String driver;
    @JsonProperty("url")
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
    @JsonProperty("batch_size")
    private int batchSize = 500;

    @Override
    public SourceType type() {
        return SourceType.JDBC;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();
        E.checkArgument(this.vendor != null, "The vendor can't be null");
        E.checkArgument(this.url != null, "The url can't be null");
        E.checkArgument(this.database != null, "The database can't be null");
        E.checkArgument(this.username != null, "The username can't be null");
        E.checkArgument(this.password != null, "The password can't be null");
        E.checkArgument(this.table != null || this.customSQL != null,
                        "At least one of table and sql can't be null");

        this.schema = this.vendor.checkSchema(this);
        if (this.driver == null) {
            this.driver = this.vendor.defaultDriver();
        }
    }

    public String customSQL() {
        return this.customSQL;
    }

    public boolean existsCustomSQL() {
        return this.customSQL != null;
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

    public int batchSize() {
        return this.batchSize;
    }

    @Override
    public FileSource asFileSource() {
        FileSource source = new FileSource();
        source.header(this.header());
        source.charset(this.charset());
        source.listFormat(this.listFormat());
        return source;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.type(), this.url());
    }
}
