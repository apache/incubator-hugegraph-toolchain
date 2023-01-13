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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.LabelUpdateEntity;
import org.apache.hugegraph.entity.schema.Property;
import org.apache.hugegraph.entity.schema.PropertyIndex;
import org.apache.hugegraph.entity.schema.PropertyKeyEntity;
import org.apache.hugegraph.entity.schema.SchemaEntity;
import org.apache.hugegraph.entity.schema.SchemaLabelEntity;
import org.apache.hugegraph.entity.schema.Timefiable;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.InternalException;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/schema")
public class SchemaController extends BaseController {

    @Autowired
    private PropertyKeyService pkService;
    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;

    @GetMapping("graphview")
    public SchemaView displayInSchemaView(@PathVariable("connId") int connId) {
        List<PropertyKeyEntity> propertyKeys = this.pkService.list(connId);
        List<VertexLabelEntity> vertexLabels = this.vlService.list(connId);
        List<EdgeLabelEntity> edgeLabels = this.elService.list(connId);

        List<Map<String, Object>> vertices = new ArrayList<>(vertexLabels.size());
        for (VertexLabelEntity entity : vertexLabels) {
            Map<String, Object> vertex = new LinkedHashMap<>();
            vertex.put("id", entity.getName());
            vertex.put("label", entity.getName());
            if (entity.getIdStrategy() == IdStrategy.PRIMARY_KEY) {
                vertex.put("primary_keys", entity.getPrimaryKeys());
            } else {
                vertex.put("primary_keys", new ArrayList<>());
            }
            Map<String, String> properties = new LinkedHashMap<>();
            this.fillProperties(properties, entity, propertyKeys);
            vertex.put("properties", properties);
            vertex.put("~style", entity.getStyle());
            vertices.add(vertex);
        }

        List<Map<String, Object>> edges = new ArrayList<>(edgeLabels.size());
        for (EdgeLabelEntity entity : edgeLabels) {
            Map<String, Object> edge = new LinkedHashMap<>();
            String edgeId = String.format(
                            "%s-%s->%s", entity.getSourceLabel(),
                            entity.getName(), entity.getTargetLabel());
            edge.put("id", edgeId);
            edge.put("label", entity.getName());
            edge.put("source", entity.getSourceLabel());
            edge.put("target", entity.getTargetLabel());
            if (entity.isLinkMultiTimes()) {
                edge.put("sort_keys", entity.getSortKeys());
            } else {
                edge.put("sort_keys", new ArrayList<>());
            }
            Map<String, String> properties = new LinkedHashMap<>();
            this.fillProperties(properties, entity, propertyKeys);
            edge.put("properties", properties);
            edge.put("~style", entity.getStyle());
            edges.add(edge);
        }
        return new SchemaView(vertices, edges);
    }

    private void fillProperties(Map<String, String> properties,
                                SchemaLabelEntity entity,
                                List<PropertyKeyEntity> propertyKeys) {
        for (Property property : entity.getProperties()) {
            String name = property.getName();
            PropertyKeyEntity pkEntity = findPropertyKey(propertyKeys, name);
            properties.put(name, pkEntity.getDataType().string());
        }
    }

    private PropertyKeyEntity findPropertyKey(List<PropertyKeyEntity> entities,
                                              String name) {
        for (PropertyKeyEntity entity : entities) {
            if (entity.getName().equals(name)) {
                return entity;
            }
        }
        throw new InternalException("schema.propertykey.not-exist", name);
    }

    @AllArgsConstructor
    private static class SchemaView {

        @JsonProperty("vertices")
        private List<Map<String, Object>> vertices;

        @JsonProperty("edges")
        private List<Map<String, Object>> edges;
    }

    public <T extends SchemaEntity> IPage<T> listInPage(
                                             Function<Integer, List<T>> fetcher,
                                             int connId, String content,
                                             String nameOrder,
                                             int pageNo, int pageSize) {
        Boolean nameOrderAsc = null;
        if (!StringUtils.isEmpty(nameOrder)) {
            Ex.check(ORDER_ASC.equals(nameOrder) || ORDER_DESC.equals(nameOrder),
                     "common.name-order.invalid", nameOrder);
            nameOrderAsc = ORDER_ASC.equals(nameOrder);
        }

        List<T> entities = fetcher.apply(connId);
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
                                       boolean mustNullable, int connId) {
        if (properties == null) {
            return;
        }
        for (Property property : properties) {
            String pkName = property.getName();
            service.checkExist(pkName, connId);
            Ex.check(mustNullable, property::isNullable,
                     "schema.propertykey.must-be-nullable", pkName);
        }
    }

    public static void checkPropertyIndexes(SchemaLabelEntity entity,
                                            int connId) {
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
                                        LabelUpdateEntity entity, int connId) {
        // All append property should be nullable
        checkProperties(service, entity.getAppendProperties(), true, connId);
    }
}
