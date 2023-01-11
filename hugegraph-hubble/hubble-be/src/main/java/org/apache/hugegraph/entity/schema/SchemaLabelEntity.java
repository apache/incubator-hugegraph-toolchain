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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface SchemaLabelEntity extends SchemaEntity {

    Set<Property> getProperties();

    List<PropertyIndex> getPropertyIndexes();

    boolean isOpenLabelIndex();

    SchemaStyle getStyle();

    @JsonIgnore
    default Set<String> getPropNames() {
        if (this.getProperties() == null) {
            return Collections.emptySet();
        }
        return this.getProperties().stream()
                   .map(Property::getName)
                   .collect(Collectors.toSet());
    }

    @JsonIgnore
    default Set<String> getNullableProps() {
        if (this.getProperties() == null) {
            return Collections.emptySet();
        }
        return this.getProperties().stream()
                   .filter(Property::isNullable)
                   .map(Property::getName)
                   .collect(Collectors.toSet());
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    default Set<String> getNonNullableProps() {
        if (this.getProperties() == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(CollectionUtils.subtract(this.getPropNames(),
                                                      this.getNullableProps()));
    }

    @JsonIgnore
    default Set<String> getIndexProps() {
        if (this.getPropertyIndexes() == null) {
            return Collections.emptySet();
        }
        return this.getPropertyIndexes().stream()
                   .map(PropertyIndex::getName)
                   .collect(Collectors.toSet());
    }
}
