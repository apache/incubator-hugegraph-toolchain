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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

public class HDFSUtil implements IOUtil {

    private static final Logger LOG = Log.logger(HDFSUtil.class);

    private final Configuration conf;
    private final FileSystem fs;
    private final String storePath;

    public HDFSUtil(String storePath) {
        this.storePath = storePath;
        this.conf = new Configuration();
        // --- 【修复代码开始】 ---
        // 核心修复：禁用 HDFS 和本地文件系统的缓存
        // 这解决了多线程并发解析时，其中一个线程关闭 FileSystem 导致其他线程报错 "Filesystem closed" 的问题
        this.conf.set("fs.hdfs.impl.disable.cache", "true");
        this.conf.set("fs.file.impl.disable.cache", "true");
        // --- 【修复代码结束】 ---
        try {
            this.fs = FileSystem.get(java.net.URI.create(storePath), this.conf);
        } catch (IOException e) {
            throw new LoadException("Failed to init HDFS file system", e);
        }
    }

    @Override
    public String getStorePath() {
        return this.storePath;
    }

    @Override
    public void write(String fileName, String... lines) {
        Path path = new Path(this.storePath, fileName);
        try (FSDataOutputStream os = this.fs.create(path, true)) {
            for (String line : lines) {
                os.write(line.getBytes(StandardCharsets.UTF_8));
                os.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to write lines to HDFS file '%s'", path), e);
        }
    }

    @Override
    public void write(String fileName, Charset charset, String... lines) {
        Path path = new Path(this.storePath, fileName);
        try (FSDataOutputStream os = this.fs.create(path, true)) {
            for (String line : lines) {
                os.write(line.getBytes(charset));
                os.write("\n".getBytes(charset));
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to write lines to HDFS file '%s'", path), e);
        }
    }

    @Override
    public void copy(String srcPath, String destPath) {
        Path src = new Path(srcPath);
        Path dest = new Path(this.storePath, destPath);
        try {
            this.fs.copyFromLocalFile(src, dest);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to copy file from '%s' to '%s'", src, dest), e);
        }
    }

    @Override
    public void delete(String fileName) {
        Path path = new Path(this.storePath, fileName);
        try {
            if (this.fs.exists(path)) {
                this.fs.delete(path, true);
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to delete file '%s'", path), e);
        }
    }

    @Override
    public void mkdir(String dir) {
        Path path = new Path(this.storePath, dir);
        try {
            this.fs.mkdirs(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to create directory '%s'", path), e);
        }
    }
    
    @Override
    public void close() {
        try {
            this.fs.close();
        } catch (IOException e) {
            LOG.warn("Failed to close HDFS file system", e);
        }
    }
}