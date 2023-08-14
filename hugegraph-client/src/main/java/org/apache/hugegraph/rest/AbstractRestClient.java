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

package org.apache.hugegraph.rest;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import okhttp3.*;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.util.JsonUtil;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRestClient implements RestClient {

//    private static final CloseableHttpClient httpclient = HttpClients.createMinimal();

    private OkHttpClient client;

//    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888));
//    OkHttpClient client = new OkHttpClient.Builder().proxy(proxy).build();

    private String baseUrl;

    public AbstractRestClient(String url, int timeout) {
        this(url, OkhttpConfig.builder()
                .timeout(timeout)
                .build());
    }

    public AbstractRestClient(String url, String user, String password,
                              Integer timeout) {
        this(url, OkhttpConfig.builder()
                .user(user).password(password)
                .timeout(timeout)
                .build());
    }

    public AbstractRestClient(String url, int timeout,
                              int maxTotal, int maxPerRoute) {
        this(url, null, null, timeout, maxTotal, maxPerRoute);
    }

    public AbstractRestClient(String url, Integer timeout, Integer idleTime,
                              Integer maxTotal, Integer maxPerRoute) {
        this(url, OkhttpConfig.builder()
                .idleTime(idleTime)
                .timeout(timeout)
                .maxTotal(maxTotal)
                .maxPerRoute(maxPerRoute)
                .build());
    }

    public AbstractRestClient(String url, String user, String password,
                              Integer timeout, Integer maxTotal, Integer maxPerRoute) {
        this(url, OkhttpConfig.builder()
                .user(user).password(password)
                .timeout(timeout)
                .maxTotal(maxTotal)
                .maxPerRoute(maxPerRoute)
                .build());
    }

    public AbstractRestClient(String url, String user, String password,
                              Integer timeout, Integer maxTotal, Integer maxPerRoute,
                              String trustStoreFile,
                              String trustStorePassword) {
        this(url, OkhttpConfig.builder()
                .user(user).password(password)
                .timeout(timeout)
                .maxTotal(maxTotal)
                .maxPerRoute(maxPerRoute)
                .trustStoreFile(trustStoreFile)
                .trustStorePassword(trustStorePassword)
                .build());
    }

    public AbstractRestClient(String url, String token, Integer timeout) {
        this(url, OkhttpConfig.builder()
                .token(token)
                .timeout(timeout)
                .build());
    }

    public AbstractRestClient(String url, String token, Integer timeout,
                              Integer maxTotal, Integer maxPerRoute) {
        this(url,OkhttpConfig.builder()
                .token(token)
                .timeout(timeout)
                .maxTotal(maxTotal)
                .maxPerRoute(maxPerRoute)
                .build());
    }

    public AbstractRestClient(String url, String token, Integer timeout,
                              Integer maxTotal, Integer maxPerRoute,
                              String trustStoreFile,
                              String trustStorePassword) {
        this(url,OkhttpConfig.builder()
                .token(token)
                .timeout(timeout)
                .maxTotal(maxTotal)
                .maxPerRoute(maxPerRoute)
                .trustStoreFile(trustStoreFile)
                .trustStorePassword(trustStorePassword)
                .build());
    }

    public AbstractRestClient(String url, OkhttpConfig okhttpConfig) {
        this.baseUrl = url;
        this.client = getOkhttpClient(okhttpConfig);
    }

    private OkHttpClient getOkhttpClient(OkhttpConfig okhttpConfig) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if(okhttpConfig.getTimeout()!=null) {
            builder.connectTimeout(okhttpConfig.getTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(okhttpConfig.getTimeout(), TimeUnit.MILLISECONDS);
        }

        if(okhttpConfig.getIdleTime()!=null) {
            ConnectionPool connectionPool = new ConnectionPool(5, okhttpConfig.getIdleTime(), TimeUnit.MILLISECONDS);
            builder.connectionPool(connectionPool);
        }


        //auth
        if(StringUtils.isNotBlank(okhttpConfig.getUser()) && StringUtils.isNotBlank(okhttpConfig.getPassword())) {
            builder.addInterceptor(new BasicAuthInterceptor(okhttpConfig.getUser(), okhttpConfig.getPassword()));
        }
        if(StringUtils.isNotBlank(okhttpConfig.getToken())) {
            builder.addInterceptor(new TokenInterceptor(okhttpConfig.getToken()));
        }

        OkHttpClient okHttpClient = builder.build();

        if(okhttpConfig.getMaxTotal()!=null) {
            okHttpClient.dispatcher().setMaxRequests(okhttpConfig.getMaxTotal());
        }

        if(okhttpConfig.getMaxPerRoute()!=null) {
            okHttpClient.dispatcher().setMaxRequestsPerHost(okhttpConfig.getMaxPerRoute());
        }

        return okHttpClient;
    }

    @Override
    public RestResult post(String path, Object object) {
        return this.post(path, object, null, null);
    }

    @Override
    public RestResult post(String path, Object object, Headers headers) {
        return this.post(path, object, headers, null);
    }

    @Override
    public RestResult post(String path, Object object, Map<String, Object> params) {
        return this.post(path, object, null, params);
    }

    private Request.Builder getRequestBuilder(String path, String id, Headers headers, Map<String, Object> params ) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder()
                .addPathSegments(path);
        if(id!=null) {
            urlBuilder.addPathSegment(id);
        }

        if(params!=null) {
            params.forEach((name, value) -> {
                if(value==null){
                    return;
                }

                if(value instanceof Collection) {
                    for (Object i : (Collection<?>) value) {
                        urlBuilder.addQueryParameter(name, String.valueOf(i));
                    }
                } else {
                    urlBuilder.addQueryParameter(name, String.valueOf(value));
                }
            });
        }

        Request.Builder builder = new Request.Builder()
                .url(urlBuilder.build());

        if(headers!=null) {
            builder.headers(headers);
        }

        this.attachAuthToRequest(builder);

        return builder;
    }

    private static RequestBody getRequestBody(Object object, Headers headers) {
        String contentType = parseContentType(headers);
        String bodyContent = "application/json".equals(contentType) ? JsonUtil.toJson(object) : String.valueOf(object);

        RequestBody requestBody = RequestBody.create(MediaType.parse(contentType), bodyContent);
        if(headers!=null && "gzip".equals(headers.get("Content-Encoding"))) {
            requestBody = gzip(requestBody);
        }
        return requestBody;
    }

    private static RequestBody gzip(final RequestBody body) {
        return new RequestBody() {
            @Override public MediaType contentType() {
                return body.contentType();
            }

            @Override public long contentLength() {
                return -1; // We don't know the compressed length in advance!
            }

            @Override public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }

    @SneakyThrows
    @Override
    public RestResult post(String path, Object object,
                           Headers headers,
                           Map<String, Object> params) {
        Request.Builder requestBuilder = getRequestBuilder(path, null, headers, params);
        requestBuilder.post(getRequestBody(object, headers));

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            checkStatus(response, 200, 201, 202);
            return new RestResult(response);
        }
    }

    @Override
    public RestResult put(String path, String id, Object object) {
        return this.put(path, id, object, ImmutableMap.of());
    }

    @Override
    public RestResult put(String path, String id, Object object,
                          Headers headers) {
        return this.put(path, id, object, headers, null);
    }

    @Override
    public RestResult put(String path, String id, Object object,
                          Map<String, Object> params) {
        return this.put(path, id, object, null, params);
    }

    @SneakyThrows
    @Override
    public RestResult put(String path, String id, Object object,
                          Headers headers,
                          Map<String, Object> params) {
        Request.Builder requestBuilder = getRequestBuilder(path, id, headers, params);
        requestBuilder.put(getRequestBody(object, headers));

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            checkStatus(response, 200, 202);
            return new RestResult(response);
        }
    }

    private static String parseContentType(Headers headers) {
        if(headers!=null) {
            String contentType = headers.get("Content-Type");
            if(contentType!=null) {
                return contentType;
            }
        }
        return "application/json";
    }

    @Override
    public RestResult get(String path) {
        return this.get(path, null, ImmutableMap.of());
    }

    @Override
    public RestResult get(String path, Map<String, Object> params) {
        return this.get(path, null, params);
    }

    @Override
    public RestResult get(String path, String id) {
        return this.get(path, id, ImmutableMap.of());
    }

    @SneakyThrows
    private RestResult get(String path, String id, Map<String, Object> params) {
        Request.Builder requestBuilder = getRequestBuilder(path, id, null, params);

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            checkStatus(response, 200);
            return new RestResult(response);
        }
    }


    @Override
    public RestResult delete(String path, Map<String, Object> params) {
        return this.delete(path, null, params);
    }

    @Override
    public RestResult delete(String path, String id) {
        return this.delete(path, id, ImmutableMap.of());
    }

    @SneakyThrows
    private RestResult delete(String path, String id,
                              Map<String, Object> params) {
        Request.Builder requestBuilder = getRequestBuilder(path, id, null, params);
        requestBuilder.delete();

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            checkStatus(response, 204, 202);
            return new RestResult(response);
        }
    }

//    protected abstract void checkStatus(HttpResponse response, int... statuses);
    protected abstract void checkStatus(Response response, int... statuses);

    @SneakyThrows
    @Override
    public void close() {
        if(client!=null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            if(client.cache()!=null) {
                client.cache().close();
            }
        }
    }

    private final ThreadLocal<String> authContext =
                                      new InheritableThreadLocal<>();

    public void setAuthContext(String auth) {
        this.authContext.set(auth);
    }

    public void resetAuthContext() {
        this.authContext.remove();
    }

    public String getAuthContext() {
        return this.authContext.get();
    }

    private void attachAuthToRequest(Request.Builder builder) {
        // Add auth header
        String auth = this.getAuthContext();
        if (StringUtils.isNotEmpty(auth)) {
            builder.addHeader("Authorization", auth);
        }
    }

    public static class HostNameVerifier implements HostnameVerifier {

        private final String url;

        public HostNameVerifier(String url) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            url = URI.create(url).getHost();
            this.url = url;
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            if (!this.url.isEmpty() && this.url.endsWith(hostname)) {
                return true;
            } else {
                HostnameVerifier verifier = HttpsURLConnection
                                            .getDefaultHostnameVerifier();
                return verifier.verify(hostname, session);
            }
        }
    }

    private static class NoCheckTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                       throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                       throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public static TrustManager[] create() {
            return new TrustManager[]{new NoCheckTrustManager()};
        }
    }




}
