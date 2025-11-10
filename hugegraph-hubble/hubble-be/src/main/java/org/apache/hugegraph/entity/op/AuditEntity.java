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

import org.apache.hugegraph.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditEntity {
    @JsonProperty("audit_datetime")
    private String datetime;

    @JsonProperty("audit_operation")
    private String operation;

    @JsonProperty("audit_action")
    private String action;

    @JsonProperty("audit_service")
    private String service;

    @JsonProperty("audit_graphspace")
    private String graphSpace;

    @JsonProperty("audit_graph")
    private String graph;

    @JsonProperty("audit_level")
    private String level;

    @JsonProperty("audit_user")
    private String user;

    @JsonProperty("audit_ip")
    private String ip;

    @JsonProperty("audit_result")
    private String result = "Success";

    public static AuditEntity fromMap(Map<String, Object> source) {
        Map<String, String> jsonData = (Map<String, String>) source.get("json");
        return JsonUtil.fromJson(JsonUtil.toJson(jsonData), AuditEntity.class);
    }
}
