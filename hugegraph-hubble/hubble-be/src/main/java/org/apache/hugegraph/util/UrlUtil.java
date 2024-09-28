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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UrlUtil {
    public static Host parseHost(String url) {
        Host host = new Host();

        String text = url;
        String scheme = null;
        int schemeIdx = url.indexOf("://");
        if (schemeIdx > 0) {
            scheme = url.substring(0, schemeIdx);
            text = url.substring(schemeIdx + 3);
        }

        int port = -1;
        int portIdx = text.lastIndexOf(":");
        if (portIdx > 0) {
            String portStr = null;
            int pathIdx = text.indexOf("/");
            if (pathIdx > 0) {
                portStr = text.substring(portIdx + 1, pathIdx);
            } else {
                portStr = text.substring(portIdx + 1);
            }
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid HTTP host: " + text,
                                                   e);
            }

            text = text.substring(0, portIdx);

            host.setScheme(scheme);
            host.setHost(text);
            host.setPort(port);
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


