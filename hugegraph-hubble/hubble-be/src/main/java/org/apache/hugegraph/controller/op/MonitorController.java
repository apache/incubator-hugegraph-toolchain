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

package org.apache.hugegraph.controller.op;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.util.E;

@RestController
@RequestMapping(Constant.API_VERSION + "monitor")
public class MonitorController extends BaseController {

    @Autowired
    private HugeConfig config;

    @GetMapping
    public Object monitor() {
        String monitorURL = null;
        // Get monitor.url from system.env
        monitorURL = System.getenv(HubbleOptions.MONITOR_URL.name());
        if (StringUtils.isEmpty(monitorURL)) {
            // get monitor.url from file: hugegraph-hubble.properties
            monitorURL = config.get(HubbleOptions.MONITOR_URL);
        }

        E.checkArgument(StringUtils.isNotEmpty(monitorURL),
                        "Please set \"monitor.url\" in system environments " +
                                "or config file(hugegraph-hubble.properties).");

       return ImmutableMap.of("url", monitorURL);
    }
}
