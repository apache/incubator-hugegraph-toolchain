package com.baidu.hugegraph.api.graph;

import java.util.List;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.graph.Edge;

/**
 * Created by liningrui on 2017/5/23.
 */
public class EdgeAPI extends GraphAPI {

    public EdgeAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.EDGE.string();
    }

    public Edge create(Edge edge) {
        RestResult result = this.client.post(path(), edge);
        return result.readObject(Edge.class);
    }

    public Edge get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(Edge.class);
    }

    public List<Edge> list() {
        RestResult result = this.client.get(path());
        return result.readList(type(), Edge.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
