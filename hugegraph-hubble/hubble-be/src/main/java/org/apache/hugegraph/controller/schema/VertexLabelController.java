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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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
import org.apache.hugegraph.entity.schema.ConflictCheckEntity;
import org.apache.hugegraph.entity.schema.ConflictDetail;
import org.apache.hugegraph.entity.schema.UsingCheckEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelStyle;
import org.apache.hugegraph.entity.schema.VertexLabelUpdateEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.schema.PropertyIndexService;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ImmutableList;

@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/schema/vertexlabels")
public class VertexLabelController extends SchemaController {

    private static final List<String> PRESET_COLORS = ImmutableList.of(
            // bright color
            "#2B65FF", "#0EB880", "#76C100", "#ED7600", "#E65055",
            "#A64EE6", "#108CEE", "#00B5D9", "#F2CA00", "#E048AE",
            // dull color
            "#5C73E6", "#569380", "#76C100", "#FE9227", "#FE585D",
            "#FD6ACE", "#4D8DDA", "#57C7E3", "#F2CA00", "#C570FF"
    );

    @Autowired
    private PropertyKeyService pkService;
    @Autowired
    private PropertyIndexService piService;
    @Autowired
    private VertexLabelService vlService;

    @GetMapping("optional-colors")
    public List<String> getOptionalColors(@PathVariable("connId") int connId) {
        return PRESET_COLORS;
    }

    @GetMapping("{name}/link")
    public List<String> getLinkEdgeLabels(@PathVariable("connId") int connId,
                                          @PathVariable("name") String name) {
        this.vlService.checkExist(name, connId);
        return this.vlService.getLinkEdgeLabels(name, connId);
    }

    @GetMapping
    public IPage<VertexLabelEntity> list(@PathVariable("connId") int connId,
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
        return this.listInPage(id -> this.vlService.list(id),
                               connId, content, nameOrder, pageNo, pageSize);
    }

    @GetMapping("{name}")
    public VertexLabelEntity get(@PathVariable("connId") int connId,
                                 @PathVariable("name") String name) {
        return this.vlService.get(name, connId);
    }

    @PostMapping
    public void create(@PathVariable("connId") int connId,
                       @RequestBody VertexLabelEntity entity) {
        this.checkParamsValid(entity, connId, true);
        this.checkEntityUnique(entity, connId, true);
        entity.setCreateTime(HubbleUtil.nowDate());
        this.vlService.add(entity, connId);
    }

    @PostMapping("check_conflict")
    public ConflictDetail checkConflicts(
                          @PathVariable("connId") int connId,
                          @RequestParam("reused_conn_id") int reusedConnId,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.cannot-be-empty", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getPkEntities()),
                 "common.param.must-be-null", "propertykeys");
        Ex.check(CollectionUtils.isEmpty(entity.getPiEntities()),
                 "common.param.must-be-null", "propertyindexes");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");
        Ex.check(connId != reusedConnId, "schema.conn.cannot-reuse-self");

        Set<String> pkNames = new HashSet<>();
        Set<String> piNames = new HashSet<>();
        for (VertexLabelEntity e : entity.getVlEntities()) {
            pkNames.addAll(e.getPropNames());
            piNames.addAll(e.getIndexProps());
        }

        entity.setPkEntities(this.pkService.list(pkNames, reusedConnId, false));
        entity.setPiEntities(this.piService.list(piNames, reusedConnId, false));
        return this.vlService.checkConflict(entity, connId, false);
    }

    @PostMapping("recheck_conflict")
    public ConflictDetail recheckConflicts(
                          @PathVariable("connId") int connId,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.cannot-be-empty", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");
        return this.vlService.checkConflict(entity, connId, true);
    }

    @PostMapping("reuse")
    public void reuse(@PathVariable("connId") int connId,
                      @RequestBody ConflictDetail detail) {
        this.vlService.reuse(detail, connId);
    }

    @PutMapping("{name}")
    public void update(@PathVariable("connId") int connId,
                       @PathVariable("name") String name,
                       @RequestBody VertexLabelUpdateEntity entity) {
        Ex.check(!StringUtils.isEmpty(name),
                 "common.param.cannot-be-null-or-empty", name);
        entity.setName(name);

        this.vlService.checkExist(name, connId);
        checkParamsValid(this.pkService, entity, connId);
        this.vlService.update(entity, connId);
    }

    @PostMapping("check_using")
    public Map<String, Boolean> checkUsing(
                                @PathVariable("connId") int connId,
                                @RequestBody UsingCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getNames()),
                 "common.param.cannot-be-empty", "names");
        Map<String, Boolean> inUsing = new LinkedHashMap<>();
        for (String name : entity.getNames()) {
            this.vlService.checkExist(name, connId);
            inUsing.put(name, this.vlService.checkUsing(name, connId));
        }
        return inUsing;
    }

    @DeleteMapping
    public void delete(@PathVariable("connId") int connId,
                       @RequestParam("names") List<String> names,
                       @RequestParam(name = "skip_using",
                                     defaultValue = "false")
                       boolean skipUsing) {
        for (String name : names) {
            this.vlService.checkExist(name, connId);
            if (this.vlService.checkUsing(name, connId)) {
                if (skipUsing) {
                    continue;
                } else {
                    throw new ExternalException("schema.vertexlabel.in-using",
                                                name);
                }
            }
            this.vlService.remove(name, connId);
        }
    }

    private void checkParamsValid(VertexLabelEntity entity, int connId,
                                  boolean checkCreateTime) {
        String name = entity.getName();
        Ex.check(name != null, "common.param.cannot-be-null", "name");
        Ex.check(Constant.SCHEMA_NAME_PATTERN.matcher(name).matches(),
                 "schema.vertexlabel.unmatch-regex");
        Ex.check(checkCreateTime, () -> entity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");
        // Check properties
        checkProperties(this.pkService, entity.getProperties(), false, connId);
        // Check primary keys
        checkPrimaryKeys(entity);
        // Check property index
        checkPropertyIndexes(entity, connId);
        // Check display fields and join symbols
        checkDisplayFields(entity);
    }

    private static void checkDisplayFields(VertexLabelEntity entity) {
        VertexLabelStyle style = entity.getStyle();
        List<String> displayFields = style.getDisplayFields();
        if (!CollectionUtils.isEmpty(displayFields)) {
            Set<String> nullableProps = entity.getNullableProps();
            Ex.check(!CollectionUtil.hasIntersection(displayFields,
                                                     nullableProps),
                     "schema.display-fields.cannot-be-nullable");
        }
    }

    private void checkPrimaryKeys(VertexLabelEntity entity) {
        IdStrategy idStrategy = entity.getIdStrategy();
        Ex.check(idStrategy != null,
                 "common.param.cannot-be-null", "id_strategy");
        List<String> primaryKeys = entity.getPrimaryKeys();
        if (idStrategy.isPrimaryKey()) {
            Ex.check(!CollectionUtils.isEmpty(entity.getProperties()),
                     "schema.vertexlabel.property.cannot-be-null-or-empty",
                     entity.getName());
            Ex.check(!CollectionUtils.isEmpty(primaryKeys),
                     "schema.vertexlabel.primarykey.cannot-be-null-or-empty",
                     entity.getName());
            // All primary keys must belong to properties
            Set<String> propNames = entity.getPropNames();
            Ex.check(propNames.containsAll(primaryKeys),
                     "schema.vertexlabel.primarykey.must-belong-to.property",
                     entity.getName(), primaryKeys, propNames);
            // Any primary key can't be nullable
            Ex.check(!CollectionUtil.hasIntersection(primaryKeys,
                                                     entity.getNullableProps()),
                     "schmea.vertexlabel.primarykey.cannot-be-nullable",
                     entity.getName());
        } else {
            Ex.check(CollectionUtils.isEmpty(primaryKeys),
                     "schema.vertexlabel.primarykey.should-be-null-or-empty",
                     entity.getName(), idStrategy);
        }
    }

    private void checkEntityUnique(VertexLabelEntity newEntity, int connId,
                                   boolean creating) {
        // The name must be unique
        String name = newEntity.getName();
        this.vlService.checkNotExist(name, connId);
    }
}
