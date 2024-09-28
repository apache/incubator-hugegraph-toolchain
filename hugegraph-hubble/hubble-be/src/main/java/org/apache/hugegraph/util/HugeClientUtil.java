/*
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

package org.apache.hugegraph.util;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ServerException;
import org.springframework.web.util.UriComponentsBuilder;


import  org.apache.hugegraph.rest.ClientException;
import com.google.common.collect.ImmutableSet;

public final class HugeClientUtil {

    private static final String DEFAULT_PROTOCOL = "http";

    private static final Set<String> ACCEPTABLE_EXCEPTIONS = ImmutableSet.of(
            "Permission denied: execute Resource"
    );

    public static HugeClient tryConnect(GraphConnection connection) {
        String graphSpace = connection.getGraphSpace();
        String graph = connection.getGraph();
        String host = connection.getHost();
        Integer port = connection.getPort();
        String token = connection.getToken();
        String username = connection.getUsername();
        String password = connection.getPassword();
        int timeout = connection.getTimeout();
        String protocol = StringUtils.isEmpty(connection.getProtocol()) ?
                          DEFAULT_PROTOCOL :
                          connection.getProtocol();
        String trustStoreFile = connection.getTrustStoreFile();
        String trustStorePassword = connection.getTrustStorePassword();

        String url = UriComponentsBuilder.newInstance()
                                  .scheme(protocol)
                                  .host(host).port(port)
                                  .toUriString();
        if (username == null) {
            username = "";
            password = "";
        }
        HugeClient client;
        try {
            client = HugeClient.builder(url, graphSpace, graph)
                               .configToken(token)
                               .configUser(username, password)
                               .configTimeout(timeout)
                               .configSSL(trustStoreFile, trustStorePassword)
                               .build();
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("The version")) {
                throw new ExternalException("client-server.version.unmatched", e);
            }
            if (message != null && (message.startsWith("Error loading trust store from") ||
                message.startsWith("Cannot find trust store file"))) {
                throw new ExternalException("https.load.truststore.error", e);
            }
            throw e;
        } catch (ServerException e) {
            String message = e.getMessage();
            if (Constant.STATUS_UNAUTHORIZED == e.status() ||
                (message != null && message.startsWith("Authentication"))) {
                throw new ExternalException(
                          "graph-connection.username-or-password.incorrect", e);
            }
            if (message != null && message.contains("Invalid syntax for " +
                                                    "username and password")) {
                throw new ExternalException(
                          "graph-connection.missing-username-password", e);
            }
            throw e;
        } catch (ClientException e) {
            Throwable cause = e.getCause();
            if (cause == null || cause.getMessage() == null) {
                throw e;
            }
            String message = cause.getMessage();
            if (message.contains("Connection refused")) {
                throw new ExternalException("service.unavailable", e, host, port);
            } else if (message.contains("java.net.UnknownHostException") ||
                       message.contains("Host name may not be null")) {
                throw new ExternalException("service.unknown-host", e, host);
            } else if (message.contains("<!doctype html>")) {
                throw new ExternalException("service.suspected-web",
                                            e, host, port);
            }
            throw e;
        }

        return client;
    }

    private static boolean isAcceptable(String message) {
        if (message == null) {
            return false;
        }
        for (String acceptableMessage : ACCEPTABLE_EXCEPTIONS) {
            if (message.contains(acceptableMessage)) {
                return true;
            }
        }
        return false;
    }
}
