/*
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

package org.apache.hugegraph.loader.source.file;

import org.apache.hugegraph.loader.constant.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirFilter {
    private static final String DEFAULT_INCLUDE;
    private static final String DEFAULT_EXCLUDE;

    static {
        DEFAULT_INCLUDE = "";
        DEFAULT_EXCLUDE = "";
    }

    @JsonProperty("include_regex")
    String includeRegex;
    @JsonProperty("exclude_regex")
    String excludeRegex;

    private transient Matcher includeMatcher;
    private transient Matcher excludeMatcher;

    public DirFilter() {
        this.includeRegex = DEFAULT_INCLUDE;
        this.excludeRegex = DEFAULT_EXCLUDE;
        this.includeMatcher = null;
        this.excludeMatcher = null;
    }

    private Matcher includeMatcher() {
        if (this.includeMatcher == null &&
            !StringUtils.isEmpty(this.includeRegex)) {
            this.includeMatcher = Pattern.compile(this.includeRegex)
                                         .matcher(Constants.EMPTY_STR);
        }
        return this.includeMatcher;
    }

    private Matcher excludeMatcher() {
        if (this.excludeMatcher == null &&
            !StringUtils.isEmpty(this.excludeRegex)) {
            this.excludeMatcher = Pattern.compile(this.excludeRegex)
                                         .matcher(Constants.EMPTY_STR);
        }

        return this.excludeMatcher;
    }

    private boolean includeMatch(String dirName) {
        if (!StringUtils.isEmpty(this.includeRegex)) {
            return this.includeMatcher().reset(dirName).matches();
        }

        return true;
    }

    private boolean excludeMatch(String dirName) {
        if (!StringUtils.isEmpty(this.excludeRegex)) {
            return this.excludeMatcher().reset(dirName).matches();
        }

        return false;
    }

    public boolean reserved(String dirName) {
        return this.includeMatch(dirName) && (!this.excludeMatch(dirName));
    }
}
