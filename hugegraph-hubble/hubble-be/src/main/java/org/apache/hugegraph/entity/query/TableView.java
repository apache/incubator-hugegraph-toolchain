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

package org.apache.hugegraph.entity.query;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableView {

    public static final TableView EMPTY = new TableView();

    public static final List<String> GENERAL_HEADER = ImmutableList.of(
            "result"
    );

    public static final List<String> VERTEX_HEADER = ImmutableList.of(
            "id", "label", "properties"
    );

    public static final List<String> EDGE_HEADER = ImmutableList.of(
            "id", "label", "source", "target", "properties"
    );

    public static final List<String> PATH_HEADER = ImmutableList.of(
            "path"
    );

    @JsonProperty("header")
    private List<String> header;
    @JsonProperty("rows")
    private List<Object> rows;
}
