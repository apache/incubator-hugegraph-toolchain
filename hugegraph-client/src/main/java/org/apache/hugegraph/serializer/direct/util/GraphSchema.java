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

package org.apache.hugegraph.serializer.direct.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;

/**
 * We could get all graph schema from server and cache/update it in client(subset of SchemaManager)
 */
public class GraphSchema {

    private final HugeClient client;
    private final Map<String, PropertyKey> propertyKeys;
    private final Map<String, VertexLabel> vertexLabels;
    private final Map<String, EdgeLabel> edgeLabels;

    public GraphSchema(HugeClient client) {
        this.client = client;
        this.propertyKeys = new HashMap<>();
        this.vertexLabels = new HashMap<>();
        this.edgeLabels = new HashMap<>();
        // init all schema
        this.updateAll();
    }

    public void updateAll() {
        this.propertyKeys.clear();
        this.vertexLabels.clear();
        this.edgeLabels.clear();

        client.schema().getPropertyKeys().forEach(pk -> this.propertyKeys.put(pk.name(), pk));
        client.schema().getVertexLabels().forEach(vl -> this.vertexLabels.put(vl.name(), vl));
        client.schema().getEdgeLabels().forEach(el -> this.edgeLabels.put(el.name(), el));
    }

    public PropertyKey getPropertyKey(String name) {
        PropertyKey propertyKey = this.propertyKeys.get(name);
        if (propertyKey == null) {
            try {
                propertyKey = this.client.schema().getPropertyKey(name);
            } catch (ServerException e) {
                throw new HugeException("The property key '%s' doesn't exist", name);
            }
        }
        return propertyKey;
    }

    public VertexLabel getVertexLabel(String name) {
        VertexLabel vertexLabel = this.vertexLabels.get(name);
        if (vertexLabel == null) {
            try {
                vertexLabel = this.client.schema().getVertexLabel(name);
            } catch (ServerException e) {
                throw new HugeException("The vertex label '%s' doesn't exist", name);
            }
        }
        return vertexLabel;
    }

    public EdgeLabel getEdgeLabel(String name) {
        EdgeLabel edgeLabel = this.edgeLabels.get(name);
        if (edgeLabel == null) {
            try {
                edgeLabel = this.client.schema().getEdgeLabel(name);
            } catch (ServerException e) {
                throw new HugeException("The edge label '%s' doesn't exist", name);
            }
        }
        return edgeLabel;
    }

    public EdgeLabel getEdgeLabel(int id) {
        for (EdgeLabel label : edgeLabels.values()) {
            if (label.id() == id) {
                return label;
            }
        }

        throw new HugeException("The edge label id '%s' doesn't exist", id);
    }
}
