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

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LoadProgress was used to record progress of loading, in order to
 * continue loading when the last work was dropped out halfway.
 * The LoadProgress will only be operated by a single thread.
 */
public final class LoadProgress {

    private static final String SERIALIZE_FILE = "progress";

    @JsonProperty("vertex")
    private final InputProgressMap vertexProgress;
    @JsonProperty("edge")
    private final InputProgressMap edgeProgress;

    public LoadProgress() {
        this.vertexProgress = new InputProgressMap();
        this.edgeProgress = new InputProgressMap();
    }

    public InputProgressMap vertex() {
        return this.vertexProgress;
    }

    public InputProgressMap edge() {
        return this.edgeProgress;
    }

    public InputProgressMap get(ElemType type) {
        if (type.isVertex()) {
            return this.vertexProgress;
        } else {
            assert type.isEdge();
            return this.edgeProgress;
        }
    }

    public long totalLoaded(ElemType type) {
        InputProgressMap lastLoadMap = type.isVertex() ?
                                       this.vertexProgress :
                                       this.edgeProgress;
        long total = 0L;
        for (InputProgress inputProgress : lastLoadMap.values()) {
            for (InputItemProgress itemProgress : inputProgress.loadedItems()) {
                total += itemProgress.offset();
            }
            if (inputProgress.loadingItem() != null) {
                total += inputProgress.loadingItem().offset();
            }
        }
        return total;
    }

    public void write(String structFileName) throws IOException {
        String fileName = getProgressFileName(structFileName);
        File file = FileUtils.getFile(fileName);
        String json = JsonUtil.toJson(this);
        FileUtils.write(file, json, Constants.CHARSET, false);
    }

    public static LoadProgress read(String structFileName) throws IOException {
        String fileName = getProgressFileName(structFileName);
        File file = FileUtils.getFile(fileName);
        if (!file.exists()) {
            return new LoadProgress();
        }
        String json = FileUtils.readFileToString(file, Constants.CHARSET);
        return JsonUtil.fromJson(json, LoadProgress.class);
    }

    private static String getProgressFileName(String structFileName) {
        int lastDotIdx = structFileName.lastIndexOf(Constants.DOT_STR);
        String prefix = structFileName.substring(0, lastDotIdx);
        return prefix + Constants.MINUS_STR + SERIALIZE_FILE;
    }
}
