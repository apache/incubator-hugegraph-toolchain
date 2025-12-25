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

package org.apache.hugegraph.controller.schema;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.HugeException;
import org.apache.hugegraph.service.schema.SchemaService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import org.apache.hugegraph.util.Log;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.entity.schema.LabelUpdateEntity;
import org.apache.hugegraph.entity.schema.Property;
import org.apache.hugegraph.entity.schema.PropertyIndex;
import org.apache.hugegraph.entity.schema.SchemaEntity;
import org.apache.hugegraph.entity.schema.SchemaLabelEntity;
import org.apache.hugegraph.entity.schema.Timefiable;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/schema")
public class SchemaController extends BaseController {

    public static Logger log = Log.logger(SchemaController.class);

    @Autowired
    private SchemaService schemaService;
    @GetMapping("groovy")
    public Object schemaGroovy(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph) {
        HugeClient client = this.authClient(graphSpace, graph);
        return ImmutableMap.of("schema", client.schema().getGroovySchema());
    }

    @PostMapping("groovy")
    public Object addSchemaGroovy(@PathVariable("graphspace") String graphSpace,
                                  @PathVariable("graph") String graph,
                                  @RequestBody SchemaGroovy schemaGroovy) {
        HugeClient client = this.authClient(graphSpace, graph);
        String content = schemaGroovy.getSchemaGroovy();
        log.info("Add schema groovy: {}", content);
        checkSchemaGroovy(content);
        try {
            client.gremlin().gremlin(content).execute();
        }catch (Exception e) {
            throw new HugeException(
                    "Add schema groovy failed. caused by" + e.getMessage());
        }
        return ImmutableMap.of("schema-groovy", content);
    }

    private void checkSchemaGroovy(String content) {
        String[] lines = content.split("\n|;");
        for (String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            if (!line.startsWith("graph.schema()")) {
                throw new ExternalException(
                        "Schema Groovy each row must start with 'graph.schema" +
                        "().'");
            }
        }
    }

    @GetMapping("groovy/export")
    public void schemaGroovyExport(@PathVariable("graphspace") String graphSpace,
                                     @PathVariable("graph") String graph,
                                     HttpServletResponse response) {
        HugeClient client = this.authClient(graphSpace, graph);
        String schema = client.schema().getGroovySchema();

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        String fileName = String.format("%s_%s.schema", graphSpace, graph);
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);
        try {
            OutputStream os = response.getOutputStream();
            os.write(schema.getBytes(StandardCharsets.UTF_8));
            os.close();
        } catch (IOException e) {
            throw new InternalException("Schema File Write Error", e);
        }
    }

    @GetMapping("graphview")
    public SchemaService.SchemaView displayInSchemaView(@PathVariable("graphspace") String graphSpace,
                                          @PathVariable("graph") String graph) {
        HugeClient client = this.authClient(graphSpace, graph);
        return schemaService.getSchemaView(client);
    }

    public <T extends SchemaEntity> IPage<T> listInPage(
                                             Function<HugeClient, List<T>> fetcher,
                                             HugeClient client, String content,
                                             String nameOrder,
                                             int pageNo, int pageSize) {
        Boolean nameOrderAsc = null;
        if (!StringUtils.isEmpty(nameOrder)) {
            Ex.check(ORDER_ASC.equals(nameOrder) || ORDER_DESC.equals(nameOrder),
                     "common.name-order.invalid", nameOrder);
            nameOrderAsc = ORDER_ASC.equals(nameOrder);
        }

        List<T> entities = fetcher.apply(client);
        if (!StringUtils.isEmpty(content)) {
            // Select by content
            entities = entities.stream()
                               .filter(c -> c.getName().contains(content))
                               .collect(Collectors.toList());
            if (nameOrderAsc != null) {
                // order by name
                this.sortByName(entities, nameOrderAsc);
            } else {
                // order by relativity
                this.sortByRelativity(entities, content);
            }
        } else {
            // Select all
            if (nameOrderAsc != null) {
                // order by name
                this.sortByName(entities, nameOrderAsc);
            } else {
                // order by time
                this.sortByCreateTime(entities, false);
            }
        }
        return PageUtil.page(entities, pageNo, pageSize);
    }

    public <T extends SchemaEntity> void sortByName(List<T> entities,
                                                    boolean asc) {
        if (asc) {
            entities.sort(Comparator.comparing(SchemaEntity::getName));
        } else {
            entities.sort(Comparator.comparing(SchemaEntity::getName)
                                    .reversed());
        }
    }

    public <T extends SchemaEntity> void sortByCreateTime(List<T> entities,
                                                          boolean asc) {
        Comparator<T> dateAscComparator = (o1, o2) -> {
            assert o1 instanceof Timefiable;
            assert o2 instanceof Timefiable;
            Date t1 = ((Timefiable) o1).getCreateTime();
            Date t2 = ((Timefiable) o2).getCreateTime();
            if (t1 == null && t2 == null) {
                return 0;
            } else if (t1 == null) {
                // t2 != null
                return -1;
            } else if (t2 == null) {
                // t1 != null
                return 1;
            } else {
                return t1.compareTo(t2);
            }
        };

        if (asc) {
            entities.sort(dateAscComparator);
        } else {
            entities.sort(dateAscComparator.reversed());
        }
    }

    public <T extends SchemaEntity> void sortByRelativity(List<T> entities,
                                                          String content) {
        Comparator<T> occurrencesComparator = (o1, o2) -> {
            String name1 = o1.getName();
            String name2 = o2.getName();
            int count1 = StringUtils.countOccurrencesOf(name1, content);
            int count2 = StringUtils.countOccurrencesOf(name2, content);
            return count2 - count1;
        };
        entities.sort(occurrencesComparator);
    }

    /**
     * Check properties are defined
     */
    public static void checkProperties(PropertyKeyService service,
                                       Set<Property> properties,
                                       boolean mustNullable,
                                       HugeClient client) {
        if (properties == null) {
            return;
        }
        for (Property property : properties) {
            String pkName = property.getName();
            service.checkExist(pkName, client);
            Ex.check(mustNullable, property::isNullable,
                     "schema.propertykey.must-be-nullable", pkName);
        }
    }

    public static void checkPropertyIndexes(SchemaLabelEntity entity,
                                            HugeClient client) {
        List<PropertyIndex> propertyIndexes = entity.getPropertyIndexes();
        if (propertyIndexes != null) {
            for (PropertyIndex propertyIndex : propertyIndexes) {
                Ex.check(propertyIndex.getOwner() == null,
                         "common.param.must-be-null", "property_index.owner");
                Ex.check(propertyIndex.getName() != null,
                         "common.param.cannot-be-null", "property_index.name");
                Ex.check(propertyIndex.getSchemaType() != null,
                         "common.param.cannot-be-null", "property_index.type");
                Ex.check(propertyIndex.getFields() != null,
                         "common.param.cannot-be-null", "property_index.fields");
            }
        }
    }

    public static void checkParamsValid(PropertyKeyService service,
                                        LabelUpdateEntity entity,
                                        HugeClient client) {
        // All append property should be nullable
        checkProperties(service, entity.getAppendProperties(), true, client);
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchemaGroovy {
        @JsonProperty("schema-groovy")
        private String schemaGroovy;

        @Override
        public String toString() {
            return this.schemaGroovy;
        }
    }
}
