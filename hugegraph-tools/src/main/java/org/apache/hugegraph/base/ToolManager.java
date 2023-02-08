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

package org.apache.hugegraph.base;

import java.io.IOException;
import java.util.List;

import org.apache.hugegraph.rest.SerializeException;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ToolManager {

    protected final ToolClient client;
    private final String type;

    public ToolManager(ToolClient.ConnectionInfo info, String type) {
        this.client = new ToolClient(info);
        this.type = type;
    }

    protected String type() {
        return this.type;
    }

    protected String graph() {
        return this.client.graph().graph();
    }

    @SuppressWarnings("unchecked")
    protected  <T> List<T> readList(String key, Class<T> clazz,
                                    String content) {
        ObjectMapper mapper = this.client.mapper();
        try {
            JsonNode root = mapper.readTree(content);
            JsonNode element = root.get(key);
            if (element == null) {
                throw new SerializeException(
                          "Can't find value of the key: %s in json.", key);
            } else {
                JavaType t = mapper.getTypeFactory()
                                   .constructParametricType(List.class, clazz);
                return (List<T>) mapper.readValue(element.toString(), t);
            }
        } catch (IOException e) {
            throw new SerializeException(
                      "Failed to deserialize %s", e, content);
        }
    }

    public void close () {
        this.client.close();
    }
}
