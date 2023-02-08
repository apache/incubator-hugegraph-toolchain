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

package org.apache.hugegraph.loader.mapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.hugegraph.loader.constant.Checkable;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.loader.util.LoadUtil;
import org.apache.hugegraph.loader.util.MappingUtil;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"version", "structs"})
public class LoadMapping implements Checkable {

    @JsonProperty("version")
    private String version;
    @JsonProperty("structs")
    private List<InputStruct> structs;
    @JsonProperty("backendStoreInfo")
    private BackendStoreInfo backendStoreInfo;

    public BackendStoreInfo getBackendStoreInfo() {
        return backendStoreInfo;
    }

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

    public LoadMapping(@JsonProperty("structs") List<InputStruct> structs) {
        this.version = Constants.V2_STRUCT_VERSION;
        this.structs = structs;
    }

    @JsonCreator
    public LoadMapping(@JsonProperty("structs") List<InputStruct> structs,
                       @JsonProperty("backendStoreInfo") BackendStoreInfo backendStoreInfo) {
        this.version = Constants.V2_STRUCT_VERSION;
        this.structs = structs;
        this.backendStoreInfo = backendStoreInfo;
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
        Map<String, FailureFile> failureFiles = this.groupFailureFiles(pathDir);
        for (String inputId : failureFiles.keySet()) {
            InputStruct struct = this.struct(inputId);
            String charset = struct.input().charset();
            FailureFile failureFile = failureFiles.get(inputId);

            FileSource source = struct.input().asFileSource();
            if (failureFile.headerFile != null) {
                // It means that header file existed
                String json;
                try {
                    json = FileUtils.readFileToString(failureFile.headerFile,
                                                      charset);
                } catch (IOException e) {
                    throw new LoadException("Failed to read header file %s",
                                            failureFile.headerFile);
                }
                List<String> header = JsonUtil.convertList(json, String.class);
                source.header(header.toArray(new String[]{}));
            }
            // Set failure data path
            source.path(failureFile.dataFile.getAbsolutePath());
            source.skippedLine().regex(Constants.SKIPPED_LINE_REGEX);
            struct.input(source);
            // Add to target structs
            targetStructs.add(struct);
        }
        return targetStructs;
    }

    private Map<String, FailureFile> groupFailureFiles(File pathDir) {
        File[] subFiles = pathDir.listFiles();
        E.checkArgument(subFiles != null && subFiles.length >= 1,
                        "Every input struct should have a failure data file, " +
                        "and a header file if need it");
        Map<String, FailureFile> failureFiles = new LinkedHashMap<>();
        for (File subFile : subFiles) {
            String inputId = LoadUtil.getFileNamePrefix(subFile);
            String suffix = LoadUtil.getFileNameSuffix(subFile);
            FailureFile failureFile = failureFiles.get(inputId);
            if (failureFile == null) {
                failureFile = new FailureFile();
            }
            if (Constants.FAILURE_SUFFIX.equals(suffix)) {
                failureFile.dataFile = subFile;
            } else {
                E.checkArgument(Constants.HEADER_SUFFIX.equals(suffix),
                                "The failure data file must end with %s or %s",
                                Constants.FAILURE_SUFFIX,
                                Constants.HEADER_SUFFIX);
                failureFile.headerFile = subFile;
            }
            failureFiles.put(inputId, failureFile);
        }
        return failureFiles;
    }

    public InputStruct struct(String id) {
        for (InputStruct struct : this.structs) {
            if (struct.id().equals(id)) {
                return struct;
            }
        }
        throw new IllegalArgumentException(String.format("There is no input struct with id '%s'",
                                                         id));
    }

    private static class FailureFile {

        // Maybe null
        private File headerFile;
        // Can't be null
        private File dataFile;
    }
}
