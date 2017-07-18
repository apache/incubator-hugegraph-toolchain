package com.baidu.hugegraph.client;

import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.message.GZipEncoder;

import com.baidu.hugegraph.exception.ClientException;

/**
 * Created by liningrui on 2017/5/23.
 */
public class RestClient {

    private Client client;
    private WebTarget target;

    public RestClient(String url) {
        this.client = ClientBuilder.newClient();
        this.client.register(GZipEncoder.class);
        this.target = this.client.target(url);
    }

    public RestClient path(String path) {
        this.target.path(path);
        return this;
    }

    // post
    public RestResult post(String path, Object object) throws ClientException {
        return this.post(path, object, null);
    }

    // post
    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers)
                           throws ClientException {
        return this.post(path, object, headers, null);
    }

    // post
    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers,
                           Map<String, Object> params) throws ClientException {
        WebTarget target = this.target;

        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue());
            }
        }

        Invocation.Builder builder = target.path(path).request();
        Entity entity = null;
        if (headers != null && !headers.isEmpty()) {
            // Add headers
            builder = builder.headers(headers);

            /*
             * We should manually specify the encoding of the entity object,
             * because Entity.json() method will reset "content encoding =
             * null" that has setted by headers before.
             */
            String encoding = (String) headers.getFirst("Content-Encoding");
            if (encoding != null) {
                entity = Entity.entity(object, new Variant(
                         MediaType.APPLICATION_JSON_TYPE,
                         (String) null, encoding));
            }
        }
        if (entity == null) {
            entity = Entity.json(object);
        }

        Response response = builder.post(entity);
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.CREATED, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult put(String path, Object object) throws ClientException {
        Response response = this.target.path(path)
                .request().put(Entity.json(object));
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    // list
    public RestResult get(String path) throws ClientException {
        Response response = this.target.path(path).request().get();
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult get(String path, Map<String, Object> params)
            throws ClientException {
        WebTarget target = this.target;
        for (Map.Entry<String, Object> param : params.entrySet()) {
            target = target.queryParam(param.getKey(), param.getValue());
        }
        Response response = target.path(path).request().get();
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
