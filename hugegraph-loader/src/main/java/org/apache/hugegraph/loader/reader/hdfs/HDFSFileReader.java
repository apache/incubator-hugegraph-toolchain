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

package org.apache.hugegraph.loader.reader.hdfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.progress.FileItemProgress;
import org.apache.hugegraph.loader.progress.InputItemProgress;
import org.apache.hugegraph.loader.source.file.Compression;
import org.apache.hugegraph.loader.source.file.FileFilter;
import org.apache.hugegraph.loader.source.hdfs.HDFSSource;
import org.apache.hugegraph.loader.source.hdfs.KerberosConfig;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.loader.reader.file.FileLineFetcher;
import org.apache.hugegraph.loader.reader.file.FileReader;
import org.apache.hugegraph.loader.reader.file.OrcFileLineFetcher;
import org.apache.hugegraph.loader.reader.file.ParquetFileLineFetcher;
import org.apache.hugegraph.util.Log;

public class HDFSFileReader extends FileReader {

    private static final Logger LOG = Log.logger(HDFSFileReader.class);

    private final FileSystem hdfs;
    private final Configuration conf;

    public HDFSFileReader(HDFSSource source) {
        super(source);
        this.conf = this.loadConfiguration();
        try {
            this.enableKerberos(source);
            this.hdfs = FileSystem.get(this.conf);
        } catch (IOException e) {
            throw new LoadException("Failed to create HDFS file system", e);
        }
        Path path = new Path(source.path());
        checkExist(this.hdfs, path);
    }

    private void enableKerberos(HDFSSource source) throws IOException {
        KerberosConfig kerberosConfig = source.kerberosConfig();
        if (kerberosConfig != null && kerberosConfig.enable()) {
            System.setProperty("java.security.krb5.conf", kerberosConfig.krb5Conf());
            UserGroupInformation.setConfiguration(this.conf);
            UserGroupInformation.loginUserFromKeytab(kerberosConfig.principal(),
                                                     kerberosConfig.keyTab());
        }
    }

    public FileSystem fileSystem() {
        return this.hdfs;
    }

    @Override
    public HDFSSource source() {
        return (HDFSSource) super.source();
    }

    @Override
    public void close() {
        super.close();
        try {
            this.hdfs.close();
        } catch (IOException e) {
            LOG.warn("Failed to close reader for {} with exception {}",
                     this.source(), e.getMessage(), e);
        }
    }

    @Override
    protected List<Readable> scanReadables() throws IOException {
        Path path = new Path(this.source().path());
        FileFilter filter = this.source().filter();
        List<Readable> paths = new ArrayList<>();
        if (this.hdfs.isFile(path)) {
            if (!filter.reserved(path.getName())) {
                throw new LoadException("Please check path name and extensions, ensure that " +
                                        "at least one path is available for reading");
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
        return paths;
    }

    @Override
    protected FileLineFetcher createLineFetcher() {
        if (Compression.ORC == this.source().compression()) {
            return new OrcFileLineFetcher(this.source(), this.conf);
        } else if (Compression.PARQUET == this.source().compression()) {
            return new ParquetFileLineFetcher(this.source(), this.conf);
        } else {
            return new FileLineFetcher(this.source());
        }
    }

    private Configuration loadConfiguration() {
        Configuration conf = new Configuration();
        conf.addResource(new Path(this.source().coreSitePath()));
        if (this.source().hdfsSitePath() != null) {
            conf.addResource(new Path(this.source().hdfsSitePath()));
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

    private static class HDFSFile implements Readable {

        private final FileSystem hdfs;
        private final Path path;

        private HDFSFile(FileSystem hdfs, Path path) {
            this.hdfs = hdfs;
            this.path = path;
        }

        public FileSystem hdfs() {
            return this.hdfs;
        }

        @Override
        public String name() {
            return this.path.getName();
        }

        @Override
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
                FileChecksum checksum = this.hdfs.getFileChecksum(this.path);
                if (checksum == null) {
                    throw new LoadException("The checksum of HDFS path '%s' " +
                                            "is null", this.path);
                }
                bytes = checksum.getBytes();
            } catch (IOException e) {
                throw new LoadException("Failed to calculate checksum " +
                                        "for HDFS path '%s'", e, this.path);
            }
            String checkSum = new String(bytes, Constants.CHARSET);
            return new FileItemProgress(name, timestamp, checkSum, 0L);
        }

        @Override
        public String toString() {
            return "HDFS: " + this.path;
        }
    }
}
