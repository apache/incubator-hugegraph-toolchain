/*
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

package org.apache.hugegraph.driver;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.space.SchemaTemplateAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.space.SchemaTemplate;

public class SchemaTemplateManager {

    private final SchemaTemplateAPI schemaTemplateAPI;

    public SchemaTemplateManager(RestClient client, String graphSpace) {
        this.schemaTemplateAPI = new SchemaTemplateAPI(client, graphSpace);
    }

    public List<String> listSchemTemplate() {
        return this.schemaTemplateAPI.list();
    }

    public Map getSchemaTemplate(String name) {
        return this.schemaTemplateAPI.get(name);
    }

    public Map createSchemaTemplate(SchemaTemplate template) {
        SchemaTemplate.SchemaTemplateReq req
                = SchemaTemplate.SchemaTemplateReq.fromBase(template);
        return this.schemaTemplateAPI.create(req);
    }

    public Map updateSchemaTemplate(SchemaTemplate template) {
        SchemaTemplate.SchemaTemplateReq req
                = SchemaTemplate.SchemaTemplateReq.fromBase(template);

        return this.schemaTemplateAPI.update(req);
    }

    public void deleteSchemaTemplate(String name) {
        this.schemaTemplateAPI.delete(name);
    }
}
