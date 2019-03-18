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

package com.baidu.hugegraph.loader.source.file;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.SourceType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileSource implements InputSource {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @JsonProperty("path")
    private String path;
    @JsonProperty("format")
    private FileFormat format;
    @JsonProperty("header")
    private List<String> header;
    @JsonProperty("delimiter")
    private String delimiter;
    @JsonProperty("charset")
    private String charset;
    @JsonProperty("date_format")
    private String dateFormat;
    @JsonProperty("compression")
    private Compression compression;
    @JsonProperty("comment_symbols")
    private Set<String> commentSymbols;

    public FileSource() {
        this.charset = DEFAULT_CHARSET;
        this.dateFormat = DEFAULT_DATE_FORMAT;
        this.compression = Compression.NONE;
        this.commentSymbols = new HashSet<>();
    }

    @Override
    public SourceType type() {
        return SourceType.FILE;
    }

    public String path() {
        return this.path;
    }

    public FileFormat format() {
        return this.format;
    }

    public List<String> header() {
        if (this.header == null) {
            return null;
        } else {
            return Collections.unmodifiableList(this.header);
        }
    }

    public String delimiter() {
        return this.delimiter;
    }

    public String charset() {
        return this.charset;
    }

    public String dateFormat() {
        return this.dateFormat;
    }

    public Compression compression() {
        return this.compression;
    }

    public Set<String> commentSymbols() {
        assert this.commentSymbols != null;
        return Collections.unmodifiableSet(this.commentSymbols);
    }

    @Override
    public String toString() {
        return String.format("%s with path %s", this.type(), this.path());
    }
}
