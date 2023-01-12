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
import org.apache.hugegraph.entity.enums.AsyncTaskStatus;
import org.apache.hugegraph.entity.enums.ExecuteStatus;
import org.apache.hugegraph.entity.enums.ExecuteType;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("execute_history")
public class ExecuteHistory implements Identifiable, Mergeable {

    @TableId(type = IdType.AUTO)
    @MergeProperty(useNew = false)
    @JsonProperty("id")
    private Integer id;

    @TableField(value = "conn_id")
    @MergeProperty
    @JsonProperty("conn_id")
    private Integer connId;

    @TableField(value = "async_id")
    @MergeProperty
    @JsonProperty("async_id")
    private Long asyncId;

    @TableField(value = "execute_type")
    @MergeProperty
    @JsonProperty("type")
    private ExecuteType type;

    @MergeProperty
    @JsonProperty("content")
    private String content;

    @TableField(value = "execute_status")
    @MergeProperty
    @JsonProperty("status")
    private ExecuteStatus status;

    @TableField(value = "async_status")
    @MergeProperty
    @JsonProperty("async_status")
    private AsyncTaskStatus asyncStatus;

    @MergeProperty
    @JsonProperty("duration")
    @JsonSerialize(using = SerializeUtil.DurationSerializer.class)
    private Long duration;

    @MergeProperty(useNew = false)
    @JsonProperty("create_time")
    private Date createTime;

    public void setAsyncStatus(AsyncTaskStatus status) {
        this.asyncStatus = status;
    }

    public void setAsyncStatus(String status) {
        this.asyncStatus = AsyncTaskStatus.valueOf(status);
    }
}
