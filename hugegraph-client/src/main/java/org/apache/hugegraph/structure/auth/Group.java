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

import java.util.Date;

import org.apache.hugegraph.structure.constant.HugeType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Group extends AuthElement {

    @JsonProperty("group_name")
    protected String name;
    @JsonProperty("group_nickname")
    protected String nickname;
    @JsonProperty("group_description")
    protected String description;

    @JsonProperty("group_create")
    @JsonFormat(pattern = DATE_FORMAT, timezone = "GMT+8")
    protected Date create;
    @JsonProperty("group_update")
    @JsonFormat(pattern = DATE_FORMAT, timezone = "GMT+8")
    protected Date update;
    @JsonProperty("group_creator")
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

    public GroupReq switchReq() {
        return new GroupReq(this);
    }

    @JsonIgnoreProperties({"graphspace"})
    public static class GroupReq extends Group {

        public GroupReq(Group group) {
            this.id = group.id();
            this.name = group.name();
            this.nickname = group.nickname;
            this.description = group.description();
            this.update = group.updateTime();
            this.create = group.createTime();
            this.creator = group.creator();
        }
    }
}
