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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public final class LoadOptions {

    private final int CPUS = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"-f", "--file"}, required = true, arity = 1,
               validateWith = {FileValidator.class},
               description = "The path of the data struct description file")
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

    @Parameter(names = {"--token"}, arity = 1,
               description = "The token of graph for authentication")
    public String token = null;

    @Parameter(names = {"--incremental-mode"}, arity = 1,
              description = "Load data from the breakpoint of last time")
    public boolean incrementalMode = false;

    @Parameter(names = {"--num-threads"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "The number of threads to use")
    public int numThreads = CPUS;

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
    public int retryTimes = 0;

    @Parameter(names = {"--retry-interval"}, arity = 1,
               validateWith = {PositiveValidator.class},
               description = "Setting the interval time before retrying")
    public int retryInterval = 10;

    @Parameter(names = {"--test-mode"}, arity = 1,
               description = "Whether the hugegraph-loader work in test mode")
    public boolean testMode = false;

    @Parameter(names = {"--help"}, help = true,
               description = "Print usage of HugeGraphLoader")
    public boolean help;

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
