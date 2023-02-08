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

import java.util.Collections;
import java.util.Set;

import org.apache.hugegraph.loader.constant.Constants;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;

public final class ListFormat {

    private static final String DEFAULT_START_SYMBOL = "";
    private static final String DEFAULT_END_SYMBOL = "";
    private static final String DEFAULT_ELEM_DELIMITER = "|";

    @JsonProperty("start_symbol")
    private String startSymbol;
    @JsonProperty("end_symbol")
    private String endSymbol;
    @JsonProperty("elem_delimiter")
    private String elemDelimiter;
    @JsonProperty("ignored_elems")
    private Set<String> ignoredElems;

    public ListFormat() {
        this(DEFAULT_START_SYMBOL, DEFAULT_END_SYMBOL, DEFAULT_ELEM_DELIMITER);
    }

    public ListFormat(String startSymbol, String endSymbol,
                      String elemDelimiter) {
        this(startSymbol, endSymbol, elemDelimiter,
             Sets.newHashSet(Constants.EMPTY_STR));
    }

    public ListFormat(String startSymbol, String endSymbol,
                      String elemDelimiter, Set<String> ignoredElems) {
        this.startSymbol = startSymbol;
        this.endSymbol = endSymbol;
        this.elemDelimiter = elemDelimiter;
        this.ignoredElems = ignoredElems;
    }

    public String startSymbol() {
        return this.startSymbol;
    }

    public void startSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    public String endSymbol() {
        return this.endSymbol;
    }

    public void endSymbol(String endSymbol) {
        this.endSymbol = endSymbol;
    }

    public String elemDelimiter() {
        return this.elemDelimiter;
    }

    public void elemDelimiter(String elemDelimiter) {
        this.elemDelimiter = elemDelimiter;
    }

    public Set<String> ignoredElems() {
        return Collections.unmodifiableSet(this.ignoredElems);
    }
}
