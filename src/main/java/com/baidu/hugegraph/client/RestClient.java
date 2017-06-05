package com.baidu.hugegraph.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.baidu.hugegraph.exception.ClientException;

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
    public RestResult post(String path, Object object) throws ClientException {
        Response response = this.target.path(path)
                .request().post(Entity.json(object));
        // If check status failed, throw client exception.
        if (!checkStatus(response, Response.Status.CREATED) &&
            !checkStatus(response, Response.Status.OK)) {
            throw response.readEntity(ClientException.class);
        }
        return new RestResult(response);
    }

    // get
    public RestResult get(String path) throws ClientException {
        Response response = this.target.path(path).request().get();
        if (!checkStatus(response, Response.Status.OK)) {
            throw response.readEntity(ClientException.class);
        }
        return new RestResult(response);
    }

    // list
    public RestResult get(String path, String id) throws ClientException {
        Response response = this.target.path(path).path(id).request().get();
        if (!checkStatus(response, Response.Status.OK)) {
            throw response.readEntity(ClientException.class);
        }
        return new RestResult(response);
    }

    // remove
    public RestResult delete(String path, String id) throws ClientException {
        Response response = this.target.path(path).path(id).request().delete();
        if (!checkStatus(response, Response.Status.NO_CONTENT)) {
            throw response.readEntity(ClientException.class);
        }
        return new RestResult(response);
    }

    public void close() {
        this.client.close();
    }

    private boolean checkStatus(Response response, Response.Status status) {
        return response.getStatus() == status.getStatusCode();
    }
}
