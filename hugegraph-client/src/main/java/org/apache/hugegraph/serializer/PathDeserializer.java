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

package org.apache.hugegraph.serializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.hugegraph.exception.InvalidResponseException;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.JsonUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class PathDeserializer extends JsonDeserializer<Path> {

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
            Object labels = JsonUtil.convertValue(labelsNode, Object.class);
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
                object = JsonUtil.convertValue(objectNode, Object.class);
            }
            path.objects(object);
        }

        // Parse node 'crosspoint'
        JsonNode crosspointNode = node.get("crosspoint");
        if (crosspointNode != null) {
            Object object = JsonUtil.convertValue(crosspointNode, Object.class);
            path.crosspoint(object);
        }
        return path;
    }

    private Object parseTypedNode(JsonNode objectNode, JsonNode typeNode) {
        String type = typeNode.asText();
        if ("vertex".equals(type)) {
            return JsonUtil.convertValue(objectNode, Vertex.class);
        } else if ("edge".equals(type)) {
            return JsonUtil.convertValue(objectNode, Edge.class);
        } else {
            throw InvalidResponseException.expectField("vertex/edge", type);
        }
    }
}
