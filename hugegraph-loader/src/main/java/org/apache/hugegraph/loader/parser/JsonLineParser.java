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

package org.apache.hugegraph.loader.parser;

import java.util.Map;

import org.apache.hugegraph.loader.exception.ReadException;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.rest.SerializeException;

public class JsonLineParser implements LineParser {

    @Override
    public Line parse(String[] header, String rawLine) {
        Map<String, Object> keyValues;
        try {
            keyValues = JsonUtil.convertMap(rawLine, String.class,
                                            Object.class);
            String[] names = names(keyValues);
            Object[] values = values(keyValues, names);
            return new Line(rawLine, names, values);
        } catch (SerializeException e) {
            throw new ReadException(rawLine, "Deserialize line '%s' error",
                                    e, rawLine);
        }
    }

    @Override
    public String[] split(String rawLine) {
        throw new UnsupportedOperationException("JsonLineParser.split()");
    }

    private static String[] names(Map<String, Object> keyValues) {
        return keyValues.keySet().toArray(new String[]{});
    }

    private static Object[] values(Map<String, Object> keyValues,
                                   String[] names) {
        Object[] values = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = keyValues.get(names[i]);
        }
        return values;
    }
}
