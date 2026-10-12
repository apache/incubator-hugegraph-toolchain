/*
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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "datasource", autoResultMap = true)
public class Datasource {

    @TableId(type = IdType.AUTO)
    @JsonProperty("datasource_id")
    private Integer id;

    @TableField("datasource_name")
    @JsonProperty("datasource_name")
    private String datasourceName;

    @TableField(value = "datasource_config", typeHandler = JacksonTypeHandler.class)
    @JsonProperty("datasource_config")
    private Map<String, Object> datasourceConfig;

    @TableField("creator")
    @JsonProperty("creator")
    private String creator;

    @TableField("create_time")
    @JsonProperty("create_time")
    private Date createTime;
}
