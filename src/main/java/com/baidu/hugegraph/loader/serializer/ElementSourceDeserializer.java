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

import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.loader.source.InputSource;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class ElementSourceDeserializer<E>
       extends InputSourceDeserializer<E> {

    private static final String FIELD_TYPE = "type";

    protected InputSource readInputSource(JsonNode node) {
        JsonNode typeNode = getNode(node, FIELD_TYPE, JsonNodeType.STRING);
        String type = typeNode.asText().toUpperCase();
        assert node instanceof ObjectNode;
        ObjectNode objectNode = (ObjectNode) node;
        // The node 'type' doesn't participate in deserialization
        objectNode.remove(FIELD_TYPE);
        switch (type) {
            case "FILE":
                return JsonUtil.convert(node, FileSource.class);
            default:
                // TODO: Expand more input sources
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'", type));
        }
    }

}
