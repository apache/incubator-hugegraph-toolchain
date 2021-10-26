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

package com.baidu.hugegraph.loader.reader;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.baidu.hugegraph.loader.constant.AutoCloseableIterator;
import com.baidu.hugegraph.loader.exception.InitException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.reader.file.LocalFileReader;
import com.baidu.hugegraph.loader.reader.hdfs.HDFSFileReader;
import com.baidu.hugegraph.loader.reader.jdbc.JDBCReader;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;

/**
 * Responsible for continuously reading the next batch of data lines
 * from the input source
 */
public interface InputReader extends AutoCloseableIterator<Line> {

    void init(LoadContext context, InputStruct struct) throws InitException;

    void confirmOffset();

    @Override
    void close();

    static InputReader create(InputSource source) {
        switch (source.type()) {
            case FILE:
                return new LocalFileReader((FileSource) source);
            case HDFS:
                return new HDFSFileReader((HDFSSource) source);
            case JDBC:
                return new JDBCReader((JDBCSource) source);
            default:
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'", source.type()));
        }
    }

    boolean multiReaders();

    public default List<InputReader> split() {
        throw new NotImplementedException("Not support multiple readers");
    }
}
