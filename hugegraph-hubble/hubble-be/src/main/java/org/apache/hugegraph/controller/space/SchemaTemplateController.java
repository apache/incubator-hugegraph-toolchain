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

package org.apache.hugegraph.controller.space;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ForbiddenException;
import org.apache.hugegraph.options.HubbleOptions;
import org.apache.hugegraph.service.space.SchemaTemplateService;
import org.apache.hugegraph.structure.space.SchemaTemplate;
import org.apache.hugegraph.util.E;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace" +
        "}/schematemplates")
public class SchemaTemplateController extends BaseController {
    @Autowired
    SchemaTemplateService schemaTemplateService;

    @Autowired
    HugeConfig config;

    private boolean isPdEnabled() {
        return config.get(HubbleOptions.PD_ENABLED);
    }

    @GetMapping("list")
    public Object listName(@PathVariable("graphspace") String graphSpace) {
        if (!isPdEnabled()) {
            return ImmutableMap.of("schemas", java.util.Collections.emptyList());
        }
        HugeClient client = this.authClient(graphSpace, null);
        List<String> names = schemaTemplateService.listName(client);

        return ImmutableMap.of("schemas", names);
    }

    @GetMapping()
    public Object list(@PathVariable("graphspace") String graphSpace,
                       @RequestParam(name = "query", required = false,
                               defaultValue = "") String query,
                       @RequestParam(name = "page_no", required = false,
                               defaultValue = "1") int pageNo,
                       @RequestParam(name = "page_size", required = false,
                               defaultValue = "10") int pageSize) {
        if (!isPdEnabled()) {
            return ImmutableMap.of("records", java.util.Collections.emptyList(),
                                  "total", 0);
        }
        HugeClient client = this.authClient(graphSpace, null);
        return schemaTemplateService.queryPage(client, query, pageNo, pageSize);
    }

    @GetMapping("{name}")
    public Object get(@PathVariable("graphspace") String graphSpace,
                      @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, null);
        return schemaTemplateService.get(client, name);
    }

    @PostMapping
    public Object create(@PathVariable("graphspace") String graphSpace,
                         @RequestBody SchemaTemplate schemaTemplate) {
        E.checkArgument(isPdEnabled(),
                "Schema template is not supported in standalone mode");
        HugeClient client = this.requireGraphSpaceWrite(graphSpace);

        return schemaTemplateService.create(client, schemaTemplate);
    }

    @DeleteMapping("{name}")
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("name") String name) {
        HugeClient client = this.requireTemplateOwnerOrManager(graphSpace,
                                                               name);
        schemaTemplateService.delete(client, name);
    }

    @PutMapping("{name}")
    public Object update(@PathVariable("graphspace") String graphSpace,
                         @PathVariable("name") String name,
                         @RequestBody SchemaTemplate schemaTemplate) {
        HugeClient client = this.requireTemplateOwnerOrManager(graphSpace,
                                                               name);
        schemaTemplate.name(name);
        return schemaTemplateService.update(client, schemaTemplate);
    }

    private HugeClient requireTemplateOwnerOrManager(String graphSpace,
                                                     String name) {
        HugeClient client = this.requireGraphSpaceWrite(graphSpace);
        if (this.authMode != null && this.authMode.anonymous()) {
            return client;
        }
        if (this.userService.isSuperAdmin(client) ||
            this.userService.isAssignSpaceAdmin(client, graphSpace)) {
            return client;
        }

        Map template = this.schemaTemplateService.get(client, name);
        if (!Objects.equals(this.getUser(), template.get("creator"))) {
            throw new ForbiddenException(
                    "Permission denied: modify schema template");
        }
        return client;
    }
}
