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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.schema.ConflictCheckEntity;
import org.apache.hugegraph.entity.schema.ConflictDetail;
import org.apache.hugegraph.entity.schema.ConflictStatus;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.EdgeLabelStyle;
import org.apache.hugegraph.entity.schema.EdgeLabelUpdateEntity;
import org.apache.hugegraph.entity.schema.Property;
import org.apache.hugegraph.entity.schema.PropertyIndex;
import org.apache.hugegraph.entity.schema.SchemaConflict;
import org.apache.hugegraph.entity.schema.SchemaEntity;
import org.apache.hugegraph.entity.schema.SchemaType;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.exception.ServerException;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.constant.EdgeLabelType;
import org.apache.hugegraph.structure.constant.Frequency;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.JsonUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class EdgeLabelService extends SchemaService {

    @Autowired
    private PropertyKeyService pkService;
    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private PropertyIndexService piService;

    public List<EdgeLabelEntity> list(HugeClient client) {
        return this.list(Collections.emptyList(), client);
    }

    public List<EdgeLabelEntity> list(Collection<String> names,
                                      HugeClient client) {
        return this.list(names, client, true, null);
    }

    public List<EdgeLabelEntity> list(Collection<String> names, HugeClient client,
                                      boolean emptyAsAll, String type) {
        List<EdgeLabel> edgeLabels;
        if (CollectionUtils.isEmpty(names)) {
            if (emptyAsAll) {
                edgeLabels = client.schema().getEdgeLabels();
            } else {
                edgeLabels = new ArrayList<>();
            }
        } else {
            edgeLabels = client.schema().getEdgeLabels(new ArrayList<>(names));
        }
        List<EdgeLabel> edgeLabelsQuery = client.schema().getEdgeLabels();
        List<IndexLabel> indexLabels = client.schema().getIndexLabels();

        List<EdgeLabelEntity> results = new ArrayList<>(edgeLabels.size());
        edgeLabels.forEach(edgeLabel -> {
            if (StringUtils.isEmpty(type) ||
                (StringUtils.isNotEmpty(type) &&
                edgeLabel.edgeLabelType() == EdgeLabelType.valueOf(type))) {
                EdgeLabelEntity entity =
                        convert(edgeLabel, indexLabels, client);

                if (edgeLabel.parent()) {
                    List<String> children = new ArrayList<>();
                    for (EdgeLabel edgeLabel1: edgeLabelsQuery) {
                        if (edgeLabel.name().equals(edgeLabel1.parentLabel())) {
                            children.add(edgeLabel1.name());
                        }
                    }
                    entity.setChildren(children);
                }

                results.add(entity);
            }
        });
        return results;
    }

    public EdgeLabelEntity get(String name, HugeClient client) {
        try {
            EdgeLabel edgeLabel = client.schema().getEdgeLabel(name);
            List<IndexLabel> indexLabels = client.schema().getIndexLabels();

            List<EdgeLabel> edgeLabelsQuery = client.schema().getEdgeLabels();
            EdgeLabelEntity entity = convert(edgeLabel, indexLabels, client);

            if (edgeLabel.parent()) {
                List<String> children = new ArrayList<>();
                for (EdgeLabel edgeLabel1: edgeLabelsQuery) {
                    if (edgeLabel.name().equals(edgeLabel1.parentLabel())) {
                        children.add(edgeLabel1.name());
                    }
                }
                entity.setChildren(children);
            }
            return entity;
        } catch (ServerException e) {
            if (e.status() == Constant.STATUS_NOT_FOUND) {
                throw new ExternalException("schema.edgelabel.not-exist",
                                            e, name);
            }
            throw new ExternalException("schema.edgelabel.get.failed", e, name);
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
        throw new ExternalException("schema.edgelabel.exist", name);
    }

    public void add(EdgeLabelEntity entity, HugeClient client) {
        EdgeLabel edgeLabel = convert(entity, client);
        try {
            client.schema().addEdgeLabel(edgeLabel);
        } catch (Exception e) {
            throw new ExternalException("schema.edgelabel.create.failed", e,
                                        entity.getName());
        }
        List<IndexLabel> indexLabels = collectIndexLabels(entity, client);
        this.piService.addBatch(indexLabels, client);
    }

    public void update(EdgeLabelUpdateEntity entity, HugeClient client) {
        EdgeLabel edgeLabel = convert(entity, client);

        // All existed indexlabels
        List<IndexLabel> existedIndexLabels = client.schema().getIndexLabels();
        List<String> existedIndexLabelNames = collectNames(existedIndexLabels);

        List<String> addedIndexLabelNames = entity.getAppendPropertyIndexNames();
        List<IndexLabel> addedIndexLabels = convertIndexLabels(
                                            entity.getAppendPropertyIndexes(),
                                            client, false, entity.getName());

        List<String> removedIndexLabelNames = entity.getRemovePropertyIndexes();

        if (addedIndexLabelNames != null) {
            for (String name : addedIndexLabelNames) {
                if (existedIndexLabelNames.contains(name)) {
                    throw new ExternalException(
                              "schema.edgelabel.update.append-index-existed",
                              entity.getName(), name);
                }
            }
        }
        if (removedIndexLabelNames != null) {
            for (String name : removedIndexLabelNames) {
                if (!existedIndexLabelNames.contains(name)) {
                    throw new ExternalException(
                              "schema.edgelabel.update.remove-index-unexisted",
                              entity.getName(), name);
                }
            }
        }

        try {
            // NOTE: property can append but doesn't support eliminate now
            client.schema().appendEdgeLabel(edgeLabel);
        } catch (Exception e) {
            throw new ExternalException("schema.edgelabel.update.failed", e,
                                        entity.getName());
        }
        this.piService.addBatch(addedIndexLabels, client);
        this.piService.removeBatch(removedIndexLabelNames, client);
    }

    public void remove(String name, HugeClient client) {
        client.schema().removeEdgeLabelAsync(name);
    }

    public ConflictDetail checkConflict(ConflictCheckEntity entity,
                                        HugeClient client,
                                        boolean compareEachOther) {
        ConflictDetail detail = new ConflictDetail(SchemaType.EDGE_LABEL);
        if (CollectionUtils.isEmpty(entity.getElEntities())) {
            return detail;
        }

        Map<String, EdgeLabelEntity> originElEntities = new HashMap<>();
        for (EdgeLabelEntity e : this.list(client)) {
            originElEntities.put(e.getName(), e);
        }

        this.pkService.checkConflict(entity.getPkEntities(), detail,
                                     client, compareEachOther);
        this.piService.checkConflict(entity.getPiEntities(), detail,
                                     client, compareEachOther);
        this.vlService.checkConflict(entity.getVlEntities(), detail,
                                     client, compareEachOther);
        for (EdgeLabelEntity elEntity : entity.getElEntities()) {
            // Firstly check if any properties are conflicted
            if (detail.anyPropertyKeyConflict(elEntity.getPropNames())) {
                detail.add(elEntity, ConflictStatus.DEP_CONFLICT);
                continue;
            }
            // Then check if any property indexes are conflicted
            if (detail.anyPropertyIndexConflict(elEntity.getIndexProps())) {
                detail.add(elEntity, ConflictStatus.DEP_CONFLICT);
                continue;
            }
            // Then determine if source/target vertex labels are conflicted
            if (detail.anyVertexLabelConflict(elEntity.getLinkLabels())) {
                detail.add(elEntity, ConflictStatus.DEP_CONFLICT);
                continue;
            }
            // Then check conflict of edge label itself
            EdgeLabelEntity originElEntity = originElEntities.get(
                                             elEntity.getName());
            ConflictStatus status = SchemaEntity.compare(elEntity,
                                                         originElEntity);
            detail.add(elEntity, status);
        }
        if (compareEachOther) {
            compareWithEachOther(detail, SchemaType.EDGE_LABEL);
        }
        return detail;
    }

    public void reuse(ConflictDetail detail, HugeClient client) {
        Ex.check(!detail.hasConflict(), "schema.cannot-reuse-conflict");

        List<PropertyKey> propertyKeys = this.pkService.filter(detail, client);
        if (!propertyKeys.isEmpty()) {
            try {
                this.pkService.addBatch(propertyKeys, client);
            } catch (Exception e) {
                throw new ExternalException("schema.propertykey.reuse.failed", e);
            }
        }

        List<VertexLabel> vertexLabels = this.vlService.filter(detail, client);
        if (!vertexLabels.isEmpty()) {
            try {
                this.vlService.addBatch(vertexLabels, client);
            } catch (Exception e) {
                this.pkService.removeBatch(propertyKeys, client);
                throw new ExternalException("schema.vertexlabel.reuse.failed", e);
            }
        }

        List<EdgeLabel> edgeLabels = this.filter(detail, client);
        if (!edgeLabels.isEmpty()) {
            try {
                this.addBatch(edgeLabels, client);
            } catch (Exception e) {
                this.vlService.removeBatch(vertexLabels, client);
                this.pkService.removeBatch(propertyKeys, client);
                throw new ExternalException("schema.edgelabel.reuse.failed", e);
            }
        }

        List<IndexLabel> indexLabels = this.piService.filter(detail, client);
        if (!indexLabels.isEmpty()) {
            try {
                this.piService.addBatch(indexLabels, client);
            } catch (Exception e) {
                this.removeBatch(edgeLabels, client);
                this.vlService.removeBatch(vertexLabels, client);
                this.pkService.removeBatch(propertyKeys, client);
                throw new ExternalException("schema.propertyindex.reuse.failed",
                                            e);
            }
        }
    }

    public List<EdgeLabel> filter(ConflictDetail detail, HugeClient client) {
        return detail.getElConflicts().stream()
                     .filter(c -> c.getStatus() == ConflictStatus.PASSED)
                     .map(SchemaConflict::getEntity)
                     .map(e -> convert(e, client))
                     .collect(Collectors.toList());
    }

    public void addBatch(List<EdgeLabel> edgeLabels, HugeClient client) {
        BiConsumer<HugeClient, EdgeLabel> consumer = (hugeClient, el) -> {
            hugeClient.schema().addEdgeLabel(el);
        };
        addBatch(edgeLabels, client, consumer, SchemaType.EDGE_LABEL);
    }

    public void removeBatch(List<EdgeLabel> edgeLabels, HugeClient client) {
        List<String> names = collectNames(edgeLabels);
        BiFunction<HugeClient, String, Long> func = (hugeClient, name) -> {
            return hugeClient.schema().removeEdgeLabelAsync(name);
        };
        removeBatch(names, client, func, SchemaType.EDGE_LABEL);
    }

    private EdgeLabelEntity convert(EdgeLabel edgeLabel,
                                    List<IndexLabel> indexLabels,
                                    HugeClient client) {
        if (edgeLabel == null) {
            return null;
        }
        Set<Property> properties = collectProperties(edgeLabel, client);
        List<PropertyIndex> propertyIndexes = collectPropertyIndexes(edgeLabel,
                                                                     indexLabels);
        boolean linkMultiTimes = edgeLabel.frequency() == Frequency.MULTIPLE;
        return EdgeLabelEntity.builder()
                              .name(edgeLabel.name())
                              .parentLabel(edgeLabel.parentLabel())
                              .edgeLabelType(edgeLabel.edgeLabelType().string())
                              .sourceLabel(edgeLabel.sourceLabel())
                              .targetLabel(edgeLabel.targetLabel())
                              .linkMultiTimes(linkMultiTimes)
                              .properties(properties)
                              .sortKeys(edgeLabel.sortKeys())
                              .propertyIndexes(propertyIndexes)
                              .openLabelIndex(edgeLabel.enableLabelIndex())
                              .style(getStyle(edgeLabel))
                              .createTime(getCreateTime(edgeLabel))
                              .build();
    }

    private static EdgeLabel convert(EdgeLabelEntity entity,
                                     HugeClient client) {
        if (entity == null) {
            return null;
        }
        Frequency frequency = entity.isLinkMultiTimes() ? Frequency.MULTIPLE :
                                                          Frequency.SINGLE;
        EdgeLabelStyle style = entity.getStyle();
        EdgeLabel.Builder builder =
                client.schema().edgeLabel(entity.getName());
        if (EdgeLabelType.valueOf(entity.getEdgeLabelType()).parent()) {
            builder.asBase();
        } else {
            builder.sourceLabel(entity.getSourceLabel())
                   .targetLabel(entity.getTargetLabel())
                   .frequency(frequency)
                   .properties(toStringArray(entity.getPropNames()))
                   .sortKeys(toStringArray(entity.getSortKeys()))
                   .nullableKeys(toStringArray(entity.getNullableProps()))
                   .enableLabelIndex(entity.getOpenLabelIndex())
                   .userdata(USER_KEY_CREATE_TIME, entity.getCreateTime())
                   .userdata(USER_KEY_STYLE, JsonUtil.toJson(style));
        }

        if (EdgeLabelType.valueOf(entity.getEdgeLabelType()).sub()) {
            builder.withBase(entity.getParentLabel());
        }

        return builder.build();
    }

    private static EdgeLabel convert(EdgeLabelUpdateEntity entity,
                                     HugeClient client) {
        if (entity == null) {
            return null;
        }
        Set<String> properties = new HashSet<>();
        if (entity.getAppendProperties() != null) {
            entity.getAppendProperties().forEach(p -> {
                properties.add(p.getName());
            });
        }

        EdgeLabel.Builder builder;
        builder = client.schema().edgeLabel(entity.getName())
                        .properties(toStringArray(properties))
                        .nullableKeys(toStringArray(properties));
        EdgeLabel edgeLabel = builder.build();
        Map<String, Object> userdata = edgeLabel.userdata();

        EdgeLabelStyle style = entity.getStyle();
        if (style != null) {
            userdata.put(USER_KEY_STYLE, JsonUtil.toJson(style));
        }
        return edgeLabel;
    }

    private static EdgeLabelStyle getStyle(SchemaElement element) {
        String styleValue = (String) element.userdata().get(USER_KEY_STYLE);
        if (styleValue != null) {
            return JsonUtil.fromJson(styleValue, EdgeLabelStyle.class);
        } else {
            return new EdgeLabelStyle();
        }
    }
}
