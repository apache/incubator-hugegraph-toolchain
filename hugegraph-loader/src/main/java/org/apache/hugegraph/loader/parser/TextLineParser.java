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

import java.util.Arrays;

import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.ReadException;
import org.apache.hugegraph.loader.reader.line.Line;
import org.apache.hugegraph.util.StringUtil;

public class TextLineParser implements LineParser {

    // Default is "\t"
    private final String delimiter;

    public TextLineParser(String delimiter) {
        this.delimiter = delimiter != null ? delimiter : Constants.TAB_STR;
    }

    public String delimiter() {
        return this.delimiter;
    }

    @Override
    public Line parse(String[] header, String rawLine) throws ReadException {
        String[] columns = this.split(rawLine);
        if (columns.length > header.length) {
            // Ignore extra empty string at the tail of line
            int extra = columns.length - header.length;
            if (!this.tailColumnEmpty(columns, extra)) {
                throw new ReadException(rawLine,
                                        "The column length '%s' doesn't match with " +
                                        "header length '%s' on: %s",
                                        columns.length, header.length, rawLine);
            }
            String[] subColumns = new String[header.length];
            System.arraycopy(columns, 0, subColumns, 0, header.length);
            return new Line(rawLine, header, subColumns);
        } else if (columns.length < header.length) {
            // Fill with an empty string
            String[] supColumns = new String[header.length];
            System.arraycopy(columns, 0, supColumns, 0, columns.length);
            Arrays.fill(supColumns, columns.length, supColumns.length,
                        Constants.EMPTY_STR);
            return new Line(rawLine, header, supColumns);
        }
        return new Line(rawLine, header, columns);
    }

    @Override
    public String[] split(String line) {
        return StringUtil.split(line, this.delimiter);
    }

    private boolean tailColumnEmpty(String[] columns, int count) {
        for (int i = 0; i < count; i++) {
            int tailIdx = columns.length - 1 - i;
            if (!columns[tailIdx].equals(Constants.EMPTY_STR)) {
                return false;
            }
        }
        return true;
    }
}
