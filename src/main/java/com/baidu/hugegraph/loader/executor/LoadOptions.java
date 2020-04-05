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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.util.LoadUtil;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public final class LoadOptions {

    private static final Logger LOG = Log.logger(LoadOptions.class);

    private final int CPUS = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"-f", "--file"}, required = true, arity = 1,
               validateWith = {FileValidator.class},
               description = "The path of the data mapping description file")
    public String file;

    @Parameter(names = {"-s", "--schema"}, arity = 1,
               validateWith = {FileValidator.class},
               description = "The schema file path which to create manually")
    public String schema;

    @Parameter(names = {"-g", "--graph"}, required = true, arity = 1,
               description = "The namespace of the graph to load into")
    public String graph;

    @Parameter(names = {"-h", "--host"}, arity = 1,
               validateWith = {UrlValidator.class},
               description = "The host/IP of HugeGraphServer")
    public String host = "localhost";

    @Parameter(names = {"-p", "--port"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The port of HugeGraphServer")
    public int port = 8080;

    @Parameter(names = {"--username"}, arity = 1,
               description = "The username of graph for authentication")
    public String username = null;

    @Parameter(names = {"--token"}, arity = 1,
               description = "The token of graph for authentication")
    public String token = null;

    @Parameter(names = {"--clear-all-data"}, arity = 1,
               description = "Whether to clear all old data before loading")
    public boolean clearAllData = false;

    @Parameter(names = {"--clear-timeout"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The timeout waiting for clearing all data ")
    public int clearTimeout = 240;

    @Parameter(names = {"--incremental-mode"}, arity = 1,
               description = "Load data from the breakpoint of last time")
    public boolean incrementalMode = false;

    @Parameter(names = {"--failure-mode"}, arity = 1,
               description = "Load data from the failure records, in this " +
                             "mode, only full load is supported, any read " +
                             "or parsing errors will cause load task stop")
    public boolean failureMode = false;

    @Parameter(names = {"--batch-insert-threads"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The number of threads to execute batch insert")
    public int batchInsertThreads = CPUS;

    @Parameter(names = {"--single-insert-threads"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The number of threads to execute single insert")
    public int singleInsertThreads = 8;

    @Parameter(names = {"--max-conn"}, arity = 1,
               description = "Max number of HTTP connections to server")
    public int maxConnections = CPUS * 4;

    @Parameter(names = {"--max-conn-per-route"}, arity = 1,
               description = "Max number of HTTP connections to each route")
    public int maxConnectionsPerRoute = CPUS * 2;

    @Parameter(names = {"--batch-size"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The number of lines in each submit")
    public int batchSize = 500;

    @Parameter(names = {"--shutdown-timeout"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The timeout of awaitTermination in seconds")
    public int shutdownTimeout = 10;

    @Parameter(names = {"--check-vertex"}, arity = 1,
               description = "Check vertices exists while inserting edges")
    public boolean checkVertex = false;

    @Parameter(names = {"--max-read-errors"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The maximum number of rows that read error " +
                             "before exiting")
    public int maxReadErrors = 1;

    @Parameter(names = {"--max-parse-errors"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The maximum number of rows that parse error " +
                             "before exiting")
    public int maxParseErrors = 1;

    @Parameter(names = {"--max-insert-errors"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The maximum number of rows that insert error " +
                             "before exiting")
    public int maxInsertErrors = 500;

    @Parameter(names = {"--timeout"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The timeout of HugeClient request")
    public int timeout = 60;

    @Parameter(names = {"--retry-times"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "Setting the max retry times when loading timeout")
    public int retryTimes = 3;

    @Parameter(names = {"--retry-interval"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "Setting the interval time before retrying")
    public int retryInterval = 10;

    @Parameter(names = {"--dry-run"}, arity = 1,
               description = "Dry run means that only parse but doesn't load")
    public boolean dryRun = false;

    @Parameter(names = {"--print-progress"}, arity = 1,
               description = "Whether to print real-time load progress")
    public boolean printProgress = true;

    @Parameter(names = {"--test-mode"}, arity = 1,
               description = "Whether the hugegraph-loader work in test mode")
    public boolean testMode = false;

    @Parameter(names = {"--help"}, help = true,
               description = "Print usage of HugeGraphLoader")
    public boolean help;

    public static LoadOptions parseOptions(String[] args) {
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
                        "The mapping file must be specified");
        E.checkArgument(options.file.endsWith(Constants.JSON_SUFFIX),
                        "The mapping file name must be end with %s",
                        Constants.JSON_SUFFIX);
        File mappingFile = new File(options.file);
        if (!mappingFile.canRead()) {
            LOG.error("The mapping file must be readable: '{}'", mappingFile);
            LoadUtil.exitWithUsage(commander, Constants.EXIT_CODE_ERROR);
        }

        // Check option "-g"
        E.checkArgument(!StringUtils.isEmpty(options.graph),
                        "The graph must be specified");
        // Check option "-h"
        if (!options.host.startsWith(Constants.HTTP_PREFIX)) {
            options.host = Constants.HTTP_PREFIX + options.host;
        }
        // Check option --incremental-mode and --failure-mode
        E.checkArgument(!(options.incrementalMode && options.failureMode),
                        "The option --incremental-mode and --failure-mode " +
                        "can't be true at same time");
        if (options.failureMode) {
            LOG.info("The failure-mode will scan the entire error file");
            options.maxReadErrors = Constants.NO_LIMIT;
            options.maxParseErrors = Constants.NO_LIMIT;
            options.maxInsertErrors = Constants.NO_LIMIT;
        }
        return options;
    }

    public static class UrlValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            String regex = "^(http://)?"
                    + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP URL, like: 10.0.0.1
                    + "|" // Or domain name
                    + "([0-9a-z_!~*'()-]+\\.)*[0-9a-z_!~*'()-]+)$";
            if (!value.matches(regex)) {
                throw new ParameterException(String.format(
                          "Invalid url value of args '%s': '%s'", name, value));
            }
        }
    }

    public static class DirectoryValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            File file = new File(value);
            if (!file.exists() || !file.isDirectory()) {
                throw new ParameterException(String.format(
                          "Ensure the directory exists and is indeed a " +
                          "directory instead of a file: '%s'", value));
            }
        }
    }

    public static class FileValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            File file = new File(value);
            if (!file.exists() || !file.isFile()) {
                throw new ParameterException(String.format(
                          "Ensure the file exists and is indeed a file " +
                          "instead of a directory: '%s'", value));
            }
        }
    }

    public static class PositiveValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            int retry = Integer.parseInt(value);
            if (retry <= 0) {
                throw new ParameterException(String.format(
                          "Parameter '%s' should be positive, but got '%s'",
                          name, value));
            }
        }
    }
}
