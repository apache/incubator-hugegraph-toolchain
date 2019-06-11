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
import java.util.List;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
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

    private InputProgress oldProgress;
    private InputProgress newProgress;

    private final FileSource source;
    private final List<Readable> readables;
    private int index;
    private BufferedReader reader;

    public Readers(FileSource source, List<Readable> readables) {
        E.checkNotNull(source, "source");
        E.checkNotNull(readables, "readables");
        this.source = source;
        this.readables = readables;
        this.index = -1;
        this.reader = null;
    }

    public void progress(InputProgress oldProgress, InputProgress newProgress) {
        E.checkNotNull(oldProgress != null, "old progress");
        E.checkNotNull(newProgress != null, "new progress");
        this.oldProgress = oldProgress;
        this.newProgress = newProgress;
    }

    public int index() {
        return this.index;
    }

    /**
     * Read next line in the files(actual are readable, called as file just for
     * convenience), open a new file to read when the previous was read to end
     */
    public String readNextLine() throws IOException {
        // Open the first file need to read
        if (this.reader == null && (this.reader = this.openNext()) == null) {
            return null;
        }

        String line;
        while ((line = this.reader.readLine()) == null) {
            // The current file is read at the end, ready to read next one
            this.close(true);
            // Open the second or subsequent files
            this.reader = this.openNext();
            if (this.reader == null) {
                return null;
            }
        }

        this.newProgress.increaseLoadingOffset();
        return line;
    }

    public String skipOffset(boolean needHeader) {
        if (this.reader == null && (this.reader = this.openNext()) == null) {
            return null;
        }

        String header = null;
        long offset = this.oldProgress.loadingOffset();
        long start = 0L;
        try {
            if (needHeader) {
                header = this.reader.readLine();
                start++;
            }
            for (long i = start; i < offset; i++) {
                this.reader.readLine();
            }
        } catch (IOException e) {
            throw new LoadException("Failed to skip the first %s lines " +
                                    "of file %s, please ensure the file " +
                                    "file must have at least %s lines");
        }
        this.newProgress.addLoadingOffset(offset);
        return header;
    }

    public void close(boolean updateProgress) throws IOException {
        if (updateProgress) {
            this.newProgress.markLoadingItemLoaded();
        }
        if (this.index < this.readables.size()) {
            Readable readable = this.readables.get(this.index);
            LOG.info("Ready to close '{}'", readable);
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
        InputItemProgress inputItem = readable.inputItemProgress();
        InputItemProgress matchItem = this.oldProgress.matchLoadedItem(inputItem);
        // The file has been loaded before and it is not changed
        if (matchItem != null) {
            this.newProgress.addLoadedItem(matchItem);
            return this.openNext();
        }
        this.newProgress.addLoadingItem(inputItem);

        return this.openReader(readable);
    }

    private BufferedReader openReader(Readable readable) {
        LOG.info("Ready to open '{}'", readable);
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
                                    readable);
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
            case PACK200:
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
}