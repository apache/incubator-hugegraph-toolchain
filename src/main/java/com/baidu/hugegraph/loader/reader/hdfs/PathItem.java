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
import java.util.Arrays;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.progress.InputItem;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PathItem extends InputItem {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("last_modified")
    private final long timestamp;
    @JsonProperty("check_sum")
    private final byte[] checkSum;

    @JsonCreator
    public PathItem(@JsonProperty("name") String name,
                    @JsonProperty("last_modified") long timestamp,
                    @JsonProperty("check_sum") byte[] checkSum) {
        this.name = name;
        this.timestamp = timestamp;
        this.checkSum = checkSum;
    }

    public PathItem(HDFSReader.ReadablePath readablePath) {
        FileSystem hdfs = readablePath.hdfs();
        Path path = readablePath.path();
        this.name = path.getName();
        try {
            this.timestamp = hdfs.getFileStatus(path).getModificationTime();
        } catch (IOException e) {
            throw new LoadException("Failed to get last modified time " +
                                    "for hdfs path '%s'", e, path);
        }
        try {
            this.checkSum = hdfs.getFileChecksum(path).getBytes();
        } catch (IOException e) {
            throw new LoadException("Failed to calculate checksum " +
                                    "for hdfs path '%s'", e, path);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PathItem)) {
            return false;
        }
        PathItem other = (PathItem) obj;
        return this.name.equals(other.name) &&
               this.timestamp == other.timestamp &&
               Arrays.equals(this.checkSum, other.checkSum);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() ^
               Long.hashCode(this.timestamp) ^
               Arrays.hashCode(this.checkSum);
    }
}
