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
import java.util.NoSuchElementException;

import com.baidu.hugegraph.exception.SerializeException;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ResultSet {

    @JsonProperty
    private List<Object> data;
    @JsonProperty
    private Map<String, ?> meta;

    private ObjectMapper mapper;
    private int index;

    public ResultSet() {
        this.mapper = new ObjectMapper();
        this.index = 0;
    }

    public List<Object> data() {
        return data;
    }

    public Result one() {
        if (index >= this.data.size()) {
            return null;
        }

        Object object = this.data().get(this.index++);
        Class clazz = parseResultClass(object);

        if (clazz.equals(object.getClass())) {
            return new Result(object);
        }

        try {
            return new Result(this.mapper.readValue(
                    this.mapper.writeValueAsString(object), clazz));
        } catch (Exception e) {
            throw new SerializeException(String.format(
                    "Failed to deserialize: %s", object), e);
        }
    }

    /**
     * TODO: Still need to constantly add and optimize
     * @param object
     * @return
     */
    private Class parseResultClass(Object object) {
        if (object.getClass().equals(LinkedHashMap.class)) {
            LinkedHashMap map = (LinkedHashMap) object;
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
        return new Iterator<Result>() {

            @Override
            public boolean hasNext() {
                return index < data.size();
            }

            @Override
            public Result next() {
                Result result = one();
                if (result == null) {
                    throw new NoSuchElementException();
                }
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
