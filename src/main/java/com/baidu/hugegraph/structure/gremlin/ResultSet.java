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

package com.baidu.hugegraph.structure.gremlin;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.rest.SerializeException;
import com.baidu.hugegraph.serializer.PathDeserializer;
import com.baidu.hugegraph.serializer.VertexDeserializer;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.util.E;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ResultSet {

    private static ObjectMapper mapper = new ObjectMapper();

    @JsonProperty
    private List<Object> data;
    @JsonProperty
    private Map<String, ?> meta;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Vertex.class, new VertexDeserializer());
        module.addDeserializer(Path.class, new PathDeserializer());
        mapper.registerModule(module);
    }

    public List<Object> data() {
        return this.data;
    }

    public int size() {
        return this.data.size();
    }

    public Result get(int index) {
        if (index >= this.data.size()) {
            return null;
        }

        Object object = this.data().get(index);
        if (object == null) {
            return null;
        }

        Class<?> clazz = this.parseResultClass(object);
        if (clazz.equals(object.getClass())) {
            return new Result(object);
        }

        try {
            String rawValue = mapper.writeValueAsString(object);
            return new Result(mapper.readValue(rawValue, clazz));
        } catch (Exception e) {
            throw new SerializeException(
                      "Failed to deserialize: %s", e, object);
        }
    }

    /**
     * TODO: Still need to constantly add and optimize
     */
    private Class<?> parseResultClass(Object object) {
        if (object.getClass().equals(LinkedHashMap.class)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            String type = (String) map.get("type");
            if (type != null) {
                if (type.equals("vertex")) {
                    return Vertex.class;
                } else if (type.equals("edge")) {
                    return Edge.class;
                }
            } else {
                if (map.get("labels") != null) {
                    return Path.class;
                }
            }
        }

        return object.getClass();
    }

    public Iterator<Result> iterator() {
        E.checkState(this.data != null, "Invalid response from server");

        return new Iterator<Result>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return this.index < ResultSet.this.data.size();
            }

            @Override
            public Result next() {
                return get(this.index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
