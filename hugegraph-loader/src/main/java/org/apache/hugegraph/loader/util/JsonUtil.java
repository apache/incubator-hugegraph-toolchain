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

package org.apache.hugegraph.loader.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import org.apache.hugegraph.loader.progress.InputProgress;
import org.apache.hugegraph.loader.serializer.DeserializeException;
import org.apache.hugegraph.loader.serializer.InputProgressDeser;
import org.apache.hugegraph.loader.serializer.InputSourceDeser;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.rest.SerializeException;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class JsonUtil {

    private static final Logger LOG = Log.logger(JsonUtil.class);
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
            LOG.error("Failed to serialize objects", e);
            throw new SerializeException("Failed to serialize objects", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            LOG.error("Failed to deserialize json", e);
            throw new SerializeException("Failed to deserialize json", e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        E.checkState(json != null, "Json value can't be null for '%s'",
                     typeRef.getType());
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (IOException e) {
            LOG.error("Failed to deserialize json", e);
            throw new DeserializeException("Failed to deserialize json", e);
        }
    }

    public static <T> T convert(JsonNode node, Class<T> clazz) {
        return MAPPER.convertValue(node, clazz);
    }

    public static <T> Set<T> convertSet(String json, Class<T> clazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructCollectionType(LinkedHashSet.class, clazz);
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to deserialize json", e);
            throw new DeserializeException("Failed to deserialize json", e);
        }
    }

    public static <T> Set<T> convertSet(JsonNode node, Class<T> clazz) {
        JavaType type = MAPPER.getTypeFactory().
                              constructCollectionType(LinkedHashSet.class, clazz);
        return MAPPER.convertValue(node, type);
    }

    public static <T> List<T> convertList(String json, Class<T> clazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructCollectionType(ArrayList.class, clazz);
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            LOG.error("Failed to deserialize json", e);
            throw new DeserializeException("Failed to deserialize json", e);
        }
    }

    public static <T> List<T> convertList(JsonNode node, Class<T> clazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructCollectionType(List.class, clazz);
        return MAPPER.convertValue(node, type);
    }

    public static <K, V> Map<K, V> convertMap(String json, Class<K> kClazz,
                                              Class<V> vClazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructMapType(Map.class, kClazz, vClazz);
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            LOG.error("Failed to deserialize json", e);
            throw new DeserializeException("Failed to deserialize json", e);
        }
    }

    public static <K, V> Map<K, V> convertMap(JsonNode node, Class<K> kClazz,
                                              Class<V> vClazz) {
        JavaType type = MAPPER.getTypeFactory()
                              .constructMapType(Map.class, kClazz, vClazz);
        return MAPPER.convertValue(node, type);
    }
}
