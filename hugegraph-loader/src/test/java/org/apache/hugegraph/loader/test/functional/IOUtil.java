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

package org.apache.hugegraph.loader.test.functional;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Writer;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoUtils;

import org.apache.hugegraph.loader.source.file.Compression;

public interface IOUtil {

    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    CompressorStreamFactory FACTORY = new CompressorStreamFactory();

    String storePath();

    Configuration config();

    void mkdirs(String path);

    default void write(String fileName, String... lines) {
        this.write(fileName, DEFAULT_CHARSET, Compression.NONE, lines);
    }

    default void write(String fileName, Charset charset, String... lines) {
        this.write(fileName, charset, Compression.NONE, lines);
    }

    default void write(String fileName, Compression compression, String... lines) {
        this.write(fileName, DEFAULT_CHARSET, compression, lines);
    }

    void write(String fileName, Charset charset, Compression compression, String... lines);

    default void writeOrc(String fileName, TypeInfo typeInfo, Object... values) {
        Path path = new Path(this.storePath(), fileName);
        ObjectInspector inspector =
                TypeInfoUtils.getStandardJavaObjectInspectorFromTypeInfo(typeInfo);
        OrcFile.WriterOptions options = OrcFile.writerOptions(this.config())
                                               .inspector(inspector);

        Object row = Arrays.asList(values);
        try (Writer writer = OrcFile.createWriter(path, options)) {
            writer.addRow(row);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to write values '%s' to file '%s' " +
                                                     "in ORC compression format", row, path), e);
        }
    }

    void copy(String srcPath, String destPath);

    void delete();

    void close();

    static void compress(OutputStream stream, Charset charset, Compression compression,
                         String... lines) throws IOException, CompressorException {
        BufferedOutputStream bos = new BufferedOutputStream(stream);
        CompressorOutputStream cos = FACTORY.createCompressorOutputStream(compression.string(),
                                                                          bos);
        for (String line : lines) {
            cos.write(line.getBytes(charset));
            cos.write("\n".getBytes(charset));
        }
        cos.flush();
        cos.close();
    }
}
