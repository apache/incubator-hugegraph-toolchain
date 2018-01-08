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
import java.util.concurrent.Callable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.GZipEncoder;

import com.baidu.hugegraph.exception.ClientException;
import com.baidu.hugegraph.exception.ServerException;
import com.google.common.collect.ImmutableMap;

public class RestClient {

    private static final int SECOND = 1000;

    private Client client;
    private WebTarget target;

    public RestClient(String url, int timeout) {
        this.client = ClientBuilder.newClient();
        this.client.property(ClientProperties.CONNECT_TIMEOUT, timeout * SECOND);
        this.client.property(ClientProperties.READ_TIMEOUT, timeout * SECOND);
        this.client.register(GZipEncoder.class);
        this.target = this.client.target(url);
    }

    public RestClient path(String path) {
        this.target.path(path);
        return this;
    }

    private Response request(Callable<Response> method) {
        try {
            return method.call();
        } catch (Exception e) {
            throw new ClientException("Failed to do request", e);
        }
    }

    public RestResult post(String path, Object object) throws ServerException {
        return this.post(path, object, null);
    }

    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers)
                           throws ServerException {
        return this.post(path, object, headers, null);
    }

    public RestResult post(String path, Object object,
                           MultivaluedMap<String, Object> headers,
                           Map<String, Object> params) throws ServerException {
        WebTarget target = this.target;
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                target = target.queryParam(param.getKey(), param.getValue());
            }
        }

        Ref<Invocation.Builder> builder = Refs.of(target.path(path).request());

        String encoding = null;
        if (headers != null && !headers.isEmpty()) {
            // Add headers
            builder.set(builder.get().headers(headers));
            encoding = (String) headers.getFirst("Content-Encoding");
        }

        /*
         * We should specify the encoding of the entity object manually,
         * because Entity.json() method will reset "content encoding =
         * null" that has been set up by headers before.
         */
        Ref<Entity<?>> entity = Refs.of(null);
        if (encoding == null) {
            entity.set(Entity.json(object));
        } else {
            Variant variant = new Variant(MediaType.APPLICATION_JSON_TYPE,
                                          (String) null, encoding);
            entity.set(Entity.entity(object, variant));
        }

        Response response = this.request(() -> {
            return builder.get().post(entity.get());
        });
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.CREATED, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult put(String path, Object object) throws ServerException {
        return this.put(path, object, ImmutableMap.of());
    }

    public RestResult put(String path, Object object,
                          Map<String, Object> params) throws ServerException {
        Ref<WebTarget> target = Refs.of(this.target);
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                target.set(target.get().queryParam(key, params.get(key)));
            }
        }

        Response response = this.request(() -> {
            return target.get().path(path).request().put(Entity.json(object));
        });
        // If check status failed, throw client exception.
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult get(String path) throws ServerException {
        Response response = this.request(() -> {
            return this.target.path(path).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult get(String path, Map<String, Object> params)
                          throws ServerException {
        Ref<WebTarget> target = Refs.of(this.target);
        for (String key : params.keySet()) {
            target.set(target.get().queryParam(key, params.get(key)));
        }
        Response response = this.request(() -> {
            return target.get().path(path).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult get(String path, String id) throws ServerException {
        Response response = this.request(() -> {
            return this.target.path(path).path(id).request().get();
        });
        checkStatus(response, Response.Status.OK);
        return new RestResult(response);
    }

    public RestResult delete(String path, Map<String, Object> params)
                             throws ServerException {
        Ref<WebTarget> target = Refs.of(this.target);
        for (String key : params.keySet()) {
            target.set(target.get().queryParam(key, params.get(key)));
        }
        Response response = this.request(() -> {
            return target.get().path(path).request().delete();
        });
        checkStatus(response, Response.Status.NO_CONTENT);
        return new RestResult(response);
    }

    public RestResult delete(String path, String id) throws ServerException {
        Response response = this.request(() -> {
            return this.target.path(path).path(id).request().delete();
        });
        checkStatus(response, Response.Status.NO_CONTENT);
        return new RestResult(response);
    }

    public void close() {
        this.client.close();
    }

    private static void checkStatus(Response response,
                                    Response.Status... status) {
        if (!Arrays.asList(status).contains(response.getStatusInfo())) {
            RestResult rs = new RestResult(response);
            ServerException exception;
            try {
                exception = rs.readObject(ServerException.class);
            } catch (Exception ignored) {
                exception = new ServerException(rs.content());
            }
            exception.status(response.getStatus());
            throw exception;
        }
    }

    public static String buildPath(String... paths) {
        return StringUtils.join(paths, "/");
    }
}
