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
                     Constants.TAB_STR);
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

    @Override
    public String[] header() {
        return this.header;
    }

    @Override
    public Line parse(String line) {
        String[] columns = this.split(line);
        if (columns.length > this.header.length) {
            // Ignore extra empty string at the tail of line
            int extra = columns.length - this.header.length;
            if (!this.tailColumnEmpty(columns, extra)) {
                throw new ParseException(line,
                          "The column length '%s' doesn't match with " +
                          "header length '%s' on: %s",
                          columns.length, this.header.length, line);
            }
            String[] subColumns = new String[this.header.length];
            System.arraycopy(columns, 0, subColumns, 0, this.header.length);
            return new Line(line, this.header, subColumns);
        } else if (columns.length < this.header.length) {
            // Fill with an empty string
            String[] supColumns = new String[this.header.length];
            System.arraycopy(columns, 0, supColumns, 0, columns.length);
            Arrays.fill(supColumns, columns.length, supColumns.length,
                        Constants.EMPTY_STR);
            return new Line(line, this.header, supColumns);
        }
        return new Line(line, this.header, columns);
    }

    @Override
    public boolean needHeader() {
        return this.header == null;
    }

    @Override
    public void parseHeader(String line) {
        if (line == null || line.isEmpty()) {
            throw new ParseException("The file header can't be empty " +
                                     "under path '%s'", this.source.path());
        }

        // If doesn't specify header, the first line is treated as header
        String[] columns = this.split(line);
        assert columns.length > 0;
        if (this.header == null) {
            this.header = columns;
        }
    }

//    @Override
//    public boolean matchHeader(Line line) {
//        if (line == null ) {
//            throw new ParseException("The file header can't be empty " +
//                                     "under path '%s'", this.source.path());
//        }
//
//        assert this.header != null;
//
//        return Arrays.equals(this.header, line.values());
//    }

    public String[] split(String line) {
        return StringUtil.split(line, this.delimiter);
    }

    private boolean tailColumnEmpty(String[] columns, int count) {
        for (int i = 0; i < count; i++) {
            int tailIdx = columns.length - 1 - i;
            if (!columns[tailIdx].equals(Constants.EMPTY_STR)) {
                return false;
            }
        }
        return true;
    }
}
