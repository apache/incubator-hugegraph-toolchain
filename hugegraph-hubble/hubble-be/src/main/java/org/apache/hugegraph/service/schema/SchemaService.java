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

package org.apache.hugegraph.service.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.apache.hugegraph.config.HugeConfig;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.entity.schema.ConflictDetail;
import org.apache.hugegraph.entity.schema.ConflictStatus;
import org.apache.hugegraph.entity.schema.Property;
import org.apache.hugegraph.entity.schema.PropertyIndex;
import org.apache.hugegraph.entity.schema.SchemaConflict;
import org.apache.hugegraph.entity.schema.SchemaEntity;
import org.apache.hugegraph.entity.schema.SchemaLabelEntity;
import org.apache.hugegraph.entity.schema.SchemaType;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.SchemaLabel;
import org.apache.hugegraph.util.HubbleUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class SchemaService {

    public static final String USER_KEY_CREATE_TIME = "~create_time";
    public static final String USER_KEY_STYLE = "~style";

    @Autowired
    private HugeConfig config;
    @Autowired
    private HugeClientPoolService poolService;

    public HugeConfig config() {
        return this.config;
    }

    public HugeClient client(int connId) {
        return this.poolService.getOrCreate(connId);
    }

    public static <T extends SchemaElement> List<String> collectNames(
                                                         List<T> schemas) {
        return schemas.stream().map(SchemaElement::name)
                      .collect(Collectors.toList());
    }

    public static Set<Property> collectProperties(SchemaLabel schemaLabel) {
        Set<Property> properties = new HashSet<>();
        Set<String> nullableKeys = schemaLabel.nullableKeys();
        for (String property : schemaLabel.properties()) {
            boolean nullable = nullableKeys.contains(property);
            properties.add(new Property(property, nullable));
        }
        return properties;
    }

    public static List<PropertyIndex> collectPropertyIndexes(
                                      SchemaLabel schemaLabel,
                                      List<IndexLabel> indexLabels) {
        List<PropertyIndex> propertyIndexes = new ArrayList<>();
        if (indexLabels == null) {
            return propertyIndexes;
        }
        for (IndexLabel indexLabel : indexLabels) {
            if (indexLabel.baseType().string().equals(schemaLabel.type()) &&
                indexLabel.baseValue().equals(schemaLabel.name())) {
                SchemaType schemaType = SchemaType.convert(indexLabel.baseType());
                PropertyIndex propertyIndex;
                propertyIndex = new PropertyIndex(indexLabel.baseValue(),
                                                  schemaType,
                                                  indexLabel.name(),
                                                  indexLabel.indexType(),
                                                  indexLabel.indexFields());
                propertyIndexes.add(propertyIndex);
            }
        }
        return propertyIndexes;
    }

    public static List<IndexLabel> collectIndexLabels(List<String> names,
                                                      HugeClient client) {
        if (CollectionUtils.isEmpty(names)) {
            return Collections.emptyList();
        } else {
            return client.schema().getIndexLabels(names);
        }
    }

    public static List<IndexLabel> collectIndexLabels(SchemaLabelEntity entity,
                                                      HugeClient client) {
        List<PropertyIndex> propertyIndexes = entity.getPropertyIndexes();
        if (CollectionUtils.isEmpty(propertyIndexes)) {
            return Collections.emptyList();
        }

        boolean isVertex = entity.getSchemaType().isVertexLabel();
        String baseValue = entity.getName();
        SchemaManager schema = client.schema();
        List<IndexLabel> indexLabels = new ArrayList<>(propertyIndexes.size());
        for (PropertyIndex index : propertyIndexes) {
            String[] fields = toStringArray(index.getFields());
            IndexLabel indexLabel = schema.indexLabel(index.getName())
                                          .on(isVertex, baseValue)
                                          .indexType(index.getType())
                                          .by(fields)
                                          .build();
            indexLabels.add(indexLabel);
        }
        return indexLabels;
    }

    public static List<IndexLabel> convertIndexLabels(List<PropertyIndex> entities,
                                                      HugeClient client,
                                                      boolean isVertex,
                                                      String baseValue) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        List<IndexLabel> indexLabels = new ArrayList<>(entities.size());
        SchemaManager schema = client.schema();
        for (PropertyIndex index : entities) {
            String[] fields = toStringArray(index.getFields());
            IndexLabel indexLabel = schema.indexLabel(index.getName())
                                          .on(isVertex, baseValue)
                                          .indexType(index.getType())
                                          .by(fields)
                                          .build();
            indexLabels.add(indexLabel);
        }
        return indexLabels;
    }

    public static <T extends SchemaEntity>
           void compareWithEachOther(ConflictDetail detail, SchemaType type) {
        List<SchemaConflict<T>> conflicts = detail.getConflicts(type);
        for (int i = 0; i < conflicts.size(); i++) {
            SchemaConflict<T> conflict = conflicts.get(i);
            if (conflict.getStatus().isConflicted()) {
                continue;
            }
            ConflictStatus status = compareWithOthers(i, conflicts);
            conflict.setStatus(status);
        }
    }

    public static <T extends SchemaEntity>
           ConflictStatus compareWithOthers(int currentIdx,
                                            List<SchemaConflict<T>> conflicts) {
        SchemaConflict<T> current = conflicts.get(currentIdx);
        T currentEntity = current.getEntity();
        // May changed
        ConflictStatus status = current.getStatus();
        for (int i = 0; i < conflicts.size(); i++) {
            if (currentIdx == i) {
                continue;
            }
            SchemaConflict<T> other = conflicts.get(i);
            T otherEntity = other.getEntity();
            if (!currentEntity.getName().equals(otherEntity.getName())) {
                continue;
            }

            if (currentEntity.equals(otherEntity)) {
                status = ConflictStatus.EXISTED;
            } else {
                status = ConflictStatus.DUPNAME;
                break;
            }
        }
        return status;
    }

    public static <T extends SchemaElement> void addBatch(
           List<T> schemas, HugeClient client,
           BiConsumer<HugeClient, T> func, SchemaType type) {
        if (CollectionUtils.isEmpty(schemas)) {
            return;
        }
        Date now = HubbleUtil.nowDate();
        for (T schema : schemas) {
            schema.resetId();
            if (!(schema instanceof IndexLabel)) {
                schema.userdata().put(USER_KEY_CREATE_TIME, now);
            }
            func.accept(client, schema);
        }
    }

    public static <T extends SchemaElement> List<Long> addBatch(
           List<T> schemas, HugeClient client,
           BiFunction<HugeClient, T, Long> func, SchemaType type) {
        List<Long> tasks = new ArrayList<>();
        if (CollectionUtils.isEmpty(schemas)) {
            return tasks;
        }
        Date now = HubbleUtil.nowDate();
        for (T schema : schemas) {
            schema.resetId();
            if (!(schema instanceof IndexLabel)) {
                schema.userdata().put(USER_KEY_CREATE_TIME, now);
            }
            tasks.add(func.apply(client, schema));
        }
        return tasks;
    }

    public static List<Long> removeBatch(
           List<String> names, HugeClient client,
           BiFunction<HugeClient, String, Long> func, SchemaType type) {
        List<Long> tasks = new ArrayList<>();
        if (CollectionUtils.isEmpty(names)) {
            return tasks;
        }
        for (String name : names) {
            tasks.add(func.apply(client, name));
        }
        return tasks;
    }

    public static void removeBatch(List<String> names, HugeClient client,
                                   BiConsumer<HugeClient, String> func,
                                   SchemaType type) {
        if (CollectionUtils.isEmpty(names)) {
            return;
        }
        for (String name : names) {
            func.accept(client, name);
        }
    }

    public static Date getCreateTime(SchemaElement element) {
        Object createTimeValue = element.userdata().get(USER_KEY_CREATE_TIME);
        if (!(createTimeValue instanceof Long)) {
            return new Date(0);
        }
        return new Date((long) createTimeValue);
    }

    public static String[] toStringArray(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return new String[]{};
        }
        return collection.toArray(new String[]{});
    }
}
