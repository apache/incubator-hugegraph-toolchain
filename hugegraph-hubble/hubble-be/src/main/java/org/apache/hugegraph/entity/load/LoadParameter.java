/*
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

package org.apache.hugegraph.entity.load;

import org.apache.hugegraph.annotation.MergeProperty;
import org.apache.hugegraph.common.Mergeable;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadParameter implements Mergeable {

    @MergeProperty
    @JsonProperty("check_vertex")
    private boolean checkVertex = false;

    @MergeProperty
    @JsonProperty("insert_timeout")
    private int insertTimeout = 60;

    @MergeProperty
    @JsonProperty("max_parse_errors")
    private int maxParseErrors = 1;

    @MergeProperty
    @JsonProperty("max_insert_errors")
    private int maxInsertErrors = 500;

    @MergeProperty
    @JsonProperty("retry_times")
    private int retryTimes = 3;

    @MergeProperty
    @JsonProperty("retry_interval")
    private int retryInterval = 10;
}
