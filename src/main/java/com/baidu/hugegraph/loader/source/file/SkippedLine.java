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

package com.baidu.hugegraph.loader.source.file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.reader.Line;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SkippedLine {

    @JsonProperty("regex")
    private final String regex;

    private transient Matcher matcher;

    public SkippedLine() {
        this.regex = Constants.SKIPPED_LINE_REGEX;
        this.matcher = null;
    }

    public String regex() {
        return this.regex;
    }

    private Matcher matcher() {
        if (this.matcher == null) {
            this.matcher = Pattern.compile(this.regex).matcher(Constants.EMPTY_STR);
        }
        return this.matcher;
    }

    public boolean matches(String line) {
        return this.matcher().reset(line).matches();
    }
}
