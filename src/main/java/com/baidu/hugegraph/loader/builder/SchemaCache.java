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
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.util.HugeClientHolder;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public final class SchemaCache {

    private final HugeClient client;
    private final Map<String, PropertyKey> propertyKeys;
    private final Map<String, VertexLabel> vertexLabels;
    private final Map<String, EdgeLabel> edgeLabels;

    public SchemaCache() {
        LoadOptions options = LoadContext.get().options();
        this.client = HugeClientHolder.get(options);
        this.propertyKeys = new HashMap<>();
        this.vertexLabels = new HashMap<>();
        this.edgeLabels = new HashMap<>();
    }

    public PropertyKey getPropertyKey(String name) {
        PropertyKey schema = this.propertyKeys.get(name);
        if (schema == null) {
            schema = this.client.schema().getPropertyKey(name);
            this.propertyKeys.put(name, schema);
        }
        return schema;
    }

    public VertexLabel getVertexLabel(String name) {
        VertexLabel schema = this.vertexLabels.get(name);
        if (schema == null) {
            schema = this.client.schema().getVertexLabel(name);
            this.vertexLabels.put(name, schema);
        }
        return schema;
    }

    public EdgeLabel getEdgeLabel(String name) {
        EdgeLabel schema = this.edgeLabels.get(name);
        if (schema == null) {
            schema = this.client.schema().getEdgeLabel(name);
            this.edgeLabels.put(name, schema);
        }
        return schema;
    }
}
