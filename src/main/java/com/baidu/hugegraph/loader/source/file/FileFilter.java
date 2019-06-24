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

package com.baidu.hugegraph.loader.source.file;

import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

public class FileFilter {

    private static final String ALL_SUFFIX = "*";

    @JsonProperty("suffix")
    private Set<String> suffixes;

    public FileFilter() {
        this.suffixes = ImmutableSet.of(ALL_SUFFIX);
    }

    public Set<String> suffixes() {
        return this.suffixes;
    }

    public boolean reserved(String name) {
        if (this.suffixes.size() == 1 && this.suffixes.contains(ALL_SUFFIX)) {
            return true;
        }
        return this.suffixes.contains(FilenameUtils.getExtension(name));
    }
}
