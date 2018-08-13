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

import com.baidu.hugegraph.loader.reader.file.CsvFileReader;
import com.baidu.hugegraph.loader.reader.file.FileReader;
import com.baidu.hugegraph.loader.reader.file.JsonFileReader;
import com.baidu.hugegraph.loader.reader.file.TextFileReader;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.InputSource;

public class InputReaderFactory {

    public static InputReader create(InputSource source) {
        switch (source.type()) {
            case FILE:
                return createFileReader((FileSource) source);
            default:
                // TODO: Expand more input sources
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'", source.type()));
        }
    }

    private static FileReader createFileReader(FileSource source) {
        FileFormat format = source.format();
        switch (format) {
            case CSV:
                return new CsvFileReader(source);
            case TEXT:
                return new TextFileReader(source);
            case JSON:
                return new JsonFileReader(source);
            default:
                throw new AssertionError(String.format(
                          "Unsupported file format '%s'", source));
        }
    }
}
