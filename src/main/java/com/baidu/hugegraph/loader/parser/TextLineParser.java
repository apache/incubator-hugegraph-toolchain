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

import java.util.Arrays;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.StringUtil;

public class TextLineParser implements LineParser {

    private final FileSource source;

    // Default is "\t"
    private final String delimiter;
    private String[] header;

    public TextLineParser(FileSource source) {
        this(source, source.delimiter() != null ?
                     source.delimiter() :
                     Constants.TEXT_DELIMITER);
    }

    public TextLineParser(FileSource source, String delimiter) {
        this.source = source;
        this.delimiter = delimiter;
        if (this.source.header() != null) {
            this.header = this.source.header().toArray(new String[]{});
        }
    }

    public String delimiter() {
        return this.delimiter;
    }

    public String[] header() {
        return this.header;
    }

    @Override
    public Line parse(String line) {
        String[] columns = this.split(line);
        // Ignore extra separator at the end of line
        if (columns.length != this.header.length) {
            if (!this.lastColumnIsEmpty(columns)) {
                throw new ParseException(line,
                          "The column length '%s' doesn't match with " +
                          "header length '%s' on: %s",
                          columns.length, this.header.length, line);
            }
            String[] subColumns = new String[columns.length - 1];
            System.arraycopy(columns, 0, subColumns, 0, columns.length - 1);
            return new Line(line, this.header, subColumns);
        }
        return new Line(line, this.header, columns);
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
        String[] columns = this.split(line);
        assert columns.length > 0;
        if (this.header == null) {
            this.header = columns;
        } else if (!Arrays.equals(this.header, columns)) {
            // Has been parsed from the previous file
            throw new ParseException("The headers of different files must be " +
                                     "same under path '%s'", this.source.path());
        }
        return true;
    }

    public String[] split(String line) {
        return StringUtil.split(line, this.delimiter);
    }

    private boolean lastColumnIsEmpty(String[] columns) {
        int last = columns.length - 1;
        return columns.length - 1 == this.header.length &&
               columns[last].equals(Constants.EMPTY);
    }
}
