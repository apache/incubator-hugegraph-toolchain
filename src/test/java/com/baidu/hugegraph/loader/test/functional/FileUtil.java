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

package com.baidu.hugegraph.loader.test.functional;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

public class FileUtil {

    private static final String DEFAULT_CHARSET = "UTF-8";

    public static String newCSVLine(Object... parts) {
        return StringUtils.join(parts, ",");
    }

    public static void clear(String fileName) {
        File file = org.apache.commons.io.FileUtils.getFile(fileName);
        checkFileValid(file, true);
        try {
            FileUtils.write(file, "", DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to clear file '%s'", fileName), e);
        }
    }

    public static void append(String fileName, String line) {
        append(fileName, line, DEFAULT_CHARSET);
    }

    public static void append(String fileName, String line, String charset) {
        File file = org.apache.commons.io.FileUtils.getFile(fileName);
        checkFileValid(file, true);
        try {
            FileUtils.writeLines(file, charset, ImmutableList.of(line), true);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to append line '%s' to file '%s'",
                      line, fileName), e);
        }
    }

    public static void delete(String fileName) {
        try {
            FileUtils.forceDelete(FileUtils.getFile(fileName));
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                      "Failed to delete file '%s'", fileName), e);
        }
    }

    private static void checkFileValid(File file, boolean autoCreate) {
        if (!file.exists()) {
            if (autoCreate) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(String.format(
                              "Failed to create file '%s'", file.getName()), e);
                }
            } else {
                throw new RuntimeException(String.format(
                          "Please ensure the file '%s' exist", file.getName()));
            }
        } else {
            if (!file.isFile() || !file.canWrite()) {
                throw new RuntimeException(String.format(
                          "Please ensure the file '%s' is writable",
                          file.getName()));
            }
        }
    }
}
