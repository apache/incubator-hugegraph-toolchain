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

package org.apache.hugegraph.handler;

import org.apache.hugegraph.license.ServerInfo;
import org.apache.hugegraph.options.HubbleOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import org.apache.hugegraph.config.HugeConfig;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
// TODO: Why we need this class?
public class CustomApplicationRunner implements ApplicationRunner {

    @Autowired
    private HugeConfig config;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String serverId = this.config.get(HubbleOptions.SERVER_ID);
        ServerInfo serverInfo = new ServerInfo(serverId);
    }
}
