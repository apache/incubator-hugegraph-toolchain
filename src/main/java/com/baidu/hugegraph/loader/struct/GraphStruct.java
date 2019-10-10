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

package com.baidu.hugegraph.loader.struct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphStruct implements Checkable {

    private static final Logger LOG = Log.logger(GraphStruct.class);

    @JsonProperty("vertices")
    private final List<VertexStruct> vertexStructs;
    @JsonProperty("edges")
    private final List<EdgeStruct> edgeStructs;

    public GraphStruct() {
        this.vertexStructs = new ArrayList<>();
        this.edgeStructs = new ArrayList<>();
    }

    public static GraphStruct of(LoadContext context) {
        LoadOptions options = context.options();
        File file = FileUtils.getFile(options.file);
        try {
            String json = FileUtils.readFileToString(file, Constants.CHARSET);
            GraphStruct struct = JsonUtil.fromJson(json, GraphStruct.class);
            struct.check();
            return struct;
        } catch (IOException | IllegalArgumentException e) {
            throw new LoadException(
                      "Failed to parse graph struct description file '%s'",
                      e, options.file);
        }
    }

    @Override
    public void check() throws IllegalArgumentException {
        LOG.info("Checking vertex struct descriptions");
        this.vertexStructs.forEach(VertexStruct::check);
        this.checkNoSameStruct(this.vertexStructs);

        LOG.info("Checking edge struct descriptions");
        this.edgeStructs.forEach(EdgeStruct::check);
        this.checkNoSameStruct(this.edgeStructs);
    }

    @SuppressWarnings("unchecked")
    public <ES extends ElementStruct> List<ES> structs(ElemType type) {
        if (type.isVertex()) {
            return (List<ES>) this.vertexStructs;
        } else {
            assert type.isEdge();
            return (List<ES>) this.edgeStructs;
        }
    }

    @SuppressWarnings("unchecked")
    public <ES extends ElementStruct> List<ES> failureStructs(
                                               ElemType type,
                                               LoadOptions options) {
        List<ES> sourceStructs = (List<ES>) (type.isVertex() ?
                                             this.vertexStructs :
                                             this.edgeStructs);
        List<ES> targetStructs = new ArrayList<>();
        for (ES struct : sourceStructs) {
            InputSource inputSource = struct.input();
            E.checkState(inputSource instanceof FileSource,
                         "The input source must be instance of FileSource");
            FileSource fileSource = (FileSource) inputSource;

            String dir = LoadUtil.getStructDirPrefix(options);
            String path = Paths.get(dir, Constants.FAILURE_HISTORY_DIR,
                                    struct.uniqueKey()).toString();
            File pathDir = FileUtils.getFile(path);
            // path dir unexist means that no failure data
            if (!pathDir.exists()) {
                continue;
            }
            if (!pathDir.isDirectory()) {
                throw new LoadException("The path '%s' used as input path of " +
                                        "failure struct must be directory",
                                        path);
            }

            // Set failure data path
            fileSource.path(path);
            // In order to distinguish from the normal loaded strcut
            struct.resetUniqueKey();

            targetStructs.add(struct);
        }
        return targetStructs;
    }

    private <T extends ElementStruct> void checkNoSameStruct(List<T> structs) {
        Set<String> uniqueKeys = structs.stream().map(ElementStruct::uniqueKey)
                                        .collect(Collectors.toSet());
        E.checkArgument(structs.size() == uniqueKeys.size(),
                        "Please ensure there is no same struct in %s", structs);
    }
}
