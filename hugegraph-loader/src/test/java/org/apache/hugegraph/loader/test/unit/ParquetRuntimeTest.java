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

package org.apache.hugegraph.loader.test.unit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.reader.InputReader;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.builder.SchemaCache;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.junit.Test;

public class ParquetRuntimeTest {

    @Test
    public void testOrdinaryReaderLoadsSnappyParquet() throws Exception {
        main(new String[0]);
    }

    // Can also run with only distribution lib jars and compiled test classes.
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("parquet-runtime-");
        Path file = directory.resolve("fixture.parquet");
        try {
            Configuration conf = new Configuration();
            conf.set("fs.file.impl", "org.apache.hadoop.fs.RawLocalFileSystem");
            MessageType schema = MessageTypeParser.parseMessageType(
                    "message fixture { required int32 number; required binary name (UTF8); }");
            try (ParquetWriter<Group> writer = ExampleParquetWriter
                    .builder(new org.apache.hadoop.fs.Path(file.toUri()))
                    .withConf(conf).withType(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY).build()) {
                writer.write(new SimpleGroupFactory(schema).newGroup()
                        .append("number", 42).append("name", "中文😀"));
            }
            InputSource source = JsonUtil.fromJson(JsonUtil.toJson(Map.of(
                    "type", "file", "path", file.toString(),
                    "format", "CSV", "compression", "PARQUET")), InputSource.class);
            source.check();
            InputReader parent = InputReader.create(source);
            List<InputReader> readers = parent.split();
            if (readers.size() != 1) {
                throw new AssertionError("Expected exactly one Parquet reader");
            }
            LoadOptions options = new LoadOptions();
            options.host = null;
            options.direct = true;
            options.file = directory.resolve("progress.json").toString();
            SchemaCache cache = new SchemaCache(Collections.emptyList(),
                                                 Collections.emptyList(),
                                                 Collections.emptyList());
            LoadContext context = LoadContext.forOffline(options, cache);
            try {
                InputStruct struct = new InputStruct(null, null);
                struct.id("parquet-runtime");
                struct.input(source);
                InputReader reader = readers.get(0);
                try {
                    reader.init(context, struct);
                    Line line = reader.next();
                    if (!line.values()[0].equals(42) ||
                        !line.values()[1].equals("中文😀") || reader.hasNext()) {
                        throw new AssertionError("Parquet readback mismatch");
                    }
                } finally {
                    reader.close();
                    parent.close();
                }
            } finally {
                context.close();
            }
        } finally {
            Files.deleteIfExists(directory.resolve(".fixture.parquet.crc"));
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
