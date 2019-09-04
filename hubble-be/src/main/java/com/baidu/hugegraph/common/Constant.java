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

package com.baidu.hugegraph.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public final class Constant {

    public static final Charset CHARSET = UTF_8;

    public static final Set<String> LANGUAGES = ImmutableSet.of(
            "en_US",
            "zh_CN"
    );

    public static final String MODULE_NAME = "hubble-be";
    public static final String CONFIG_FILE = "hugegraph-hubble.properties";

    public static final String CONTROLLER_PACKAGE =
                               "com.baidu.hugegraph.controller";

    public static final String COOKIE_USER = "user";

    public static final Set<String> LIKE_WILDCARDS = ImmutableSet.of(
            "%", "_", "^", "[", "]"
    );
}
