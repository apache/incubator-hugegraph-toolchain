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

package org.apache.hugegraph.entity.op;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hugegraph.util.ESUtil;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntity {
    @JsonProperty("log_datetime")
    private String datetime;

    @JsonProperty("log_service")
    private String service;

    @JsonProperty("log_host")
    private String host;

    @JsonProperty("log_level")
    private String level;

    @JsonProperty("log_message")
    private String message;

    public static LogEntity fromMap(Map<String, Object> map) {
        LogEntity logEntity = new LogEntity();
        logEntity.setDatetime(
                ESUtil.parseTimestamp(
                        (String) ESUtil.getValueByPath(map, "@timestamp".split("\\."))));
        logEntity.setHost(
                (String) ESUtil.getValueByPath(map, "host.name".split("\\.")));
        logEntity.setService(
                (String) ESUtil.getValueByPath(map, "fields.source"
                        .split("\\.")));
        logEntity.setLevel(
                (String) ESUtil.getValueByPath(map, "level".split("\\.")));
        logEntity.setMessage(
                (String) ESUtil.getValueByPath(map, "message".split("\\.")));
        return logEntity;
    }
}

