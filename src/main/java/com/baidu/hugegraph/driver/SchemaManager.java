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

/**
 * Created by liningrui on 2017/5/23.
 */
public class SchemaManager {

    private PropertyKeyAPI propertyKeyApi;
    private VertexLabelAPI vertexLabelApi;
    private EdgeLabelAPI edgeLabelAPI;
    private IndexLabelAPI indexLabelAPI;

    public SchemaManager(String url, String graph) {
        RestClient client = new RestClient(url);
        this.propertyKeyApi = new PropertyKeyAPI(client, graph);
        this.vertexLabelApi = new VertexLabelAPI(client, graph);
        this.edgeLabelAPI = new EdgeLabelAPI(client, graph);
        this.indexLabelAPI = new IndexLabelAPI(client, graph);
    }

    public PropertyKey.Builder makePropertyKey(String name) {
        return new PropertyKey.Builder(name, this);
    }

    public VertexLabel.Builder makeVertexLabel(String name) {
        return new VertexLabel.Builder(name, this);
    }

    public EdgeLabel.Builder makeEdgeLabel(String name) {
        return new EdgeLabel.Builder(name, this);
    }

    public IndexLabel.Builder makeIndexLabel(String name) {
        return new IndexLabel.Builder(name, this);
    }

    public void addPropertyKey(PropertyKey propertyKey) {
        this.propertyKeyApi.create(propertyKey);
    }

    public PropertyKey getPropertyKey(String name) {
        return this.propertyKeyApi.get(name);
    }

    public List<PropertyKey> getPropertyKeys() {
        return this.propertyKeyApi.list();
    }

    public void removePropertyKey(String name) {
        this.propertyKeyApi.delete(name);
    }

    public void addVertexLabel(VertexLabel vertexLabel) {
        this.vertexLabelApi.create(vertexLabel);
    }

    public VertexLabel getVertexLabel(String name) {
        return this.vertexLabelApi.get(name);
    }

    public List<VertexLabel> getVertexLabels() {
        return this.vertexLabelApi.list();
    }

    public void removeVertexLabel(String name) {
        this.vertexLabelApi.delete(name);
    }

    public void addEdgeLabel(EdgeLabel edgeLabel) {
        this.edgeLabelAPI.create(edgeLabel);
    }

    public EdgeLabel getEdgeLabel(String name) {
        return this.edgeLabelAPI.get(name);
    }

    public List<EdgeLabel> getEdgeLabels() {
        return this.edgeLabelAPI.list();
    }

    public void removeEdgeLabel(String name) {
        this.edgeLabelAPI.delete(name);
    }

    public void addIndexLabel(IndexLabel indexLabel) {
        this.indexLabelAPI.create(indexLabel);
    }

    public IndexLabel getIndexLabel(String name) {
        return this.indexLabelAPI.get(name);
    }

    public List<IndexLabel> getIndexLabels() {
        return this.indexLabelAPI.list();
    }

    public void removeIndexLabel(String name) {
        this.indexLabelAPI.delete(name);
    }
}
