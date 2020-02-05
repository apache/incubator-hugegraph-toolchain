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

package com.baidu.hugegraph.loader.failure;

import java.nio.file.Paths;

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.exception.ReadException;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.Log;

public final class FailLogger {

    private static final Logger LOG = Log.logger(FailLogger.class);

    private final FailWriter parseWriter;
    private final FailWriter insertWriter;

    public FailLogger(LoadContext context, InputStruct struct) {
        String dir = LoadUtil.getStructDirPrefix(context.options());
        String prefix = struct.id();
        String charset = struct.input().charset();
        /*
         * If user prepare to hanlde failures, new failure record will write
         * to the new failure file, and the old failure file need to rename
         */
        boolean append = !context.options().reloadFailure;

        String path = path(dir, prefix, Constants.PARSE_FAILURE_SUFFIX);
        this.parseWriter = new FailWriter(path, charset, append);
        path = path(dir, prefix, Constants.INSERT_FAILURE_SUFFIX);
        this.insertWriter = new FailWriter(path, charset, append);
    }

    private static String path(String dir, String prefix, String suffix) {
        // The path format like: mapping/current/file1.parse-error
        String name = prefix + Constants.DOT_STR + suffix;
        return Paths.get(dir, Constants.FAILURE_CURRENT_DIR, name).toString();
    }

    public void write(ReadException e) {
        this.parseWriter.write(e);
    }

    public void write(ParseException e) {
        this.parseWriter.write(e);
    }

    public void write(InsertException e) {
        this.insertWriter.write(e);
    }

    public void close() {
        this.parseWriter.close();
        this.insertWriter.close();
    }
}
