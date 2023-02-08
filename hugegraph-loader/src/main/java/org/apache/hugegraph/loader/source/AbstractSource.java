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

package org.apache.hugegraph.loader.source;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.source.file.ListFormat;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractSource implements InputSource, Serializable {

    @JsonProperty("header")
    private String[] header;
    @JsonProperty("charset")
    private String charset;
    @JsonProperty("list_format")
    private ListFormat listFormat;

    public AbstractSource() {
        this.header = null;
        this.charset = Constants.CHARSET.name();
        this.listFormat = null;
    }

    @Override
    public void check() throws IllegalArgumentException {
        if (this.header != null) {
            E.checkArgument(this.header.length > 0,
                            "The header can't be empty if " +
                            "it has been customized");
            E.checkArgument(CollectionUtil.allUnique(Arrays.asList(this.header)),
                            "The header can't contain duplicate columns, " +
                            "but got %s", Arrays.toString(this.header));
        }
    }

    @Override
    public String[] header() {
        return this.header;
    }

    public void header(String[] header) {
        this.header = header;
    }

    public void header(List<String> header) {
        this.header = header.toArray(new String[]{});
    }

    @Override
    public String charset() {
        return this.charset;
    }

    public void charset(Charset charset) {
        this.charset = charset.name();
    }

    public void charset(String charset) {
        this.charset = charset;
    }

    public ListFormat listFormat() {
        return this.listFormat;
    }

    public void listFormat(ListFormat listFormat) {
        this.listFormat = listFormat;
    }
}
