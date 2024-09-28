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

package org.apache.hugegraph.service.space;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.space.SchemaTemplate;
import org.apache.hugegraph.util.PageUtil;

@Service
public class SchemaTemplateService {

    public List<String> listName(HugeClient client) {
        return client.schemaTemplateManager().listSchemTemplate();
    }

    public IPage<Map> queryPage(HugeClient client, String query, int pageNo,
                                           int pageSize) {
        List<String> names = client.schemaTemplateManager().listSchemTemplate();

        List<Map> results =
                names.stream().filter((s) -> s.contains(query)).sorted()
                     .map((s) -> client.schemaTemplateManager()
                                       .getSchemaTemplate(s)
                     ).collect(Collectors.toList());

        return PageUtil.page(results, pageNo, pageSize);
    }

    public Map get(HugeClient client, String name) {
        return client.schemaTemplateManager().getSchemaTemplate(name);
    }

    public Map create(HugeClient client, SchemaTemplate schemaTemplate) {
        return client.schemaTemplateManager().createSchemaTemplate(schemaTemplate);
    }

    public void delete(HugeClient client, String name) {
        client.schemaTemplateManager().deleteSchemaTemplate(name);
    }

    public Map update(HugeClient client,
                      SchemaTemplate schemaTemplate) {
        return client.schemaTemplateManager().updateSchemaTemplate(schemaTemplate);
    }
}
