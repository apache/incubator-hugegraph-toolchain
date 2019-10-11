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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.Log;

public final class FailureLogger {

    private static final Logger LOG = Log.logger(FailureLogger.class);

    private final Writer parseWriter;
    private final Writer insertWriter;

    public FailureLogger(LoadContext context, ElementStruct struct) {
        String dir = LoadUtil.getStructDirPrefix(context.options());
        String uniqueKey = struct.uniqueKey();
        /*
         * If user prepare to hanlde failures, new failure record will write
         * to the new failure file, and the old failure file need to rename
         */
        boolean append = !context.options().reloadFailure;

        String path = path(dir, uniqueKey, Constants.PARSE_FAILURE_SUFFIX);
        this.parseWriter = new Writer(path, append);
        path = path(dir, uniqueKey, Constants.INSERT_FAILURE_SUFFIX);
        this.insertWriter = new Writer(path, append);
    }

    private static String path(String dir, String uniqueKey, String suffix) {
        // The path format is: struct/current/person-f17h1220_parse-error.data
        String name = uniqueKey + Constants.UNDERLINE_STR +
                      suffix + Constants.FAILURE_EXTENSION;
        return Paths.get(dir, Constants.FAILURE_CURRENT_DIR, name).toString();
    }

    public void write(ParseException e) {
        this.parseWriter.write(e);
    }

    public void write(InsertException e) {
        this.insertWriter.write(e);
    }

    public void close() {
        this.parseWriter.close();
        this.insertWriter.close();
    }

    private static final class Writer {

        private final File file;
        // BufferedWriter is thread safe
        private final BufferedWriter writer;

        public Writer(String name, boolean append) {
            this.file = FileUtils.getFile(name);
            checkFileAvailable(this.file);
            FileWriter fw;
            try {
                fw = new FileWriter(this.file, append);
            } catch (IOException e) {
                throw new LoadException("Failed to create writer for file '%s'",
                                        e, this.file);
            }
            this.writer = new BufferedWriter(fw);
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
}
