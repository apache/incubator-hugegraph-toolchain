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

package com.baidu.hugegraph.loader.reader.file;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.progress.InputItem;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileItem extends InputItem {

    @JsonProperty("name")
    private final String name;
    @JsonProperty("last_modified")
    private final long timestamp;
    @JsonProperty("check_sum")
    private final long checkSum;

    @JsonCreator
    public FileItem(@JsonProperty("name") String name,
                    @JsonProperty("last_modified") long timestamp,
                    @JsonProperty("check_sum") long checkSum) {
        this.name = name;
        this.timestamp = timestamp;
        this.checkSum = checkSum;
    }

    public FileItem(FileReader.ReadableFile readableFile) {
        File file = readableFile.file();
        this.name = file.getName();
        this.timestamp = file.lastModified();
        try {
            this.checkSum = FileUtils.checksumCRC32(file);
        } catch (IOException e) {
            throw new LoadException("Failed to calculate checksum" +
                                            "for local file '%s'", e, file);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileItem)) {
            return false;
        }
        FileItem other = (FileItem) obj;
        return this.name.equals(other.name) &&
               this.timestamp == other.timestamp &&
               this.checkSum == other.checkSum;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() ^
               Long.hashCode(this.timestamp) ^
               Long.hashCode(this.checkSum);
    }
}
