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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.hugegraph.structure.constant.HugeType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Access extends AuthElement {

    @JsonProperty("graphspace")
    protected String graphSpace;
    @JsonProperty("role")
    protected Object role;
    @JsonProperty("target")
    protected Object target;
    @JsonProperty("access_permission")
    protected HugePermission permission;
    @JsonProperty("access_description")
    protected String description;

    @JsonProperty("access_create")
    @JsonFormat(pattern = DATE_FORMAT, timezone = "GMT+8")
    protected Date create;
    @JsonProperty("access_update")
    @JsonFormat(pattern = DATE_FORMAT, timezone = "GMT+8")
    protected Date update;
    @JsonProperty("access_creator")
    protected String creator;

    @Override
    public String type() {
        return HugeType.ACCESS.string();
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

    public String graphSpace() {
        return this.graphSpace;
    }

    public void graphSpace(String graphSpace) {
        this.graphSpace = graphSpace;
    }

    public Object role() {
        return this.role;
    }

    public void role(Object role) {
        if (role instanceof Role) {
            role = ((Role) role).id();
        }
        this.role = role;
    }

    public Object target() {
        return this.target;
    }

    public void target(Object target) {
        if (target instanceof Target) {
            target = ((Target) target).id();
        }
        this.target = target;
    }

    public HugePermission permission() {
        return this.permission;
    }

    public void permission(HugePermission permission) {
        this.permission = permission;
    }

    public String description() {
        return this.description;
    }

    public void description(String description) {
        this.description = description;
    }

    public AccessReq switchReq() {
        return new AccessReq(this);
    }

    @JsonIgnoreProperties({"graphspace"})
    public static class AccessReq extends Access {

        public AccessReq(Access access) {
            this.id = access.id();
            this.role = access.role();
            this.target = access.target();
            this.permission = access.permission();
            this.description = access.description();
            this.create = access.createTime();
            this.update = access.updateTime();
            this.creator = access.creator();
        }
    }
}
