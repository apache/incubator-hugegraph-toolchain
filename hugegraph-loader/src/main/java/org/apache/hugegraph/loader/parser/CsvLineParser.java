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

import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.ReadException;
import org.simpleflatmapper.csv.CsvParser;

public class CsvLineParser extends TextLineParser {

    private final CsvParser.DSL dsl;

    public CsvLineParser() {
        super(Constants.COMMA_STR);
        char separator = this.delimiter().charAt(0);
        this.dsl = CsvParser.separator(separator);
    }

    @Override
    public String[] split(String line) {
        try {
            return this.dsl.reader(line).iterator().next();
        } catch (IOException | NoSuchElementException e) {
            throw new ReadException(line, "Parse line '%s' error", e, line);
        }
    }
}
