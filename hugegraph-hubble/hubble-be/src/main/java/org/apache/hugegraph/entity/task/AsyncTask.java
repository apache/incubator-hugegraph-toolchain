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

package org.apache.hugegraph.entity.task;

import java.util.Date;

import org.apache.hugegraph.annotation.MergeProperty;
import org.apache.hugegraph.util.SerializeUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName(value = "async_task", autoResultMap = true)
public class AsyncTask {

    @TableId(type = IdType.AUTO)
    @MergeProperty(useNew = false)
    @JsonProperty("id")
    private Integer id;

    @TableField(value = "conn_id")
    @MergeProperty
    @JsonProperty("conn_id")
    private Integer connId;

    @TableField(value = "task_id")
    @MergeProperty
    @JsonProperty("task_id")
    private Integer taskId;

    @TableField(value = "task_name")
    @MergeProperty
    @JsonProperty("task_name")
    private String taskName;

    @TableField(value = "task_reason")
    @MergeProperty
    @JsonProperty("task_reason")
    private String taskReason;

    @TableField(value = "task_type")
    @MergeProperty
    @JsonProperty("task_type")
    private Integer taskType;

    @TableField(value = "algorithm_name")
    @MergeProperty
    @JsonProperty("algorithm_name")
    private String algorithmName;

    @TableField(value = "task_content")
    @MergeProperty
    @JsonProperty("task_content")
    private String taskContent;

    @TableField(value = "task_status")
    @MergeProperty
    @JsonProperty("task_status")
    private Integer taskStatus;

    @TableField("task_duration")
    @MergeProperty
    @JsonProperty("task_duration")
    @JsonSerialize(using = SerializeUtil.DurationSerializer.class)
    private Long taskDuration;

    @MergeProperty(useNew = false)
    @JsonProperty("update_time")
    private Date updateTime;

    @MergeProperty(useNew = false)
    @JsonProperty("create_time")
    private Date createTime;
}
