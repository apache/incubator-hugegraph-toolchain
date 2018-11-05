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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baidu.hugegraph.loader.source.EdgeSource;
import com.baidu.hugegraph.loader.source.InputSource;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class EdgeSourceDeserializer
       extends ElementSourceDeserializer<EdgeSource> {

    @Override
    @SuppressWarnings("unchecked")
    public EdgeSource deserialize(JsonParser parser,
                                  DeserializationContext ctxt)
                                  throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        JsonNode labelNode = getNode(node, "label", JsonNodeType.STRING);
        String label = labelNode.asText();

        JsonNode sourceNode = getNode(node, "source", JsonNodeType.ARRAY);
        List<String> source = this.read(sourceNode, List.class);

        JsonNode targetNode = getNode(node, "target", JsonNodeType.ARRAY);
        List<String> target = this.read(targetNode, List.class);

        JsonNode inputNode = getNode(node, "input", JsonNodeType.OBJECT);
        InputSource input = this.readInputSource(inputNode);

        JsonNode mappingNode = node.get("mapping");
        Map<String, String> mapping = null;
        if (mappingNode != null) {
            mapping = this.read(mappingNode, Map.class);
        } else {
            mapping = new HashMap<>();
        }

        JsonNode ignoredNode = node.get("ignored");
        Set<String> ignored = null;
        if (ignoredNode != null) {
            ignored = this.read(ignoredNode, Set.class);
        } else {
            ignored = new HashSet<>();
        }

        return new EdgeSource(label, input, source, target, mapping, ignored);
    }
}
