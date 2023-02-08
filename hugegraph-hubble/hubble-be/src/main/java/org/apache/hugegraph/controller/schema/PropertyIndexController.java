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

package org.apache.hugegraph.controller.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.entity.schema.PropertyIndex;
import org.apache.hugegraph.service.schema.PropertyIndexService;
import org.apache.hugegraph.structure.constant.HugeType;
import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/schema/propertyindexes")
public class PropertyIndexController extends SchemaController {

    @Autowired
    private PropertyIndexService service;

    @GetMapping
    public IPage<PropertyIndex> list(@PathVariable("connId") int connId,
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
}
