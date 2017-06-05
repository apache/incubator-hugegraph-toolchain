package com.baidu.hugegraph.api.graphs;

import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.exception.ClientException;
import com.baidu.hugegraph.structure.constant.HugeType;

/**
 * Created by liningrui on 2017/5/31.
 */
public class GraphsAPI extends API {

    public GraphsAPI(RestClient client) {
        super(client);
        this.path(type());
    }

    public String type() {
        return HugeType.GRAPHS.string();
    }

    public Map<String, String> get(String name) {
        try {
            RestResult result = this.client.get(path(), name);
            return result.readObject(Map.class);
        } catch (ClientException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public List<String> list() {
        try {
            RestResult result = this.client.get(path());
            return result.readList(type(), String.class);
        } catch (ClientException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
