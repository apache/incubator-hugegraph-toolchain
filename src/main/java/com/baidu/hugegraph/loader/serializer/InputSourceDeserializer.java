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

package com.baidu.hugegraph.loader.serializer;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public abstract class InputSourceDeserializer<E> extends JsonDeserializer<E> {

    private static final ObjectMapper mapper = new ObjectMapper();

    protected <T> T read(JsonNode node, Class<T> clazz) {
        return this.mapper.convertValue(node, clazz);
    }

    protected static JsonNode getNode(JsonNode node, String name,
                                      JsonNodeType nodeType) {
        JsonNode subNode = node.get(name);
        if (subNode == null || subNode.getNodeType() != nodeType) {
            throw DeserializerException.expectField(name, node);
        }
        return subNode;
    }
}
