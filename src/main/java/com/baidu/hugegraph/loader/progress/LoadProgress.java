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
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.constant.Constants;
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

    public static String format(LoadOptions options, String timestamp) {
        String dir = LoadUtil.getStructDirPrefix(options);
        String name = Constants.PROGRESS_FILE + Constants.BLANK_STR + timestamp;
        return Paths.get(dir, name).toString();
    }
}
