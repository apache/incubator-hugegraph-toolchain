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
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.progress.InputItemProgress;
import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.progress.InputProgressMap;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public abstract class FileReader implements InputReader {

    private static final Logger LOG = Log.logger(FileReader.class);

    private final FileSource source;

    private List<Readable> readables;
    private FileLineFetcher fetcher;
    private int index;
    private Line nextLine;

    private InputProgress oldProgress;
    private InputProgress newProgress;

    public FileReader(FileSource source) {
        this.source = source;
        this.readables = null;
        this.fetcher = null;
        this.index = -1;
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    protected abstract List<Readable> scanReadables() throws IOException;

    protected abstract FileLineFetcher createLineFetcher();

    @Override
    public void init(LoadContext context, ElementStruct struct) {
        LOG.info("Opening struct '{}'", struct);
        this.progress(context, struct);

        try {
            this.readables = this.scanReadables();
            // Sort readable files by name
            this.readables.sort(Comparator.comparing(Readable::name));
        } catch (IOException e) {
            throw new LoadException("Failed to scan readable files for '%s'",
                                    e, this.source);
        }

        this.fetcher = this.createLineFetcher();
        this.fetcher.readHeaderIfNeeded(this.readables);
    }

    @Override
    public long confirmOffset() {
        return this.newProgress.confirmOffset();
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
    public void close() throws IOException {
        Readable readable = this.currentReadable();
        LOG.debug("Ready to close '{}'", readable);
        this.fetcher.closeReader(readable);
    }

    private Readable currentReadable() {
        E.checkState(this.index >= 0 && this.index < this.readables.size(),
                     "Invalid readable index %s", this.index);
        return this.readables.get(this.index);
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
        this.oldProgress = oldInputProgress;
        this.newProgress = newInputProgress;
    }

    private Line readNextLine() throws IOException {
        if (!this.fetcher.ready() && !this.openNextReadable()) {
            return null;
        }

        Line line;
        try {
            while ((line = this.fetcher.fetch()) == null) {
                // The current file is read at the end, ready to read next one
                this.fetcher.closeReader(this.currentReadable());
                if (!this.openNextReadable()) {
                    // There is no readable file
                    return null;
                }
            }
        } finally {
            // Update loading progress even if throw exception
            this.newProgress.loadingItem().offset(this.fetcher.offset());
        }
        return line;
    }

    private boolean openNextReadable() {
        while (++this.index < this.readables.size()) {
            Readable readable = this.currentReadable();
            LoadStatus status = this.checkLastLoadStatus(readable);
            /*
             * If the file has been loaded fully, skip it
             * If the file has been loaded in half, skip the last offset
             * If the file has not been loaded, load it
             */
            if (status == LoadStatus.LOADED) {
                continue;
            }

            LOG.debug("Ready to open '{}'", readable);
            this.fetcher.openReader(readable);
            if (status == LoadStatus.LOADED_HALF) {
                long offset = this.oldProgress.loadingOffset();
                this.fetcher.skipOffset(readable, offset);
            }
            return true;
        }
        return false;
    }

    private LoadStatus checkLastLoadStatus(Readable readable) {
        // NOTE: calculate check sum is a bit time consuming
        InputItemProgress input = readable.inputItemProgress();
        InputItemProgress loaded = this.oldProgress.matchLoadedItem(input);
        // The file has been loaded before and it is not changed
        if (loaded != null) {
            this.newProgress.addLoadedItem(loaded);
            return LoadStatus.LOADED;
        }

        InputItemProgress loading = this.oldProgress.matchLoadingItem(input);
        if (loading != null) {
            // The file has been loaded half before and it is not changed
            this.newProgress.addLoadingItem(loading);
            return LoadStatus.LOADED_HALF;
        } else {
            this.newProgress.addLoadingItem(input);
            return LoadStatus.NOT_LOADED;
        }
    }

    private enum LoadStatus {

        LOADED,

        LOADED_HALF,

        NOT_LOADED
    }
}
