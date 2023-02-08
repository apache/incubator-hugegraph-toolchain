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

package org.apache.hugegraph.loader.reader.jdbc;

import java.sql.SQLException;

import org.apache.hugegraph.loader.exception.LoadException;
import org.postgresql.core.Utils;

public final class JDBCUtil {

    public static String escapeMysql(String value) {
        int length = value.length();
        if (!isEscapeNeededForString(value, length)) {
            return '\'' + value + '\'';
        }

        StringBuilder buf = new StringBuilder((int) (length * 1.1D));
        buf.append('\'');

        for (int i = 0; i < length; ++i) {
            char c = value.charAt(i);
            switch (c) {
                case '\u0000':
                    buf.append('\\');
                    buf.append('0');
                    break;
                case '\n':
                    buf.append('\\');
                    buf.append('n');
                    break;
                case '\r':
                    buf.append('\\');
                    buf.append('r');
                    break;
                case '\u001a':
                    buf.append('\\');
                    buf.append('Z');
                    break;
                case '"':
                    /*
                     * Doesn't need to add '\', because we wrap string with "'"
                     * Assume that we don't use Ansi Mode
                     */
                    buf.append('"');
                    break;
                case '\'':
                    buf.append('\\');
                    buf.append('\'');
                    break;
                case '\\':
                    buf.append('\\');
                    buf.append('\\');
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }

        buf.append('\'');
        return buf.toString();
    }

    public static String escapePostgresql(String value) {
        StringBuilder builder = new StringBuilder(8 + value.length());
        builder.append('\'');
        try {
            Utils.escapeLiteral(builder, value, false);
        } catch (SQLException e) {
            throw new LoadException("Failed to escape '%s'", e, value);
        }
        builder.append('\'');
        return builder.toString();
    }

    public static String escapeOracle(String value) {
        // TODO: check it
        return escapeMysql(value);
    }

    public static String escapeSqlserver(String value) {
        // TODO: check it
        return escapeMysql(value);
    }

    private static boolean isEscapeNeededForString(String sql, int length) {
        boolean needsEscape = false;
        for (int i = 0; i < length; ++i) {
            char c = sql.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\'':
                case '\\':
                case '\u001a':
                case '\u0000':
                    needsEscape = true;
                    break;
                default:
                    break;
            }

            if (needsEscape) {
                break;
            }
        }

        return needsEscape;
    }
}
