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

package com.baidu.hugegraph.entity.load;

import java.util.List;

import com.baidu.hugegraph.annotation.MergeProperty;
import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.common.Mergeable;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSetting implements Mergeable {

    @MergeProperty
    @JsonProperty("has_header")
    private boolean hasHeader;

    @JsonProperty("column_names")
    private List<String> columnNames;

    @JsonProperty("column_values")
    private List<String> columnValues;

    @MergeProperty
    @JsonProperty("format")
    private String format = "CSV";

    @MergeProperty
    @JsonProperty("delimiter")
    private String delimiter = ",";

    @MergeProperty
    @JsonProperty("charset")
    private String charset = Constant.CHARSET.name();

    @MergeProperty
    @JsonProperty("date_format")
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    @MergeProperty
    @JsonProperty("time_zone")
    private String timeZone = "GMT+8";

    @MergeProperty
    @JsonProperty("skipped_line")
    private String skippedLine = "(^#|^//).*|";

    @MergeProperty
    @JsonProperty("list_format")
    private ListFormat listFormat = new ListFormat();
}
