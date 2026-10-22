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

package org.apache.hugegraph.entity.query;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GremlinResult {

    @JsonProperty("type")
    private Type type;

    @JsonProperty("json_view")
    private JsonView jsonView;

    @JsonProperty("table_view")
    private TableView tableView;

    @JsonProperty("graph_view")
    private GraphView graphView;

    @JsonProperty("pathnum")
    private Integer pathnum;

    @JsonProperty("default_limit_applied")
    private boolean defaultLimitApplied;

    @JsonProperty("default_limit")
    private int defaultLimit;

    @JsonProperty("graph_truncated")
    private boolean graphTruncated;

    @JsonProperty("graph_vertex_limit")
    private int graphVertexLimit;

    @JsonProperty("graph_edge_limit")
    private int graphEdgeLimit;

    @JsonProperty("result_truncated")
    private boolean resultTruncated;

    @JsonProperty("result_limit")
    private int resultLimit;

    @JsonProperty("properties_truncated")
    private boolean propertiesTruncated;

    @JsonProperty("property_preview_limit")
    private int propertyPreviewLimit;

    @JsonProperty("property_preview_vertex_ids")
    private List<String> propertyPreviewVertexIds;

    @JsonProperty("property_preview_edge_ids")
    private List<String> propertyPreviewEdgeIds;

    public enum Type {

        EMPTY,

        GENERAL,

        VERTEX,

        EDGE,

        PATH;

        public boolean isEmpty() {
            return this == EMPTY;
        }

        public boolean isGeneral() {
            return this == GENERAL;
        }

        public boolean isVertex() {
            return this == VERTEX;
        }

        public boolean isEdge() {
            return this == EDGE;
        }

        public boolean isPath() {
            return this == PATH;
        }

        public boolean isGraph() {
            return this == VERTEX || this == EDGE || this == PATH;
        }
    }
}
