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

package com.baidu.hugegraph.loader.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphSource {

    @JsonProperty("vertices")
    private List<VertexSource> vertexSources;
    @JsonProperty("edges")
    private List<EdgeSource> edgeSources;

    public GraphSource() {
        this.vertexSources = new ArrayList<>();
        this.edgeSources = new ArrayList<>();
    }

    public static GraphSource of(String filePath) {
        try {
            File file = FileUtils.getFile(filePath);
            String json = FileUtils.readFileToString(file, "UTF-8");
            return JsonUtil.fromJson(json, GraphSource.class);
        } catch (Exception e) {
            throw new LoadException("Read data source file '%s' error",
                                    e, filePath);
        }
    }

    public List<VertexSource> vertexSources() {
        return this.vertexSources;
    }

    public List<EdgeSource> edgeSources() {
        return this.edgeSources;
    }
}
