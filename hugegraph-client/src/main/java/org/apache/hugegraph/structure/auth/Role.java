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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hugegraph.structure.constant.HugeType;

import java.util.Date;

public class Role extends AuthElement {
    @JsonProperty("graphspace")
    protected String graphSpace;
    @JsonProperty("role_name")
    protected String name;
    @JsonProperty("role_nickname")
    protected String nickname;
    @JsonProperty("role_description")
    protected String description;

    @JsonProperty("role_create")
    @JsonFormat(pattern = DATE_FORMAT, timezone = "GMT+8")
    protected Date create;
    @JsonProperty("role_update")
    @JsonFormat(pattern = DATE_FORMAT, timezone = "GMT+8")
    protected Date update;
    @JsonProperty("role_creator")
    protected String creator;

    @Override
    public String type() {
        return HugeType.GROUP.string();
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

    public void graphSpace(String graphSpace) {
        this.graphSpace = graphSpace;
    }

    public String graphSpace() {
        return this.graphSpace;
    }

    public String nickname() {
        return this.nickname;
    }

    public void nickname(String nickname) {
        this.nickname = nickname;
    }

    public String description() {
        return this.description;
    }

    public void description(String description) {
        this.description = description;
    }

    public RoleReq switchReq() {
        return new RoleReq(this);
    }

    @JsonIgnoreProperties({"graphspace"})
    public static class RoleReq extends Role {

        public RoleReq(Role role) {
            this.id = role.id();
            this.name = role.name();
            this.nickname = role.nickname;
            this.description = role.description();
            this.update = role.updateTime();
            this.create = role.createTime();
            this.creator = role.creator();
        }
    }
}
