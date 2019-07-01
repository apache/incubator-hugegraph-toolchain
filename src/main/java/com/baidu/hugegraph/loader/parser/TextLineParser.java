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

import java.util.List;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.google.common.base.Splitter;

public class TextLineParser implements LineParser {

    private final FileSource source;

    // Default is "\t"
    private final String delimiter;
    private List<String> header;

    public TextLineParser(FileSource source) {
        this(source, source.delimiter() != null ?
                     source.delimiter() :
                     Constants.TEXT_DELIMITER);
    }

    public TextLineParser(FileSource source, String delimiter) {
        this.source = source;
        this.delimiter = delimiter;
        if (this.source.header() != null) {
            this.header = this.source.header();
        }
    }

    public String delimiter() {
        return this.delimiter;
    }

    public List<String> header() {
        return this.header;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Line parse(String line) {
        List<String> columns = this.split(line);
        // Ignore extra separator at the end of line
        if (columns.size() != this.header.size()) {
            if (this.lastColumnIsEmpty(columns)) {
                columns = columns.subList(0, columns.size() - 1);
            } else {
                throw new ParseException(line,
                          "The column length '%s' doesn't match with " +
                          "header length '%s' on: %s",
                          columns.size(), this.header.size(), line);
            }
        }
        return new Line(line, this.header, (List<Object>) (Object) columns);
    }

    @Override
    public boolean needHeader() {
        return this.header == null;
    }

    @Override
    public boolean parseHeader(String line) {
        /*
         * All lines will be treated as data line if the header is
         * user specified explicitly
         */
        if (this.source.header() != null) {
            return false;
        }

        if (line == null || line.isEmpty()) {
            throw new ParseException("The file header can't be empty " +
                                     "under path '%s'", this.source.path());
        }

        // If doesn't specify header, the first line is treated as header
        List<String> columns = this.split(line);
        assert !columns.isEmpty();
        if (this.header == null) {
            this.header = columns;
        } else if (!this.header.equals(columns)) {
            // Has been parsed from the previous file
            throw new ParseException("The headers of different files must be " +
                                     "same under path '%s'", this.source.path());
        }
        return true;
    }

    public List<String> split(String line) {
        return Splitter.on(this.delimiter).splitToList(line);
    }

    private boolean lastColumnIsEmpty(List<String> columns) {
        int last = columns.size() - 1;
        return columns.size() - 1 == this.header.size() &&
               columns.get(last).equals(Constants.EMPTY_STR);
    }
}
