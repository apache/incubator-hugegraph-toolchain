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

import com.baidu.hugegraph.loader.source.file.FileSource;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class FileSourceDeserializer extends
        InputSourceDeserializer<FileSource> {

    private static final String FIELD_FILE_PATH = "path";
    private static final String FIELD_FILE_FORMAT = "format";

    @Override
    public FileSource deserialize(JsonParser parser,
                                  DeserializationContext ctxt)
                                  throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        // Make sure the node 'path' and 'format' exist
        getNode(node, FIELD_FILE_PATH, JsonNodeType.STRING);
        getNode(node, FIELD_FILE_FORMAT, JsonNodeType.STRING);
        return this.read(node, FileSource.class);
    }
}
