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

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.reader.file.AbstractFileReader;
import com.baidu.hugegraph.loader.source.hdfs.HDFSSource;
import com.baidu.hugegraph.util.Log;

public class HDFSReader extends AbstractFileReader {

    private static final Logger LOG = Log.logger(HDFSReader.class);

    private final FileSystem hdfs;

    public HDFSReader(HDFSSource source) {
        super(source);
        Configuration config = loadConfiguration();
        LOG.info("Opening readers for hdfs source {}", source);
        try {
            this.hdfs = FileSystem.get(URI.create(source.path()), config);
        } catch (IOException e) {
            throw new LoadException("Failed to create hdfs file system", e);
        }
        Path path = new Path(source.path());
        checkExist(this.hdfs, path);
    }

    public FileSystem fileSystem() {
        return this.hdfs;
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

        List<Readable> readables = new ArrayList<>();
        if (this.hdfs.isFile(path)) {
            readables.add(new ReadablePath(this.hdfs, path));
        } else {
            assert this.hdfs.isDirectory(path);
            FileStatus[] statuses = this.hdfs.listStatus(path);
            Path[] paths = FileUtil.stat2Paths(statuses);
            for (Path subPath : paths) {
                readables.add(new ReadablePath(this.hdfs, subPath));
            }
        }
        return new Readers(this.source(), readables);
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

    private static void checkExist(FileSystem fs, Path path) {
        try {
            if (!fs.exists(path)) {
                throw new LoadException(
                          "Please ensure the file or directory exist: '%s'",
                          path);
            }
        } catch (IOException e) {
            throw new LoadException(
                      "Some exception occured when check hdfs path '%s' exist",
                      path);
        }
    }

    private static Path path(String configPath, String configFile) {
        return new Path(Paths.get(configPath, configFile).toString());
    }

    private static class ReadablePath implements Readable {

        private final FileSystem hdfs;
        private final Path path;

        private ReadablePath(FileSystem hdfs, Path path) {
            this.hdfs = hdfs;
            this.path = path;
        }

        @Override
        public InputStream open() throws IOException {
            try {
                return this.hdfs.open(this.path);
            } catch (IOException e) {
                throw new LoadException(
                          "Failed to create input stream for hdfs file '%s'",
                          this.path.getName());
            }
        }

        @Override
        public String toString() {
            return "HDFS" + this.path;
        }
    }
}
