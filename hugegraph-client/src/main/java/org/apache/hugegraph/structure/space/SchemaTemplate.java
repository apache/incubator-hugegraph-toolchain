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

package org.apache.hugegraph.structure.space;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaTemplate {

    @JsonProperty("name")
    private String name;
    @JsonProperty("schema")
    private String schema;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("update_time")
    private String updateTime;

    @JsonProperty("creator")
    private String creator;

    public SchemaTemplate() {
    }

    public SchemaTemplate(String name, String schema) {
        this.name = name;
        this.schema = schema;
    }

    public static SchemaTemplate fromMap(Map<String, String> map) {
        return new SchemaTemplate(map.get("name"), map.get("schema"));
    }

    public void name(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public String schema() {
        return this.schema;
    }

    public void schema(String schema) {
        this.schema = schema;
    }

    public String createTime() {
        return createTime;
    }

    public void createTime(String createTime) {
        this.createTime = createTime;
    }

    public String updateTime() {
        return updateTime;
    }

    public void updateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String creator() {
        return creator;
    }

    public void creator(String creator) {
        this.creator = creator;
    }

    public Map<String, String> asMap() {
        return ImmutableMap.of("name", this.name, "schema", this.schema);
    }

    @JsonIgnoreProperties({"create_time", "update_time", "creator"})
    public static class SchemaTemplateReq extends SchemaTemplate {

        public static SchemaTemplateReq fromBase(SchemaTemplate schemaTemplate) {
            SchemaTemplateReq req = new SchemaTemplateReq();
            req.name(schemaTemplate.name);
            req.schema(schemaTemplate.schema);

            return req;
        }
    }
}
