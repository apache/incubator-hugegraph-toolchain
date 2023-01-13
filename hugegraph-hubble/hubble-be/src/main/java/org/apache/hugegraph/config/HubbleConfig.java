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

import java.io.File;

import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.options.HubbleOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.hugegraph.common.Constant;

@Configuration
public class HubbleConfig {

    @Autowired
    private ApplicationArguments arguments;

    @Bean
    public HugeConfig hugeConfig() {
        String[] args = this.arguments.getSourceArgs();
        if (args.length > 1) {
            throw new ExternalException(
                      "HugeGraphHubble accept up to one param as config file");
        } else if (args.length == 0) {
            args = new String[]{Constant.CONFIG_FILE};
        }

        // Register hubble config options
        OptionSpace.register(Constant.MODULE_NAME, HubbleOptions.instance());
        String conf = args[0];
        try {
            String path = HubbleConfig.class.getClassLoader()
                                            .getResource(conf).getPath();
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                conf = path;
            }
        } catch (Exception ignored) {
        }
        return new HugeConfig(conf);
    }
}
