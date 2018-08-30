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
import java.util.Collections;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.util.E;

public class RowFetcher {

    private final String database;
    private final String table;

    private final Connection conn;

    private List<String> columns;
    private int batchSize;
    private Line nextBatchStartRow;
    private boolean finished;

    public RowFetcher(JDBCSource source) throws SQLException {
        this.database = source.database();
        this.table = source.table();
        this.batchSize = source.batchSize();
        this.conn = this.connect(source);
        this.columns = new ArrayList<>();
        this.finished = false;
    }

    private Connection connect(JDBCSource source) throws SQLException {
        String url = source.url();
        String database = source.database();
        if (url.endsWith("/")) {
            url = String.format("%s%s", url, database);
        } else {
            url = String.format("%s/%s", url, database);
        }

        int maxTimes = source.reconnectMaxTimes();
        int interval = source.reconnectInterval();

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setPath(url)
                  .setParameter("rewriteBatchedStatements", "true")
                  .setParameter("useServerPrepStmts", "false")
                  .setParameter("autoReconnect", "true")
                  .setParameter("maxReconnects", String.valueOf(maxTimes))
                  .setParameter("initialTimeout", String.valueOf(interval));

        String driverName = source.driver();
        String username = source.username();
        String password = source.password();
        try {
            // Register JDBC driver
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            throw new LoadException("Invalid driver class '%s'", driverName);
        }
        return DriverManager.getConnection(url, username, password);
    }

    public void readColumns() throws SQLException {
        String sql = String.format("SELECT COLUMN_NAME " +
                                   "FROM INFORMATION_SCHEMA.COLUMNS " +
                                   "WHERE TABLE_NAME = '%s' " +
                                   "AND TABLE_SCHEMA = '%s';",
                                   this.table, this.database);
        try (Statement stmt = this.conn.createStatement();
             ResultSet result = stmt.executeQuery(sql)) {
            while (result.next()) {
                this.columns.add(result.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            this.close();
            throw e;
        }
        E.checkArgument(!this.columns.isEmpty(),
                        "The colmuns of the table '%s' shouldn't be empty",
                        this.table);
    }

    public List<Line> nextBatch() throws SQLException {
        if (this.finished) {
            return null;
        }

        String select = this.buildSql();

        List<Line> batch = new ArrayList<>(this.batchSize + 1);
        try (Statement stmt = this.conn.createStatement();
             ResultSet result = stmt.executeQuery(select)) {
            while (result.next()) {
                List<Object> values = new ArrayList<>(this.columns.size());
                for (int i = 1, n = this.columns.size(); i <= n; i++) {
                    Object value = result.getObject(i);
                    if (value == null) {
                        value = "NULL";
                    }
                    values.add(value);
                }
                Line line = new Line(Collections.unmodifiableList(this.columns),
                                     values);
                batch.add(line);
            }
        } catch (SQLException e) {
            this.close();
            throw e;
        }

        if (batch.size() != this.batchSize + 1) {
            this.finished = true;
        } else {
            // Remove the last one
            this.nextBatchStartRow = batch.remove(batch.size() - 1);
        }
        return batch;
    }

    public String buildSql() {
        int limit = this.batchSize + 1;

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM ").append(this.table);

        if (this.nextBatchStartRow != null) {
            WhereBuilder where = new WhereBuilder(true);
            where.gte(this.nextBatchStartRow.names(),
                      this.nextBatchStartRow.values());
            sqlBuilder.append(where.build());
        }
        sqlBuilder.append(" LIMIT ").append(limit);
        sqlBuilder.append(";");
        return sqlBuilder.toString();
    }

    public void close() throws SQLException {
        this.conn.close();
    }
}
