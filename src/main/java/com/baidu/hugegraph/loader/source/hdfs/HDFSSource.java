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

import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HDFSSource extends FileSource {

    @JsonProperty("fs_default_fs")
    private String fsDefaultFS;

    @JsonProperty("core_site_path")
    private String coreSitePath;

    @Override
    public SourceType type() {
        return SourceType.HDFS;
    }

    @Override
    public void check() throws IllegalArgumentException {
        super.check();
        if (this.fsDefaultFS != null) {
            E.checkArgument(!this.fsDefaultFS.isEmpty(),
                            "The fs_default_fs can't be empty when " +
                            "configured it");
        }
        if (this.coreSitePath != null) {
            E.checkArgument(!this.coreSitePath.isEmpty(),
                            "The core_site_path can't be empty when " +
                            "configured it");
        }
    }

    public String fsDefaultFS() {
        return this.fsDefaultFS;
    }

    public String coreSitePath() {
        return this.coreSitePath;
    }

    @Override
    public String toString() {
        return String.format("%s with path %s", this.type(), this.path());
    }
}
