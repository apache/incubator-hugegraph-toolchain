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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public abstract class FileReader implements InputReader {

    private static final Logger LOG = Log.logger(FileReader.class);

    private final FileSource source;

    private Readers readers;
    private String[] header;
    private Line nextLine;

    public FileReader(FileSource source) {
        this.source = source;
        this.readers = null;
        this.header = null;
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    protected abstract Readers openReaders() throws IOException;

    protected abstract List<Readable> scanReadables() throws IOException;

    @Override
    public void init(LoadContext context, ElementStruct struct) {
        LOG.info("Opening struct '{}'", struct);
        try {
            this.readers = this.openReaders();
        } catch (IOException e) {
            throw new LoadException("Failed to open readers for struct '%s'",
                                    e, struct);
        }
        this.progress(context, struct);

        if (this.readers.needReadHeader()) {
            this.header = this.readers.readHeader();
            if (this.header == null) {
                throw new LoadException("Failed to read header from " +
                                        "file source '%s'", this.source);
            }
            InputSource inputSource = struct.input();
            E.checkState(inputSource instanceof FileSource,
                         "The InputSource must be FileSource when need header");

            FileSource fileSource = (FileSource) inputSource;
            fileSource.header(this.header);
        } else {
            this.header = this.readers.header();
        }

        this.readers.skipOffset();
    }

    @Override
    public long confirmOffset() {
        return this.readers.confirmOffset();
    }

    private void progress(LoadContext context, ElementStruct struct) {
        ElemType type = struct.type();
        InputProgressMap oldProgress = context.oldProgress().type(type);
        InputProgressMap newProgress = context.newProgress().type(type);
        InputProgress oldInputProgress = oldProgress.getByStruct(struct);
        if (oldInputProgress == null) {
            oldInputProgress = new InputProgress(struct);
        }
        InputProgress newInputProgress = newProgress.getByStruct(struct);
        assert newInputProgress != null;
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
            this.readers.close();
        }
    }

    /**
     * This method will be called multi times, it makes sense to
     * improve the performance of each of its sub-methods.
     */
    private Line fetch() {
        int index = this.readers.index();
        while (true) {
            Line line = this.readNextLine();
            if (line == null) {
                return null;
            }
            if (this.needSkipLine(line)) {
                continue;
            }
            boolean openNext = (index != this.readers.index());
            index = this.readers.index();
            if (openNext && this.matchHeader(line)) {
                continue;
            }
            return line;
        }
    }

    private Line readNextLine() {
        assert this.readers != null;
        try {
            return this.readers.readNextLine();
        } catch (IOException e) {
            throw new LoadException("Error while reading the next line", e);
        }
    }

    private boolean needSkipLine(Line line) {
        return this.source.skippedLine().matches(line.toString());
    }

    private boolean matchHeader(Line line) {
        if (this.source.format() == FileFormat.JSON) {
            return false;
        }
        if (line == null) {
            throw new ParseException("The file header can't be empty " +
                                     "under path '%s'", this.source.path());
        }

        assert this.header != null;
        return Arrays.equals(this.header, line.values());
    }
}
