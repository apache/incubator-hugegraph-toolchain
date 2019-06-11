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

package com.baidu.hugegraph.loader.reader.file;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.LoadContext;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.parser.CsvLineParser;
import com.baidu.hugegraph.loader.parser.JsonLineParser;
import com.baidu.hugegraph.loader.parser.LineParser;
import com.baidu.hugegraph.loader.parser.TextLineParser;
import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public abstract class FileReader implements InputReader {

    private static final Logger LOG = Log.logger(FileReader.class);

    private final FileSource source;
    private final LineParser parser;
    private Readers readers;
    private Line nextLine;

    public FileReader(FileSource source) {
        this.source = source;
        this.parser = createLineParser(source);
        this.readers = null;
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    protected abstract Readers openReaders() throws IOException;

    @Override
    public void init(LoadContext context, ElementStruct struct) {
        LOG.info("Opening struct {}", this.source);
        try {
            this.readers = this.openReaders();
        } catch (IOException e) {
            throw new LoadException("Failed to open readers for '%s'",
                                    this.source);
        }
        this.progress(context, struct);

        boolean needHeader = this.parser.needHeader();
        String headerLine = this.readers.skipOffset(needHeader);
        if (needHeader) {
            if (headerLine != null) {
                this.parser.parseHeader(headerLine);
            } else {
                throw new LoadException("Failed to read header from " +
                                        "file struct '%s'", this.source);
            }
        }
    }

    private void progress(LoadContext context, ElementStruct struct) {
        ElemType type = struct.type();
        InputProgressMap oldProgress = context.oldProgress().get(type);
        InputProgressMap newProgress = context.newProgress().get(type);
        InputProgress oldInputProgress = oldProgress.getByStruct(struct);
        if (oldInputProgress == null) {
            oldInputProgress = new InputProgress(struct);
        }
        InputProgress newInputProgress = newProgress.getByStruct(struct);
        this.readers.progress(oldInputProgress, newInputProgress);
    }

    @Override
    public boolean hasNext() {
        if (this.nextLine != null) {
            return true;
        }
        this.nextLine = this.fetch();
        return this.nextLine != null;
    }

    @Override
    public Line next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("Reached the end of file");
        }
        Line line = this.nextLine;
        this.nextLine = null;
        return line;
    }

    @Override
    public void close() throws IOException {
        if (this.readers != null) {
            this.readers.close(false);
        }
    }

    protected Line fetch() {
        int index = this.readers.index();
        while (true) {
            String rawLine = this.readNextLine();
            if (rawLine == null) {
                return null;
            }
            if (this.needSkipLine(rawLine)) {
                continue;
            }
            boolean openNext = index != this.readers.index();
            index = this.readers.index();
            if (openNext && this.parser.parseHeader(rawLine)) {
                continue;
            }
            return this.parser.parse(rawLine);
        }
    }

    private String readNextLine() {
        E.checkState(this.readers != null, "The readers shouldn't be null");
        try {
            return this.readers.readNextLine();
        } catch (IOException e) {
            throw new LoadException("Error while reading the next line", e);
        }
    }

    private boolean needSkipLine(String line) {
        return line.matches(this.source.skippedLine().regex());
    }

    private static LineParser createLineParser(FileSource source) {
        FileFormat format = source.format();
        switch (format) {
            case CSV:
                return new CsvLineParser(source);
            case TEXT:
                return new TextLineParser(source);
            case JSON:
                return new JsonLineParser(source);
            default:
                throw new AssertionError(String.format(
                          "Unsupported file format '%s'", source));
        }
    }
}
