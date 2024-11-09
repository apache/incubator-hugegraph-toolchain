/*
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

package org.apache.hugegraph.client;

import java.util.Map;

import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.rest.AbstractRestClient;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.rest.RestClientConfig;
import org.apache.hugegraph.rest.RestHeaders;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.serializer.PathDeserializer;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.VersionUtil;
import org.apache.hugegraph.util.VersionUtil.Version;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class RestClient extends AbstractRestClient {

    private static final int SECOND = 1000;

    private static boolean filter = false;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Path.class, new PathDeserializer());
        RestResult.registerModule(module);
    }

    private Version apiVersion = null;

    public RestClient(String url, String username, String password, int timeout) {
        super(url, username, password, timeout * SECOND);
    }

    @Deprecated
    public RestClient(String url, String username, String password, int timeout,
                      int maxConns, int maxConnsPerRoute,
                      String trustStoreFile, String trustStorePassword) {
        super(url, username, password, timeout * SECOND, maxConns,
              maxConnsPerRoute, trustStoreFile, trustStorePassword);
    }

    public RestClient(String url, RestClientConfig config) {
        super(url, config);
    }

    private static String removeDefaultGsPrefix(String path) {
        final String DEFAULT_GS_PATH_PREFIX = "graphspaces/DEFAULT/";
        return path.replaceFirst(DEFAULT_GS_PATH_PREFIX, "");
    }

    public void apiVersion(Version version) {
        E.checkNotNull(version, "api version");
        this.apiVersion = version;
    }

    public Version apiVersion() {
        return this.apiVersion;
    }

    public void checkApiVersion(String minVersion, String message) {
        if (this.apiVersionLt(minVersion)) {
            throw new ClientException("HugeGraphServer API version must be >= %s to support " +
                                      "%s, but current HugeGraphServer API version is: %s",
                                      minVersion, message, this.apiVersion.get());
        }
    }

    public boolean apiVersionLt(String minVersion) {
        String apiVersion = this.apiVersion == null ? null : this.apiVersion.get();
        return apiVersion != null && !VersionUtil.gte(apiVersion, minVersion);
    }

    @Override
    public RestResult post(String path, Object object) {
        return super.post(filter ? path : removeDefaultGsPrefix(path), object);
    }

    @Override
    public RestResult get(String path, String id) {
        return super.get(filter ? path : removeDefaultGsPrefix(path), id);
    }

    @Override
    public RestResult delete(String path, Map<String, Object> params) {
        return super.delete(filter ? path : removeDefaultGsPrefix(path), params);
    }

    @Override
    public RestResult delete(String path, String id) {
        return super.delete(filter ? path : removeDefaultGsPrefix(path), id);
    }

    @Override
    public RestResult post(String path, Object object, RestHeaders headers) {
        return super.post(filter ? path : removeDefaultGsPrefix(path), object, headers, null);
    }

    @Override
    public RestResult post(String path, Object object, Map<String, Object> params) {
        return super.post(filter ? path : removeDefaultGsPrefix(path), object, null, params);
    }

    @Override
    public RestResult put(String path, String id, Object object) {
        return super.put(filter ? path : removeDefaultGsPrefix(path), id, object, null, null);
    }

    @Override
    public RestResult put(String path, String id, Object object, RestHeaders headers) {
        return super.put(filter ? path : removeDefaultGsPrefix(path), id, object, headers, null);
    }

    @Override
    public RestResult put(String path, String id, Object object, Map<String, Object> params) {
        return super.put(filter ? path : removeDefaultGsPrefix(path), id, object, null, params);
    }

    @Override
    public RestResult get(String path) {
        return super.get(filter ? path : removeDefaultGsPrefix(path));
    }

    @Override
    public RestResult get(String path, Map<String, Object> params) {
        return super.get(filter ? path : removeDefaultGsPrefix(path), params);
    }

    @Override
    protected void checkStatus(okhttp3.Response response, int... statuses) {
        boolean match = filter;
        for (int status : statuses) {
            if (status == response.code()) {
                match = true;
                break;
            }
        }
        if (!match) {
            throw ServerException.fromResponse(response);
        }
    }
}
