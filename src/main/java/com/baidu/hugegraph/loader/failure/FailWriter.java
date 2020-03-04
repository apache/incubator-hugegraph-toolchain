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

package com.baidu.hugegraph.loader.failure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.exception.ReadException;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.util.Log;

public final class FailWriter {

    private static final Logger LOG = Log.logger(FailWriter.class);

    private final InputStruct struct;
    private volatile boolean writedHeader;

    private final File file;
    // BufferedWriter is thread safe
    private final BufferedWriter writer;

    public FailWriter(InputStruct struct, String name,
                      String charset, boolean append) {
        this.struct = struct;
        this.writedHeader = false;
        this.file = FileUtils.getFile(name);
        checkFileAvailable(this.file);
        try {
            OutputStream stream = new FileOutputStream(this.file, append);
            Writer streamWriter = new OutputStreamWriter(stream, charset);
            this.writer = new BufferedWriter(streamWriter);
        } catch (IOException e) {
            throw new LoadException("Failed to create writer for file '%s'",
                                    e, this.file);
        }
    }

    public void write(ReadException e) {
        this.writeHeaderIfNeeded();
        try {
            this.writeLine("#### READ ERROR: " + e.getMessage());
            this.writeLine(e.line());
        } catch (IOException ex) {
            throw new LoadException("Failed to write read error '%s'",
                                    ex, e.line());
        }
    }

    public void write(ParseException e) {
        this.writeHeaderIfNeeded();
        try {
            this.writeLine("#### PARSE ERROR: " + e.getMessage());
            this.writeLine(e.line());
        } catch (IOException ex) {
            throw new LoadException("Failed to write parse error '%s'",
                                    ex, e.line());
        }
    }

    public void write(InsertException e) {
        this.writeHeaderIfNeeded();
        try {
            this.writeLine("#### INSERT ERROR: " + e.getMessage());
            this.writeLine(e.line());
        } catch (IOException ex) {
            throw new LoadException("Failed to write insert error '%s'",
                                    ex, e.line());
        }
    }

    private void writeLine(String line) throws IOException {
        this.writer.write(line);
        this.writer.newLine();
    }

    private void writeHeaderIfNeeded() {
        if (this.struct.input().header() == null || this.writedHeader) {
            return;
        }
        synchronized (this.file) {
            if (!this.writedHeader) {
                String headerLine = StringUtils.join(this.struct.input().header(),
                                                     Constants.COMMA_STR);
                try {
                    this.writeLine(headerLine);
                } catch (IOException e) {
                    throw new LoadException("Failed to write header '%s'",
                                            e);
                }
                this.writedHeader = true;
            }
        }
    }

    public void close() {
        try {
            // No need to flush() manually, close() will do it automatically
            this.writer.close();
        } catch (IOException e) {
            LOG.error("Failed to close writer for file '{}'", file);
        }
        if (this.file.length() == 0) {
            LOG.debug("The file {} is empty, delete it", this.file);
            this.file.delete();
        }
    }

    private static void checkFileAvailable(File file) {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                return;
            } catch (IOException e) {
                throw new LoadException("Failed to create new file '%s'",
                                        e, file);
            }
        }
        if (file.isDirectory()) {
            throw new LoadException("Please ensure there is no directory " +
                                    "with the same name: '%s'", file);
        } else {
            if (file.length() > 0) {
                LOG.warn("The existed file {} will be overwritten", file);
            }
        }
        if (!file.canWrite()) {
            throw new LoadException("Please ensure the existed file is " +
                                    "writable: '%s'", file);
        }
    }
}
