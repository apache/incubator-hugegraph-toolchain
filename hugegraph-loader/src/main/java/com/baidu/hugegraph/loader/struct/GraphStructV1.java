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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.baidu.hugegraph.loader.mapping.BackendStoreInfo;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The GraphStructV1 is deprecated, it will be removed sometime in the future
 */
@Deprecated
public class GraphStructV1 implements Checkable {

    private static final Logger LOG = Log.logger(GraphStructV1.class);

    @JsonProperty("version")
    private String version;
    @JsonProperty("vertices")
    private final List<VertexStructV1> vertexStructs;
    @JsonProperty("edges")
    private final List<EdgeStructV1> edgeStructs;

    @JsonProperty("backendStoreInfo")
    private BackendStoreInfo backendStoreInfo;

    public GraphStructV1() {
        this.vertexStructs = new ArrayList<>();
        this.edgeStructs = new ArrayList<>();
    }

    public static GraphStructV1 of(LoadContext context) {
        LoadOptions options = context.options();
        File file = FileUtils.getFile(options.file);
        try {
            String json = FileUtils.readFileToString(file, Constants.CHARSET);
            GraphStructV1 struct = JsonUtil.fromJson(json, GraphStructV1.class);
            struct.check();
            return struct;
        } catch (IOException | IllegalArgumentException e) {
            throw new LoadException(
                      "Failed to parse graph mapping description file '%s'",
                      e, options.file);
        }
    }

    public BackendStoreInfo getBackendStoreInfo() {
        return backendStoreInfo;
    }

    @Override
    public void check() throws IllegalArgumentException {
        LOG.info("Checking vertex mapping descriptions");
        this.vertexStructs.forEach(VertexStructV1::check);
        this.checkNoSameStruct(this.vertexStructs);

        LOG.info("Checking edge mapping descriptions");
        this.edgeStructs.forEach(EdgeStructV1::check);
        this.checkNoSameStruct(this.edgeStructs);
    }

    @SuppressWarnings("unchecked")
    public <ES extends ElementStructV1> List<ES> structs(ElemType type) {
        if (type.isVertex()) {
            return (List<ES>) this.vertexStructs;
        } else {
            assert type.isEdge();
            return (List<ES>) this.edgeStructs;
        }
    }

    @SuppressWarnings("unchecked")
    public <ES extends ElementStructV1> List<ES> structs() {
        return (List<ES>) ListUtils.union(this.vertexStructs, this.edgeStructs);
    }

    private <ES extends ElementStructV1> void checkNoSameStruct(
                                              List<ES> structs) {
        Set<String> uniqueKeys = structs.stream()
                                        .map(ElementStructV1::uniqueKey)
                                        .collect(Collectors.toSet());
        E.checkArgument(structs.size() == uniqueKeys.size(),
                        "Please ensure there is no same mapping in %s",
                        structs);
    }
}
