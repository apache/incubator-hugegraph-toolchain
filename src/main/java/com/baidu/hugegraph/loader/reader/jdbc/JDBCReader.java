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

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.util.Log;

public class JDBCReader implements InputReader {

    private static final Logger LOG = Log.logger(JDBCReader.class);

    private final JDBCSource source;
    private final RowFetcher fetcher;

    private int offset;
    private List<Line> batch;

    public JDBCReader(JDBCSource source) {
        this.source = source;
        try {
            this.fetcher = new RowFetcher(source);
        } catch (Exception e) {
            throw new LoadException("Failed to connect database via '%s'",
                                    e, source.url());
        }
        this.offset = 0;
        this.batch = null;
    }

    public JDBCSource source() {
        return this.source;
    }

    @Override
    public void init() {
        try {
            this.fetcher.readColumns();
        } catch (SQLException e) {
            throw new LoadException("Failed to read column names", e);
        }
    }

    @Override
    public boolean hasNext() {
        if (this.batch == null || this.offset >= this.batch.size()) {
            try {
                this.batch = this.fetcher.nextBatch();
                this.offset = 0;
            } catch (Exception e) {
                throw new LoadException("Read next row error", e);
            }
        }
        return this.batch != null;
    }

    @Override
    public Line next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("Reach end of table");
        }
        return this.batch.get(this.offset++);
    }

    @Override
    public void close() throws Exception {
        this.fetcher.close();
    }
}
