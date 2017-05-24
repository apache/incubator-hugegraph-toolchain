package com.baidu.hugegraph.api.schema;

import java.util.List;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.VertexLabel;

/**
 * Created by liningrui on 2017/5/23.
 */
public class VertexLabelAPI extends SchemaAPI {

    public VertexLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.VERTEX_LABEL.string();
    }

    public void create(VertexLabel vertexLabel) {
        this.client.post(path(), vertexLabel);
    }

    public VertexLabel get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(VertexLabel.class);
    }

    public List<VertexLabel> list() {
        RestResult result = this.client.get(path());
        return result.readList(type(), VertexLabel.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
