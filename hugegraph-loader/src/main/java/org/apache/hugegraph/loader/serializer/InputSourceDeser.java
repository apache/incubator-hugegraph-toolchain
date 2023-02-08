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

package org.apache.hugegraph.loader.serializer;

import java.io.IOException;

import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.loader.source.InputSource;
import org.apache.hugegraph.loader.source.SourceType;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.hdfs.HDFSSource;
import org.apache.hugegraph.loader.source.jdbc.JDBCSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class InputSourceDeser extends JsonDeserializer<InputSource> {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_VENDOR = "vendor";

    @Override
    public InputSource deserialize(JsonParser parser,
                                   DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return readInputSource(node);
    }

    private static InputSource readInputSource(JsonNode node) {
        JsonNode typeNode = getNode(node, FIELD_TYPE, JsonNodeType.STRING);
        String type = typeNode.asText().toUpperCase();
        SourceType sourceType = SourceType.valueOf(type);
        assert node instanceof ObjectNode;
        ObjectNode objectNode = (ObjectNode) node;
        // The node 'type' doesn't participate in deserialization
        objectNode.remove(FIELD_TYPE);
        switch (sourceType) {
            case FILE:
                return JsonUtil.convert(node, FileSource.class);
            case HDFS:
                return JsonUtil.convert(node, HDFSSource.class);
            case JDBC:
                JsonNode vendorNode = getNode(node, FIELD_VENDOR,
                                              JsonNodeType.STRING);
                vendorNode = TextNode.valueOf(vendorNode.asText()
                                                        .toUpperCase());
                objectNode.replace(FIELD_VENDOR, vendorNode);
                return JsonUtil.convert(node, JDBCSource.class);
            default:
                throw new AssertionError(String.format("Unsupported input source '%s'", type));
        }
    }

    private static JsonNode getNode(JsonNode node, String name,
                                    JsonNodeType nodeType) {
        JsonNode subNode = node.get(name);
        if (subNode == null || subNode.getNodeType() != nodeType) {
            throw DeserializeException.expectField(name, node);
        }
        return subNode;
    }
}
