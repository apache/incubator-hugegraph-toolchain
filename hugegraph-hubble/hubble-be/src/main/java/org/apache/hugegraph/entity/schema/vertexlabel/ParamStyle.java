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

package org.apache.hugegraph.entity.schema.vertexlabel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.entity.schema.SchemaStyle;
import org.apache.hugegraph.entity.schema.VertexLabelStyle;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class ParamStyle extends SchemaStyle {
    @JsonProperty("color")
    private String color;

    @JsonProperty("size")
    private VertexLabelStyle.Size size;

    public ParamStyle() {
        this(null, null);
    }

    @JsonCreator
    public ParamStyle(@JsonProperty("color") String color,
                            @JsonProperty("size") VertexLabelStyle.Size size) {
        this.color = !StringUtils.isEmpty(color) ? color : "#5C73E6";
        this.size = size != null ? size : VertexLabelStyle.Size.NORMAL;
    }

    public enum Size {

        HUGE,

        BIG,

        NORMAL,

        SMALL,

        TINY
    }
}
