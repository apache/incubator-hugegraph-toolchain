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

package com.baidu.hugegraph.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;

public final class GremlinUtil {

    private static final Set<String> LIMIT_SUFFIXES = ImmutableSet.of(
            // vertex
            ".V()", ".out()", ".in()", ".both()", ".outV()", ".inV()",
            ".bothV()", ".otherV()",
            // edge
            ".E()", ".outE()", ".inE()", ".bothE()",
            // path
            ".path()", ".simplePath()", ".cyclicPath()",
            // has
            ".hasLabel(STR)", ".hasLabel(NUM)"
    );

    private static final String[] COMPILE_SEARCH_LIST = new String[]{
            "\\.", "\\(", "\\)"
    };
    private static final String[] COMPILE_TARGET_LIST = new String[]{
            "\\\\.", "\\\\(", "\\\\)"
    };

    private static final String[] ESCAPE_SEARCH_LIST = new String[]{
            "\\", "\"", "'", "\n"
    };
    private static final String[] ESCAPE_TARGET_LIST = new String[]{
            "\\\\", "\\\"", "\\'", "\\n"
    };

    private static final Map<String, Pattern> limitPatterns =
                                              compileToPattern(LIMIT_SUFFIXES);

    public static String escapeId(Object id) {
        if (!(id instanceof String)) {
            return id.toString();
        }
        String text = (String) id;
        text = StringUtils.replaceEach(text, ESCAPE_SEARCH_LIST,
                                       ESCAPE_TARGET_LIST);
        return (String) escape(text);
    }

    public static Object escape(Object object) {
        if (!(object instanceof String)) {
            return object;
        }
        return StringUtils.wrap((String) object, '\'');
    }

    public static String optimizeLimit(String gremlin, int limit) {
        for (Pattern pattern : limitPatterns.values()) {
            Matcher matcher = pattern.matcher(gremlin);
            if (matcher.find()) {
                return gremlin + ".limit(" + limit + ")";
            }
        }
        return gremlin;
    }

    public static Map<String, Pattern> compileToPattern(Set<String> texts) {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        for (String text : texts) {
            String regex = StringUtils.replaceEach(text, COMPILE_SEARCH_LIST,
                                                   COMPILE_TARGET_LIST);
            String finalRegex;
            // Assume that (STR), (NUM) and () not exist at the same time
            if (text.contains("(STR)")) {
                // single quote
                finalRegex = restrictAsEnd(regex.replaceAll("STR",
                                                            "'[\\\\s\\\\S]+'"));
                patterns.put(finalRegex, Pattern.compile(finalRegex));
                // double quotes
                finalRegex = restrictAsEnd(regex.replaceAll("STR",
                                                            "\"[\\\\s\\\\S]+\""));
                patterns.put(finalRegex, Pattern.compile(finalRegex));
            } else if (text.contains("(NUM)")) {
                finalRegex = restrictAsEnd(regex.replaceAll("NUM", "[\\\\d]+"));
                patterns.put(finalRegex, Pattern.compile(finalRegex));
            } else if (text.contains("()")) {
                finalRegex = restrictAsEnd(regex);
                patterns.put(finalRegex, Pattern.compile(finalRegex));
            }
        }
        return patterns;
    }

    public static String restrictAsEnd(String regex) {
        return "(" + regex + ")$";
    }
}
