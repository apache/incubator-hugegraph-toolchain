package com.baidu.hugegraph.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * Created by liningrui on 2017/5/23.
 */
public class RestClient {

    private Client client;
    private WebTarget target;

    public RestClient(String url) {
        this.client = ClientBuilder.newClient();
        this.target = this.client.target(url);
    }

    public RestClient path(String path) {
        this.target.path(path);
        return this;
    }

    // post
    public RestResult post(String path, Object object) {
        Response response = this.target.path(path)
                .request().post(Entity.json(object));
        return new RestResult(response);
    }

    // get
    public RestResult get(String path) {
        Response response = this.target.path(path).request().get();
        return new RestResult(response);
    }

    // list
    public RestResult get(String path, String id) {
        Response response = this.target.path(path).path(id).request().get();
        return new RestResult(response);
    }

    // remove
    public RestResult delete(String path, String id) {
        Response response = this.target.path(path).path(id).request().delete();
        return new RestResult(response);
    }

    public void close() {
        this.client.close();
    }
}
