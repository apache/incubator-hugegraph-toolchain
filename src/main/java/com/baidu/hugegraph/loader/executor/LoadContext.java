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

package com.baidu.hugegraph.loader.executor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.constant.ElemType;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.failure.FailureLogger;
import com.baidu.hugegraph.loader.progress.LoadProgress;
import com.baidu.hugegraph.loader.struct.ElementStruct;
import com.baidu.hugegraph.loader.summary.LoadSummary;
import com.baidu.hugegraph.loader.util.DateUtil;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.beust.jcommander.JCommander;

public final class LoadContext {

    private static final Logger LOG = Log.logger(LoadContext.class);

    // The time at the beginning of loading, accurate to seconds
    private final String timestamp;
    private ElemType loadingType;

    private volatile boolean stopped;
    private final LoadOptions options;
    private final LoadSummary summary;
    // The old progress just used to read
    private final LoadProgress oldProgress;
    private final LoadProgress newProgress;
    // Each Element struct corresponds to a FailureLogger
    private final Map<String, FailureLogger> loggers;

    public LoadContext(String[] args) {
        this.timestamp = DateUtil.now(Constants.DATE_FORMAT);
        this.loadingType = null;
        this.stopped = false;
        this.options = parseCheckOptions(args);
        this.summary = new LoadSummary();
        this.oldProgress = parseLoadProgress(this.options);
        this.newProgress = new LoadProgress();
        this.loggers = new ConcurrentHashMap<>();
    }

    public String timestamp() {
        return this.timestamp;
    }

    public ElemType loadingType() {
        return this.loadingType;
    }

    public void loadingType(ElemType type) {
        this.loadingType = type;
    }

    public boolean stopped() {
        return this.stopped;
    }

    public void stopLoading() {
        this.stopped = true;
    }

    public LoadOptions options() {
        return this.options;
    }

    public LoadSummary summary() {
        return this.summary;
    }

    public LoadProgress oldProgress() {
        return this.oldProgress;
    }

    public LoadProgress newProgress() {
        return this.newProgress;
    }

    public FailureLogger failureLogger(ElementStruct struct) {
        return this.loggers.computeIfAbsent(struct.uniqueKey(), k -> {
            LOG.info("Create failure logger for struct '{}'", struct);
            return new FailureLogger(this, struct);
        });
    }

    public void close() {
        for (FailureLogger logger : this.loggers.values()) {
            logger.close();
        }
        try {
            this.newProgress().write(this);
        } catch (IOException e) {
            LOG.error("Failed to write load progress", e);
        }
        HugeClientHolder.close();
    }

    private static LoadOptions parseCheckOptions(String[] args) {
        LoadOptions options = new LoadOptions();
        JCommander commander = JCommander.newBuilder()
                                         .addObject(options)
                                         .build();
        commander.parse(args);
        // Print usage and exit
        if (options.help) {
            LoadUtil.exitWithUsage(commander, Constants.EXIT_CODE_NORM);
        }
        // Check options
        // Check option "-f"
        E.checkArgument(!StringUtils.isEmpty(options.file),
                        "Must specified struct description file");
        File structFile = new File(options.file);
        if (!structFile.canRead()) {
            LOG.error("Struct file must be readable: '{}'",
                      structFile.getAbsolutePath());
            LoadUtil.exitWithUsage(commander, Constants.EXIT_CODE_ERROR);
        }

        // Check option "-g"
        E.checkArgument(!StringUtils.isEmpty(options.graph),
                        "Must specified a graph");
        // Check option "-h"
        String httpPrefix = "http://";
        if (!options.host.startsWith(httpPrefix)) {
            options.host = httpPrefix + options.host;
        }
        return options;
    }

    private static LoadProgress parseLoadProgress(LoadOptions options) {
        if (!options.incrementalMode) {
            return new LoadProgress();
        }

        String dir = LoadUtil.getStructFilePrefix(options);
        File dirFile = FileUtils.getFile(dir);
        if (!dirFile.exists()) {
            return new LoadProgress();
        }

        File[] subDirs = dirFile.listFiles();
        if (subDirs == null || subDirs.length == 0) {
            return new LoadProgress();
        }

        String lastTime = Constants.EMPTY_STR;
        for (File subDir : subDirs) {
            String subDirName = subDir.getName();
            if (StringUtils.compare(subDirName, lastTime) >= 0) {
                lastTime = subDirName;
            }
        }
        if (lastTime.equals(Constants.EMPTY_STR)) {
            return new LoadProgress();
        }

        String fileName = LoadProgress.format(options, lastTime);
        try {
            return LoadProgress.read(fileName);
        } catch (IOException e) {
            throw new LoadException("Failed to read progress file", e);
        }
    }
}
