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

package org.apache.hugegraph.loader.test.functional;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;

import org.apache.hugegraph.loader.source.file.Compression;

public class FileUtil implements IOUtil {

    private final String storePath;
    private final Configuration conf;

    public FileUtil(String storePath) {
        this.storePath = storePath;
        this.conf = new Configuration();
    }

    @Override
    public String storePath() {
        return this.storePath;
    }

    @Override
    public Configuration config() {
        return this.conf;
    }

    @Override
    public void mkdirs(String dir) {
        String path = Paths.get(this.storePath, dir).toString();
        try {
            FileUtils.forceMkdir(FileUtils.getFile(path));
        } catch (IOException e) {
            throw new RuntimeException(format("Failed to make directory on path '%s'", path), e);
        }
    }

    @Override
    public void write(String fileName, Charset charset,
                      Compression compression, String... lines) {
        String path = Paths.get(this.storePath, fileName).toString();
        File file = org.apache.commons.io.FileUtils.getFile(path);
        this.checkFile(file);

        if (compression == Compression.NONE) {
            try {
                FileUtils.writeLines(file, charset.name(), Arrays.asList(lines), true);
            } catch (IOException e) {
                throw new RuntimeException(format("Failed to write lines '%s' to file '%s'",
                                                  Arrays.asList(lines), path), e);
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                IOUtil.compress(fos, charset, compression, lines);
            } catch (IOException | CompressorException e) {
                throw new RuntimeException(format("Failed to write lines '%s' to file '%s' in " +
                                                  "'%s' compression format",
                                                  Arrays.asList(lines), path, compression), e);
            }
        }
    }

    @Override
    public void copy(String srcPath, String destPath) {
        try {
            FileUtils.copyFile(new File(srcPath), new File(destPath));
        } catch (IOException e) {
            throw new RuntimeException(format("Failed to copy file '%s' to '%s'",
                                              srcPath, destPath));
        }
    }

    @Override
    public void delete() {
        try {
            FileUtils.forceDelete(FileUtils.getFile(this.storePath));
        } catch (FileNotFoundException ignored) {
            // pass
        } catch (IOException e) {
            throw new RuntimeException(format("Failed to delete file '%s'", this.storePath), e);
        }
    }

    @Override
    public void close() {
        // pass
    }

    private void checkFile(File file) {
        if (!file.exists()) {
            try {
                FileUtils.forceMkdirParent(file);
            } catch (IOException e) {
                throw new RuntimeException(format("Failed to make parent directory '%s'",
                                                  file), e);
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(format("Failed to create new file '%s'", file), e);
            }
        } else {
            if (!file.isFile() || !file.canWrite()) {
                throw new RuntimeException(format("Please ensure the file '%s' is writable",
                                                  file.getName()));
            }
        }
    }
}
