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

package org.apache.hugegraph.structure.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.hugegraph.structure.constant.HugeType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Target extends AuthElement {

    @JsonProperty("target_name")
    private String name;
    @JsonProperty("target_graph")
    private String graph;
    @JsonProperty("target_url")
    private String url;
    @JsonProperty("target_resources")
    private List<HugeResource> resources;

    @JsonProperty("target_create")
    @JsonFormat(pattern = DATE_FORMAT)
    protected Date create;
    @JsonProperty("target_update")
    @JsonFormat(pattern = DATE_FORMAT)
    protected Date update;
    @JsonProperty("target_creator")
    protected String creator;

    @Override
    public String type() {
        return HugeType.TARGET.string();
    }

    @Override
    public Date createTime() {
        return this.create;
    }

    @Override
    public Date updateTime() {
        return this.update;
    }

    @Override
    public String creator() {
        return this.creator;
    }

    public String name() {
        return this.name;
    }

    public void name(String name) {
        this.name = name;
    }

    public String graph() {
        return this.graph;
    }

    public void graph(String graph) {
        this.graph = graph;
    }

    public String url() {
        return this.url;
    }

    public void url(String url) {
        this.url = url;
    }

    public HugeResource resource() {
        if (this.resources == null || this.resources.size() != 1) {
            return null;
        }
        return this.resources.get(0);
    }

    public List<HugeResource> resources() {
        if (this.resources == null) {
            return null;
        }
        return Collections.unmodifiableList(this.resources);
    }

    public void resources(List<HugeResource> resources) {
        this.resources = resources;
    }

    public void resources(HugeResource... resources) {
        this.resources = Arrays.asList(resources);
    }
}
