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

import com.baidu.hugegraph.loader.reader.file.FileReader;
import com.baidu.hugegraph.loader.reader.hdfs.HDFSReader;
import com.baidu.hugegraph.loader.reader.hdfs.OrcFileReader;
import com.baidu.hugegraph.loader.reader.jdbc.JDBCReader;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.Compression;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;

public class InputReaderFactory {

    public static InputReader create(InputSource source) {
        switch (source.type()) {
            case FILE:
                    return new FileReader((FileSource) source);
            case HDFS:
                if (((FileSource) source).compression() == Compression.ORC) {
                    return new OrcFileReader((HDFSSource) source);
                } else {
                    return new HDFSReader((HDFSSource) source);
                }
            case JDBC:
                return new JDBCReader((JDBCSource) source);
            default:
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'", source.type()));
        }
    }
}
