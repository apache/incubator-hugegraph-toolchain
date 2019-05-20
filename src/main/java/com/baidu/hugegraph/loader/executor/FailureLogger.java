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

import org.slf4j.Logger;

import com.baidu.hugegraph.loader.exception.InsertException;
import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.util.JsonUtil;
import com.baidu.hugegraph.util.Log;

public class FailureLogger {

    private final Logger log;

    public static FailureLogger logger(String name) {
        return new FailureLogger(name);
    }

    private FailureLogger(String name) {
        this.log = Log.logger(name);
    }

    public void error(ParseException e) {
        this.log.error(">>>> PARSE ERROR: {}", e.getMessage());
        this.log.error("{}", e.line());
    }

    public void error(InsertException e) {
        this.log.error(">>>> INSERT ERROR: {}", e.getMessage());
        this.log.error("{}", JsonUtil.toJson(e.element()));
    }
}
