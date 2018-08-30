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

package com.baidu.hugegraph.loader.reader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public final class Line {

    private final String rawLine;
    private final List<String> names;
    private final List<Object> values;

    public Line(String rawLine, int size) {
        this.rawLine = rawLine;
        this.names = new ArrayList<>(size);
        this.values = new ArrayList<>(size);
    }

    public Line(List<String> names, List<Object> values) {
        assert names.size() == values.size();
        this.rawLine = StringUtils.join(values, ",");;
        this.names = names;
        this.values = values;
    }

    public Line(String rawLine, List<String> names, List<Object> values) {
        assert names.size() == values.size();
        this.rawLine = rawLine;
        this.names = names;
        this.values = values;
    }

    public String rawLine() {
        return this.rawLine;
    }

    public final List<String> names() {
        return this.names;
    }

    public final List<Object> values() {
        return this.values;
    }

    public void add(String name, Object value) {
        this.names.add(name);
        this.values.add(value);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < this.names.size(); i++) {
            result.put(this.names.get(i), this.values.get(i));
        }
        return result;
    }

    @Override
    public String toString() {
        return this.rawLine();
    }
}
