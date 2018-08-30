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

package com.baidu.hugegraph.loader.parser;

import java.io.IOException;
import java.util.List;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.reader.file.AbstractFileReader;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.google.common.base.Splitter;

public class TextLineParser implements LineParser {

    private static final String EMPTY_STR = "";

    private static final String DEFAULT_DELIMITER = "\t";

    // Default is "\t"
    protected String delimiter;
    protected List<String> header;

    public String delimiter() {
        return this.delimiter;
    }

    public List<String> header() {
        return this.header;
    }

    @Override
    public void init(AbstractFileReader reader) {
        /*
         * The delimiter must be initialized before header,
         * because init header may use it
         */
        this.initDelimiter(reader.source());
        this.initHeader(reader);
    }

    protected void initDelimiter(FileSource source) {
        if (source.delimiter() != null) {
            this.delimiter = source.delimiter();
        } else {
            this.delimiter = DEFAULT_DELIMITER;
        }
    }

    protected void initHeader(AbstractFileReader reader) {
        FileSource source = reader.source();
        if (source.header() != null) {
            this.header = source.header();
        } else {
            String line = null;
            try {
                line = reader.readNextLine();
            } catch (IOException e) {
                throw new LoadException("Read header line error", e);
            }
            // If doesn't specify header, the first line is considered as header
            if (line != null && !line.isEmpty()) {
                this.header = this.split(line);
            } else {
                throw new LoadException("Can't read header from empty file '%s'",
                                        source.path());
            }
            if (this.header.isEmpty()) {
                throw new LoadException("The header of file '%s' is empty",
                                        source.path());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Line parse(String rawLine) {
        List<String> columns = this.split(rawLine);
        // Ignore extra separator at the end of line
        if (columns.size() != this.header.size()) {
            if (this.lastColumnIsEmpty(columns)) {
                columns = columns.subList(0, columns.size() - 1);
            } else {
                throw new ParseException(rawLine,
                          "The column length '%s' doesn't match with " +
                          "header length '%s' on: %s",
                          columns.size(), this.header.size(), rawLine);
            }
        }
        return new Line(rawLine, this.header, (List<Object>) (Object) columns);
    }

    public List<String> split(String line) {
        return Splitter.on(this.delimiter).splitToList(line);
    }

    private boolean lastColumnIsEmpty(List<String> columns) {
        int last = columns.size() - 1;
        return columns.size() - 1 == this.header.size() &&
               columns.get(last).equals(EMPTY_STR);
    }
}
