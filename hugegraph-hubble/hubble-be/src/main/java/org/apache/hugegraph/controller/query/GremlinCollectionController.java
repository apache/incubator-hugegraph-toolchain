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

package org.apache.hugegraph.controller.query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.entity.query.GremlinCollection;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.query.GremlinCollectionService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/gremlin-collections")
public class GremlinCollectionController extends GremlinController {

    private static final int LIMIT = 100;

    private final GremlinCollectionService service;

    @Autowired
    public GremlinCollectionController(GremlinCollectionService service) {
        this.service = service;
    }

    @GetMapping
    public IPage<GremlinCollection> list(@PathVariable("connId") int connId,
                                         @RequestParam(name = "content",
                                                       required = false)
                                         String content,
                                         @RequestParam(name = "name_order",
                                                       required = false)
                                         String nameOrder,
                                         @RequestParam(name = "time_order",
                                                       required = false)
                                         String timeOrder,
                                         @RequestParam(name = "page_no",
                                                       required = false,
                                                       defaultValue = "1")
                                         int pageNo,
                                         @RequestParam(name = "page_size",
                                                       required = false,
                                                       defaultValue = "10")
                                         int pageSize) {
        Ex.check(nameOrder == null || timeOrder == null,
                 "common.name-time-order.conflict");
        Boolean nameOrderAsc = null;
        if (!StringUtils.isEmpty(nameOrder)) {
            Ex.check(ORDER_ASC.equals(nameOrder) || ORDER_DESC.equals(nameOrder),
                     "common.name-order.invalid", nameOrder);
            nameOrderAsc = ORDER_ASC.equals(nameOrder);
        }

        Boolean timeOrderAsc = null;
        if (!StringUtils.isEmpty(timeOrder)) {
            Ex.check(ORDER_ASC.equals(timeOrder) || ORDER_DESC.equals(timeOrder),
                     "common.time-order.invalid", timeOrder);
            timeOrderAsc = ORDER_ASC.equals(timeOrder);
        }
        return this.service.list(connId, content, nameOrderAsc, timeOrderAsc,
                                 pageNo, pageSize);
    }

    @GetMapping("{id}")
    public GremlinCollection get(@PathVariable("id") int id) {
        return this.service.get(id);
    }

    @PostMapping
    public GremlinCollection create(@PathVariable("connId") int connId,
                                    @RequestBody GremlinCollection newEntity) {
        this.checkParamsValid(newEntity, true);
        newEntity.setConnId(connId);
        newEntity.setCreateTime(HubbleUtil.nowDate());
        this.checkEntityUnique(newEntity, true);
        // The service is an singleton object
        synchronized (this.service) {
            Ex.check(this.service.count() < LIMIT,
                     "gremlin-collection.reached-limit", LIMIT);
            this.service.save(newEntity);
        }
        return newEntity;
    }

    @PutMapping("{id}")
    public GremlinCollection update(@PathVariable("id") int id,
                                    @RequestBody GremlinCollection newEntity) {
        this.checkIdSameAsBody(id, newEntity);
        this.checkParamsValid(newEntity, false);

        GremlinCollection oldEntity = this.service.get(id);
        if (oldEntity == null) {
            throw new ExternalException("gremlin-collection.not-exist.id", id);
        }

        GremlinCollection entity = this.mergeEntity(oldEntity, newEntity);
        this.checkEntityUnique(entity, false);
        this.service.update(entity);
        return entity;
    }

    @DeleteMapping("{id}")
    public GremlinCollection delete(@PathVariable("id") int id) {
        GremlinCollection oldEntity = this.service.get(id);
        if (oldEntity == null) {
            throw new ExternalException("gremlin-collection.not-exist.id", id);
        }
        this.service.remove(id);
        return oldEntity;
    }

    private void checkParamsValid(GremlinCollection newEntity,
                                  boolean creating) {
        Ex.check(creating, () -> newEntity.getId() == null,
                 "common.param.must-be-null", "id");

        Ex.check(newEntity.getConnId() == null,
                 "common.param.must-be-null", "conn_id");

        String name = newEntity.getName();
        this.checkParamsNotEmpty("name", name, creating);
        Ex.check(name != null, () -> Constant.COMMON_NAME_PATTERN.matcher(name)
                                                                 .matches(),
                 "gremlin-collection.name.unmatch-regex");

        String content = newEntity.getContent();
        this.checkParamsNotEmpty("content", content, creating);
        Ex.check(CONTENT_PATTERN.matcher(content).find(),
                 "gremlin-collection.content.invalid", content);
        checkContentLength(content);

        Ex.check(newEntity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");
    }

    private void checkEntityUnique(GremlinCollection newEntity,
                                   boolean creating) {
        int connId = newEntity.getConnId();
        String name = newEntity.getName();
        // NOTE: Full table scan may slow, it's better to use index
        GremlinCollection oldEntity = this.service.getByName(connId, name);
        if (creating) {
            Ex.check(oldEntity == null, "gremlin-collection.exist.name", name);
        } else {
            Ex.check(oldEntity != null,
                     () -> oldEntity.getId().equals(newEntity.getId()),
                     "gremlin-collection.exist.name", name);
        }
    }
}
