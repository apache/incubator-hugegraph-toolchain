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

package org.apache.hugegraph.controller.query;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.query.Text2GqlRequest;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.query.Text2GqlSettingsService;
import org.apache.hugegraph.service.query.Text2GqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs/{graph}/text2gql")
public class Text2GqlController extends BaseController {

    @Autowired
    private Text2GqlService service;

    @Autowired
    private Text2GqlSettingsService settings;

    @PostMapping
    public Map<String, String> generate(@PathVariable("graphspace") String graphSpace,
                                        @PathVariable("graph") String graph,
                                        @RequestBody Text2GqlRequest request) {
        HugeClient client = this.authClient(graphSpace, graph);
        if (request == null) {
            throw new ExternalException("text2gql.invalid-request");
        }
        if (request.getLanguage() == null) {
            throw new ExternalException("text2gql.invalid-language");
        }
        if ("shared".equals(request.getSource())) {
            request = this.settings.sharedRequest(request);
        } else if (!"custom".equals(request.getSource())) {
            throw new ExternalException("text2gql.invalid-request");
        }
        return Map.of("query", this.service.generate(client, request), "language", request.getLanguage().value());
    }

    @GetMapping("config")
    public Map<String, Object> config(@PathVariable("graphspace") String graphSpace,
                                      @PathVariable("graph") String graph) {
        HugeClient client = this.authClient(graphSpace, graph);
        client.schema().getPropertyKeys();
        Map<String, Object> descriptor = new LinkedHashMap<>(this.settings.availability());
        descriptor.put("custom_available", true);
        return descriptor;
    }
}
