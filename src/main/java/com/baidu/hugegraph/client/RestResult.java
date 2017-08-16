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
package com.baidu.hugegraph.client;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.baidu.hugegraph.exception.SerializeException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class RestResult {

    private int status;
    private MultivaluedMap<String, Object> headers;
    private String content;

    public RestResult(Response response) {
        this.status = response.getStatus();
        this.headers = response.getHeaders();
        this.content = response.readEntity(String.class);
    }

    public int status() {
        return status;
    }

    public MultivaluedMap<String, Object> headers() {
        return headers;
    }

    public String content() {
        return content;
    }

    public <T> T readObject(Class<T> clazz) {
        T obj;
        ObjectMapper mapper = new ObjectMapper();
        try {
            obj = mapper.readValue(this.content, clazz);
        } catch (Exception e) {
            throw new SerializeException(String.format(
                    "Failed to deserialize %s", this.content), e);
        }
        return obj;
    }

    public <T> List<T> readList(String key, Class<T> clazz) {
        List<T> objList;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(this.content);
            JsonNode element = root.get(key);
            if (element == null) {
                throw new SerializeException(String.format(
                        "Can not find value of the key: %s in json.", key));
            }
            JavaType type = mapper.getTypeFactory()
                    .constructParametricType(List.class, clazz);
            objList = mapper.readValue(element.toString(), type);
        } catch (IOException e) {
            throw new SerializeException(String.format(
                    "Failed to deserialize %s", this.content), e);
        }
        return objList;
    }

    public <T> List<T> readList(Class<T> clazz) {
        List<T> objList;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JavaType type = mapper.getTypeFactory()
                    .constructParametricType(List.class, clazz);
            objList = mapper.readValue(this.content, type);
        } catch (IOException e) {
            throw new SerializeException(String.format(
                    "Failed to deserialize %s", this.content), e);
        }
        return objList;
    }

    @Override
    public String toString() {
        return String.format("{status=%s, headers=%s, content=%s",
                this.status,
                this.headers,
                this.content);
    }
}
