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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.InputReader;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.Log;

public abstract class FileReader implements InputReader {

    private Logger LOG = Log.logger(FileReader.class);

    private static final int BUF_SIZE = 5 * 1024 * 1024;

    private final FileSource source;
    private final BufferedReader reader;
    private String nextLine;

    public FileReader(FileSource source) {
        this.source = source;
        try {
            this.reader = this.open(source);
        } catch (IOException e) {
            throw new LoadException("Failed to load input file '%s'",
                                    e, source.path());
        }
        this.nextLine = null;
    }

    public FileSource source() {
        return this.source;
    }

    public String line() {
        return this.nextLine;
    }

    @Override
    public boolean hasNext() {
        if (this.nextLine == null) {
            try {
                this.nextLine = this.reader.readLine();
            } catch (IOException e) {
                throw new LoadException("Read next line error", e);
            }
        }
        return this.nextLine != null;
    }

    @Override
    public Map<String, Object> next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException("Reach end of file");
        }
        String line = this.nextLine;
        this.nextLine = null;
        return this.transform(line);
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    protected abstract Map<String, Object> transform(String line);

    private BufferedReader open(FileSource source) throws IOException {
        String path = source.path();
        String charset = source.charset();

        File file = FileUtils.getFile(path);
        checkFile(file);

        InputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Reader isr = new InputStreamReader(fis, charset);
            return new BufferedReader(isr, BUF_SIZE);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                    LOG.warn("Failed to close file {}", path);
                }
            }
            throw e;
        }
    }

    private static void checkFile(File file) {
        if (!file.exists()) {
            throw new LoadException("Please ensure the file exist: '%s'", file);
        }
        if (!file.isFile()) {
            throw new LoadException("Please ensure the file is indeed a file " +
                                    "instead of a directory: '%s'", file);
        }
        if (!file.canRead()) {
            throw new LoadException("Please ensure the file is readable: '%s'",
                                    file);
        }
    }
}
