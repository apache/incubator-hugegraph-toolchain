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

import java.util.Arrays;
import java.util.Map;

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

    public final String[] names() {
        if (this.names != null) {
            return this.names;
        } else {
            assert this.keyValues != null;
            return this.keyValues.keySet().toArray(new String[]{});
        }
    }

    public final Object[] values() {
        if (this.values != null) {
            return this.values;
        } else {
            assert this.keyValues != null;
            String[] names = this.names();
            Object[] values = new Object[names.length];
            for (int i = 0; i < names.length; i++) {
                values[i] = this.keyValues.get(names[i]);
            }
            return values;
        }
    }

    public Map<String, Object> toMap() {
        if (this.keyValues != null) {
            return this.keyValues;
        }
        String[] names = this.names();
        Object[] values = this.values();
        this.keyValues = InsertionOrderUtil.newMap();
        for (int i = 0, n = names.length; i < n; i++) {
            this.keyValues.put(names[i], values[i]);
        }
        return this.keyValues;
    }

    public void retainAll(String[] names) {
        this.toMap().keySet().retainAll(Arrays.asList(names));
    }

    @Override
    public String toString() {
        return this.rawLine;
    }
}
