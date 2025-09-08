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

package org.apache.hugegraph.loader.reader.afs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

import org.apache.hugegraph.loader.reader.Readable;
import org.apache.hugegraph.loader.reader.file.FileReader;
import org.apache.hugegraph.loader.reader.hdfs.HDFSFileReader;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.loader.source.afs.AFSSource;
import com.google.common.collect.ImmutableSet;

public class AFSFileReader extends HDFSFileReader {
    private static final Logger LOG = Log.logger(AFSFileReader.class);
    private static final AfsStore AFS_STORE = new AfsStore();

    public AFSFileReader(AFSSource source) {
        super(source);
    }

    @Override
    public FileSystem getFileSystem(Configuration conf) throws IOException {
        return AFS_STORE.get(conf);
    }

    @Override
    public FileReader newFileReader(InputSource source, Readable readable) {
        HDFSFileReader reader = new AFSFileReader((AFSSource) source);
        reader.readables(ImmutableSet.of(readable).iterator());
        return reader;
    }

    @Override
    public void closeFileSystem(FileSystem fileSystem) {
        try {
            AFS_STORE.close(fileSystem);
        } catch (IOException e) {
            LOG.warn("Failed to close reader for {} with exception {}",
                     this.source(), e.getMessage(), e);
        }
    }

    /*
     * AFS 多次 FileSystem.get() 会返回相同 FileSystem 句柄
     * 需确认所有引用都失效后再 close()
     * */
    public static class AfsStore {
        private static Map<FileSystem, Integer> fileSystems = new HashMap<>();

        public FileSystem get(Configuration conf) throws IOException {
            FileSystem fileSystem = FileSystem.get(conf);
            synchronized (fileSystems) {
                Integer times = fileSystems.get(fileSystem);
                if (times == null) {
                    fileSystems.put(fileSystem, 1);
                } else {
                    fileSystems.put(fileSystem, times + 1);
                }
            }
            return fileSystem;
        }

        public void close(FileSystem fileSystem) throws IOException {
            synchronized (fileSystems) {
                Integer times = fileSystems.get(fileSystem);
                if (times != null) {
                    fileSystems.put(fileSystem, times - 1);
                    if (times.equals(1)) {
                        fileSystem.close();
                        fileSystems.remove(fileSystems);
                    }
                }
            }
        }
    }
}
