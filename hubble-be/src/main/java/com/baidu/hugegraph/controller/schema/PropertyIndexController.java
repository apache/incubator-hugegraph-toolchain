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

package com.baidu.hugegraph.controller.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.entity.schema.ConflictStatus;
import com.baidu.hugegraph.entity.schema.PropertyIndex;
import com.baidu.hugegraph.service.schema.PropertyIndexService;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.util.Ex;
import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping(Constant.API_VERSION + "schema/propertyindexes")
public class PropertyIndexController extends SchemaController {

    @Autowired
    private PropertyIndexService service;

    @GetMapping
    public IPage<PropertyIndex> list(@RequestParam("conn_id") int connId,
                                     @RequestParam(name = "is_vertex_label")
                                     boolean isVertexLabel,
                                     @RequestParam(name = "content",
                                                   required = false)
                                     String content,
                                     @RequestParam(name = "page_no",
                                                   required = false,
                                                   defaultValue = "1")
                                     int pageNo,
                                     @RequestParam(name = "page_size",
                                                   required = false,
                                                   defaultValue = "10")
                                     int pageSize) {
        HugeType type = isVertexLabel ? HugeType.VERTEX_LABEL :
                                        HugeType.EDGE_LABEL;
        if (StringUtils.isEmpty(content)) {
            return this.service.list(connId, type, pageNo, pageSize);
        } else {
            return this.service.list(connId, type, content, pageNo, pageSize);
        }
    }

    @PostMapping("check_conflict")
    public ConflictStatus checkConflict(@RequestBody PropertyIndex entity,
                                        @RequestParam("conn_id") int connId) {
        this.checkParamsValid(entity);
        return this.service.checkConflict(entity, connId);
    }

    private void checkParamsValid(PropertyIndex entity) {
        String name = entity.getName();
        Ex.check(name != null, "common.param.cannot-be-null", "name");
        Ex.check(Constant.SCHEMA_NAME_PATTERN.matcher(name).matches(),
                 "schema.propertyindex.unmatch-regex");
        Ex.check(entity.getOwner() != null,
                 "common.param.cannot-be-null", "owner");
        Ex.check(entity.getOwnerType() != null,
                 "common.param.cannot-be-null", "owner_type");
        Ex.check(entity.getType() != null,
                 "common.param.cannot-be-null", "type");
        Ex.check(!CollectionUtils.isEmpty(entity.getFields()),
                 "common.param.cannot-be-null", "fields");
    }
}
