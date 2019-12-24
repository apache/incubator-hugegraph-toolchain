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

package com.baidu.hugegraph.loader.test.functional;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.baidu.hugegraph.loader.source.file.Compression;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;

public interface IOUtil {

    public Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public CompressorStreamFactory FACTORY = new CompressorStreamFactory();

    public void mkdirs(String path);

    public default void write(String fileName, String... lines) {
        this.write(fileName, DEFAULT_CHARSET, Compression.NONE, lines);
    }

    public default void write(String fileName, Charset charset,
                              String... lines) {
        this.write(fileName, charset, Compression.NONE, lines);
    }

    public default void write(String fileName, Compression compression,
                              String... lines) {
        this.write(fileName, DEFAULT_CHARSET, compression, lines);
    }

    public void write(String fileName, Charset charset,
                      Compression compression, String... lines);

    public void writeOrc(String fileName, TypeInfo typeInfo, Object... values);

    public void delete();

    public void close();

    public static void compress(OutputStream stream, Charset charset,
                                Compression compression, String... lines)
                                throws IOException, CompressorException {
        BufferedOutputStream bos = new BufferedOutputStream(stream);
        CompressorOutputStream cos = FACTORY.createCompressorOutputStream(
                                             compression.string(), bos);
        for (String line : lines) {
            cos.write(line.getBytes(charset));
            cos.write("\n".getBytes(charset));
        }
        cos.flush();
        cos.close();
    }
}
