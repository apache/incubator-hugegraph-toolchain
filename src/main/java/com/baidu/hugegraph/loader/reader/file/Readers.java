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

import static com.baidu.hugegraph.util.Bytes.MB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Comparator;
import java.util.List;

import com.baidu.hugegraph.loader.parser.CsvLineParser;
import com.baidu.hugegraph.loader.parser.JsonLineParser;
import com.baidu.hugegraph.loader.parser.LineParser;
import com.baidu.hugegraph.loader.parser.TextLineParser;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.structure.Task;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.progress.InputItemProgress;
import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.Compression;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

/**
 * Used to iterate all readable data files, like local files, hdfs paths
 */
public class Readers {

    private static final Logger LOG = Log.logger(Readers.class);

    private static final long BUF_SIZE = 4 * MB;

    protected InputProgress oldProgress;
    protected InputProgress newProgress;

    protected final FileSource source;
    protected final List<Readable> readables;
    protected int index;
    protected BufferedReader reader;
    private final LineParser parser;

    public Readers(FileSource source, List<Readable> readables) {
        E.checkNotNull(source, "source");
        E.checkNotNull(readables, "readables");
        this.source = source;
        // sort readable files by name
        readables.sort(Comparator.comparing(Readable::name));
        this.readables = readables;
        this.index = -1;
        this.reader = null;
        this.parser = createLineParser(source);
    }

    public void progress(InputProgress oldProgress, InputProgress newProgress) {
        E.checkNotNull(oldProgress, "old progress");
        E.checkNotNull(newProgress, "new progress");
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
    }

    public long confirmOffset() {
        return this.newProgress.confirmOffset();
    }

    public int index() {
        return this.index;
    }

    public void resetIndex() {
        this.index = -1;
    }

    public boolean needHeader() {
        return this.parser.needHeader();
    }

    public String[] headerLine() {
        return this.parser.header();
    }

    public String[] readHeader() {
        E.checkArgument(this.readables.size() > 0,
                        "Must contain at least one readable file");

        String[] header = null;
        for (Readable readable : this.readables) {
            BufferedReader reader = this.openReader(readable);
            try {
                String line = reader.readLine();
                reader.close();
                if (line != null) {
                    this.parser.parseHeader(line);
                    header = this.parser.header();
                    break;
                }
            } catch (IOException e) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    LOG.warn("Failed to close reader of '{}'", readable);
                }
                throw new LoadException("Failed to read header from '%s'",
                                        e, readable);
            }
        }
        this.resetIndex();
        return header;
    }

    public void skipOffset() {
        long offset = this.oldProgress.loadingOffset();
        if (offset <= 0) {
            return;
        }
        if (this.reader == null && (this.reader = this.openNext()) == null) {
            return;
        }

        try {
            for (long i = 0L; i < offset; i++) {
                this.reader.readLine();
            }
        } catch (IOException e) {
            throw new LoadException("Failed to skip the first %s lines " +
                                    "of file %s, please ensure the file " +
                                    "must have at least %s lines", e, offset,
                                    this.readables.get(this.index), offset);
        }
    }

    /**
     * Read next line in the files(actual are readable, called as file just for
     * convenience), open a new file to read when the previous was read to end
     */
    public Line readNextLine() throws IOException {
        // Open the first file need to read
        if (this.reader == null && (this.reader = this.openNext()) == null) {
            return null;
        }

        String line;
        while ((line = this.reader.readLine()) == null) {
            // The current file is read at the end, ready to read next one
            this.close();
            // Open the second or subsequent files
            this.reader = this.openNext();
            if (this.reader == null) {
                return null;
            }
        }

        this.newProgress.increaseLoadingOffset();
        return this.parser.parse(line);
    }

    public Line parse(String line ) {
        return this.parser.parse(line);
    }

    public void close() throws IOException {
        if (this.index < this.readables.size()) {
            Readable readable = this.readables.get(this.index);
            LOG.debug("Ready to close '{}'", readable);
        }
        if (this.reader != null) {
            this.reader.close();
        }
    }

    private BufferedReader openNext() {
        if (++this.index >= this.readables.size()) {
            return null;
        }

        Readable readable = this.readables.get(this.index);
        if (this.checkLoaded(readable)) {
            return this.openNext();
        }
        return this.openReader(readable);
    }

    protected boolean checkLoaded(Readable readable) {
        // NOTE: calculate check sum is a bit time consuming
        InputItemProgress input = readable.inputItemProgress();
        InputItemProgress loaded = this.oldProgress.matchLoadedItem(input);
        // The file has been loaded before and it is not changed
        if (loaded != null) {
            this.newProgress.addLoadedItem(loaded);
            return true;
        }

        InputItemProgress loading = this.oldProgress.matchLoadingItem(input);
        if (loading != null) {
            this.newProgress.addLoadingItem(loading);
        } else {
            this.newProgress.addLoadingItem(input);
        }
         return false;
    }

    private BufferedReader openReader(Readable readable) {
        LOG.debug("Ready to open '{}'", readable);
        InputStream stream = null;
        try {
            stream = readable.open();
            return createBufferedReader(stream, this.source);
        } catch (IOException e) {
            throw new LoadException("Failed to open stream for '%s'",
                                    e, readable);
        } catch (Exception e) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                    LOG.warn("Failed to close stream of '{}'", readable);
                }
            }
            throw new LoadException("Failed to create reader for '%s'",
                                    e, readable);
        }
    }

    private static BufferedReader createBufferedReader(InputStream stream,
                                                       FileSource source)
                                                       throws Exception {
        E.checkNotNull(stream, "stream");
        try {
            Reader csr = createCompressReader(stream, source);
            return new BufferedReader(csr, (int) BUF_SIZE);
        } catch (IOException e) {
            try {
                stream.close();
            } catch (IOException ignored) {
                LOG.warn("Failed to close file {}", source.path());
            }
            throw e;
        }
    }

    private static Reader createCompressReader(InputStream stream,
                                               FileSource source)
                                               throws Exception {
        Compression compression = source.compression();
        String charset = source.charset();
        switch (compression) {
            case NONE:
                return new InputStreamReader(stream, charset);
            case GZIP:
            case BZ2:
            case XZ:
            case LZMA:
            case SNAPPY_RAW:
            case SNAPPY_FRAMED:
            case Z:
            case DEFLATE:
            case LZ4_BLOCK:
            case LZ4_FRAMED:
                CompressorStreamFactory factory = new CompressorStreamFactory();
                CompressorInputStream cis = factory.createCompressorInputStream(
                                            compression.string(), stream);
                return new InputStreamReader(cis, charset);
            default:
                throw new LoadException("Unsupported compression format '%s'",
                                        compression);
        }
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
