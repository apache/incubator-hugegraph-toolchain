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

package org.apache.hugegraph.loader.reader.line;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.util.E;
import com.google.common.collect.Sets;

public final class Line {

    private String rawLine;
    private String[] names;
    private Object[] values;

    public Line(String rawLine, String[] names, Object[] values) {
        E.checkArgumentNotNull(rawLine, "The rawLine can't be null");
        E.checkArgumentNotNull(names, "The names can't be null");
        E.checkArgumentNotNull(values, "The values can't be null");
        if (names.length != values.length) {
            E.checkArgument(names.length == values.length,
                            "The length of names %s should be same as values %s",
                            Arrays.toString(names), Arrays.toString(values));
        }
        this.rawLine = rawLine;
        this.names = names;
        this.values = values;
    }

    public String rawLine() {
        return this.rawLine;
    }

    public void rawLine(String rawLine) {
        this.rawLine = rawLine;
    }

    public final String[] names() {
        return this.names;
    }

    public void names(String[] names) {
        this.names = names;
    }

    public final Object[] values() {
        return this.values;
    }

    public void values(Object[] values) {
        this.values = values;
    }

    public void retainAll(String[] names) {
        Set<String> set = Sets.newHashSet(names);
        List<String> retainedNames = new ArrayList<>();
        List<Object> retainedValues = new ArrayList<>();
        for (int i = 0; i < this.names.length; i++) {
            if (set.contains(this.names[i])) {
                retainedNames.add(this.names[i]);
                retainedValues.add(this.values[i]);
            }
        }
        this.names = retainedNames.toArray(new String[]{});
        this.values = retainedValues.toArray(new Object[]{});
    }

    @Override
    public String toString() {
        return this.rawLine;
    }
}
