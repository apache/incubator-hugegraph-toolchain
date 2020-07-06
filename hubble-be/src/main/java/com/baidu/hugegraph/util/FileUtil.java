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

package com.baidu.hugegraph.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.apache.commons.io.IOUtils;

import com.baidu.hugegraph.exception.InternalException;

public final class FileUtil {

    public static long countLines(String path) {
        return countLines(new File(path));
    }

    /**
     * NOTE: If there is no blank line at the end of the file,
     * one line will be missing
     */
    public static long countLines(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format(
                      "The file %s doesn't exist", file));
        }
        long fileLength = file.length();
        LineNumberReader lineReader = null;
        try {
            FileReader fileReader = new FileReader(file);
            lineReader = new LineNumberReader(fileReader);
            lineReader.skip(fileLength);
            return lineReader.getLineNumber();
        } catch (IOException e) {
            throw new InternalException("Failed to count lines of file %s",
                                        file);
        } finally {
            if (lineReader != null) {
                IOUtils.closeQuietly(lineReader);
            }
        }
    }
}
