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

package org.apache.hugegraph.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Copied from HugeGraph(<a href="https://github.com/hugegraph/hugegraph">...</a>)
 */
public final class IdUtil {

    public static String escape(char splitor, char escape, String... values) {
        int length = values.length + 4;
        for (String value : values) {
            length += value.length();
        }
        StringBuilder escaped = new StringBuilder(length);
        // Do escape for every item in values
        for (String value : values) {
            if (escaped.length() > 0) {
                escaped.append(splitor);
            }

            if (value.indexOf(splitor) == -1) {
                escaped.append(value);
                continue;
            }

            // Do escape for current item
            for (int i = 0, n = value.length(); i < n; i++) {
                char ch = value.charAt(i);
                if (ch == splitor) {
                    escaped.append(escape);
                }
                escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    public static String[] unescape(String id, String splitor, String escape) {
        /*
         * Note that the `splitor`/`escape` maybe special characters in regular
         * expressions, but this is a frequently called method, for faster
         * execution, we forbid the use of special characters as delimiter
         * or escape sign.
         * The `limit` param -1 in split method can ensure empty string be
         * split to a part.
         */
        String[] parts = id.split("(?<!" + escape + ")" + splitor, -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = StringUtils.replace(parts[i], escape + splitor,
                                           splitor);
        }
        return parts;
    }
}
