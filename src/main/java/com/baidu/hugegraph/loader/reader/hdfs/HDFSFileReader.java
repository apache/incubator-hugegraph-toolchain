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

package com.baidu.hugegraph.loader.reader.hdfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.progress.InputItemProgress;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.reader.file.FileItemProgress;
import com.baidu.hugegraph.loader.reader.file.FileReader;
import com.baidu.hugegraph.loader.reader.file.Readers;
import com.baidu.hugegraph.loader.source.file.FileFilter;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.util.Log;

public class HDFSFileReader extends FileReader {

    private static final Logger LOG = Log.logger(HDFSFileReader.class);

    private final FileSystem hdfs;

    public HDFSFileReader(HDFSSource source) {
        super(source);
        Configuration config = this.loadConfiguration();
        try {
            this.hdfs = FileSystem.get(URI.create(source.path()), config);
        } catch (IOException e) {
            throw new LoadException("Failed to create HDFS file system", e);
        }
        Path path = new Path(source.path());
        checkExist(this.hdfs, path);
    }

    public FileSystem fileSystem() {
        return this.hdfs;
    }

    @Override
    public HDFSSource source() {
        return (HDFSSource) super.source();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (this.hdfs != null) {
            this.hdfs.close();
        }
    }

    @Override
    protected Readers openReaders() throws IOException {
        Path path = new Path(this.source().path());
        FileFilter filter = this.source().filter();
        List<Readable> paths = new ArrayList<>();
        if (this.hdfs.isFile(path)) {
            if (!filter.reserved(path.getName())) {
                throw new LoadException(
                          "Please check path name and extensions, ensure " +
                          "that at least one path is available for reading");
            }
            paths.add(new HDFSFile(this.hdfs, path));
        } else {
            assert this.hdfs.isDirectory(path);
            FileStatus[] statuses = this.hdfs.listStatus(path);
            Path[] subPaths = FileUtil.stat2Paths(statuses);
            for (Path subPath : subPaths) {
                if (filter.reserved(subPath.getName())) {
                    paths.add(new HDFSFile(this.hdfs, subPath));
                }
            }
        }
        return new Readers(this.source(), paths);
    }

    private Configuration loadConfiguration() {
        Configuration conf = new Configuration();
        String fsDefaultFS = this.source().fsDefaultFS();
        // Remote hadoop
        if (fsDefaultFS != null) {
            // TODO: Support pass more params or specify config files
            conf.set("fs.defaultFS", fsDefaultFS);
            return conf;
        }
        // Local hadoop
        String hadoopHome = System.getenv("HADOOP_HOME");
        if (hadoopHome != null && !hadoopHome.isEmpty()) {
            LOG.info("Get HADOOP_HOME {}", hadoopHome);
            String path = Paths.get(hadoopHome, "etc", "hadoop").toString();
            conf.addResource(path(path, "/core-site.xml"));
            conf.addResource(path(path, "/hdfs-site.xml"));
            conf.addResource(path(path, "/mapred-site.xml"));
            conf.addResource(path(path, "/yarn-site.xml"));
        }
        return conf;
    }

    private static void checkExist(FileSystem fs, Path path) {
        try {
            if (!fs.exists(path)) {
                throw new LoadException("Please ensure the file or directory " +
                                        "exists: '%s'", path);
            }
        } catch (IOException e) {
            throw new LoadException("An exception occurred while checking " +
                                    "HDFS path: '%s'", e, path);
        }
    }

    private static Path path(String configPath, String configFile) {
        return new Path(Paths.get(configPath, configFile).toString());
    }

    protected static class HDFSFile implements Readable {

        private final FileSystem hdfs;
        private final Path path;

        private HDFSFile(FileSystem hdfs, Path path) {
            this.hdfs = hdfs;
            this.path = path;
        }

        public FileSystem hdfs() {
            return this.hdfs;
        }

        public Path path() {
            return this.path;
        }

        @Override
        public InputStream open() throws IOException {
            return this.hdfs.open(this.path);
        }

        @Override
        public InputItemProgress inputItemProgress() {
            String name = this.path.getName();
            long timestamp;
            try {
                timestamp = this.hdfs.getFileStatus(this.path)
                                     .getModificationTime();
            } catch (IOException e) {
                throw new LoadException("Failed to get last modified time " +
                                        "for HDFS path '%s'", e, this.path);
            }
            byte[] bytes;
            try {
                bytes = this.hdfs.getFileChecksum(this.path).getBytes();
            } catch (IOException e) {
                throw new LoadException("Failed to calculate checksum " +
                                        "for HDFS path '%s'", e, this.path);
            }
            String checkSum = new String(bytes, Constants.CHARSET);
            return new FileItemProgress(name, timestamp, checkSum);
        }

        @Override
        public String toString() {
            return "HDFS: " + this.path;
        }
    }
}
