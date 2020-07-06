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

package com.baidu.hugegraph.entity.load;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.baidu.hugegraph.annotation.MergeProperty;
import com.baidu.hugegraph.entity.GraphConnection;
import com.baidu.hugegraph.entity.enums.LoadStatus;
import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.util.Ex;
import com.baidu.hugegraph.util.HubbleUtil;
import com.baidu.hugegraph.util.SerializeUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "load_task", autoResultMap = true)
@JsonPropertyOrder({"id", "conn_id", "file_id", "file_name", "vertices", "edges",
                    "load_rate", "load_progress", "file_total_lines",
                    "file_read_lines", "status", "duration", "create_time"})
public class LoadTask implements Runnable {

    @TableField(exist = false)
    @JsonIgnore
    private transient HugeGraphLoader loader;

    @TableId(type = IdType.AUTO)
    @MergeProperty(useNew = false)
    @JsonProperty("id")
    private Integer id;

    @TableField(value = "conn_id")
    @MergeProperty
    @JsonProperty("conn_id")
    private Integer connId;

    @TableField(value = "file_id")
    @MergeProperty
    @JsonProperty("file_id")
    private Integer fileId;

    @TableField(value = "file_name")
    @MergeProperty
    @JsonProperty("file_name")
    private String fileName;

    @TableField(value = "options", typeHandler = JacksonTypeHandler.class)
    @MergeProperty
    @JsonProperty(value = "options", access = JsonProperty.Access.WRITE_ONLY)
    private LoadOptions options;

    @TableField(value = "vertices", typeHandler = JacksonTypeHandler.class)
    @MergeProperty
    @JsonProperty(value = "vertices")
    private Set<String> vertices;

    @TableField(value = "edges", typeHandler = JacksonTypeHandler.class)
    @MergeProperty
    @JsonProperty(value = "edges")
    private Set<String> edges;

    @TableField("file_total_lines")
    @MergeProperty
    @JsonProperty("file_total_lines")
    private Long fileTotalLines;

    @TableField("file_read_lines")
    @MergeProperty
    @JsonProperty("file_read_lines")
    private Long fileReadLines;

    @TableField("load_status")
    @MergeProperty
    @JsonProperty("status")
    private LoadStatus status;

    @TableField("duration")
    @MergeProperty
    @JsonProperty("duration")
    @JsonSerialize(using = SerializeUtil.DurationSerializer.class)
    private Long duration;

    @MergeProperty(useNew = false)
    @JsonProperty("create_time")
    private Date createTime;

    public LoadTask(LoadOptions options, GraphConnection connection,
                    FileMapping mapping) {
        this.loader = new HugeGraphLoader(options);
        this.id = null;
        this.connId = connection.getId();
        this.fileId = mapping.getId();
        this.fileName = mapping.getName();
        this.options = options;
        if (mapping.getVertexMappings() != null) {
            this.vertices = mapping.getVertexMappings().stream()
                                   .map(ElementMapping::getLabel)
                                   .collect(Collectors.toSet());
        } else {
            this.vertices = new HashSet<>();
        }
        if (mapping.getEdgeMappings() != null) {
            this.edges = mapping.getEdgeMappings().stream()
                                .map(ElementMapping::getLabel)
                                .collect(Collectors.toSet());
        } else {
            this.edges = new HashSet<>();
        }
        this.fileTotalLines = mapping.getTotalLines();
        this.fileReadLines = 0L;
        this.status = LoadStatus.RUNNING;
        this.duration = 0L;
        this.createTime = HubbleUtil.nowDate();
    }

    @Override
    public void run() {
        log.info("LoadTaskMonitor is monitoring task : {}", this.id);
        boolean succeed;
        try {
            succeed = this.loader.load();
        } catch (Throwable e) {
            succeed = false;
            log.error("Run task {} failed. cause: {}", this.id, e.getMessage());
        }
        // Pay attention to whether the user stops actively or
        // the program stops by itself
        if (this.status.inRunning()) {
            if (succeed) {
                this.status = LoadStatus.SUCCEED;
            } else {
                this.status = LoadStatus.FAILED;
            }
        }
        this.fileReadLines = this.context().newProgress().totalInputReaded();
        this.duration += this.context().summary().totalTime();
    }

    public void restoreContext() {
        Ex.check(this.options != null,
                 "The load options shouldn't be null");
        this.loader = new HugeGraphLoader(this.options);
    }

    public LoadContext context() {
        return this.loader.context();
    }

    @JsonProperty("load_progress")
    public int getLoadProgress() {
        if (this.fileTotalLines == null || this.fileReadLines == null ||
            this.fileTotalLines == 0) {
            return 0;
        } else {
            long totalLines = this.fileTotalLines;
            if (this.fileTotalLines < this.fileReadLines) {
                /*
                 * The line counted is one less than actually read,
                 * it caused by FileUtil.countLines
                 */
                Ex.check(this.fileTotalLines + 1 == this.fileReadLines,
                         "The file total lines must be >= read lines or " +
                         "one less than read lines, but got total lines %s, " +
                         "read lines %s",
                         this.fileTotalLines, this.fileReadLines);
                totalLines = this.fileTotalLines + 1;
            }
            return (int) ((double) this.fileReadLines / totalLines * 100);
        }
    }

    @JsonProperty("load_rate")
    public String getLoadRate() {
        float rate;
        if (this.fileReadLines == null || this.duration == null ||
            this.duration == 0L) {
            rate = 0;
        } else {
            rate = this.fileReadLines * 1000.0F / this.duration;
            rate = Math.round(rate * 1000) / 1000.0F;
        }
        return String.format("%s/s", rate);
    }
}
