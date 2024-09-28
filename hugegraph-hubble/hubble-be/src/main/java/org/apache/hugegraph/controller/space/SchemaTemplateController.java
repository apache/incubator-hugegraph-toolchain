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
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.service.space.SchemaTemplateService;
import org.apache.hugegraph.structure.space.SchemaTemplate;


@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace" +
        "}/schematemplates")
public class SchemaTemplateController extends BaseController {
    @Autowired
    SchemaTemplateService schemaTemplateService;

    @GetMapping("list")
    public Object listName(@PathVariable("graphspace") String graphSpace) {
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
        HugeClient client = this.authClient(graphSpace, null);

        return schemaTemplateService.create(client, schemaTemplate);
    }

    @DeleteMapping("{name}")
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, null);
        schemaTemplateService.delete(client, name);
    }

    @PutMapping("{name}")
    public Object update(@PathVariable("graphspace") String graphSpace,
                         @PathVariable("name") String name,
                         @RequestBody SchemaTemplate schemaTemplate) {
        HugeClient client = this.authClient(graphSpace, null);
        schemaTemplate.name(name);
        return schemaTemplateService.update(client, schemaTemplate);
    }
}
