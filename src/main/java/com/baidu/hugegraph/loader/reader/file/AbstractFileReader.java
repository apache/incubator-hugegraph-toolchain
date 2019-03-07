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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.parser.CsvLineParser;
import com.baidu.hugegraph.loader.parser.JsonLineParser;
import com.baidu.hugegraph.loader.parser.LineParser;
import com.baidu.hugegraph.loader.parser.TextLineParser;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.Compression;
import com.baidu.hugegraph.loader.source.file.FileFormat;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;

public abstract class AbstractFileReader implements InputReader {

    private static final Logger LOG = Log.logger(AbstractFileReader.class);

    private static final int BUF_SIZE = 5 * 1024 * 1024;

    private final FileSource source;
    private Readers readers;
    private LineParser parser;
    private Line nextLine;

    public AbstractFileReader(FileSource source) {
        this.source = source;
        this.readers = null;
        this.parser = null;
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    protected abstract Readers openReaders() throws IOException;

    @Override
    public void init() {
        try {
            this.readers = this.openReaders();
        } catch (IOException e) {
            throw new LoadException("Failed to open readers for '%s'",
                                    this.source);
        }
        this.parser = createLineParser(this.source);
        this.parser.init(this);
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
            throw new NoSuchElementException("Reach end of file");
        }
        Line line = this.nextLine;
        this.nextLine = null;
        return line;
    }

    @Override
    public void close() throws IOException {
        this.readers.close();
    }

    public String readNextLine() throws IOException {
        return this.readers.readNextLine();
    }

    protected Line fetch() {
        String rawLine;
        try {
            rawLine = this.readNextLine();
        } catch (IOException e) {
            throw new LoadException("Read next line error", e);
        }
        if (rawLine == null) {
            return null;
        }

        // Skip the comment line
        if (this.isCommentLine(rawLine)) {
            return this.fetch();
        } else {
            return this.parser.parse(rawLine);
        }
    }

    private boolean isCommentLine(String line) {
        return this.source.commentSymbols().stream().anyMatch(line::startsWith);
    }

    private boolean isDuplicateHeader(String line) {
        assert line != null;
        // Json file doesn't exist header line
        if (this.parser.getClass().isAssignableFrom(TextLineParser.class)) {
            return false;
        }
        /*
         * All lines will be treated as data line if the header is
         * user specified explicitly
         */
        if (this.source.header() != null) {
            return false;
        }
        TextLineParser parser = (TextLineParser) this.parser;
        E.checkState(parser.header() != null && !parser.header().isEmpty(),
                     "The header shoudn't be null or empty");
        List<String> columns = parser.split(line);
        if (parser.header().size() != columns.size()) {
            return false;
        }
        for (int i = 0; i < parser.header().size(); i++) {
            if (!parser.header().get(i).equals(columns.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static BufferedReader createBufferedReader(InputStream stream,
                                                       FileSource source)
                                                       throws Exception {
        E.checkNotNull(stream, "InputStream");
        try {
            Reader csr = createCompressReader(stream, source);
            return new BufferedReader(csr, BUF_SIZE);
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
            case ORC:
                throw new LoadException(
                          "Orc compression should loaded by OrcFileReader " +
                          "instead of AbstractFileReader");
            default:
                throw new LoadException("Unsupported compression format '%s'",
                                        compression);
        }
    }

    private static LineParser createLineParser(FileSource source) {
        FileFormat format = source.format();
        switch (format) {
            case CSV:
                return new CsvLineParser();
            case TEXT:
                return new TextLineParser();
            case JSON:
                return new JsonLineParser();
            default:
                throw new AssertionError(String.format(
                          "Unsupported file format '%s'", source));
        }
    }

    /**
     * Used to iterate all readable data source, like files, paths
     */
    protected final class Readers {

        private final FileSource source;
        private final List<Readable> readables;
        private BufferedReader reader;
        private int index;

        public Readers(FileSource source, List<Readable> readables) {
            this.source = source;
            this.readables = readables;
            this.index = 0;
            if (readables == null || readables.isEmpty()) {
                this.reader = null;
            } else {
                // Open the first one
                this.reader = this.open(this.index);
            }
        }

        private BufferedReader open(int index) {
            assert index < this.readables.size();
            Readable readable = this.readables.get(index);
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

        private void close(int index) throws IOException {
            assert index < this.readables.size();
            Readable readable = this.readables.get(index);
            LOG.info("Ready to close '{}'", readable);
            this.close();
        }

        public String readNextLine() throws IOException {
            // reader is null means there is no file
            if (this.reader == null) {
                return null;
            }

            boolean openNext = false;
            String line;
            while ((line = this.reader.readLine()) == null) {
                // The current file is read at the end, ready to read next one
                this.close(this.index);

                if (++this.index < this.readables.size()) {
                    // Open the second or subsequent readables, need
                    this.reader = this.open(this.index);
                    openNext = true;
                } else {
                    return null;
                }
            }
            // Determine if need to skip duplicate header
            if (openNext && isDuplicateHeader(line)) {
                line = this.readNextLine();
            }
            return line;
        }

        private void close() throws IOException {
            if (this.reader != null) {
                this.reader.close();
            }
        }
    }
}
