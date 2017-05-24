package com.baidu.hugegraph.api;

import com.baidu.hugegraph.client.RestClient;

/**
 * Created by liningrui on 2017/5/20.
 */
public abstract class API {

    protected RestClient client;
    protected String path;

    public API(RestClient client) {
        this.client = client;
    }

    public String path() {
        return this.path;
    }

    public void path(String path) {
        this.path = path;
    }
}
