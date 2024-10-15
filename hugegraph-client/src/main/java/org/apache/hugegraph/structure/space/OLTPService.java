/*
 * Copyright 2017 HugeGraph Authors
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

package org.apache.hugegraph.structure.space;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OLTPService {
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("deployment_type")
    private DepleymentType depleymentType;

    @JsonProperty("type")
    private String type = "OLTP";

    @JsonProperty("count")
    private int count = 1; // 最大可运行节点
    @JsonProperty("running")
    private int running;

    @JsonProperty("cpu_limit")
    private int cpuLimit = 1;
    @JsonProperty("memory_limit")
    private int memoryLimit = 4; // GB
    @JsonProperty("storage_limit")
    private int storageLimit = 100;

    @JsonProperty("route_type")
    private String routeType = null;
    @JsonProperty("port")
    private int port = 0;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("update_time")
    private String updateTime;

    @JsonProperty("urls")
    private List<String> urls = new ArrayList<>();

    @JsonProperty("configs")
    private Map<String, Object> configs = new HashMap();

    @JsonProperty
    private ServiceStatus status = ServiceStatus.UNKNOWN;

    public enum DepleymentType {
        K8S,
        MANUAL;
    }

    public enum ServiceStatus {
        UNKNOWN,  // 未知
        STARTING, // 启动中
        RUNNING,  // 运行中
        STOPPED   // 停止
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DepleymentType getDepleymentType() {
        return depleymentType;
    }

    public void setDepleymentType(
            DepleymentType depleymentType) {
        this.depleymentType = depleymentType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getRunning() {
        return running;
    }

    public void setRunning(int running) {
        this.running = running;
    }

    public int getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(int cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public int getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public int getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(int storageLimit) {
        this.storageLimit = storageLimit;
    }

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public Map<String, Object> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, Object> configs) {
        this.configs = configs;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(
            ServiceStatus status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public boolean checkIsK8s() {
        return DepleymentType.K8S.equals(this.depleymentType);
    }

    @JsonIgnoreProperties(value={"configs", "create_time", "update_time",
            "running", "status"}, ignoreUnknown = true)
    public static class OLTPServiceReq extends OLTPService {
        public static OLTPServiceReq fromBase(OLTPService service) {
            OLTPServiceReq req = new OLTPServiceReq();

            req.setName(service.name);
            req.setDescription(service.description);
            req.setDepleymentType(service.depleymentType);
            req.setType(service.type);
            req.setCount(service.count);
            req.setCpuLimit(service.cpuLimit);
            req.setMemoryLimit(service.memoryLimit);
            req.setStorageLimit(service.storageLimit);
            req.setRouteType(service.routeType);
            req.setPort(service.port);
            req.setUrls(service.urls);

            return req;
        }
    }
}
