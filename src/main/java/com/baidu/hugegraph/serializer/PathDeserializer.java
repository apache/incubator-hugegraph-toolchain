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

package com.baidu.hugegraph.serializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.baidu.hugegraph.exception.InvalidResponseException;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Path;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class PathDeserializer extends JsonDeserializer<Path> {

    private final ObjectMapper mapper;

    public PathDeserializer() {
        this.mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Vertex.class, new VertexDeserializer());
        this.mapper.registerModule(module);
    }

    @Override
    public Path deserialize(JsonParser parser, DeserializationContext ctxt)
                            throws IOException {

        JsonNode node = parser.getCodec().readTree(parser);
        Path path = new Path();

        // Parse node 'labels'
        JsonNode labelsNode = node.get("labels");
        if (labelsNode != null) {
            if (labelsNode.getNodeType() != JsonNodeType.ARRAY) {
                throw InvalidResponseException.expectField("labels", node);
            }
            Object labels = this.mapper.convertValue(labelsNode, Object.class);
            ((List<?>) labels).forEach(path::labels);
        }

        // Parse node 'objects'
        JsonNode objectsNode = node.get("objects");
        if (objectsNode == null ||
            objectsNode.getNodeType() != JsonNodeType.ARRAY) {
            throw InvalidResponseException.expectField("objects", node);
        }

        Iterator<JsonNode> objects = objectsNode.elements();
        while (objects.hasNext()) {
            JsonNode objectNode = objects.next();
            JsonNode typeNode = objectNode.get("type");
            Object object;
            if (typeNode != null) {
                object = parseTypedNode(objectNode, typeNode);
            } else {
                object = this.mapper.convertValue(objectNode, Object.class);
            }
            path.objects(object);
        }

        // Parse node 'crosspoint'
        JsonNode crosspointNode = node.get("crosspoint");
        if (crosspointNode != null) {
            Object object = this.mapper.convertValue(crosspointNode, Object.class);
            path.crosspoint(object);
        }
        return path;
    }

    private Object parseTypedNode(JsonNode objectNode, JsonNode typeNode) {
        String type = typeNode.asText();
        if (type.equals("vertex")) {
            return this.mapper.convertValue(objectNode, Vertex.class);
        } else if (type.equals("edge")) {
            return this.mapper.convertValue(objectNode, Edge.class);
        } else {
            throw InvalidResponseException.expectField("vertex/edge", type);
        }
    }
}
