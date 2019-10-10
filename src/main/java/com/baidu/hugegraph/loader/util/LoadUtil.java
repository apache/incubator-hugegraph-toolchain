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

package com.baidu.hugegraph.loader.util;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.beust.jcommander.JCommander;

public final class LoadUtil {

    public static boolean needHandleFailures(LoadOptions options) {
        if (!options.incrementalMode || !options.reloadFailure) {
            return false;
        }

        String dir = LoadUtil.getStructDirPrefix(options);
        File dirFile = FileUtils.getFile(dir);
        if (!dirFile.exists()) {
            return false;
        }

        return true;
    }

    public static String getStructDirPrefix(LoadOptions options) {
        String structFileName = options.file;
        int lastDotIdx = structFileName.lastIndexOf(Constants.DOT_STR);
        return structFileName.substring(0, lastDotIdx);
    }

    public static void exitWithUsage(JCommander commander, int code) {
        commander.usage();
        System.exit(code);
    }
}
