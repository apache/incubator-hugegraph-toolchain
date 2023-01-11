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

package org.apache.hugegraph.service;

import org.springframework.stereotype.Service;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.entity.GraphConnection;
import org.apache.hugegraph.options.HubbleOptions;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class SettingSSLService {

    public void configSSL(HugeConfig config, GraphConnection connection) {
        String protocol = config.get(HubbleOptions.SERVER_PROTOCOL);
        if (protocol != null && protocol.equals("https")) {
            connection.setProtocol(protocol);
            String trustStoreFile = config.get(
                                    HubbleOptions.CLIENT_TRUSTSTORE_FILE);
            String trustStorePass = config.get(
                                    HubbleOptions.CLIENT_TRUSTSTORE_PASSWORD);
            connection.setTrustStoreFile(trustStoreFile);
            connection.setTrustStorePassword(trustStorePass);
        }
    }
}
