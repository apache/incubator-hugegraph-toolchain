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

package com.baidu.hugegraph.loader.parser;

import java.util.Map;

import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.baidu.hugegraph.rest.SerializeException;

public class JsonLineParser implements LineParser {

    @Override
    public Line parse(String[] header, String line) {
        Map<String, Object> keyValues;
        try {
            keyValues = JsonUtil.convertMap(line, String.class, Object.class);
            return new Line(line, keyValues);
        } catch (SerializeException e) {
            throw new ParseException(line, "Deserialize line '%s' error",
                                     e, line);
        }
    }

    @Override
    public String[] split(String rawLine) {
        throw new UnsupportedOperationException("JsonLineParser.split()");
    }
}
