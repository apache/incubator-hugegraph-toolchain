/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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

    public RestResult post(String path, Object object) throws ClientException {
        return this.post(path, object, null);
    }

    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers)
                           throws ClientException {
        return this.post(path, object, headers, null);
    }

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
             * We should specify the encoding of the entity object manually,
             * because Entity.json() method will reset "content encoding =
             * null" that has been set up by headers before.
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

    public RestResult put(String path, Object object,
                          Map<String, Object> params) throws ClientException {

        WebTarget target = this.target;

        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue());
            }
        }

        Response response = target.path(path).request().put(Entity.json(object));
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

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

    public RestResult get(String path, String id) throws ClientException {
        Response response = this.target.path(path).path(id).request().get();
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult delete(String path, String id) throws ClientException {
        Response response = this.target.path(path).path(id).request().delete();
        checkStatus(response, Response.Status.NO_CONTENT);
        return new RestResult(response);
    }

    public void close() {
        this.client.close();
    }

    private void checkStatus(Response response, Response.Status... status) {
        if (!Arrays.asList(status).contains(response.getStatusInfo())) {
            RestResult rs = new RestResult(response);
            ClientException exception = null;
            try {
                exception = rs.readObject(ClientException.class);
            } catch (Exception ignored) {
                exception = new ClientException(rs.content());
            }
            exception.status(response.getStatus());
            throw exception;
        }
    }
}
