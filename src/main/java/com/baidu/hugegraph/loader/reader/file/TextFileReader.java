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

package com.baidu.hugegraph.loader.reader.file;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.google.common.base.Splitter;

public class TextFileReader extends FileReader {

    private static final String DEFAULT_DELIMITER = "\t";

    // Default is "\t"
    protected String delimiter;
    protected List<String> header;

    public TextFileReader(FileSource source) {
        super(source);
        this.delimiter = DEFAULT_DELIMITER;
        this.header = null;
    }

    @Override
    public void init() {
        /*
         * The delimiter must be initialized before header, because init header
         * may use it
         */
        this.initDelimiter();
        this.initHeader();
    }

    protected void initDelimiter() {
        if (this.source().delimiter() != null) {
            this.delimiter = this.source().delimiter();
        }
    }

    protected void initHeader() {
        if (this.source().header() != null) {
            this.header = this.source().header();
        } else {
            // If doesn't specify header, the first line is considered as header
            if (this.hasNext()) {
                this.header = this.split(this.line());
                this.next();
            } else {
                throw new LoadException("Can't load data from empty file '%s'",
                                        this.source().path());
            }
            if (this.header.isEmpty()) {
                throw new LoadException("The header is empty",
                                        this.source().path());
            }
        }
    }

    @Override
    public Map<String, Object> transform(String line) {
        List<String> columns = this.split(line);
        if (columns.size() != this.header.size()) {
            throw new ParseException(line,
                      "The column length '%s' doesn't match with " +
                      "header length '%s' on: %s",
                      columns.size(), this.header.size(), line);
        }
        Map<String, Object> keyValues = new HashMap<>();
        for (int i = 0; i < this.header.size(); i++) {
            keyValues.put(this.header.get(i), columns.get(i));
        }
        return keyValues;
    }

    protected List<String> split(String line) {
        return Splitter.on(this.delimiter).splitToList(line);
    }
}
