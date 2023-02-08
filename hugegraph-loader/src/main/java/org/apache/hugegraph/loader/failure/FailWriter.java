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

package org.apache.hugegraph.loader.failure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.hugegraph.loader.exception.InsertException;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.exception.ParseException;
import org.apache.hugegraph.loader.exception.ReadException;
import org.slf4j.Logger;

import org.apache.hugegraph.util.Log;

public final class FailWriter {

    private static final Logger LOG = Log.logger(FailWriter.class);

    private final File file;
    // BufferedWriter is thread safe
    private final BufferedWriter writer;

    public FailWriter(File file, String charset, boolean append) {
        checkFileAvailable(file);
        this.file = file;
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(file, append);
            Writer streamWriter = new OutputStreamWriter(stream, charset);
            this.writer = new BufferedWriter(streamWriter);
        } catch (IOException e) {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
            throw new LoadException("Failed to create writer for file '%s'", e,
                                    file);
        }
    }

    public void write(ReadException e) {
        try {
            this.writeLine("#### READ ERROR: " + e.getMessage());
            this.writeLine(e.line());
        } catch (IOException ex) {
            throw new LoadException("Failed to write read error '%s'",
                                    ex, e.line());
        }
    }

    public void write(ParseException e) {
        try {
            this.writeLine("#### PARSE ERROR: " + e.getMessage());
            this.writeLine(e.line());
        } catch (IOException ex) {
            throw new LoadException("Failed to write parse error '%s'",
                                    ex, e.line());
        }
    }

    public void write(InsertException e) {
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

    public void close() {
        try {
            // No need to flush() manually, close() will do it automatically
            this.writer.close();
        } catch (IOException e) {
            LOG.error("Failed to close writer for file '{}'", this.file);
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
