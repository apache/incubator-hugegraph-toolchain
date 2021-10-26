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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public class RowFetcher extends Fetcher {

    private static final Logger LOG = Log.logger(RowFetcher.class);


    private String[] columns;
    private String[] primaryKeys;
    private Line nextStartRow;
    private boolean fullyFetched;

    public RowFetcher(JDBCSource source) throws SQLException {
        super(source);
        this.columns = null;
        this.primaryKeys = null;
        this.nextStartRow = null;
        this.fullyFetched = false;
    }

    @Override
    public String[] readHeader() throws SQLException {
        String sql = this.source.vendor().buildGetHeaderSql(this.source);
        LOG.debug("The sql for reading headers is: {}", sql);
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
        E.checkArgument(ArrayUtils.isNotEmpty(this.columns),
                "The colmuns of the table '%s' shouldn't be empty",
                this.source.table());
        return this.columns;
    }

    @Override
    public void readPrimaryKey() throws SQLException {
        String sql = this.source.vendor().buildGetPrimaryKeySql(this.source);
        LOG.debug("The sql for reading primary keys is: {}", sql);
        try (Statement stmt = this.conn.createStatement();
             ResultSet result = stmt.executeQuery(sql)) {
            List<String> columns = new ArrayList<>();
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME"));
            }
            this.primaryKeys = columns.toArray(new String[]{});
        } catch (SQLException e) {
            this.close();
            throw e;
        }
        E.checkArgument(ArrayUtils.isNotEmpty(this.primaryKeys),
                "The primary keys of the table '%s' shouldn't be empty",
                this.source.table());
    }

    @Override
    public List<Line> nextBatch() throws SQLException {
        if (this.fullyFetched) {
            return null;
        }

        String select = this.source.vendor().buildSelectSql(this.source,
                this.nextStartRow);
        LOG.debug("The sql for select is: {}", select);

        List<Line> batch = new ArrayList<>(this.source.batchSize() + 1);
        try (Statement stmt = this.conn.createStatement();
             ResultSet result = stmt.executeQuery(select)) {
            while (result.next()) {
                Object[] values = new Object[this.columns.length];
                for (int i = 1, n = this.columns.length; i <= n; i++) {
                    Object value = result.getObject(i);
                    if (value == null) {
                        value = Constants.NULL_STR;
                    }
                    values[i - 1] = value;
                }
                String rawLine = StringUtils.join(values, Constants.COMMA_STR);
                Line line = new Line(rawLine, this.columns, values);
                batch.add(line);
            }
        } catch (SQLException e) {
            this.close();
            throw e;
        }

        if (batch.size() != this.source.batchSize() + 1) {
            this.fullyFetched = true;
        } else {
            // Remove the last one
            Line lastLine = batch.remove(batch.size() - 1);
            lastLine.retainAll(this.primaryKeys);
            this.nextStartRow = lastLine;
        }
        return batch;
    }

    @Override
    public void close() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            LOG.warn("Failed to close connection", e);
        }
    }
}
