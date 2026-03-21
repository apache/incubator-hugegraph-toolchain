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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.apache.hugegraph.annotation.MergeProperty;
import org.apache.hugegraph.common.AppType;
import org.apache.hugegraph.common.Mergeable;
import org.apache.hugegraph.util.JsonUtil;


@Data
@Builder
@TableName("app_info")
public class ApplicationInfo implements Mergeable {
        // graph_name in mysql {idc}-{graph_space}-{graph} (eg:bddwd-DEFAULT-graph)
        @TableField(value = "graph_name")
        @MergeProperty
        @JsonProperty("graph_name")
        private String graphName;

        @TableField(value = "app_name")
        @MergeProperty
        @JsonProperty("app_name")
        private String appName;

        @TableField(value = "app_type")
        @MergeProperty
        @JsonProperty("app_type")
        private AppType appType;

        @TableField(value = "count_query")
        @MergeProperty
        @JsonProperty("count_query")
        private String countQuery;

        @TableField(value = "distribution_query")
        @MergeProperty
        @JsonProperty("distribution_query")
        private String distributionQuery;

        @TableField(value = "description")
        @MergeProperty
        @JsonProperty("description")
        private String description;

        @Override
        public String toString() {
                return JsonUtil.toJson(this);
        }
}
