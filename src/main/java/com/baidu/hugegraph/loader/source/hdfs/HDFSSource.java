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

package com.baidu.hugegraph.loader.source.hdfs;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HDFSSource extends FileSource {

    @JsonProperty("core_site_path")
    private String coreSitePath;

    @Override
    public SourceType type() {
        return SourceType.HDFS;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();
        E.checkArgument(!StringUtils.isEmpty(this.coreSitePath),
                        "The core_site_path can't be empty");
        File file = FileUtils.getFile(Paths.get(this.coreSitePath).toString());
        E.checkArgument(file.exists() && file.isFile(),
                        "The core site file '%s' is not an existing file",
                        file);
    }

    public String coreSitePath() {
        return this.coreSitePath;
    }

    @Override
    public String toString() {
        return String.format("%s with path %s", this.type(), this.path());
    }
}
