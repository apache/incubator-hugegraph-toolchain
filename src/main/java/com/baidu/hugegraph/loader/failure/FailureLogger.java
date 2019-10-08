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
        String directory = LoadUtil.getStructFilePrefix(context.options());
        String timestamp = context.timestamp();
        String uniqueKey = struct.uniqueKey();
        this.parseWriter = new Writer(path(directory, timestamp, uniqueKey,
                                           Constants.PARSE_FAILURE_SUFFIX));
        this.insertWriter = new Writer(path(directory, timestamp, uniqueKey,
                                            Constants.INSERT_FAILURE_SUFFIX));
    }

    private static String path(String directory, String timestamp,
                               String uniqueKey, String suffix) {
        // The path format is: %s/%s/%s-%s
        String fileName = uniqueKey + Constants.MINUS_STR + suffix +
                          Constants.FAILURE_EXTENSION;
        return Paths.get(directory, timestamp, fileName).toString();
    }

    public void error(ParseException e) {
        this.parseWriter.write(e);
    }

    public void error(InsertException e) {
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

        public Writer(String name) {
            this.file = FileUtils.getFile(name);
            checkFileAvailable(this.file);
            try {
                this.writer = new BufferedWriter(new FileWriter(this.file));
            } catch (IOException e) {
                throw new LoadException("Failed to create writer for file '%s'",
                                        e, this.file);
            }
        }

        public void write(ParseException e) {
            try {
                this.writer.write("#### PARSE ERROR: " + e.getMessage());
                this.writer.newLine();
                this.writer.write(e.line());
                this.writer.newLine();
            } catch (IOException ex) {
                throw new LoadException("Write parse exception error", ex);
            }
        }

        public void write(InsertException e) {
            try {
                this.writer.write("#### INSERT ERROR: " + e.getMessage());
                this.writer.newLine();
                this.writer.write(e.line());
                this.writer.newLine();
            } catch (IOException ex) {
                throw new LoadException("Write insert exception error", ex);
            }
        }

        public void close() {
            try {
                // No need to flush() manually, close() will do it automatically
                this.writer.close();
            } catch (IOException e) {
                LOG.error("Failed to close writer for file '{}'", file);
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
