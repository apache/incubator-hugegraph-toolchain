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

package com.baidu.hugegraph.loader.source.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.constant.Checkable;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphSource implements Checkable {

    private static final Logger LOG = Log.logger(GraphSource.class);

    @JsonProperty("vertices")
    private final List<VertexSource> vertexSources;
    @JsonProperty("edges")
    private final List<EdgeSource> edgeSources;

    public GraphSource() {
        this.vertexSources = new ArrayList<>();
        this.edgeSources = new ArrayList<>();
    }

    public static GraphSource of(String filePath) {
        File file = FileUtils.getFile(filePath);
        try {
            String json = FileUtils.readFileToString(file, Constants.CHARSET);
            GraphSource source = JsonUtil.fromJson(json, GraphSource.class);
            source.check();
            return source;
        } catch (IOException | IllegalArgumentException e) {
            throw new LoadException("Read graph source file '%s' error",
                                    e, filePath);
        }
    }

    @Override
    public void check() throws IllegalArgumentException {
        LOG.info("Checking vertex sources");
        this.vertexSources.forEach(VertexSource::check);
        this.checkNoSameSource(this.vertexSources);
        LOG.info("Checking edge sources");
        this.edgeSources.forEach(EdgeSource::check);
        this.checkNoSameSource(this.edgeSources);
    }

    public List<VertexSource> vertexSources() {
        return this.vertexSources;
    }

    public List<EdgeSource> edgeSources() {
        return this.edgeSources;
    }

    private <T extends ElementSource> void checkNoSameSource(List<T> sources) {
        Set<String> uniqueKeys = sources.stream().map(ElementSource::uniqueKey)
                                        .collect(Collectors.toSet());
        E.checkArgument(sources.size() == uniqueKeys.size(),
                        "Please ensure there is no same source in %s", sources);
    }
}
