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

package org.apache.hugegraph.loader.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SchemaCache {

    private final HugeClient client;
    private final Map<String, PropertyKey> propertyKeys;
    private final Map<String, VertexLabel> vertexLabels;
    private final Map<String, EdgeLabel> edgeLabels;

    public SchemaCache(HugeClient client) {
        this.client = client;
        this.propertyKeys = new HashMap<>();
        this.vertexLabels = new HashMap<>();
        this.edgeLabels = new HashMap<>();
    }

    @JsonCreator
    public SchemaCache(@JsonProperty(value = "propertykeys")
                       List<PropertyKey> propertyKeyList,
                       @JsonProperty("vertexlabels")
                       List<VertexLabel> vertexLabelList,
                       @JsonProperty("edgelabels")
                       List<EdgeLabel> edgeLabelList) {
        this.client = null;
        this.propertyKeys = new HashMap<>();
        this.vertexLabels = new HashMap<>();
        this.edgeLabels = new HashMap<>();
        propertyKeyList.forEach(pk -> {
            this.propertyKeys.put(pk.name(), pk);
        });
        vertexLabelList.forEach(vl -> {
            this.vertexLabels.put(vl.name(), vl);
        });
        edgeLabelList.forEach(el -> {
            this.edgeLabels.put(el.name(), el);
        });
    }

    public void updateAll() {
        this.propertyKeys.clear();
        client.schema().getPropertyKeys().forEach(pk -> {
            this.propertyKeys.put(pk.name(), pk);
        });
        this.vertexLabels.clear();
        client.schema().getVertexLabels().forEach(vl -> {
            this.vertexLabels.put(vl.name(), vl);
        });
        this.edgeLabels.clear();
        client.schema().getEdgeLabels().forEach(el -> {
            this.edgeLabels.put(el.name(), el);
        });
    }

    public PropertyKey getPropertyKey(String name) {
        PropertyKey propertyKey = this.propertyKeys.get(name);
        if (propertyKey == null) {
            try {
                propertyKey = this.client.schema().getPropertyKey(name);
            } catch (ServerException e) {
                throw new LoadException("The property key '%s' doesn't exist",
                                        name);
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
                throw new LoadException("The vertex label '%s' doesn't exist",
                                        name);
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
                throw new LoadException("The edge label '%s' doesn't exist",
                                        name);
            }
        }
        return edgeLabel;
    }

    public boolean isEmpty() {
        return this.propertyKeys.isEmpty() &&
               this.vertexLabels.isEmpty() &&
               this.edgeLabels.isEmpty();
    }
}
