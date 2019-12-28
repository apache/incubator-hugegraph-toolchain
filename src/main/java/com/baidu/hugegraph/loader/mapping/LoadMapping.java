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

package com.baidu.hugegraph.loader.mapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.util.StructParseUtil;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoadMapping implements Checkable {

    @JsonProperty("version")
    private String version;
    @JsonProperty("structs")
    private List<InputStruct> structs;

    public static LoadMapping of(LoadContext context) {
        LoadOptions options = context.options();
        File file = FileUtils.getFile(options.file);
        LoadMapping mapping;
        try {
            String json = FileUtils.readFileToString(file, Constants.CHARSET);
            mapping = StructParseUtil.parse(json);
        } catch (IOException e) {
            throw new LoadException("Failed to read mapping mapping file '%s'",
                                    e, options.file);
        } catch (IllegalArgumentException e) {
            throw new LoadException("Failed to parse mapping mapping file '%s'",
                                    e, options.file);
        }
        try {
            mapping.check();
        } catch (IllegalArgumentException e) {
            throw new LoadException("Invalid mapping mapping file '%s'",
                                    e, options.file);
        }
        return mapping;
    }

    @JsonCreator
    public LoadMapping(@JsonProperty("structs") List<InputStruct> structs) {
        this.version = Constants.V2_STRUCT_VERSION;
        this.structs = structs;
    }

    @Override
    public void check() throws IllegalArgumentException {
        E.checkArgument(!StringUtils.isEmpty(this.version),
                        "The version can't be null or empty");
        E.checkArgument(this.version.equals(Constants.V2_STRUCT_VERSION),
                        "The version must be '%s', but got '%s'",
                        Constants.V2_STRUCT_VERSION, this.version);
        E.checkArgument(!CollectionUtils.isEmpty(this.structs),
                        "The structs can't be null or empty");
        this.structs.forEach(InputStruct::check);
        Set<String> uniqueIds = this.structs.stream().map(InputStruct::id)
                                             .collect(Collectors.toSet());
        E.checkArgument(this.structs.size() == uniqueIds.size(),
                        "The structs cannot contain the same id mapping");
    }

    public List<InputStruct> structs() {
        return this.structs;
    }

    public List<InputStruct> structsForFailure(LoadOptions options) {
        List<InputStruct> targetStructs = new ArrayList<>();
        for (InputStruct struct : this.structs) {
            String dir = LoadUtil.getStructDirPrefix(options);
            String path = Paths.get(dir, Constants.FAILURE_HISTORY_DIR,
                                    struct.id()).toString();
            File pathDir = FileUtils.getFile(path);
            // It means no failure data if the path directory does not exist
            if (!pathDir.exists()) {
                continue;
            }
            if (!pathDir.isDirectory()) {
                throw new LoadException("The path '%s' of failure mapping " +
                                        "must be directory", path);
            }

            InputSource inputSource = struct.input();
            FileSource fileSource = inputSource.asFileSource();
            // Set failure data path
            fileSource.path(path);
            struct.input(fileSource);
            // In order to distinguish from the normal loaded strcut
//            mapping.setFailureUniqueKey();
            targetStructs.add(struct);
        }
        return targetStructs;
    }

}
