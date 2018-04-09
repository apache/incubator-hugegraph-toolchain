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

package com.baidu.hugegraph.driver;

import java.util.List;

import com.baidu.hugegraph.api.schema.EdgeLabelAPI;
import com.baidu.hugegraph.api.schema.IndexLabelAPI;
import com.baidu.hugegraph.api.schema.PropertyKeyAPI;
import com.baidu.hugegraph.api.schema.VertexLabelAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.structure.schema.BuilderProxy;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public class SchemaManager {

    private PropertyKeyAPI propertyKeyAPI;
    private VertexLabelAPI vertexLabelAPI;
    private EdgeLabelAPI edgeLabelAPI;
    private IndexLabelAPI indexLabelAPI;

    public SchemaManager(RestClient client, String graph) {
        this.propertyKeyAPI = new PropertyKeyAPI(client, graph);
        this.vertexLabelAPI = new VertexLabelAPI(client, graph);
        this.edgeLabelAPI = new EdgeLabelAPI(client, graph);
        this.indexLabelAPI = new IndexLabelAPI(client, graph);
    }

    public PropertyKey.Builder propertyKey(String name) {
        PropertyKey.Builder builder = new PropertyKey.BuilderImpl(name, this);
        BuilderProxy<PropertyKey.Builder> proxy = new BuilderProxy<>(builder);
        return proxy.proxy();

    }

    public VertexLabel.Builder vertexLabel(String name) {
        VertexLabel.Builder builder = new VertexLabel.BuilderImpl(name, this);
        BuilderProxy<VertexLabel.Builder> proxy = new BuilderProxy<>(builder);
        return proxy.proxy();
    }

    public EdgeLabel.Builder edgeLabel(String name) {
        EdgeLabel.Builder builder = new EdgeLabel.BuilderImpl(name, this);
        BuilderProxy<EdgeLabel.Builder> proxy = new BuilderProxy<>(builder);
        return proxy.proxy();
    }

    public IndexLabel.Builder indexLabel(String name) {
        IndexLabel.Builder builder = new IndexLabel.BuilderImpl(name, this);
        BuilderProxy<IndexLabel.Builder> proxy = new BuilderProxy<>(builder);
        return proxy.proxy();
    }

    public PropertyKey addPropertyKey(PropertyKey propertyKey) {
        return this.propertyKeyAPI.create(propertyKey);
    }

    public PropertyKey appendPropertyKey(PropertyKey propertyKey) {
        return this.propertyKeyAPI.append(propertyKey);
    }

    public PropertyKey eliminatePropertyKey(PropertyKey propertyKey) {
        return this.propertyKeyAPI.eliminate(propertyKey);
    }

    public void removePropertyKey(String name) {
        this.propertyKeyAPI.delete(name);
    }

    public PropertyKey getPropertyKey(String name) {
        return this.propertyKeyAPI.get(name);
    }

    public List<PropertyKey> getPropertyKeys() {
        return this.propertyKeyAPI.list();
    }

    public VertexLabel addVertexLabel(VertexLabel vertexLabel) {
        return this.vertexLabelAPI.create(vertexLabel);
    }

    public VertexLabel appendVertexLabel(VertexLabel vertexLabel) {
        return this.vertexLabelAPI.append(vertexLabel);
    }

    public VertexLabel eliminateVertexLabel(VertexLabel vertexLabel) {
        return this.vertexLabelAPI.eliminate(vertexLabel);
    }

    public void removeVertexLabel(String name) {
        this.vertexLabelAPI.delete(name);
    }

    public VertexLabel getVertexLabel(String name) {
        return this.vertexLabelAPI.get(name);
    }

    public List<VertexLabel> getVertexLabels() {
        return this.vertexLabelAPI.list();
    }

    public EdgeLabel addEdgeLabel(EdgeLabel edgeLabel) {
        return this.edgeLabelAPI.create(edgeLabel);
    }

    public EdgeLabel appendEdgeLabel(EdgeLabel edgeLabel) {
        return this.edgeLabelAPI.append(edgeLabel);
    }

    public EdgeLabel eliminateEdgeLabel(EdgeLabel edgeLabel) {
        return this.edgeLabelAPI.eliminate(edgeLabel);
    }

    public void removeEdgeLabel(String name) {
        this.edgeLabelAPI.delete(name);
    }

    public EdgeLabel getEdgeLabel(String name) {
        return this.edgeLabelAPI.get(name);
    }

    public List<EdgeLabel> getEdgeLabels() {
        return this.edgeLabelAPI.list();
    }

    public IndexLabel addIndexLabel(IndexLabel indexLabel) {
        return this.indexLabelAPI.create(indexLabel);
    }

    public void removeIndexLabel(String name) {
        this.indexLabelAPI.delete(name);
    }

    public IndexLabel getIndexLabel(String name) {
        return this.indexLabelAPI.get(name);
    }

    public List<IndexLabel> getIndexLabels() {
        return this.indexLabelAPI.list();
    }
}
