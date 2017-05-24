package com.baidu.hugegraph.api.schema;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;

/**
 * Created by liningrui on 2017/5/23.
 */
public abstract class SchemaAPI extends API {

    private static final String PATH = "graphs/%s/schema/%s";

    public SchemaAPI(RestClient client, String graph) {
        super(client);
        this.path(String.format(PATH, graph, type()));
    }

    public abstract String type();
}
