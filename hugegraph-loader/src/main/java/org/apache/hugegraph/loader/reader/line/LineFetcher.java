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

package org.apache.hugegraph.loader.reader.line;

import java.io.IOException;
import java.util.List;

import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.util.E;

/**
 * Maybe can be used as base class for JDBC RowFetcher
 */
public abstract class LineFetcher {

    private final InputSource source;
    // Used for update progress for reader
    private long offset;

    public LineFetcher(InputSource source) {
        E.checkNotNull(source, "source");
        this.source = source;
        this.offset = 0L;
    }

    public InputSource source() {
        return this.source;
    }

    public long offset() {
        return this.offset;
    }

    protected void resetOffset() {
        this.offset = 0L;
    }

    protected void addOffset(long offset) {
        this.offset += offset;
    }

    protected void increaseOffset() {
        this.offset++;
    }

    public abstract boolean ready();

    public abstract void resetReader();

    public abstract boolean needReadHeader();

    public abstract String[] readHeader(List<Readable> readables);

    public abstract void openReader(Readable readable);

    public abstract void closeReader() throws IOException;

    public abstract Line fetch() throws IOException;
}
