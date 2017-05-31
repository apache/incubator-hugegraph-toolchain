package com.baidu.hugegraph.driver;

import java.util.Map;

import com.baidu.hugegraph.api.graphs.GraphsAPI;
import com.baidu.hugegraph.client.RestClient;

/**
 * Created by liningrui on 2017/5/31.
 */
public class GraphsManager {

    private GraphsAPI graphsApi;

    public GraphsManager(String url) {
        RestClient client = new RestClient(url);
        this.graphsApi = new GraphsAPI(client);
    }

    public Map<String, String> getGraph(String graph) {
        return this.graphsApi.get(graph);
    }
}
