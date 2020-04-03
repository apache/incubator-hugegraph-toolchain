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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.loader.util.MappingUtil;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"version", "structs"})
public class LoadMapping implements Checkable {

    @JsonProperty("version")
    private String version;
    @JsonProperty("structs")
    private List<InputStruct> structs;

    public static LoadMapping of(String filePath) {
        File file = FileUtils.getFile(filePath);
        LoadMapping mapping;
        try {
            String json = FileUtils.readFileToString(file, Constants.CHARSET);
            mapping = MappingUtil.parse(json);
        } catch (IOException e) {
            throw new LoadException("Failed to read mapping mapping file '%s'",
                                    e, filePath);
        } catch (IllegalArgumentException e) {
            throw new LoadException("Failed to parse mapping mapping file '%s'",
                                    e, filePath);
        }
        try {
            mapping.check();
        } catch (IllegalArgumentException e) {
            throw new LoadException("Invalid mapping file '%s'", e, filePath);
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
        String dir = LoadUtil.getStructDirPrefix(options);
        String path = Paths.get(dir, Constants.FAILURE_DATA).toString();
        File pathDir = FileUtils.getFile(path);
        // It means no failure data if the path directory does not exist
        if (!pathDir.exists()) {
            return targetStructs;
        }
        Map<String, Pair<File, File>> inputFiles = this.collectInputFiles(pathDir);
        for (String inputId : inputFiles.keySet()) {
            InputStruct struct = this.struct(inputId);
            String charset = struct.input().charset();
            Pair<File, File> filePair = inputFiles.get(inputId);
            String json;
            try {
                json = FileUtils.readFileToString(filePair.getLeft(), charset);
            } catch (IOException e) {
                throw new LoadException("Failed to read header file %s",
                                        filePair.getLeft());
            }
            List<String> header = JsonUtil.convertList(json, String.class);
            FileSource source = struct.input().asFileSource();
            source.header(header.toArray(new String[]{}));
            // Set failure data path
            source.path(filePair.getRight().getAbsolutePath());
            source.skippedLine().regex(Constants.SKIPPED_LINE_REGEX);
            struct.input(source);
            // Add to target structs
            targetStructs.add(struct);
        }
        return targetStructs;
    }

    private Map<String, Pair<File, File>> collectInputFiles(File pathDir) {
        File[] subFiles = pathDir.listFiles();
        E.checkArgument(subFiles != null && subFiles.length == 2,
                        "Every input struct should have️ a pair of " +
                        "data file and header file");
        Arrays.sort(subFiles, Comparator.comparing(File::getName));
        Map<String, Pair<File, File>> inputFiles = new LinkedHashMap<>();
        for (int i = 0; i < subFiles.length; i += 2) {
            File dataFile = subFiles[i];
            File headerFile = subFiles[i + 1];
            // TODO: check data file and header file has same preifx
            int idx = dataFile.getName().lastIndexOf(Constants.DOT_STR);
            String inputId = dataFile.getName().substring(0, idx);
            inputFiles.put(inputId, Pair.of(headerFile, dataFile));
        }
        return inputFiles;
    }

    public InputStruct struct(String id) {
        for (InputStruct struct : this.structs) {
            if (struct.id().equals(id)) {
                return struct;
            }
        }
        throw new IllegalArgumentException(String.format(
                  "There is no input struct with id '%s'", id));
    }
}
