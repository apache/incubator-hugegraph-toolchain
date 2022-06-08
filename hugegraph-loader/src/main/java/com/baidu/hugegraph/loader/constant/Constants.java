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

package com.baidu.hugegraph.loader.constant;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public final class Constants {

    public static final int EXIT_CODE_NORM = 0;
    public static final int EXIT_CODE_ERROR = -1;

    public static final Charset CHARSET = Charsets.UTF_8;
    public static final String HTTP_PREFIX = "http://";
    public static final String HTTPS_PREFIX = "https://";
    public static final String JSON_SUFFIX = ".json";
    public static final String GROOVY_SCHEMA = "schema";
    public static final String TRUST_STORE_FILE = "conf/hugegraph.truststore";

    public static final String FIELD_VERSION = "version";
    public static final String V1_STRUCT_VERSION = "1.0";
    public static final String V2_STRUCT_VERSION = "2.0";

    public static final String EMPTY_STR = "";
    public static final String BLANK_STR = " ";
    public static final String DOT_STR = ".";
    public static final String MINUS_STR = "-";
    public static final String UNDERLINE_STR = "_";
    public static final String COMMA_STR = ",";
    public static final String TAB_STR = "\t";
    public static final String NULL_STR = "NULL";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP = "timestamp";
    public static final String TIME_ZONE = "GMT+8";
    public static final String SKIPPED_LINE_REGEX = "(^#|^//).*|";
    public static final String FAILURE = "failure";
    public static final String FAILURE_DATA = "failure-data";
    public static final String FAILURE_SUFFIX = ".error";
    public static final String HEADER_SUFFIX = ".header";
    public static final String TEMP_SUFFIX = ".temp";
    public static final String DEDUP_SUFFIX = ".dedup";
    public static final String LOAD_PROGRESS = "load-progress";

    public static final String PARSE_WORKER = "parse-worker-%s";
    public static final String BATCH_WORKER = "batch-worker-%d";
    public static final String SINGLE_WORKER = "single-worker-%d";
    public static final long BATCH_PRINT_FREQ = 10_000_000L;
    public static final long SINGLE_PRINT_FREQ = 10_000L;

    public static final int TIME_RANGE_CAPACITY = 1000;

    public static final int STATUS_OK = 200;
    public static final int STATUS_BAD_REQUEST = 400;
    public static final int STATUS_UNAUTHORIZED = 401;
    public static final int STATUS_NOT_FOUND = 404;

    public static final int NO_LIMIT = -1;
    public static final int VERTEX_ID_LIMIT = 128;
    public static final String[] SEARCH_LIST = new String[]{":", "!"};
    public static final String[] TARGET_LIST = new String[]{"`:", "`!"};

    public static final String CDC_DATA = "data";
    public static final String CDC_OP = "op";
}
