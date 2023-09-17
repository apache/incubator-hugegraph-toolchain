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

package org.apache.hugegraph.spark.connector.client;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.HugeClientBuilder;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.rest.ClientException;
import org.apache.hugegraph.spark.connector.constant.Constants;
import org.apache.hugegraph.spark.connector.exception.LoadException;
import org.apache.hugegraph.spark.connector.options.HGOptions;
import org.apache.hugegraph.util.E;

public final class HGClientHolder implements Serializable {

    public static HugeClient create(HGOptions options) {
        String host = options.host();
        int port = options.port();
        String graph = options.graph();
        String username = options.username();
        String token = options.token();
        String trustStoreFile = options.trustStoreFile();
        String trustStoreToken = options.trustStoreToken();

        boolean useHttps = options.protocol().equals(Constants.HTTPS_SCHEMA);
        String address = host + Constants.COLON_STR + port;
        if (!host.startsWith(Constants.HTTP_PREFIX) && !host.startsWith(Constants.HTTPS_PREFIX)) {
            if (useHttps) {
                address = Constants.HTTPS_PREFIX + address;
            } else {
                address = Constants.HTTP_PREFIX + address;
            }
        }
        username = Objects.nonNull(username) ? username : graph;
        HugeClientBuilder builder;
        try {
            builder = HugeClient.builder(address, graph)
                                .configUser(username, token)
                                .configTimeout(options.timeout())
                                .configPool(options.maxConnection(),
                                            options.maxConnectionPerRoute());
            if (useHttps) {
                String trustFile = trustStoreFile;
                if (Objects.isNull(trustStoreFile)) {
                    String homePath = System.getProperty(Constants.CONNECTOR_HOME_PATH);
                    E.checkArgument(StringUtils.isNotEmpty(homePath),
                                    "The system property 'connector.home.path' " +
                                    "can't be null or empty when enable https protocol");
                    trustFile = Paths.get(homePath, Constants.TRUST_STORE_PATH).toString();
                }
                trustStoreToken = Objects.isNull(trustStoreToken) ?
                                  Constants.DEFAULT_TRUST_STORE_TOKEN : trustStoreToken;
                builder.configSSL(trustFile, trustStoreToken);
            }
            return builder.build();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if (Objects.nonNull(message) && message.startsWith("The version")) {
                throw new LoadException("The version of hugegraph-client and " +
                                        "hugegraph-server don't match", e);
            }
            throw e;
        } catch (ServerException e) {
            String message = e.getMessage();
            if (Constants.STATUS_UNAUTHORIZED == e.status() ||
                (Objects.nonNull(message) && message.startsWith("Authentication"))) {
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
                throw new LoadException(
                        String.format("The service %s:%s is unavailable", host, port), e);
            } else if (message.contains("java.net.UnknownHostException") ||
                       message.contains("Host name may not be null")) {
                throw new LoadException(String.format("The host %s is unknown", host), e);
            } else if (message.contains("connect timed out")) {
                throw new LoadException(String.format("Connect service %s:%s timeout, " +
                                                      "please check service is available " +
                                                      "and network is unobstructed", host, port),
                                        e);
            }
            throw e;
        }
    }
}
