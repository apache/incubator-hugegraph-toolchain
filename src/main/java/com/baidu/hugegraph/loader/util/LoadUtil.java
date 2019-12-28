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
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.util.E;
import com.beust.jcommander.JCommander;

public final class LoadUtil {

    public static long lastLoaded(LoadContext context, ElemType type) {
        return 0L;
//        if (context.options().incrementalMode) {
//            return context.oldProgress().totalLoaded(type);
//        } else {
//            return 0L;
//        }
    }

    public static String getStructDirPrefix(LoadOptions options) {
        String structFileName = options.file;
        E.checkArgument(options.file.endsWith(Constants.JSON_SUFFIX),
                        "The mapping description file name must be end with %s",
                        Constants.JSON_SUFFIX);
        int lastDotIdx = structFileName.lastIndexOf(Constants.DOT_STR);
        return structFileName.substring(0, lastDotIdx);
    }

    public static void moveFailureFiles(LoadContext context) {
        LoadOptions options = context.options();
        if (!LoadUtil.needHandleFailures(options)) {
            return;
        }

        String dir = LoadUtil.getStructDirPrefix(options);
        File structDir = FileUtils.getFile(dir);
        File currentDir = FileUtils.getFile(structDir,
                                            Constants.FAILURE_CURRENT_DIR);
        File[] subFiles = currentDir.listFiles((d, name) -> {
            return name.endsWith(Constants.PARSE_FAILURE_SUFFIX) ||
                   name.endsWith(Constants.INSERT_FAILURE_SUFFIX);
        });
        if (subFiles == null || subFiles.length == 0) {
            return;
        }
        for (File srcFile : subFiles) {
            String destName = getHistoryFailureFileName(context,
                                                        srcFile.getName());
            try {
                FileUtils.moveFile(srcFile, new File(destName));
            } catch (IOException e) {
                throw new LoadException("Failed to move old failure file '%s'",
                                        e, srcFile);
            }
        }
    }

    private static boolean needHandleFailures(LoadOptions options) {
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

    private static String getHistoryFailureFileName(LoadContext context,
                                                    String fileName) {
        int idx = fileName.lastIndexOf(Constants.DOT_STR);
        E.checkState(idx > 0, "Invalid failure file name '%s'", fileName);

        // like person-5117b102
        String uniqueKey = fileName.substring(0, idx);
        // like parse-error or insert-error
        String fileSuffix = fileName.substring(idx + 1);

        String dir = LoadUtil.getStructDirPrefix(context.options());
        String name = context.timestamp() + Constants.DOT_STR + fileSuffix;
        return Paths.get(dir, Constants.FAILURE_HISTORY_DIR, uniqueKey, name)
                    .toString();
    }

    public static void exitWithUsage(JCommander commander, int code) {
        commander.usage();
        System.exit(code);
    }
}
