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

package org.apache.hugegraph.loader.reader;

import org.apache.hugegraph.loader.constant.AutoCloseableIterator;
import org.apache.hugegraph.loader.exception.InitException;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.reader.file.LocalFileReader;
import org.apache.hugegraph.loader.reader.hdfs.HDFSFileReader;
import org.apache.hugegraph.loader.reader.jdbc.JDBCReader;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.hdfs.HDFSSource;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;

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
                throw new AssertionError(String.format("Unsupported input source '%s'",
                                                       source.type()));
        }
    }
}
