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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.schema.ConflictCheckEntity;
import org.apache.hugegraph.entity.schema.ConflictDetail;
import org.apache.hugegraph.entity.schema.ConflictStatus;
import org.apache.hugegraph.entity.schema.PropertyKeyEntity;
import org.apache.hugegraph.entity.schema.SchemaConflict;
import org.apache.hugegraph.entity.schema.SchemaEntity;
import org.apache.hugegraph.entity.schema.SchemaType;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.Ex;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class PropertyKeyService extends SchemaService {

    public List<PropertyKeyEntity> list(HugeClient client) {
        return this.list(Collections.emptyList(), client);
    }

    public List<PropertyKeyEntity> list(Collection<String> names,
                                        HugeClient client) {
        return this.list(names, client, true);
    }

    public List<PropertyKeyEntity> list(Collection<String> names,
                                        HugeClient client,
                                        boolean emptyAsAll) {
        List<PropertyKey> propertyKeys;
        if (CollectionUtils.isEmpty(names)) {
            if (emptyAsAll) {
                propertyKeys = client.schema().getPropertyKeys();
            } else {
                propertyKeys = new ArrayList<>();
            }
        } else {
            propertyKeys = client.schema().getPropertyKeys(new ArrayList<>(names));
        }
        List<PropertyKeyEntity> results = new ArrayList<>(propertyKeys.size());
        propertyKeys.forEach(propertyKey -> {
            results.add(convert(propertyKey));
        });
        return results;
    }

    public PropertyKeyEntity get(String name, HugeClient client) {
        try {
            PropertyKey propertyKey = client.schema().getPropertyKey(name);
            return convert(propertyKey);
        } catch (ServerException e) {
            if (e.status() == Constant.STATUS_NOT_FOUND) {
                throw new ExternalException("schema.propertykey.not-exist",
                                            e, name);
            }
            throw new ExternalException("schema.propertykey.get.failed",
                                        e, name);
        }
    }

    public List<PropertyKey> get(List<String> pks, HugeClient client) {
        try {
            return client.schema().getPropertyKeys(pks);
        } catch (ServerException e) {
            if (e.status() == Constant.STATUS_NOT_FOUND) {
                throw new ExternalException("schema.propertykey.not-exist",
                                            e, pks);
            }
            throw new ExternalException("schema.propertykey.get.failed",
                                        e, pks);
        }
    }

    public void checkExist(String name, HugeClient client) {
        // Throw exception if it doesn't exist
        this.get(name, client);
    }

    public void checkNotExist(String name, HugeClient client) {
        try {
            this.get(name, client);
        } catch (ExternalException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServerException &&
                ((ServerException) cause).status() == Constant.STATUS_NOT_FOUND) {
                return;
            }
            throw e;
        }
        throw new ExternalException("schema.propertykey.exist", name);
    }

    public void add(PropertyKeyEntity entity, HugeClient client) {
        PropertyKey propertyKey = convert(entity, client);
        client.schema().addPropertyKey(propertyKey);
    }

    public void remove(String name, HugeClient client) {
        client.schema().removePropertyKey(name);
    }

    /**
     * Check the property key is being used, used means that there is
     * any vertex label or edge label contains the property(name)
     */
    public boolean checkUsing(String name, HugeClient client) {
        List<VertexLabel> vertexLabels = client.schema().getVertexLabels();
        for (VertexLabel vertexLabel : vertexLabels) {
            if (vertexLabel.properties().contains(name)) {
                return true;
            }
        }
        List<EdgeLabel> edgeLabels = client.schema().getEdgeLabels();
        for (EdgeLabel edgeLabel : edgeLabels) {
            if (edgeLabel.properties().contains(name)) {
                return true;
            }
        }
        return false;
    }

    public ConflictDetail checkConflict(ConflictCheckEntity entity,
                                        HugeClient client,
                                        boolean compareEachOther) {
        ConflictDetail detail = new ConflictDetail(SchemaType.PROPERTY_KEY);
        if (CollectionUtils.isEmpty(entity.getPkEntities())) {
            return detail;
        }

        this.checkConflict(entity.getPkEntities(), detail,
                           client, compareEachOther);
        return detail;
    }

    public void checkConflict(List<PropertyKeyEntity> entities,
                              ConflictDetail detail, HugeClient client,
                              boolean compareEachOther) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        Map<String, PropertyKeyEntity> originEntities = new HashMap<>();
        for (PropertyKeyEntity entity : this.list(client)) {
            originEntities.put(entity.getName(), entity);
        }
        // Compare reused entity with origin in target graph
        for (PropertyKeyEntity entity : entities) {
            PropertyKeyEntity originEntity = originEntities.get(entity.getName());
            ConflictStatus status = SchemaEntity.compare(entity, originEntity);
            detail.add(entity, status);
        }
        // Compare resued entities with each other
        if (compareEachOther) {
            compareWithEachOther(detail, SchemaType.PROPERTY_KEY);
        }
    }

    public void reuse(ConflictDetail detail, HugeClient client) {
        // Assume that the conflict detail is valid
        Ex.check(!detail.hasConflict(), "schema.cannot-reuse-conflict");

        List<PropertyKey> propertyKeys = this.filter(detail, client);
        if (propertyKeys.isEmpty()) {
            return;
        }
        try {
            this.addBatch(propertyKeys, client);
        } catch (Exception e) {
            throw new ExternalException("schema.propertykey.reuse.failed", e);
        }
    }

    public List<PropertyKey> filter(ConflictDetail detail, HugeClient client) {
        return detail.getPkConflicts().stream()
                     .filter(c -> c.getStatus() == ConflictStatus.PASSED)
                     .map(SchemaConflict::getEntity)
                     .map(e -> convert(e, client))
                     .collect(Collectors.toList());
    }

    public void addBatch(List<PropertyKey> propertyKeys, HugeClient client) {
        BiConsumer<HugeClient, PropertyKey> consumer = (hugeClient, pk) -> {
            hugeClient.schema().addPropertyKey(pk);
        };
        addBatch(propertyKeys, client, consumer, SchemaType.PROPERTY_KEY);
    }

    public void removeBatch(List<PropertyKey> propertyKeys, HugeClient client) {
        List<String> names = collectNames(propertyKeys);
        BiConsumer<HugeClient, String> consumer = (hugeClient, name) -> {
            hugeClient.schema().removePropertyKey(name);
        };
        removeBatch(names, client, consumer, SchemaType.PROPERTY_KEY);
    }

    public static PropertyKeyEntity convert(PropertyKey propertyKey) {
        if (propertyKey == null) {
            return null;
        }
        return PropertyKeyEntity.builder()
                                .name(propertyKey.name())
                                .dataType(propertyKey.dataType())
                                .cardinality(propertyKey.cardinality())
                                .createTime(getCreateTime(propertyKey))
                                .build();
    }

    public static PropertyKey convert(PropertyKeyEntity entity,
                                      HugeClient client) {
        if (entity == null) {
            return null;
        }
        return client.schema()
                     .propertyKey(entity.getName())
                     .dataType(entity.getDataType())
                     .cardinality(entity.getCardinality())
                     .userdata(USER_KEY_CREATE_TIME, entity.getCreateTime())
                     .build();
    }
}
