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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.E;

/**
 * LoadProgress was used to record progress of loading, in order to
 * continue loading when the last work was dropped out halfway.
 * The LoadProgress will only be operated by a single thread.
 */
public final class LoadProgress extends HashMap<String, InputProgress> {

    public InputProgress addStruct(InputStruct struct) {
        E.checkNotNull(struct, "mapping mapping");
        this.put(struct.id(), new InputProgress(struct));
        return this.get(struct.id());
    }

    public void markLoaded(InputStruct struct, boolean markAll) {
        InputProgress progress = this.get(struct.id());
        E.checkArgumentNotNull(progress, "Invalid mapping '%s'", struct);
        progress.markLoaded(markAll);
    }

    public void write(LoadContext context) throws IOException {
        String fileName = format(context.options(), context.timestamp());
        File file = FileUtils.getFile(fileName);
        String json = JsonUtil.toJson(this);
        FileUtils.write(file, json, Constants.CHARSET, false);
    }

    public static LoadProgress read(File file) throws IOException {
        String json = FileUtils.readFileToString(file, Constants.CHARSET);
        return JsonUtil.fromJson(json, LoadProgress.class);
    }

    public static LoadProgress parse(LoadOptions options) {
        if (!options.incrementalMode) {
            return new LoadProgress();
        }

        String dir = LoadUtil.getStructDirPrefix(options);
        File dirFile = FileUtils.getFile(dir);
        if (!dirFile.exists()) {
            return new LoadProgress();
        }

        File[] subFiles = dirFile.listFiles((d, name) -> {
            return name.startsWith(Constants.LOAD_PROGRESS);
        });
        if (subFiles == null || subFiles.length == 0) {
            return new LoadProgress();
        }

        // Sort progress files by time, then get the last progress file
        List<File> progressFiles = Arrays.asList(subFiles);
        progressFiles.sort(Comparator.comparing(File::getName));
        File lastProgressFile = progressFiles.get(progressFiles.size() - 1);
        try {
            return LoadProgress.read(lastProgressFile);
        } catch (IOException e) {
            throw new LoadException("Failed to read progress file", e);
        }
    }

    public static String format(LoadOptions options, String timestamp) {
        String dir = LoadUtil.getStructDirPrefix(options);
        String name = Constants.LOAD_PROGRESS + Constants.UNDERLINE_STR +
                      timestamp;
        return Paths.get(dir, name).toString();
    }
}
