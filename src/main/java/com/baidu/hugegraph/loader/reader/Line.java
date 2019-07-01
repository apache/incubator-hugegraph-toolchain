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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.baidu.hugegraph.util.InsertionOrderUtil;

public final class Line {

    private final String rawLine;
    private String[] names;
    private Object[] values;
    private Map<String, Object> keyValues;

    public Line(String rawLine, Map<String, Object> keyValues) {
        this.rawLine = rawLine;
        this.names = null;
        this.values = null;
        this.keyValues = keyValues;
    }

    public Line(List<String> names, List<Object> values) {
        assert names.size() == values.size();
        this.rawLine = StringUtils.join(values, ",");
        this.names = names.toArray(new String[]{});
        this.values = values.toArray(new Object[]{});
        this.keyValues = null;
    }

    public Line(String rawLine, String[] names, Object[] values) {
        assert names.length == values.length;
        this.rawLine = rawLine;
        this.names = names;
        this.values = values;
        this.keyValues = null;
    }

    public String rawLine() {
        return this.rawLine;
    }

    public final List<String> names() {
        if (this.names != null) {
            return Arrays.asList(this.names);
        } else {
            assert this.keyValues != null;
            return new ArrayList<>(this.keyValues.keySet());
        }
    }

    public final List<Object> values() {
        if (this.values != null) {
            return Arrays.asList(this.values);
        } else {
            assert this.keyValues != null;
            List<String> names = this.names();
            List<Object> results = new ArrayList<>(names.size());
            names.forEach(name -> results.add(this.keyValues.get(name)));
            return results;
        }
    }

    public Map<String, Object> toMap() {
        if (this.keyValues != null) {
            return this.keyValues;
        }
        this.keyValues = InsertionOrderUtil.newMap();
        for (int i = 0, n = names.length; i < n; i++) {
            this.keyValues.put(names[i], values[i]);
        }
        return this.keyValues;
    }

    @Override
    public String toString() {
        return this.rawLine();
    }
}
