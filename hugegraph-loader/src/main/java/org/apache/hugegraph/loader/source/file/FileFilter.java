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

package org.apache.hugegraph.loader.source.file;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

public class FileFilter implements Serializable {

    private static final String ALL_EXTENSION = "*";

    @JsonProperty("extensions")
    private Set<String> extensions;

    public FileFilter() {
        this.extensions = ImmutableSet.of(ALL_EXTENSION);
    }

    public Set<String> extensions() {
        return this.extensions;
    }

    public boolean reserved(String name) {
        if (this.extensions.size() == 1 &&
            this.extensions.contains(ALL_EXTENSION)) {
            return true;
        }
        return this.extensions.contains(FilenameUtils.getExtension(name));
    }
}
