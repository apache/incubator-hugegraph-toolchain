package com.baidu.hugegraph.api.schema;

import java.util.List;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.EdgeLabel;

/**
 * Created by liningrui on 2017/5/23.
 */
public class EdgeLabelAPI extends SchemaAPI {

    public EdgeLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.EDGE_LABEL.string();
    }

    public void create(EdgeLabel edgeLabel) {
        this.client.post(path(), edgeLabel);
    }

    public void append(EdgeLabel edgeLabel) {
        this.client.put(path(), edgeLabel);
    }

    public EdgeLabel get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(EdgeLabel.class);
    }

    public List<EdgeLabel> list() {
        RestResult result = this.client.get(path());
        return result.readList(type(), EdgeLabel.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
