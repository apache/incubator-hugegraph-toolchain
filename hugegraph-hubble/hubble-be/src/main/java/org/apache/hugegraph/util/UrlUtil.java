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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UrlUtil {

    public static Host parseHost(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid HTTP host: " + url);
        }

        String text = url.trim();
        try {
            URI uri = parseUri(text);
            String host = normalizeHost(uri.getHost());
            int port = uri.getPort();
            String scheme = uri.getScheme();

            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid HTTP host: " + text);
            }

            if (port < 0 && hasEmptyExplicitPort(uri)) {
                throw new IllegalArgumentException("Invalid HTTP host: " + text);
            }
            if (port < 0) {
                port = defaultPort(scheme);
            }
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("Invalid HTTP host: " + text);
            }

            return new Host(host, port, scheme);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid HTTP host: " + text, e);
        }
    }

    private static URI parseUri(String text) throws URISyntaxException {
        if (hasScheme(text) || text.startsWith("//")) {
            return new URI(text);
        }
        return new URI("//" + text);
    }

    private static boolean hasScheme(String text) {
        int schemeIdx = text.indexOf("://");
        if (schemeIdx <= 0 || !Character.isLetter(text.charAt(0))) {
            return false;
        }

        for (int i = 1; i < schemeIdx; i++) {
            char c = text.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' &&
                c != '.') {
                return false;
            }
        }
        return true;
    }

    private static int defaultPort(String scheme) {
        if (scheme == null) {
            return -1;
        }
        switch (scheme.toLowerCase(Locale.ROOT)) {
            case "http":
                return 80;
            case "https":
                return 443;
            default:
                return -1;
        }
    }

    private static boolean hasEmptyExplicitPort(URI uri) {
        String authority = uri.getRawAuthority();
        if (authority == null) {
            return false;
        }
        int userInfoIdx = authority.lastIndexOf('@');
        String hostPort = authority.substring(userInfoIdx + 1);
        return hostPort.endsWith(":");
    }

    private static String normalizeHost(String host) {
        if (host != null && host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Host {
        protected String host;
        protected int port;
        protected String scheme;
    }
}
