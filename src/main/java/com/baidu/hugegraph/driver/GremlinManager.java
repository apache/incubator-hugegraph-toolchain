package com.baidu.hugegraph.driver;

import com.baidu.hugegraph.api.gremlin.GremlinAPI;
import com.baidu.hugegraph.api.gremlin.GremlinRequest;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.structure.gremlin.Response;
import com.baidu.hugegraph.structure.gremlin.ResultSet;

/**
 * Created by liningrui on 2017/5/16.
 */
public class GremlinManager {

    private GremlinAPI gremlinApi;

    public GremlinManager(String url) {
        RestClient client = new RestClient(url);
        this.gremlinApi = new GremlinAPI(client);
    }

    public ResultSet execute(GremlinRequest request) {
        Response response = this.gremlinApi.post(request);
        // TODO: Can add some checks later
        return response.result();
    }

    public GremlinRequest.Builder gremlin(String gremlin) {
        return new GremlinRequest.Builder(gremlin, this);
    }
}
