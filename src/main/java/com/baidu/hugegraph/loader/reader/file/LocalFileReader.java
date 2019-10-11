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

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.progress.InputItemProgress;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.FileFilter;
import com.baidu.hugegraph.loader.source.file.FileSource;

public class LocalFileReader extends FileReader {

    public LocalFileReader(FileSource source) {
        super(source);
    }

    @Override
    protected Readers openReaders() {
        File file = FileUtils.getFile(this.source().path());
        checkExistAndReadable(file);

        FileFilter filter = this.source().filter();
        List<Readable> files = new ArrayList<>();
        if (file.isFile()) {
            if (!filter.reserved(file.getName())) {
                throw new LoadException(
                          "Please check file name and extensions, ensure " +
                          "that at least one file is available for reading");
            }
            files.add(new LocalFile(file));
        } else {
            assert file.isDirectory();
            File[] subFiles = file.listFiles();
            if (subFiles == null) {
                throw new LoadException("Error while listing the files of " +
                                        "path '%s'", file);
            }
            for (File subFile : subFiles) {
                if (filter.reserved(subFile.getName())) {
                    files.add(new LocalFile(subFile));
                }
            }
        }
        return new Readers(this.source(), files);
    }

    private static void checkExistAndReadable(File file) {
        if (!file.exists()) {
            throw new LoadException("Please ensure the file or directory " +
                                    "exists: '%s'", file);
        }
        if (!file.canRead()) {
            throw new LoadException("Please ensure the file or directory " +
                                    "is readable: '%s'", file);
        }
    }

    protected static class LocalFile implements Readable {

        private final File file;

        public LocalFile(File file) {
            this.file = file;
        }

        public File file() {
            return this.file;
        }

        @Override
        public String name() {
            return this.file.getName();
        }

        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(this.file);
        }

        @Override
        public InputItemProgress inputItemProgress() {
            String name = this.file.getName();
            long timestamp = this.file.lastModified();
            String checkSum;
            try {
                checkSum = String.valueOf(FileUtils.checksumCRC32(this.file));
            } catch (IOException e) {
                throw new LoadException("Failed to calculate checksum for " +
                                        "local file '%s'", e, this.file);
            }
            return new FileItemProgress(name, timestamp, checkSum, 0L);
        }

        @Override
        public String toString() {
            return "FILE: " + this.file;
        }
    }
}
