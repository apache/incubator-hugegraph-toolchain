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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.structure.constant.HugeType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Target extends AuthElement {

    @JsonProperty("target_name")
    protected String name;
    @JsonProperty("graphspace")
    protected String graphSpace;
    @JsonProperty("target_graph")
    protected String graph;
    @JsonProperty("target_url")
    protected String url;
    @JsonProperty("target_description")
    protected String description;
    @JsonProperty("target_resources")
    protected Map<String, List<HugeResource>> resources;

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

    public String graphSpace() {
        return this.graphSpace;
    }

    public void graphSpace(String graphSpace) {
        this.graphSpace = graphSpace;
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

    public String description() {
        return this.description;
    }

    public void description(String description) {
        this.description = description;
    }

    public HugeResource resource() {
        if (this.resources == null || this.resources.isEmpty()) {
            return null;
        }
        if (this.resources.size() != 1) {
            return null;
        }
        Map.Entry<String, List<HugeResource>> entry = this.resources.entrySet().iterator().next();
        List<HugeResource> list = entry.getValue();
        if (list == null || list.size() != 1) {
            return null;
        }
        return list.get(0);
    }

    public Map<String, List<HugeResource>> resources() {
        if (this.resources == null) {
            return null;
        }
        return Collections.unmodifiableMap(this.resources);
    }

    public void resources(Map<String, List<HugeResource>> resources) {
        this.resources = resources;
    }

    public void resources(List<HugeResource> resources) {
        if (resources == null) {
            this.resources = null;
            return;
        }
        Map<String, List<HugeResource>> map = new HashMap<>();
        map.put("DEFAULT", new ArrayList<>(resources));
        this.resources = map;
    }

    public void resources(HugeResource... resources) {
        if (resources == null) {
            this.resources = null;
            return;
        }
        List<HugeResource> list = Arrays.asList(resources);
        resources(list);
    }
}
