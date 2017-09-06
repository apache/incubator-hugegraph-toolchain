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
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;


public class SchemaManager {

    private PropertyKeyAPI propertyKeyApi;
    private VertexLabelAPI vertexLabelApi;
    private EdgeLabelAPI edgeLabelAPI;
    private IndexLabelAPI indexLabelAPI;

    public SchemaManager(RestClient client, String graph) {
        this.propertyKeyApi = new PropertyKeyAPI(client, graph);
        this.vertexLabelApi = new VertexLabelAPI(client, graph);
        this.edgeLabelAPI = new EdgeLabelAPI(client, graph);
        this.indexLabelAPI = new IndexLabelAPI(client, graph);
    }

    public PropertyKey.Builder propertyKey(String name) {
        return new PropertyKey.Builder(name, this);
    }

    public VertexLabel.Builder vertexLabel(String name) {
        return new VertexLabel.Builder(name, this);
    }

    public EdgeLabel.Builder edgeLabel(String name) {
        return new EdgeLabel.Builder(name, this);
    }

    public IndexLabel.Builder indexLabel(String name) {
        return new IndexLabel.Builder(name, this);
    }

    public void addPropertyKey(PropertyKey propertyKey) {
        this.propertyKeyApi.create(propertyKey);
    }

    public void removePropertyKey(String name) {
        this.propertyKeyApi.delete(name);
    }

    public PropertyKey getPropertyKey(String name) {
        return this.propertyKeyApi.get(name);
    }

    public List<PropertyKey> getPropertyKeys() {
        return this.propertyKeyApi.list();
    }

    public void addVertexLabel(VertexLabel vertexLabel) {
        this.vertexLabelApi.create(vertexLabel);
    }

    public void appendVertexLabel(VertexLabel vertexLabel) {
        this.vertexLabelApi.append(vertexLabel);
    }

    public void eliminateVertexLabel(VertexLabel vertexLabel) {
        this.vertexLabelApi.eliminate(vertexLabel);
    }

    public void removeVertexLabel(String name) {
        this.vertexLabelApi.delete(name);
    }

    public VertexLabel getVertexLabel(String name) {
        return this.vertexLabelApi.get(name);
    }

    public List<VertexLabel> getVertexLabels() {
        return this.vertexLabelApi.list();
    }

    public void addEdgeLabel(EdgeLabel edgeLabel) {
        this.edgeLabelAPI.create(edgeLabel);
    }

    public void appendEdgeLabel(EdgeLabel edgeLabel) {
        this.edgeLabelAPI.append(edgeLabel);
    }

    public void eliminateEdgeLabel(EdgeLabel edgeLabel) {
        this.edgeLabelAPI.eliminate(edgeLabel);
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

    public void addIndexLabel(IndexLabel indexLabel) {
        this.indexLabelAPI.create(indexLabel);
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
