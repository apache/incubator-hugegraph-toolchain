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

import java.util.Date;

import org.apache.hugegraph.annotation.MergeProperty;
import org.apache.hugegraph.common.Identifiable;
import org.apache.hugegraph.common.Mergeable;
import org.apache.hugegraph.util.JsonUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TableName("edit_history")
public class ElementEditHistory implements Identifiable, Mergeable {

    @TableId(type = IdType.AUTO)
    @MergeProperty(useNew = false)
    @JsonProperty("id")
    private Integer id;

    @TableField(value = "graphspace")
    @MergeProperty
    @JsonProperty("graphspace")
    private String graphspace;

    @TableField(value = "graph")
    @MergeProperty
    @JsonProperty("graph")
    private String graph;

    @TableField(value = "element_id")
    @MergeProperty
    @JsonProperty("element_id")
    private String elementId;

    @TableField(value = "label")
    @MergeProperty
    @JsonProperty("label")
    private String label;

    @TableField(value = "property_num")
    @MergeProperty
    @JsonProperty("property_num")
    private int propertyNum;

    @TableField(value = "option_type")
    @MergeProperty
    @JsonProperty("option_type")
    private String optionType;

    @TableField(value = "option_time")
    @MergeProperty
    @JsonProperty("option_time")
    private Date optionTime;

    @TableField(value = "option_person")
    @MergeProperty
    @JsonProperty("option_person")
    private String optionPerson;

    @TableField(value = "content")
    @MergeProperty
    @JsonProperty("content")
    private String content;

    public ElementEditHistory(Integer id, String graphspace, String graph,
                              String elementId, String label, int propertyNum,
                              String optionType, Date optionTime,
                              String optionPerson, String content) {
        this.id = id;
        this.graphspace = graphspace;
        this.graph = graph;
        this.elementId = elementId;
        this.label = label;
        this.propertyNum = propertyNum;
        this.optionType = optionType;
        this.optionTime = optionTime;
        this.optionPerson = optionPerson;
        this.content = content;
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
