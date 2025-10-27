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

package org.apache.hugegraph.loader.reader.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;

public abstract class Fetcher {


    protected JDBCSource source;
    protected Connection conn;
    private static final Logger LOG = Log.logger(Fetcher.class);

    public Fetcher(JDBCSource source) throws SQLException {
        this.source = source;
        this.conn = this.connect();
    }

    public JDBCSource getSource() {
        return source;
    }

    public Connection getConn() {
        return conn;
    }

    private Connection connect() throws SQLException {
        String url = this.getSource().vendor().buildUrl(this.source);
        if (url == null) {
            throw new LoadException("Invalid url !");
        }
        LOG.info("Connect to database {}", url);
        String driverName = this.source.driver();
        String username = this.source.username();
        String password = this.source.password();
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            throw new LoadException("Invalid driver class '%s'", e, driverName);
        }
        return DriverManager.getConnection(url,
                username,
                password);
    }

    abstract String[] readHeader() throws SQLException;

    abstract void readPrimaryKey() throws SQLException;

    abstract void close();

    abstract List<Line> nextBatch()  throws SQLException;
}
