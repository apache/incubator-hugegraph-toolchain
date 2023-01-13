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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class VertexLabelStyle extends SchemaStyle {

    @JsonProperty("icon")
    private String icon;

    @JsonProperty("color")
    private String color;

    @JsonProperty("size")
    private Size size;

    @JsonProperty("display_fields")
    private List<String> displayFields;

    @JsonProperty("join_symbols")
    private List<String> joinSymbols;

    public VertexLabelStyle() {
        this(null, null, null, null, null);
    }

    @JsonCreator
    public VertexLabelStyle(@JsonProperty("icon") String icon,
                            @JsonProperty("color") String color,
                            @JsonProperty("size") Size size,
                            @JsonProperty("display_fields")
                            List<String> displayFields,
                            @JsonProperty("join_symbols")
                            List<String> joinSymbols) {
        this.icon = icon != null ? icon : "";
        this.color = !StringUtils.isEmpty(color) ? color : "#5C73E6";
        this.size = size != null ? size : Size.NORMAL;
        this.displayFields = !CollectionUtils.isEmpty(displayFields) ?
                             displayFields : ImmutableList.of("~id");
        this.joinSymbols = !CollectionUtils.isEmpty(joinSymbols) ?
                           joinSymbols : ImmutableList.of("-");
    }

    public enum Size {

        HUGE,

        BIG,

        NORMAL,

        SMALL,

        TINY
    }
}
