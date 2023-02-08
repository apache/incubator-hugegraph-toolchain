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

package org.apache.hugegraph.entity.load;

import java.util.Date;

import org.apache.hugegraph.annotation.MergeProperty;
import org.apache.hugegraph.entity.enums.JobStatus;
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
@TableName(value = "job_manager", autoResultMap = true)
public class JobManager {

    @TableId(type = IdType.AUTO)
    @MergeProperty(useNew = false)
    @JsonProperty("id")
    private Integer id;

    @TableField(value = "conn_id")
    @MergeProperty
    @JsonProperty("conn_id")
    private Integer connId;

    @TableField(value = "job_name")
    @MergeProperty
    @JsonProperty("job_name")
    private String jobName;

    @TableField(value = "job_remarks")
    @MergeProperty
    @JsonProperty("job_remarks")
    private String jobRemarks;

    @TableField(value = "job_size")
    @MergeProperty
    @JsonProperty("job_size")
    @JsonSerialize(using = SerializeUtil.SizeSerializer.class)
    private long jobSize;

    @TableField("job_status")
    @MergeProperty
    @JsonProperty("job_status")
    private JobStatus jobStatus;

    @TableField("job_duration")
    @MergeProperty
    @JsonProperty("job_duration")
    @JsonSerialize(using = SerializeUtil.DurationSerializer.class)
    private long jobDuration;

    @TableField("update_time")
    @MergeProperty(useNew = false)
    @JsonProperty("update_time")
    private Date updateTime;

    @TableField("create_time")
    @MergeProperty(useNew = false)
    @JsonProperty("create_time")
    private Date createTime;
}
