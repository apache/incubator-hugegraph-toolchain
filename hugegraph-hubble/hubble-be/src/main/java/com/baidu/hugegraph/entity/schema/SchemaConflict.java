/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.hugegraph.entity.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaConflict<T extends SchemaEntity> {

    @JsonProperty("entity")
    private T entity;

    @JsonProperty("status")
    private ConflictStatus status;
}
