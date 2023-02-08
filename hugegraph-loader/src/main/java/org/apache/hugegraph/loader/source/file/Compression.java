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

package org.apache.hugegraph.loader.source.file;

public enum Compression {

    NONE("none"),

    GZIP("gz"),

    BZ2("bzip2"),

    XZ("xz"),

    LZMA("lzma"),

    SNAPPY_RAW("snappy-raw"),

    SNAPPY_FRAMED("snappy-framed"),

    Z("z"),

    DEFLATE("deflate"),

    LZ4_BLOCK("lz4-block"),

    LZ4_FRAMED("lz4-framed"),

    ORC("orc"),

    PARQUET("parquet");

    private final String name;

    Compression(String name) {
        this.name = name;
    }

    public String string() {
        return this.name;
    }
}
