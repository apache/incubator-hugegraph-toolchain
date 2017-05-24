package com.baidu.hugegraph.driver;

import com.baidu.hugegraph.api.gremlin.GremlinAPI;
import com.baidu.hugegraph.api.gremlin.GremlinRequest;
import com.baidu.hugegraph.client.RestClient;

/**
 * Created by liningrui on 2017/5/16.
 */
public class GremlinManager {

    private GremlinAPI gremlinApi;

    public GremlinManager(String url) {
        RestClient client = new RestClient(url);
        this.gremlinApi = new GremlinAPI(client);
    }

    public String execute(GremlinRequest request) {
        return this.gremlinApi.post(request);
    }

    public GremlinRequest.Builder gremlin(String gremlin) {
        return new GremlinRequest.Builder(gremlin, this);
    }
}
