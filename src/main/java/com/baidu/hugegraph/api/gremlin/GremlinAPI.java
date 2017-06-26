package com.baidu.hugegraph.api.gremlin;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.gremlin.Response;

public class GremlinAPI extends API {

    public GremlinAPI(RestClient client) {
        super(client);
        this.path(HugeType.GREMLIN.string());
    }

    public Response post(GremlinRequest request) {
        RestResult result = this.client.post(path(), request);
        return result.readObject(Response.class);
    }

}
