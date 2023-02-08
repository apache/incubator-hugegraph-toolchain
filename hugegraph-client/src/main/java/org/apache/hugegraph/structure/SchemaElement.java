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

package org.apache.hugegraph.structure;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class SchemaElement extends Element {

    @JsonProperty("id")
    protected long id;
    @JsonProperty("name")
    protected String name;
    @JsonProperty("properties")
    protected Set<String> properties;
    @JsonProperty("check_exist")
    protected boolean checkExist;
    @JsonProperty("user_data")
    protected Map<String, Object> userdata;
    @JsonProperty("status")
    protected String status;

    public SchemaElement(String name) {
        this.name = name;
        this.properties = new ConcurrentSkipListSet<>();
        this.userdata = new ConcurrentHashMap<>();
        this.checkExist = true;
        this.status = null;
    }

    @Override
    public Long id() {
        return this.id;
    }

    public void resetId() {
        this.id = 0L;
    }

    public String name() {
        return this.name;
    }

    public Set<String> properties() {
        return this.properties;
    }

    public Map<String, Object> userdata() {
        return this.userdata;
    }

    public String status() {
        return this.status;
    }

    public boolean checkExist() {
        return this.checkExist;
    }

    public void checkExist(boolean checkExist) {
        this.checkExist = checkExist;
    }
}
