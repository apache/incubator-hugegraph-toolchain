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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.schema.ConflictDetail;
import org.apache.hugegraph.entity.schema.ConflictStatus;
import org.apache.hugegraph.entity.schema.PropertyIndex;
import org.apache.hugegraph.entity.schema.SchemaConflict;
import org.apache.hugegraph.entity.schema.SchemaEntity;
import org.apache.hugegraph.entity.schema.SchemaType;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.util.PageUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class PropertyIndexService extends SchemaService {

    public List<PropertyIndex> list(int connId) {
        return this.list(Collections.emptyList(), connId);
    }

    public List<PropertyIndex> list(Collection<String> names, int connId) {
        return this.list(names, connId, true);
    }

    public List<PropertyIndex> list(Collection<String> names, int connId,
                                    boolean emptyAsAll) {
        HugeClient client = this.client(connId);
        List<IndexLabel> indexLabels;
        if (CollectionUtils.isEmpty(names)) {
            if (emptyAsAll) {
                indexLabels = client.schema().getIndexLabels();
            } else {
                indexLabels = new ArrayList<>();
            }
        } else {
            indexLabels = client.schema().getIndexLabels(new ArrayList<>(names));
        }
        List<PropertyIndex> results = new ArrayList<>(indexLabels.size());
        indexLabels.forEach(indexLabel -> {
            results.add(convert(indexLabel));
        });
        return results;
    }

    public IPage<PropertyIndex> list(int connId, HugeType type,
                                     int pageNo, int pageSize) {
        HugeClient client = this.client(connId);
        List<IndexLabel> indexLabels = client.schema().getIndexLabels();

        List<PropertyIndex> results = new ArrayList<>();
        for (IndexLabel indexLabel : indexLabels) {
            if (!indexLabel.baseType().equals(type)) {
                continue;
            }
            // Collect all indexlabels
            results.add(convert(indexLabel));
        }
        // Sort by owner name a-z, then index name a-z
        results.sort((o1, o2) -> {
            String owner1 = o1.getOwner();
            String owner2 = o2.getOwner();
            if (!owner1.equals(owner2)) {
                return owner1.compareTo(owner2);
            }
            return o1.getName().compareTo(o2.getName());
        });
        return PageUtil.page(results, pageNo, pageSize);
    }

    /**
     * The sort result like that, content is 'name'
     * --------------+------------------------+---------------------------------
     * base_value    | index label name       | fields
     * --------------+------------------------+---------------------------------
     * xxxname       | xxxByName              | name
     * --------------+------------------------+---------------------------------
     *               | personByName           | name
     * person        +------------------------+---------------------------------
     *               | personByAgeAndName     | age name
     * --------------+------------------------+---------------------------------
     *               | softwareByName         | name
     * software      +------------------------+---------------------------------
     *               | softwareByPriveAndName | price name
     * --------------+------------------------+---------------------------------
     */
    public IPage<PropertyIndex> list(int connId, HugeType type, String content,
                                     int pageNo, int pageSize) {
        HugeClient client = this.client(connId);
        List<IndexLabel> indexLabels = client.schema().getIndexLabels();

        Map<String, List<PropertyIndex>> matchedResults = new HashMap<>();
        Map<String, List<PropertyIndex>> unMatchResults = new HashMap<>();
        for (IndexLabel indexLabel : indexLabels) {
            if (!indexLabel.baseType().equals(type)) {
                continue;
            }
            String baseValue = indexLabel.baseValue();
            List<PropertyIndex> groupedIndexes;
            // Collect indexlabels that contains content
            boolean match = baseValue.contains(content);
            if (match) {
                groupedIndexes = matchedResults.computeIfAbsent(baseValue,
                                                k -> new ArrayList<>());
            } else {
                groupedIndexes = unMatchResults.computeIfAbsent(baseValue,
                                                k -> new ArrayList<>());
            }
            match = match || indexLabel.name().contains(content) ||
                    indexLabel.indexFields().stream()
                              .anyMatch(f -> f.contains(content));
            if (match) {
                groupedIndexes.add(convert(indexLabel));
            }
        }

        // Sort matched results by relevance
        if (!StringUtils.isEmpty(content)) {
            for (Map.Entry<String, List<PropertyIndex>> entry :
                 matchedResults.entrySet()) {
                List<PropertyIndex> groupedIndexes = entry.getValue();
                groupedIndexes.sort(new Comparator<PropertyIndex>() {
                    final int highScore = 2;
                    final int lowScore = 1;
                    @Override
                    public int compare(PropertyIndex o1, PropertyIndex o2) {
                        int o1Score = 0;
                        if (o1.getName().contains(content)) {
                            o1Score += highScore;
                        }
                        if (o1.getFields().stream()
                              .anyMatch(field -> field.contains(content))) {
                            o1Score += lowScore;
                        }

                        int o2Score = 0;
                        if (o2.getName().contains(content)) {
                            o2Score += highScore;
                        }
                        if (o2.getFields().stream()
                              .anyMatch(field -> field.contains(content))) {
                            o2Score += lowScore;
                        }
                        return o2Score - o1Score;
                    }
                });
            }
        }
        List<PropertyIndex> all = new ArrayList<>();
        matchedResults.values().forEach(all::addAll);
        unMatchResults.values().forEach(all::addAll);
        return PageUtil.page(all, pageNo, pageSize);
    }

    private PropertyIndex get(String name, int connId) {
        HugeClient client = this.client(connId);
        try {
            IndexLabel indexLabel = client.schema().getIndexLabel(name);
            return convert(indexLabel);
        } catch (ServerException e) {
            if (e.status() == Constant.STATUS_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    public List<Long> addBatch(List<IndexLabel> indexLabels, HugeClient client) {
        BiFunction<HugeClient, IndexLabel, Long> func = (hugeClient, il) -> {
            return hugeClient.schema().addIndexLabelAsync(il);
        };
        return addBatch(indexLabels, client, func,
                        SchemaType.PROPERTY_INDEX);
    }

    public List<Long> removeBatch(List<String> indexLabels, HugeClient client) {
        BiFunction<HugeClient, String, Long> func = (hugeClient, name) -> {
            return hugeClient.schema().removeIndexLabelAsync(name);
        };
        return removeBatch(indexLabels, client, func, SchemaType.PROPERTY_INDEX);
    }

    public void checkConflict(List<PropertyIndex> entities,
                              ConflictDetail detail, int connId,
                              boolean compareEachOther) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        Map<String, PropertyIndex> originEntities = new HashMap<>();
        for (PropertyIndex entity : this.list(connId)) {
            originEntities.put(entity.getName(), entity);
        }
        for (PropertyIndex entity : entities) {
            if (detail.anyPropertyKeyConflict(entity.getFields())) {
                detail.add(entity, ConflictStatus.DEP_CONFLICT);
                continue;
            }
            PropertyIndex originEntity = originEntities.get(entity.getName());
            ConflictStatus status = SchemaEntity.compare(entity, originEntity);
            detail.add(entity, status);
        }
        // Compare resued entities with each other
        if (compareEachOther) {
            compareWithEachOther(detail, SchemaType.PROPERTY_INDEX);
        }
    }

    public ConflictStatus checkConflict(PropertyIndex entity, int connId) {
        HugeClient client = this.client(connId);
        String name = entity.getName();
        IndexLabel newIndexLabel = convert(entity, client);
        IndexLabel oldIndexLabel = convert(this.get(name, connId), client);
        if (oldIndexLabel == null) {
            return ConflictStatus.PASSED;
        } else if (isEqual(newIndexLabel, oldIndexLabel)) {
            return ConflictStatus.EXISTED;
        } else {
            return ConflictStatus.DUPNAME;
        }
    }

    public List<IndexLabel> filter(ConflictDetail detail, HugeClient client) {
        return detail.getPiConflicts().stream()
                     .filter(c -> c.getStatus() == ConflictStatus.PASSED)
                     .map(SchemaConflict::getEntity)
                     .map(e -> convert(e, client))
                     .collect(Collectors.toList());
    }

    public static PropertyIndex convert(IndexLabel indexLabel) {
        return PropertyIndex.builder()
                            .owner(indexLabel.baseValue())
                            .ownerType(SchemaType.convert(indexLabel.baseType()))
                            .name(indexLabel.name())
                            .type(indexLabel.indexType())
                            .fields(indexLabel.indexFields())
                            .build();
    }

    public static IndexLabel convert(PropertyIndex entity, HugeClient client) {
        if (entity == null) {
            return null;
        }
        boolean isVertex = entity.getOwnerType().isVertexLabel();
        String[] fields = toStringArray(entity.getFields());
        return client.schema().indexLabel(entity.getName())
                     .on(isVertex, entity.getOwner())
                     .indexType(entity.getType())
                     .by(fields)
                     .build();
    }

    private static boolean isEqual(IndexLabel oldSchema, IndexLabel newSchema) {
        return oldSchema.name().equals(newSchema.name()) &&
               oldSchema.baseType().equals(newSchema.baseType()) &&
               oldSchema.baseValue().equals(newSchema.baseValue()) &&
               oldSchema.indexType().equals(newSchema.indexType()) &&
               oldSchema.indexFields().equals(newSchema.indexFields());
    }
}
