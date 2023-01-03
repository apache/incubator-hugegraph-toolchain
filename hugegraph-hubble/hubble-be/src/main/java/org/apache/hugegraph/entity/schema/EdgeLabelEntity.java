/*
 * Copyright 2017 HugeGraph Authors
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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.util.HubbleUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeLabelEntity implements SchemaLabelEntity, Timefiable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("source_label")
    private String sourceLabel;

    @JsonProperty("target_label")
    private String targetLabel;

    @JsonProperty("link_multi_times")
    private boolean linkMultiTimes;

    @JsonProperty("properties")
    private Set<Property> properties;

    @JsonProperty("sort_keys")
    private List<String> sortKeys;

    @JsonProperty("property_indexes")
    private List<PropertyIndex> propertyIndexes;

    @JsonProperty("open_label_index")
    private boolean openLabelIndex;

    @JsonProperty("style")
    private EdgeLabelStyle style;

    @JsonProperty("create_time")
    private Date createTime;

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.EDGE_LABEL;
    }

    @JsonIgnore
    public Set<String> getLinkLabels() {
        return ImmutableSet.of(this.sourceLabel, this.targetLabel);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EdgeLabelEntity)) {
            return false;
        }
        EdgeLabelEntity other = (EdgeLabelEntity) object;
        return this.name.equals(other.name) &&
               this.sourceLabel.equals(other.sourceLabel) &&
               this.targetLabel.equals(other.targetLabel) &&
               this.linkMultiTimes == other.linkMultiTimes &&
               HubbleUtil.equalCollection(this.properties, other.properties) &&
               HubbleUtil.equalCollection(this.sortKeys, other.sortKeys) &&
               HubbleUtil.equalCollection(this.propertyIndexes,
                                          other.propertyIndexes) &&
               this.openLabelIndex == other.openLabelIndex;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
