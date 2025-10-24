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

package org.apache.hugegraph.loader.util;

import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.util.E;
import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.HugeClientBuilder;
import org.apache.hugegraph.driver.factory.PDHugeClientFactory;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.loader.constant.Constants;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadOptions;
// import org.apache.hugegraph.loader.fake.FakeHugeClient;

public final class HugeClientHolder {

    public static final Logger LOG = Log.logger(HugeClientHolder.class);

    public static HugeClient create(LoadOptions options) {
        return create(options, true);
    }

    /**
     * Creates and returns a HugeClient instance based on the provided options.
     * @param options the configuration options for the HugeClient
     * @param useDirect indicates whether the direct connection option is enabled
     * @return a HugeClient instance
     */
    public static HugeClient create(LoadOptions options, boolean useDirect) {

        // if (useDirect && options.direct) {
        //     HugeClientBuilder builder = HugeClient.builder(options.pdPeers,
        //                                                    options.graphSpace,
        //                                                    options.graph);

        //     // use FakeHugeClient to connect to pd-store directly.
        //     LOG.info("create FakeHugeClient with pd address {}",
        //              options.pdPeers);
        //     return FakeHugeClient.getInstance(builder, options);
        // }

        if (StringUtils.isNotEmpty(options.pdPeers)) {
            pickHostFromMeta(options);
        }
        boolean useHttps = options.protocol != null &&
                           options.protocol.equals(LoadOptions.HTTPS_SCHEMA);
        String address = options.host + ":" + options.port;
        if (!options.host.startsWith(Constants.HTTP_PREFIX) &&
            !options.host.startsWith(Constants.HTTPS_PREFIX)) {
            if (useHttps) {
                address = Constants.HTTPS_PREFIX + address;
            } else {
                address = Constants.HTTP_PREFIX + address;
            }
        }
        String username = options.username != null ?
                          options.username : options.graph;
        HugeClientBuilder builder;
        try {
            builder = HugeClient.builder(address, options.graphSpace,
                                         options.graph)
                                .configTimeout(options.timeout)
                                .configToken(options.token)
                                .configUser(username, options.password)
                                .configPool(options.maxConnections,
                                            options.maxConnectionsPerRoute);

            if (useHttps) {
                String trustFile;
                if (options.trustStoreFile == null) {
                    String homePath = System.getProperty("loader.home.path");
                    E.checkArgument(StringUtils.isNotEmpty(homePath),
                                    "The system property 'loader.home.path' " +
                                    "can't be null or empty when enable " +
                                    "https protocol");
                    trustFile = Paths.get(homePath, Constants.TRUST_STORE_FILE)
                                     .toString();
                } else {
                    trustFile = options.trustStoreFile;
                }
                // Hard code: "hugegraph"
                String token = options.trustStoreToken == null ?
                               "hugegraph" : options.trustStoreToken;
                builder.configSSL(trustFile, token);
            }
            return builder.build();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("The version")) {
                throw new LoadException("The version of hugegraph-client and " +
                                        "hugegraph-server don't match", e);
            }
            throw e;
        } catch (ServerException e) {
            String message = e.getMessage();
            if (Constants.STATUS_UNAUTHORIZED == e.status() ||
                (message != null && message.startsWith("Authentication"))) {
                throw new LoadException("Incorrect username or password", e);
            }
            throw e;
        } catch (ClientException e) {
            Throwable cause = e.getCause();
            if (cause == null || cause.getMessage() == null) {
                throw e;
            }
            String message = cause.getMessage();
            if (message.contains("Connection refused")) {
                throw new LoadException("The service %s:%s is unavailable", e,
                                        options.host, options.port);
            } else if (message.contains("java.net.UnknownHostException") ||
                       message.contains("Host name may not be null")) {
                throw new LoadException("The host %s is unknown", e,
                                        options.host);
            } else if (message.contains("connect timed out")) {
                throw new LoadException("Connect service %s:%s timeout, " +
                                        "please check service is available " +
                                        "and network is unobstructed", e,
                                        options.host, options.port);
            }
            throw e;
        }
    }

    protected static void pickHostFromMeta(LoadOptions options) {
        PDHugeClientFactory clientFactory =
                new PDHugeClientFactory(options.pdPeers, options.routeType);

        List<String> urls = clientFactory.getAutoURLs(options.cluster,
                                                      options.graphSpace, null);

        E.checkState(CollectionUtils.isNotEmpty(urls), "No avaliable service!");

        int r = (int) Math.floor(Math.random() * urls.size());
        String url = urls.get(r);

        UrlParseUtil.Host hostInfo = UrlParseUtil.parseHost(url);

        E.checkState(StringUtils.isNotEmpty(hostInfo.getHost()),
                     "Parse url ({}) from pd meta error", url);

        options.host = hostInfo.getHost();
        options.port = hostInfo.getPort();

        if (StringUtils.isNotEmpty(hostInfo.getScheme())) {
            options.protocol = hostInfo.getScheme();
        }

        clientFactory.close();
    }
}
