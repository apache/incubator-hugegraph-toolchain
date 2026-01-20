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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.source.file.Compression;
import org.apache.hugegraph.util.Log;

public class HDFSUtil implements IOUtil {

    private static final Logger LOG = Log.logger(HDFSUtil.class);

    private final String storePath;
    private final Configuration conf;
    private final FileSystem hdfs;

    public HDFSUtil(String storePath) {
        this.storePath = storePath;
        this.conf = loadConfiguration();
        // HDFS doesn't support write by default
        this.conf.setBoolean("dfs.support.write", true);
        this.conf.setBoolean("fs.hdfs.impl.disable.cache", true);
        try {
            this.hdfs = FileSystem.get(URI.create(storePath), this.conf);
        } catch (IOException e) {
            throw new LoadException("Failed to create HDFS file system", e);
        }
    }

    @Override
    public String storePath() {
        return this.storePath;
    }

    @Override
    public Configuration config() {
        return this.conf;
    }

    private static Configuration loadConfiguration() {
        // Just use local hadoop with default config in test
        String fileName = "hdfs_with_core_site_path/core-site.xml";
        String confPath = Objects.requireNonNull(HDFSUtil.class.getClassLoader()
                                                               .getResource(fileName)).getPath();
        Configuration conf = new Configuration();
        conf.addResource(new Path(confPath));
        return conf;
    }

    @Override
    public void mkdirs(String dir) {
        Path path = new Path(this.storePath, dir);
        try {
            this.hdfs.mkdirs(path);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to create directory '%s'", path), e);
        }
    }

    @Override
    public void write(String fileName, Charset charset,
                      Compression compress, String... lines) {
        Path path = new Path(this.storePath, fileName);
        checkPath(path);

        if (compress == Compression.NONE) {
            try (FSDataOutputStream fos = this.hdfs.append(path)) {
                for (String line : lines) {
                    fos.write(line.getBytes(charset));
                    fos.write("\n".getBytes(charset));
                }
                fos.flush();
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to write lines '%s' to path '%s'",
                                                         Arrays.asList(lines), path), e);
            }
        } else {
            try (FSDataOutputStream fos = this.hdfs.append(path)) {
                IOUtil.compress(fos, charset, compress, lines);
            } catch (IOException | CompressorException e) {
                throw new RuntimeException(String.format("Failed to write lines '%s' to file " +
                                                         "'%s' in '%s' compression format",
                                                         Arrays.asList(lines), path, compress), e);
            }
        }
    }

    @Override
    public void copy(String srcPath, String destPath) {
        try {
            FileUtil.copy(new File(srcPath), this.hdfs, new Path(destPath),
                          false, this.conf);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy file '%s' to '%s'",
                                                     srcPath, destPath));
        }
    }

    @Override
    public void delete() {
        Path path = new Path(this.storePath);
        try {
            this.hdfs.delete(path, true);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to delete file '%s'", path), e);
        }
    }

    @Override
    public void close() {
        try {
            this.hdfs.close();
        } catch (IOException e) {
            LOG.warn("Failed to close HDFS file system", e);
        }
    }

    private void checkPath(Path path) {
        try {
            if (!this.hdfs.exists(path)) {
                this.hdfs.mkdirs(path.getParent());
                this.hdfs.createNewFile(path);
            } else {
                if (!this.hdfs.isFile(path)) {
                    throw new RuntimeException(String.format("Please ensure the path '%s' is file",
                                                             path.getName()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to check HDFS path '%s'", path), e);
        }
    }
}
