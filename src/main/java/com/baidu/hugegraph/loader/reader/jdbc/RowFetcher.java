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

package com.baidu.hugegraph.loader.reader.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public class RowFetcher {

    private static final Logger LOG = Log.logger(RowFetcher.class);

    private final JDBCSource source;
    private final Connection conn;
    private String[] columns;
    private Line nextBatchStartRow;
    private boolean finished;

    public RowFetcher(JDBCSource source) throws SQLException {
        this.source = source;
        this.conn = this.connect();
        this.columns = null;
        this.finished = false;
    }

    private Connection connect() throws SQLException {
        String url = this.source.url();
        String database = this.source.database();
        if (url.endsWith("/")) {
            url = String.format("%s%s", url, database);
        } else {
            url = String.format("%s/%s", url, database);
        }

        int maxTimes = this.source.reconnectMaxTimes();
        int interval = this.source.reconnectInterval();

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setPath(url)
                  .setParameter("characterEncoding", "utf-8")
                  .setParameter("rewriteBatchedStatements", "true")
                  .setParameter("useServerPrepStmts", "false")
                  .setParameter("autoReconnect", "true")
                  .setParameter("maxReconnects", String.valueOf(maxTimes))
                  .setParameter("initialTimeout", String.valueOf(interval));

        String driverName = this.source.driver();
        String username = this.source.username();
        String password = this.source.password();
        try {
            // Register JDBC driver
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            throw new LoadException("Invalid driver class '%s'", e, driverName);
        }
        return DriverManager.getConnection(url, username, password);
    }

    public void readHeader() throws SQLException {
        String database = this.source.database();
        String table = this.source.table();
        String sql = this.source.vendor().buildGetHeaderSql(database, table);

        try (Statement stmt = this.conn.createStatement();
             ResultSet result = stmt.executeQuery(sql)) {
            List<String> columns = new ArrayList<>();
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME"));
            }
            this.columns = columns.toArray(new String[]{});
        } catch (SQLException e) {
            this.close();
            throw e;
        }
        E.checkArgument(this.columns != null && this.columns.length != 0,
                        "The colmuns of the table '%s' shouldn't be empty",
                        table);
    }

    public List<Line> nextBatch() throws SQLException {
        if (this.finished) {
            return null;
        }

        String select = this.buildSql();

        List<Line> batch = new ArrayList<>(this.source.batchSize() + 1);
        try (Statement stmt = this.conn.createStatement();
             ResultSet result = stmt.executeQuery(select)) {
            while (result.next()) {
                Object[] values = new Object[this.columns.length];
                for (int i = 1, n = this.columns.length; i <= n; i++) {
                    Object value = result.getObject(i);
                    if (value == null) {
                        value = "NULL";
                    }
                    values[i - 1] = value;
                }
                String rawLine = StringUtils.join(values, ",");
                Line line = new Line(rawLine, this.columns, values);
                batch.add(line);
            }
        } catch (SQLException e) {
            this.close();
            throw e;
        }

        if (batch.size() != this.source.batchSize() + 1) {
            this.finished = true;
        } else {
            // Remove the last one
            this.nextBatchStartRow = batch.remove(batch.size() - 1);
        }
        return batch;
    }

    public String buildSql() {
        int limit = this.source.batchSize() + 1;

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM ").append(this.source.table());

        if (this.nextBatchStartRow != null) {
            WhereBuilder where = new WhereBuilder(this.source.vendor(), true);
            where.gte(this.nextBatchStartRow.names(),
                      this.nextBatchStartRow.values());
            sqlBuilder.append(where.build());
        }
        sqlBuilder.append(" LIMIT ").append(limit);
        sqlBuilder.append(";");
        return sqlBuilder.toString();
    }

    public void close() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            LOG.warn("Failed to close connection", e);
        }
    }
}
