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

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.hugegraph.loader.exception.InitException;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;
import org.apache.hugegraph.loader.reader.AbstractReader;

public class JDBCReader extends AbstractReader {

    private final JDBCSource source;
    private final RowFetcher fetcher;

    private List<Line> batch;
    private int offsetInBatch;

    public JDBCReader(JDBCSource source) {
        this.source = source;
        try {
            this.fetcher = new RowFetcher(source);
        } catch (Exception e) {
            throw new LoadException("Failed to connect database via '%s'",
                                    e, source.url());
        }
        this.batch = null;
        this.offsetInBatch = 0;
    }

    public JDBCSource source() {
        return this.source;
    }

    @Override
    public void init(LoadContext context, InputStruct struct) throws InitException {
        this.progress(context, struct);
        if (!this.source.existsCustomSQL()) {
            try {
                this.source.header(this.fetcher.readHeader());
                this.fetcher.readPrimaryKey();
            } catch (SQLException e) {
                throw new InitException("Failed to fetch table structure info", e);
            }
        }
    }

    @Override
    public void confirmOffset() {
        // TODO: save offset
    }

    @Override
    public boolean hasNext() {
        if (this.batch == null || this.offsetInBatch >= this.batch.size()) {
            try {
                this.batch = this.fetcher.nextBatch();
                this.offsetInBatch = 0;
            } catch (Exception e) {
                throw new LoadException("Error while reading the next row", e);
            }
        }
        return this.batch != null && !this.batch.isEmpty();
    }

    @Override
    public Line next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("Reached end of table");
        }
        return this.batch.get(this.offsetInBatch++);
    }

    @Override
    public void close() {
        this.fetcher.close();
    }
}
