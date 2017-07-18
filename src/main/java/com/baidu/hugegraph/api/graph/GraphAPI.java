package com.baidu.hugegraph.api.graph;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;

/**
 * Created by liningrui on 2017/5/23.
 */
public abstract class GraphAPI extends API {

    private static final String PATH = "graphs/%s/graph/%s";

    protected static final String BATCH_ENCODING = "gzip";

    private String batchPath;

    public GraphAPI(RestClient client, String graph) {
        super(client);
        this.path(String.format(PATH, graph, type()));
        this.batchPath = String.format("%s/%s", this.path, "batch");
    }

    public abstract String type();

    public String batchPath() {
        return this.batchPath;
    }
}
