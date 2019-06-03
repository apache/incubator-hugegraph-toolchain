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

package com.baidu.hugegraph.loader.source.desc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.LoadContext;
import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphDesc implements Checkable {

    private static final Logger LOG = Log.logger(GraphDesc.class);

    @JsonProperty("vertices")
    private final List<VertexDesc> vertexDescs;
    @JsonProperty("edges")
    private final List<EdgeDesc> edgeDescs;

    public GraphDesc() {
        this.vertexDescs = new ArrayList<>();
        this.edgeDescs = new ArrayList<>();
    }

    public static GraphDesc of(LoadContext context) {
        LoadOptions options = context.options();
        File file = FileUtils.getFile(options.file);
        try {
            String json = FileUtils.readFileToString(file, Constants.CHARSET);
            GraphDesc source = JsonUtil.fromJson(json, GraphDesc.class);
            source.check();
            return source;
        } catch (IOException | IllegalArgumentException e) {
            throw new LoadException("Read graph desc file '%s' error",
                                    e, options.file);
        }
    }

    @Override
    public void check() throws IllegalArgumentException {
        LOG.info("Checking vertex descs");
        this.vertexDescs.forEach(VertexDesc::check);
        this.checkNoSameSource(this.vertexDescs);
        LOG.info("Checking edge descs");
        this.edgeDescs.forEach(EdgeDesc::check);
        this.checkNoSameSource(this.edgeDescs);
    }

    public List<VertexDesc> vertexDescs() {
        return this.vertexDescs;
    }

    public List<EdgeDesc> edgeDescs() {
        return this.edgeDescs;
    }

    private <T extends ElementDesc> void checkNoSameSource(List<T> sources) {
        Set<String> uniqueKeys = sources.stream().map(ElementDesc::uniqueKey)
                                        .collect(Collectors.toSet());
        E.checkArgument(sources.size() == uniqueKeys.size(),
                        "Please ensure there is no same desc in %s", sources);
    }
}
