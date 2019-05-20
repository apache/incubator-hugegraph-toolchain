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
import java.util.Set;

import com.baidu.hugegraph.loader.progress.InputItem;
import com.baidu.hugegraph.loader.progress.InputProgress;
import com.baidu.hugegraph.loader.reader.file.FileItem;
import com.baidu.hugegraph.loader.reader.hdfs.PathItem;
import com.baidu.hugegraph.loader.source.SourceType;
import com.baidu.hugegraph.loader.util.JsonUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.ImmutableSet;

public class InputProgressDeser extends JsonDeserializer<InputProgress> {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_LOADED_ITEMS = "loaded_items";
    private static final String FIELD_LOADING_ITEM = "loading_item";

    @Override
    public InputProgress deserialize(JsonParser parser,
                                     DeserializationContext context)
                                     throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return readInputProgress(node);
    }

    @SuppressWarnings("unchecked")
    private static InputProgress readInputProgress(JsonNode node) {
        JsonNode typeNode = getNode(node, FIELD_TYPE, JsonNodeType.STRING);
        String type = typeNode.asText().toUpperCase();
        SourceType sourceType = SourceType.valueOf(type);
        JsonNode loadedItemsNode = getNode(node, FIELD_LOADED_ITEMS,
                                           JsonNodeType.ARRAY);
        JsonNode loadingItemNode = getNode(node, FIELD_LOADING_ITEM,
                                           JsonNodeType.OBJECT,
                                           JsonNodeType.NULL);
        Set<InputItem> loadedItems;
        InputItem loadingItem;
        switch (sourceType) {
            case FILE:
                loadedItems = (Set<InputItem>) (Object) JsonUtil.convertSet(
                              loadedItemsNode, FileItem.class);
                loadingItem = JsonUtil.convert(loadingItemNode, FileItem.class);
                break;
            case HDFS:
                loadedItems = (Set<InputItem>) (Object) JsonUtil.convertSet(
                              loadedItemsNode, PathItem.class);
                loadingItem = JsonUtil.convert(loadingItemNode, PathItem.class);
                break;
            case JDBC:
            default:
                throw new AssertionError(String.format(
                          "Unsupported input source '%s'", type));
        }
        return new InputProgress(sourceType, loadedItems, loadingItem);
    }

    private static JsonNode getNode(JsonNode node, String name,
                                    JsonNodeType... nodeTypes) {
        JsonNode subNode = node.get(name);
        if (subNode == null) {
            throw DeserializeException.expectField(name, node);
        }
        if (!ImmutableSet.copyOf(nodeTypes).contains(subNode.getNodeType())) {
            throw DeserializeException.expectTypes(name, nodeTypes);
        }
        return subNode;
    }
}
