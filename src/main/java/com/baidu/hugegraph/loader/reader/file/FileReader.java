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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.InitException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.progress.InputItemProgress;
import com.baidu.hugegraph.loader.reader.AbstractReader;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.Log;

public abstract class FileReader extends AbstractReader {

    private static final Logger LOG = Log.logger(FileReader.class);

    private final FileSource source;

    private Iterator<Readable> readables;
    private Readable readable;
    private FileLineFetcher fetcher;
    private Line nextLine;

    public FileReader(FileSource source) {
        this.source = source;
        this.readables = null;
        this.readable = null;
        this.fetcher = null;
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    public void readables(Iterator<Readable> readables) {
        this.readables = readables;
    }

    public Readable readable() {
        if (this.readable != null) {
            return this.readable;
        }
        if (this.readables.hasNext()) {
            this.readable = this.readables.next();
            return this.readable;
        }
        return null;
    }

    @Override
    public boolean multiReaders() {
        return true;
    }

    @Override
    public List<InputReader> split() {
        List<Readable> readableList;
        try {
            readableList = this.scanReadables();
            // Sort readable files by name
            readableList.sort(Comparator.comparing(Readable::name));
        } catch (IOException e) {
            throw new InitException("Failed to scan readable files for '%s'",
                                    e, this.source);
        }
        this.fetcher = this.createLineFetcher();
        this.fetcher.readHeaderIfNeeded(readableList);

        this.readables = readableList.iterator();
        List<InputReader> readers = new ArrayList<>();
        while (this.readables.hasNext()) {
            Readable readable = this.readables.next();
            FileReader fileReader = this.newFileReader(this.source, readable);
            fileReader.fetcher = fileReader.createLineFetcher();
            readers.add(fileReader);
        }
        return readers;
    }

    protected abstract FileReader newFileReader(InputSource source,
                                                Readable readable);

    protected abstract List<Readable> scanReadables() throws IOException;

    protected abstract FileLineFetcher createLineFetcher();

    @Override
    public void init(LoadContext context, InputStruct struct)
                     throws InitException {
        this.progress(context, struct);
    }

    @Override
    public void confirmOffset() {
        this.newProgress.confirmOffset();
    }

    @Override
    public boolean hasNext() {
        if (this.nextLine != null) {
            return true;
        }
        try {
            this.nextLine = this.readNextLine();
        } catch (IOException e) {
            throw new LoadException("Error while reading the next line", e);
        }
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
    public void close() {
        if (this.readable == null) {
            return;
        }
        LOG.debug("Ready to close '{}'", this.readable);
        try {
            this.fetcher.closeReader();
        } catch (IOException e) {
            LOG.warn("Failed to close reader for {} with exception {}",
                     this.source, e);
        }
    }

    private Line readNextLine() throws IOException {
        if (!this.fetcher.ready() && !this.openNextReadable()) {
            return null;
        }

        Line line;
        try {
            while ((line = this.fetcher.fetch()) == null) {
                // The current file is read at the end, ready to read next one
                this.fetcher.closeReader();
                if (!this.openNextReadable()) {
                    // There is no readable file
                    return null;
                }
            }
        } finally {
            // Update loading progress even if throw exception
            this.newProgress.loadingItem(this.readable.name())
                            .offset(this.fetcher.offset());
        }
        return line;
    }

    private boolean openNextReadable() {
        while (this.moveToNextReadable()) {
            LoadStatus status = this.checkLastLoadStatus(this.readable);
            /*
             * If the file has been loaded fully, skip it
             * If the file has been loaded in half, skip the last offset
             * If the file has not been loaded, load it
             */
            if (status == LoadStatus.LOADED) {
                continue;
            }

            LOG.info("In loading '{}'", this.readable);
            this.fetcher.openReader(this.readable);
            if (status == LoadStatus.LOADED_HALF) {
                long offset = this.oldProgress.loadingItem(this.readable.name())
                                              .offset();
                this.fetcher.skipOffset(this.readable, offset);
            }
            return true;
        }
        return false;
    }

    private boolean moveToNextReadable() {
        boolean hasNext = this.readables.hasNext();
        if (hasNext) {
            this.readable = this.readables.next();
        }
        return hasNext;
    }

    private LoadStatus checkLastLoadStatus(Readable readable) {
        // NOTE: calculate check sum is a bit time consuming
        InputItemProgress input = readable.inputItemProgress();
        InputItemProgress loaded = this.oldProgress.matchLoadedItem(input);
        // The file has been loaded before and it is not changed
        if (loaded != null) {
            this.newProgress.addLoadedItem(readable.name(), loaded);
            return LoadStatus.LOADED;
        }

        InputItemProgress loading = this.oldProgress.matchLoadingItem(input);
        if (loading != null) {
            // The file has been loaded half before and it is not changed
            this.newProgress.addLoadingItem(readable.name(), loading);
            return LoadStatus.LOADED_HALF;
        } else {
            this.newProgress.addLoadingItem(readable.name(), input);
            return LoadStatus.NOT_LOADED;
        }
    }

    private enum LoadStatus {

        LOADED,

        LOADED_HALF,

        NOT_LOADED
    }
}
