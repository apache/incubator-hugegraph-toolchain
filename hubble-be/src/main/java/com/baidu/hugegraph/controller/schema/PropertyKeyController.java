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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.entity.schema.ConflictCheckEntity;
import com.baidu.hugegraph.entity.schema.ConflictDetail;
import com.baidu.hugegraph.entity.schema.PropertyKeyEntity;
import com.baidu.hugegraph.entity.schema.UsingCheckEntity;
import com.baidu.hugegraph.exception.ExternalException;
import com.baidu.hugegraph.service.schema.PropertyKeyService;
import com.baidu.hugegraph.util.Ex;
import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "schema/propertykeys")
public class PropertyKeyController extends SchemaController {

    @Autowired
    private PropertyKeyService service;

    @GetMapping
    public IPage<PropertyKeyEntity> list(@RequestParam("conn_id") int connId,
                                         @RequestParam(name = "content",
                                                       required = false)
                                         String content,
                                         @RequestParam(name = "name_order",
                                                       required = false)
                                         String nameOrder,
                                         @RequestParam(name = "page_no",
                                                       required = false,
                                                       defaultValue = "1")
                                         int pageNo,
                                         @RequestParam(name = "page_size",
                                                       required = false,
                                                       defaultValue = "10")
                                         int pageSize) {
        return this.listInPage(id -> this.service.list(id),
                               connId, content, nameOrder, pageNo, pageSize);
    }

    @GetMapping("{name}")
    public PropertyKeyEntity get(@PathVariable("name") String name,
                                 @RequestParam("conn_id") int connId) {
        PropertyKeyEntity entity = this.service.get(name, connId);
        Ex.check(entity != null, "schema.propertykey.not-exist", name);
        return entity;
    }

    @PostMapping
    public void create(@RequestBody PropertyKeyEntity entity,
                       @RequestParam("conn_id") int connId) {
        this.checkParamsValid(entity, true);
        this.checkEntityUnique(entity, connId);
        entity.setCreateTime(new Date());
        this.service.add(entity, connId);
    }

    @PostMapping("check_conflict")
    public ConflictDetail checkConflict(
                          @RequestBody ConflictCheckEntity entity,
                          @RequestParam("conn_id") int connId) {
        List<PropertyKeyEntity> entities = entity.getPkEntities();
        Ex.check(!CollectionUtils.isEmpty(entities),
                 "common.param.cannot-be-empty", "entities");
        Ex.check(CollectionUtils.isEmpty(entity.getPiEntities()),
                 "common.param.must-be-null", "propertyindexes");
        Ex.check(CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.must-be-null", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");
        entities.forEach(e -> this.checkParamsValid(e, false));
        return this.service.checkConflict(entity, connId, false);
    }

    @PostMapping("recheck_conflict")
    public ConflictDetail recheckConflict(
                          @RequestBody ConflictCheckEntity entity,
                          @RequestParam("conn_id") int connId) {
        Ex.check(!CollectionUtils.isEmpty(entity.getPkEntities()),
                 "common.param.cannot-be-empty", "propertykeys");
        Ex.check(CollectionUtils.isEmpty(entity.getPiEntities()),
                 "common.param.must-be-null", "propertyindexes");
        Ex.check(CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.must-be-null", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");
        return this.service.checkConflict(entity, connId, true);
    }

    @PostMapping("reuse")
    public void reuse(@RequestBody ConflictDetail detail,
                      @RequestParam("conn_id") int connId) {
        this.service.reuse(detail, connId);
    }

    @PostMapping("check_using")
    public Map<String, Boolean> checkUsing(@RequestBody UsingCheckEntity entity,
                                           @RequestParam("conn_id") int connId) {
        Ex.check(!CollectionUtils.isEmpty(entity.getNames()),
                 "common.param.cannot-be-empty", "names");
        Map<String, Boolean> inUsing = new LinkedHashMap<>();
        for (String name : entity.getNames()) {
            Ex.check(this.service.exist(name, connId),
                     "schema.propertykey.not-exist", name);
            inUsing.put(name, this.service.checkUsing(name, connId));
        }
        return inUsing;
    }

    /**
     * Should request "check_using" before delete
     */
    @DeleteMapping
    public void delete(@RequestParam List<String> names,
                       @RequestParam(name = "skip_using",
                                     defaultValue = "false")
                       boolean skipUsing,
                       @RequestParam("conn_id") int connId) {
        for (String name : names) {
            PropertyKeyEntity entity = this.service.get(name, connId);
            Ex.check(entity != null, "schema.propertykey.not-exist", name);
            if (this.service.checkUsing(name, connId)) {
                if (skipUsing) {
                    continue;
                } else {
                    throw new ExternalException("schema.propertykey.in-using",
                                                name);
                }
            }
            this.service.remove(name, connId);
        }
    }

    private void checkParamsValid(PropertyKeyEntity entity,
                                  boolean checkCreateTime) {
        String name = entity.getName();
        Ex.check(name != null, "common.param.cannot-be-null", "name");
        Ex.check(Constant.SCHEMA_NAME_PATTERN.matcher(name).matches(),
                 "schema.propertykey.unmatch-regex");
        Ex.check(entity.getDataType() != null,
                 "common.param.cannot-be-null", "data_type");
        Ex.check(entity.getCardinality() != null,
                 "common.param.cannot-be-null", "cardinality");
        Ex.check(checkCreateTime, () -> entity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");
    }

    private void checkEntityUnique(PropertyKeyEntity newEntity, int connId) {
        // The name must be unique
        String name = newEntity.getName();
        PropertyKeyEntity oldEntity = this.service.get(name, connId);
        Ex.check(oldEntity == null, "schema.propertykey.exist", name);
    }
}
