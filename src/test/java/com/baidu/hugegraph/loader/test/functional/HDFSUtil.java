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

package com.baidu.hugegraph.loader.test.functional;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.util.Log;

public class HDFSUtil implements IOUtil {

    private static final Logger LOG = Log.logger(HDFSUtil.class);

    private final String storePath;
    private final FileSystem hdfs;

    public HDFSUtil(String storePath) {
        this.storePath = storePath;
        Configuration config = loadConfiguration();
        // HDFS doesn't support append by default
        config.setBoolean("dfs.support.append", true);
        config.setBoolean("fs.hdfs.impl.disable.cache", true);
        try {
            this.hdfs = FileSystem.get(URI.create(storePath), config);
        } catch (IOException e) {
            throw new LoadException("Failed to create hdfs file system", e);
        }
    }

    private static Configuration loadConfiguration() {
        Configuration conf = new Configuration();
        String hadoopHome = System.getenv("HADOOP_HOME");
        LOG.info("Get HADOOP_HOME {}", hadoopHome);
        String path = Paths.get(hadoopHome, "etc", "hadoop").toString();
        conf.addResource(path(path, "/core-site.xml"));
        conf.addResource(path(path, "/hdfs-site.xml"));
        conf.addResource(path(path, "/mapred-site.xml"));
        conf.addResource(path(path, "/yarn-site.xml"));
        return conf;
    }

    private static Path path(String configPath, String configFile) {
        return new Path(Paths.get(configPath, configFile).toString());
    }

    @Override
    public void mkdirs(String dir) {
        Path path = new Path(this.storePath, dir);
        try {
            this.hdfs.mkdirs(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to create directory '%s'", path), e);
        }
    }

    @Override
    public void append(String fileName, String... lines) {
        append(fileName, DEFAULT_CHARSET, lines);
    }

    @Override
    public void append(String fileName, Charset charset, String... lines) {
        Path path = new Path(this.storePath, fileName);
        try {
            checkPath(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to check path '%s' valid", path), e);
        }

        try {
            OutputStream out = this.hdfs.append(path);
            for (String line : lines) {
                out.write(line.getBytes(charset));
                out.write("\n".getBytes(charset));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to append lines '%s' to path '%s'",
                      Arrays.asList(lines), path), e);
        }
    }

    @Override
    public void delete() {
        Path path = new Path(this.storePath);
        try {
            this.hdfs.delete(path, true);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to delete file '%s'", path), e);
        }
    }

    @Override
    public void close() {
        try {
            this.hdfs.close();
        } catch (IOException e) {
            LOG.warn("Failed to close hdfs", e);
        }
    }

    private void checkPath(Path path) throws IOException {
        if (!this.hdfs.exists(path)) {
            this.hdfs.mkdirs(path.getParent());
            this.hdfs.createNewFile(path);
        } else {
            if (!this.hdfs.isFile(path)) {
                throw new RuntimeException(String.format(
                          "Please ensure the path '%s' is writable",
                          path.getName()));
            }
        }
    }
}
