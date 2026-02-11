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

package org.apache.hugegraph.config;

import org.apache.hugegraph.driver.factory.PDHugeClientFactory;
import org.apache.hugegraph.options.HubbleOptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class MetaConfig {


    @Autowired
    private HugeConfig config;

    @Bean("cluster")
    public String getCluster() {
        return this.config.get(HubbleOptions.PD_CLUSTER);
    }

    @Bean
    PDHugeClientFactory pdHugeClientFactory() {

        String pdAddrs = this.config.get(HubbleOptions.PD_PEERS);

        String routeType = this.config.get(HubbleOptions.ROUTE_TYPE);

        return new PDHugeClientFactory(pdAddrs, routeType);
    }

}
