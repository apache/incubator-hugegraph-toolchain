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

package com.baidu.hugegraph.loader.builder;

import java.util.HashMap;
import java.util.Map;

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public final class SchemaCache {

    private final Map<String, PropertyKey> propertyKeys;
    private final Map<String, VertexLabel> vertexLabels;
    private final Map<String, EdgeLabel> edgeLabels;

    public SchemaCache(HugeClient client) {
        this.propertyKeys = new HashMap<>();
        client.schema().getPropertyKeys().forEach(pk -> {
            propertyKeys.put(pk.name(), pk);
        });

        this.vertexLabels = new HashMap<>();
        client.schema().getVertexLabels().forEach(vl -> {
            vertexLabels.put(vl.name(), vl);
        });

        this.edgeLabels = new HashMap<>();
        client.schema().getEdgeLabels().forEach(el -> {
            edgeLabels.put(el.name(), el);
        });
    }

    public PropertyKey getPropertyKey(String name) {
        PropertyKey propertyKey = this.propertyKeys.get(name);
        if (propertyKey == null) {
            throw new LoadException("The property key '%s' doesn't exist",
                                    name);
        }
        return propertyKey;
    }

    public VertexLabel getVertexLabel(String name) {
        VertexLabel vertexLabel = this.vertexLabels.get(name);
        if (vertexLabel == null) {
            throw new LoadException("The vertex label '%s' doesn't exist",
                                    name);
        }
        return vertexLabel;
    }

    public EdgeLabel getEdgeLabel(String name) {
        EdgeLabel edgeLabel = this.edgeLabels.get(name);
        if (edgeLabel == null) {
            throw new LoadException("The edge label '%s' doesn't exist", name);
        }
        return edgeLabel;
    }

    public boolean isEmpty() {
        return this.propertyKeys.isEmpty() &&
               this.vertexLabels.isEmpty() &&
               this.edgeLabels.isEmpty();
    }
}
