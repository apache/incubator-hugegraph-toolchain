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
import java.util.List;
import java.util.Set;

import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.serializer.InputProgressDeser;
import com.baidu.hugegraph.loader.serializer.InputSourceDeser;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.rest.SerializeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(InputSource.class, new InputSourceDeser());
        module.addDeserializer(InputProgress.class, new InputProgressDeser());
        registerModule(module);
    }

    public static void registerModule(Module module) {
        MAPPER.registerModule(module);
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SerializeException("Failed to serialize objects", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new SerializeException("Failed to deserialize json", e);
        }
    }

    public static <T> T convert(JsonNode node, Class<T> clazz) {
        return MAPPER.convertValue(node, clazz);
    }

    public static <T> Set<T> convertSet(JsonNode node, Class<T> clazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructCollectionType(Set.class, clazz);
        return MAPPER.convertValue(node, type);
    }

    public static <T> List<T> convertList(JsonNode node, Class<T> clazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructCollectionType(List.class, clazz);
        return MAPPER.convertValue(node, type);
    }
}
