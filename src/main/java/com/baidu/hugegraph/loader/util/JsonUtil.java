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

package com.baidu.hugegraph.loader.util;

import java.io.IOException;

import com.baidu.hugegraph.loader.serializer.EdgeSourceDeserializer;
import com.baidu.hugegraph.loader.serializer.FileSourceDeserializer;
import com.baidu.hugegraph.loader.serializer.VertexSourceDeserializer;
import com.baidu.hugegraph.loader.source.EdgeSource;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.VertexSource;
import com.baidu.hugegraph.rest.SerializeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(VertexSource.class, new VertexSourceDeserializer());
        module.addDeserializer(EdgeSource.class, new EdgeSourceDeserializer());
        module.addDeserializer(FileSource.class, new FileSourceDeserializer());
        registerModule(module);
    }

    public static void registerModule(Module module) {
        mapper.registerModule(module);
    }

    public static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SerializeException("Failed to serialize objects", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new SerializeException("Failed to deserialize json", e);
        }
    }

    public static <T> T convert(JsonNode node, Class<T> clazz) {
        return mapper.convertValue(node, clazz);
    }
}
