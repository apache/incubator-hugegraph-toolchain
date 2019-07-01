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

import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.util.HugeClientWrapper;
import com.baidu.hugegraph.structure.SchemaElement;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public final class SchemaCache {

    private final HugeClient client;
    private final Table<HugeType, String, SchemaElement> schemas;

    public SchemaCache(LoadContext context) {
        this.client = HugeClientWrapper.get(context.options());
        this.schemas = HashBasedTable.create();
    }

    public PropertyKey getPropertyKey(String name) {
        SchemaElement schema = this.schemas.get(HugeType.PROPERTY_KEY, name);
        if (schema == null) {
            schema = this.client.schema().getPropertyKey(name);
            this.schemas.put(HugeType.PROPERTY_KEY, name, schema);
        }
        return (PropertyKey) schema;
    }

    public VertexLabel getVertexLabel(String name) {
        SchemaElement schema = this.schemas.get(HugeType.VERTEX_LABEL, name);
        if (schema == null) {
            schema = this.client.schema().getVertexLabel(name);
            this.schemas.put(HugeType.VERTEX_LABEL, name, schema);
        }
        return (VertexLabel) schema;
    }

    public EdgeLabel getEdgeLabel(String name) {
        SchemaElement schema = this.schemas.get(HugeType.EDGE_LABEL, name);
        if (schema == null) {
            schema = this.client.schema().getEdgeLabel(name);
            this.schemas.put(HugeType.EDGE_LABEL, name, schema);
        }
        return (EdgeLabel) schema;
    }
}
