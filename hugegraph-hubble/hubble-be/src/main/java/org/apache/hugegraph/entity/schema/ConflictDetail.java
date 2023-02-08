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

package org.apache.hugegraph.entity.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictDetail {

    @JsonProperty("type")
    private SchemaType type;

    @JsonProperty("propertykey_conflicts")
    private List<SchemaConflict<PropertyKeyEntity>> pkConflicts;

    @JsonProperty("propertyindex_conflicts")
    private List<SchemaConflict<PropertyIndex>> piConflicts;

    @JsonProperty("vertexlabel_conflicts")
    private List<SchemaConflict<VertexLabelEntity>> vlConflicts;

    @JsonProperty("edgelabel_conflicts")
    private List<SchemaConflict<EdgeLabelEntity>> elConflicts;

    @JsonCreator
    public ConflictDetail(SchemaType type) {
        this.type = type;
        this.pkConflicts = new ArrayList<>();
        this.piConflicts = new ArrayList<>();
        this.vlConflicts = new ArrayList<>();
        this.elConflicts = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public <T extends SchemaEntity> List<SchemaConflict<T>> getConflicts(
                                                            SchemaType type) {
        switch (type) {
            case PROPERTY_KEY:
                return (List<SchemaConflict<T>>) (Object) this.pkConflicts;
            case PROPERTY_INDEX:
                return (List<SchemaConflict<T>>) (Object) this.piConflicts;
            case VERTEX_LABEL:
                return (List<SchemaConflict<T>>) (Object) this.vlConflicts;
            case EDGE_LABEL:
                return (List<SchemaConflict<T>>) (Object) this.elConflicts;
            default:
                throw new AssertionError(String.format(
                          "Unknown schema type '%s'", type));
        }
    }

    public void add(PropertyKeyEntity entity, ConflictStatus status) {
        this.pkConflicts.add(new SchemaConflict<>(entity, status));
    }

    public void add(PropertyIndex entity, ConflictStatus status) {
        this.piConflicts.add(new SchemaConflict<>(entity, status));
    }

    public void add(VertexLabelEntity entity, ConflictStatus status) {
        this.vlConflicts.add(new SchemaConflict<>(entity, status));
    }

    public void add(EdgeLabelEntity entity, ConflictStatus status) {
        this.elConflicts.add(new SchemaConflict<>(entity, status));
    }

    public boolean anyPropertyKeyConflict(Collection<String> names) {
        return this.anyConflict(this.pkConflicts, names);
    }

    public boolean anyPropertyIndexConflict(Collection<String> names) {
        return this.anyConflict(this.piConflicts, names);
    }

    public boolean anyVertexLabelConflict(Collection<String> names) {
        return this.anyConflict(this.vlConflicts, names);
    }

    private <T extends SchemaEntity> boolean anyConflict(
                                             List<SchemaConflict<T>> conflicts,
                                             Collection<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return false;
        }
        return conflicts.stream().anyMatch(conflict -> {
            String name = conflict.getEntity().getName();
            return conflict.getStatus().isConflicted() && names.contains(name);
        });
    }

    public boolean hasConflict() {
        for (SchemaConflict<?> conflict : this.pkConflicts) {
            if (conflict.getStatus().isConflicted()) {
                return true;
            }
        }
        for (SchemaConflict<?> conflict : this.piConflicts) {
            if (conflict.getStatus().isConflicted()) {
                return true;
            }
        }
        for (SchemaConflict<?> conflict : this.vlConflicts) {
            if (conflict.getStatus().isConflicted()) {
                return true;
            }
        }
        for (SchemaConflict<?> conflict : this.elConflicts) {
            if (conflict.getStatus().isConflicted()) {
                return true;
            }
        }
        return false;
    }
}
