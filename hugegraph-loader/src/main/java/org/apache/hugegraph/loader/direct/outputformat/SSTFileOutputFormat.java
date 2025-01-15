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

package org.apache.hugegraph.loader.direct.outputformat;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.SstFileWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SSTFileOutputFormat extends FileOutputFormat<byte[], byte[]> {

    @Override
    public RecordWriter<byte[], byte[]> getRecordWriter(TaskAttemptContext job) throws IOException {
        Path outputPath = getDefaultWorkFile(job, ".sst");
        FileSystem fs = outputPath.getFileSystem(job.getConfiguration());
        FSDataOutputStream fileOut = fs.create(outputPath, false);
        return new RocksDBSSTFileRecordWriter(fileOut, outputPath, fs);
    }

    public static class RocksDBSSTFileRecordWriter extends RecordWriter<byte[], byte[]> {
        private final FSDataOutputStream out;
        private final SstFileWriter sstFileWriter;
        private final Path outputPath;
        private final FileSystem fs;
        private final File localSSTFile;
        private boolean hasData = false;

        public RocksDBSSTFileRecordWriter(FSDataOutputStream out, Path outputPath, FileSystem fs) throws IOException {
            this.out = out;
            this.outputPath = outputPath;
            this.fs = fs;
            Options options = new Options();
            options.setCreateIfMissing(true);
            this.localSSTFile = File.createTempFile("sstfile", ".sst");
            this.sstFileWriter = new SstFileWriter(new org.rocksdb.EnvOptions(), options);
            try {
                this.sstFileWriter.open(localSSTFile.getAbsolutePath());
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(byte[] key, byte[] value) throws IOException {
            try {
                sstFileWriter.put(key, value);
                if (!hasData) {
                    hasData = true;
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void close(TaskAttemptContext context) throws IOException {
            try {
                if (hasData) {
                    sstFileWriter.finish();
                    try (InputStream in = new FileInputStream(localSSTFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    out.close();
                } else {
                    localSSTFile.delete();
                    out.close();
                    fs.delete(outputPath, false);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
}
