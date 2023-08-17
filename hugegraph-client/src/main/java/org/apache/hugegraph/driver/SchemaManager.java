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

package org.apache.hugegraph.driver;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.task.TaskAPI;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.api.schema.EdgeLabelAPI;
import org.apache.hugegraph.api.schema.IndexLabelAPI;
import org.apache.hugegraph.api.schema.PropertyKeyAPI;
import org.apache.hugegraph.api.schema.SchemaAPI;
import org.apache.hugegraph.api.schema.VertexLabelAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.schema.BuilderProxy;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;

public class SchemaManager {

    private PropertyKeyAPI propertyKeyAPI;
    private VertexLabelAPI vertexLabelAPI;
    private EdgeLabelAPI edgeLabelAPI;
    private IndexLabelAPI indexLabelAPI;
    private SchemaAPI schemaAPI;
    private TaskAPI taskAPI;

    public SchemaManager(RestClient client, String graph) {
        this.propertyKeyAPI = new PropertyKeyAPI(client, graph);
        this.vertexLabelAPI = new VertexLabelAPI(client, graph);
        this.edgeLabelAPI = new EdgeLabelAPI(client, graph);
        this.indexLabelAPI = new IndexLabelAPI(client, graph);
        this.schemaAPI = new SchemaAPI(client, graph);
        this.taskAPI = new TaskAPI(client, graph);
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
        return this.addPropertyKey(propertyKey, TaskAPI.TASK_TIMEOUT);
    }

    public PropertyKey addPropertyKey(PropertyKey propertyKey, long seconds) {
        PropertyKey.PropertyKeyWithTask task = this.propertyKeyAPI
                                                   .create(propertyKey);
        if (task.taskId() != 0L) {
            this.taskAPI.waitUntilTaskSuccess(task.taskId(), seconds);
        }
        return task.propertyKey();
    }

    public long addPropertyKeyAsync(PropertyKey propertyKey) {
        PropertyKey.PropertyKeyWithTask task = this.propertyKeyAPI
                                                   .create(propertyKey);
        return task.taskId();
    }

    public PropertyKey appendPropertyKey(PropertyKey propertyKey) {
        return this.propertyKeyAPI.append(propertyKey).propertyKey();
    }

    public PropertyKey eliminatePropertyKey(PropertyKey propertyKey) {
        return this.propertyKeyAPI.eliminate(propertyKey).propertyKey();
    }

    public PropertyKey clearPropertyKey(PropertyKey propertyKey) {
        return this.clearPropertyKey(propertyKey, TaskAPI.TASK_TIMEOUT);
    }

    public PropertyKey clearPropertyKey(PropertyKey propertyKey, long seconds) {
        PropertyKey.PropertyKeyWithTask task = this.propertyKeyAPI
                                                   .clear(propertyKey);
        if (task.taskId() != 0L) {
            this.taskAPI.waitUntilTaskSuccess(task.taskId(), seconds);
        }
        return task.propertyKey();
    }

    public long clearPropertyKeyAsync(PropertyKey propertyKey) {
        PropertyKey.PropertyKeyWithTask task = this.propertyKeyAPI
                                                   .clear(propertyKey);
        return task.taskId();
    }

    public void removePropertyKey(String name) {
        this.removePropertyKey(name, TaskAPI.TASK_TIMEOUT);
    }

    public void removePropertyKey(String name, long seconds) {
        long task = this.propertyKeyAPI.delete(name);
        this.taskAPI.waitUntilTaskSuccess(task, seconds);
    }

    public long removePropertyKeyAsync(String name) {
        return this.propertyKeyAPI.delete(name);
    }

    public PropertyKey getPropertyKey(String name) {
        return this.propertyKeyAPI.get(name);
    }

    public List<PropertyKey> getPropertyKeys() {
        return this.propertyKeyAPI.list();
    }

    public List<PropertyKey> getPropertyKeys(List<String> names) {
        return this.propertyKeyAPI.list(names);
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
        long task = this.vertexLabelAPI.delete(name);
        this.taskAPI.waitUntilTaskSuccess(task, TaskAPI.TASK_TIMEOUT);
    }

    public void removeVertexLabel(String name, long seconds) {
        long task = this.vertexLabelAPI.delete(name);
        this.taskAPI.waitUntilTaskSuccess(task, seconds);
    }

    public long removeVertexLabelAsync(String name) {
        return this.vertexLabelAPI.delete(name);
    }

    public VertexLabel getVertexLabel(String name) {
        return this.vertexLabelAPI.get(name);
    }

    public List<VertexLabel> getVertexLabels() {
        return this.vertexLabelAPI.list();
    }

    public List<VertexLabel> getVertexLabels(List<String> names) {
        return this.vertexLabelAPI.list(names);
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
        this.removeEdgeLabel(name, TaskAPI.TASK_TIMEOUT);
    }

    public void removeEdgeLabel(String name, long seconds) {
        long task = this.edgeLabelAPI.delete(name);
        this.taskAPI.waitUntilTaskSuccess(task, seconds);
    }

    public long removeEdgeLabelAsync(String name) {
        return this.edgeLabelAPI.delete(name);
    }

    public EdgeLabel getEdgeLabel(String name) {
        return this.edgeLabelAPI.get(name);
    }

    public List<EdgeLabel> getEdgeLabels() {
        return this.edgeLabelAPI.list();
    }

    public List<EdgeLabel> getEdgeLabels(List<String> names) {
        return this.edgeLabelAPI.list(names);
    }

    public IndexLabel addIndexLabel(IndexLabel indexLabel) {
        return this.addIndexLabel(indexLabel, TaskAPI.TASK_TIMEOUT);
    }

    public IndexLabel addIndexLabel(IndexLabel indexLabel, long seconds) {
        IndexLabel.IndexLabelWithTask cil = this.indexLabelAPI
                                                .create(indexLabel);
        if (cil.taskId() != 0L) {
            this.taskAPI.waitUntilTaskSuccess(cil.taskId(), seconds);
        }
        return cil.indexLabel();
    }

    public long addIndexLabelAsync(IndexLabel indexLabel) {
        IndexLabel.IndexLabelWithTask cil = this.indexLabelAPI
                                                .create(indexLabel);
        return cil.taskId();
    }

    public IndexLabel appendIndexLabel(IndexLabel indexLabel) {
        return this.indexLabelAPI.append(indexLabel);
    }

    public IndexLabel eliminateIndexLabel(IndexLabel indexLabel) {
        return this.indexLabelAPI.eliminate(indexLabel);
    }

    public void removeIndexLabel(String name) {
        this.removeIndexLabel(name, TaskAPI.TASK_TIMEOUT);
    }

    public void removeIndexLabel(String name, long secondss) {
        long task = this.indexLabelAPI.delete(name);
        this.taskAPI.waitUntilTaskSuccess(task, secondss);
    }

    public long removeIndexLabelAsync(String name) {
        return this.indexLabelAPI.delete(name);
    }

    public IndexLabel getIndexLabel(String name) {
        return this.indexLabelAPI.get(name);
    }

    public List<IndexLabel> getIndexLabels() {
        return this.indexLabelAPI.list();
    }

    public List<IndexLabel> getIndexLabels(List<String> names) {
        return this.indexLabelAPI.list(names);
    }

    public Map<String, List<SchemaElement>> getSchema() {
        return this.schemaAPI.list();
    }
}
