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

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.log4j.Log4j2;
import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.schema.ConflictCheckEntity;
import org.apache.hugegraph.entity.schema.ConflictDetail;
import org.apache.hugegraph.entity.schema.PropertyKeyEntity;
import org.apache.hugegraph.entity.schema.UsingCheckEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/schema/propertykeys")
public class PropertyKeyController extends SchemaController {

    @Autowired
    private PropertyKeyService service;

    @GetMapping
    public IPage<PropertyKeyEntity> list(@PathVariable("graphspace") String graphSpace,
                                         @PathVariable("graph") String graph,
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
        HugeClient client = this.authClient(graphSpace, graph);
        return this.listInPage(id -> this.service.list(id),
                               client, content, nameOrder, pageNo, pageSize);
    }

    @GetMapping("{name}")
    public PropertyKeyEntity get(@PathVariable("graphspace") String graphSpace,
                                 @PathVariable("graph") String graph,
                                 @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.get(name, client);
    }

    @PostMapping
    public void create(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @RequestBody PropertyKeyEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);
        this.checkParamsValid(entity, true);
        this.checkEntityUnique(entity, client);
        entity.setCreateTime(HubbleUtil.nowDate());
        this.service.add(entity, client);
    }

    @PostMapping("check_conflict")
    public ConflictDetail checkConflict(
                          @PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestBody ConflictCheckEntity entity) {
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
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.checkConflict(entity, client, false);
    }

    @PostMapping("recheck_conflict")
    public ConflictDetail recheckConflict(
                          @PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getPkEntities()),
                 "common.param.cannot-be-empty", "propertykeys");
        Ex.check(CollectionUtils.isEmpty(entity.getPiEntities()),
                 "common.param.must-be-null", "propertyindexes");
        Ex.check(CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.must-be-null", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");
        HugeClient client = this.authClient(graphSpace, graph);
        return this.service.checkConflict(entity, client, true);
    }

    @PostMapping("reuse")
    public void reuse(@PathVariable("graphspace") String graphSpace,
                      @PathVariable("graph") String graph,
                      @RequestBody ConflictDetail detail) {
        Ex.check(!CollectionUtils.isEmpty(detail.getPkConflicts()),
                 "common.param.cannot-be-empty", "propertykey_conflicts");
        HugeClient client = this.authClient(graphSpace, graph);
        this.service.reuse(detail, client);
    }

    @PostMapping("check_using")
    public Map<String, Boolean> checkUsing(@PathVariable("graphspace") String graphSpace,
                                           @PathVariable("graph") String graph,
                                           @RequestBody UsingCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getNames()),
                 "common.param.cannot-be-empty", "names");
        Map<String, Boolean> inUsing = new LinkedHashMap<>();
        HugeClient client = this.authClient(graphSpace, graph);
        for (String name : entity.getNames()) {
            this.service.checkExist(name, client);
            inUsing.put(name, this.service.checkUsing(name, client));
        }
        return inUsing;
    }

    /**
     * Should request "check_using" before delete
     */
    @DeleteMapping
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @RequestParam List<String> names,
                       @RequestParam(name = "skip_using",
                                     defaultValue = "false")
                       boolean skipUsing) {
        HugeClient client = this.authClient(graphSpace, graph);
        for (String name : names) {
            this.service.checkExist(name, client);
            if (this.service.checkUsing(name, client)) {
                if (skipUsing) {
                    continue;
                } else {
                    throw new ExternalException("schema.propertykey.in-using",
                                                name);
                }
            }
            this.service.remove(name, client);
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

    private void checkEntityUnique(PropertyKeyEntity newEntity,
                                   HugeClient client) {
        // The name must be unique
        String name = newEntity.getName();
        this.service.checkNotExist(name, client);
    }
}
