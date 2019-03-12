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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.Log;

public class FileReader extends AbstractFileReader {

    private static final Logger LOG = Log.logger(FileReader.class);

    public FileReader(FileSource source) {
        super(source);
    }

    @Override
    protected Readers openReaders() throws IOException {
        File file = FileUtils.getFile(this.source().path());
        checkExistAndReadable(file);

        List<Readable> files = new ArrayList<>();
        if (file.isFile()) {
            files.add(new ReadableFile(file));
        } else {
            assert file.isDirectory();
            File[] subFiles = file.listFiles();
            if (subFiles == null) {
                throw new LoadException(
                          "Error when list files of path '%s'", file);
            }
            for (File subFile : subFiles) {
                files.add(new ReadableFile(subFile));
            }
        }
        return new Readers(this.source(), files);
    }

    private static void checkExistAndReadable(File file) {
        if (!file.exists()) {
            throw new LoadException(
                      "Please ensure the file or directory exist: '%s'", file);
        }
        if (!file.canRead()) {
            throw new LoadException(
                      "Please ensure the file or directory is readable: '%s'",
                      file);
        }
    }

    private static class ReadableFile implements Readable {

        private final File file;

        public ReadableFile(File file) {
            this.file = file;
        }

        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(this.file);
        }

        @Override
        public String toString() {
            return "FILE:" + this.file;
        }
    }
}
