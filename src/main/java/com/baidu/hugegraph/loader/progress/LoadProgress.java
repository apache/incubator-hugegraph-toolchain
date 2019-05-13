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

package com.baidu.hugegraph.loader.progress;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.source.ElementSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.InsertionOrderUtil;
import com.baidu.hugegraph.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LoadProgress was used to record progress of loading, in order to continue
 * loading when the last work was dropped out halfway.
 * The LoadProgress will only be operated by a single thread
 */
public final class LoadProgress {

    // TODO: How to ensure file path under LOADER_HOME
    private static final String SERIALIZE_FILE_NAME = "progress";

    @JsonProperty("loading_elem_type")
    private ElemType loadingElemType;
    @JsonProperty("loaded_elem_sources")
    private Set<String> loadedElemSources;
    @JsonProperty("loading_elem_source")
    private String loadingElemSource;
    @JsonProperty("input_progress")
    private InputProgress inputProgress;

    public LoadProgress() {
        this.loadingElemType = ElemType.VERTEX;
        this.loadedElemSources = InsertionOrderUtil.newSet();
        this.loadingElemSource = null;
        this.inputProgress = null;
    }

    public ElemType loadingElemType() {
        return this.loadingElemType;
    }

    public void loadingElemType(ElemType elemType) {
        if (this.loadingElemType.isEdge() && elemType.isVertex()) {
            throw new IllegalArgumentException("Must load vertex than edge");
        }
        if (this.loadingElemType != elemType) {
            this.loadingElemType = elemType;
            this.loadedElemSources = InsertionOrderUtil.newSet();
            this.loadingElemSource = null;
            this.inputProgress = null;
        }
    }

    public Set<String> loadedElemSources() {
        return this.loadedElemSources;
    }

    public void loadedElemSources(ElementSource source) {
        E.checkNotNull(source, "loaded element source");
        E.checkNotNull(source.uniqueKey(), "loaded element source unique key");
        this.loadedElemSources.add(source.uniqueKey());
        // If current loading element source finished, reset it
        if (source.uniqueKey().equals(this.loadingElemSource)) {
            this.loadingElemSource = null;
            this.inputProgress = null;
        }
    }

    public String loadingElemSource() {
        return this.loadingElemSource;
    }

    public void loadingElemSource(ElementSource source) {
        E.checkNotNull(source, "loading element source");
        E.checkNotNull(source.uniqueKey(), "loading element source unique key");
        if (!source.uniqueKey().equals(this.loadingElemSource)) {
            this.loadingElemSource = source.uniqueKey();
            this.inputProgress = null;
        }
    }

    public InputProgress inputSource() {
        return inputProgress;
    }

    public void inputSource(InputProgress source) {
        this.inputProgress = source;
    }

    public void write() throws IOException {
        File file = FileUtils.getFile(SERIALIZE_FILE_NAME);
        String json = JsonUtil.toJson(this);
        FileUtils.write(file, json, Charset.defaultCharset(), false);
    }

    public static LoadProgress read() throws IOException {
        File file = FileUtils.getFile(SERIALIZE_FILE_NAME);
        String json = FileUtils.readFileToString(file, Charset.defaultCharset());
        return JsonUtil.fromJson(json, LoadProgress.class);
    }
}
