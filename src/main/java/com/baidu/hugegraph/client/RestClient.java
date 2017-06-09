package com.baidu.hugegraph.client;

import java.util.Arrays;

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
        checkStatus(response, Response.Status.CREATED, Response.Status.OK);
        return new RestResult(response);
    }

    // list
    public RestResult get(String path) throws ClientException {
        Response response = this.target.path(path).request().get();
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    // get
    public RestResult get(String path, String id) throws ClientException {
        Response response = this.target.path(path).path(id).request().get();
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    // remove
    public RestResult delete(String path, String id) throws ClientException {
        Response response = this.target.path(path).path(id).request().delete();
        checkStatus(response, Response.Status.NO_CONTENT);
        return new RestResult(response);
    }

    public void close() {
        this.client.close();
    }

    private void checkStatus(Response response, Response.Status... statuses) {
        if (!Arrays.asList(statuses).contains(response.getStatusInfo())) {
            RestResult rs = new RestResult(response);
            ClientException exception = null;
            try {
                exception = rs.readObject(ClientException.class);
            } catch (Exception e) {
                // ignore e
                exception = new ClientException(rs.content());
            }
            exception.status(response.getStatus());
            throw exception;
        }
    }
}
