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

package com.baidu.hugegraph.loader.reader.line;

import java.util.Arrays;
import java.util.Map;

import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.InsertionOrderUtil;

public class Line {

    private final String rawLine;
    private String[] names;
    private Object[] values;
    private final Map<String, Object> keyValues;

    public Line(String rawLine, Map<String, Object> keyValues) {
        E.checkArgumentNotNull(rawLine, "The rawLine can't be null");
        E.checkArgumentNotNull(keyValues, "The keyValues can't be null");
        this.rawLine = rawLine;
        this.keyValues = keyValues;
        this.names = getNames(keyValues);
        this.values = getValues(keyValues, this.names);
    }

    public Line(String rawLine, String[] names, Object[] values) {
        E.checkArgumentNotNull(rawLine, "The rawLine can't be null");
        E.checkArgumentNotNull(names, "The names can't be null");
        E.checkArgumentNotNull(values, "The values can't be null");
        E.checkArgument(names.length == values.length,
                        "The length of names %s should be same as values %s");
        this.rawLine = rawLine;
        this.names = names;
        this.values = values;
        this.keyValues = InsertionOrderUtil.newMap();
        for (int i = 0; i < this.names.length; i++) {
            this.keyValues.put(this.names[i], this.values[i]);
        }
    }

    public String rawLine() {
        return this.rawLine;
    }

    public final String[] names() {
        return this.names;
    }

    public final Object[] values() {
        return this.values;
    }

    public Map<String, Object> keyValues() {
        return this.keyValues;
    }

    public void retainAll(String[] names) {
        this.keyValues.keySet().retainAll(Arrays.asList(names));
        this.names = getNames(keyValues);
        this.values = getValues(keyValues, this.names);
    }

    private static String[] getNames(Map<String, Object> keyValues) {
        return keyValues.keySet().toArray(new String[]{});
    }

    private static Object[] getValues(Map<String, Object> keyValues,
                                      String[] names) {
        Object[] values = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = keyValues.get(names[i]);
        }
        return values;
    }

    @Override
    public String toString() {
        return this.rawLine;
    }
}
