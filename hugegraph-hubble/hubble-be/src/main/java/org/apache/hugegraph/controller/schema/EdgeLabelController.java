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
import java.util.List;
import java.util.Set;

import org.apache.hugegraph.driver.HugeClient;
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
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.EdgeLabelStyle;
import org.apache.hugegraph.entity.schema.EdgeLabelUpdateEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.PropertyIndexService;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.EdgeLabelType;
import org.apache.hugegraph.util.CollectionUtil;
import org.apache.hugegraph.util.Ex;
import org.apache.hugegraph.util.HubbleUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.ImmutableList;

@RestController
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}/schema/edgelabels")
public class EdgeLabelController extends SchemaController {

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
    @Autowired
    private EdgeLabelService elService;

    @GetMapping("optional-colors")
    public List<String> getOptionalColors() {
        return PRESET_COLORS;
    }

    @GetMapping
    public IPage<EdgeLabelEntity> list(@PathVariable("graphspace")
                                                   String graphSpace,
                                       @PathVariable("graph") String graph,
                                       @RequestParam(name = "edgelabel_type",
                                                     required = false)
                                                   String type,
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
        return this.listInPage(id -> this.elService.list(null, id, true, type),
                               client, content, nameOrder, pageNo, pageSize);
    }

    @GetMapping("{name}")
    public EdgeLabelEntity get(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.elService.get(name, client);
    }

    @PostMapping
    public void create(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @RequestBody EdgeLabelEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);
        this.checkParamsValid(entity, client, true);
        this.checkEntityUnique(entity, client, true);
        entity.setCreateTime(HubbleUtil.nowDate());
        this.elService.add(entity, client);
    }

    @PostMapping("check_conflict")
    public ConflictDetail checkConflict(
                          @PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestParam("reused_graphspace") String reusedGraphSpace,
                          @RequestParam("reused_graph") String reusedGraph,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.cannot-be-empty", "edgelabels");
        Ex.check(CollectionUtils.isEmpty(entity.getPkEntities()),
                 "common.param.must-be-null", "propertykeys");
        Ex.check(CollectionUtils.isEmpty(entity.getPiEntities()),
                 "common.param.must-be-null", "propertyindexes");
        Ex.check(CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.must-be-null", "vertexlabels");
        Ex.check(graphSpace != reusedGraphSpace && graph != reusedGraph,
                 "schema.conn.cannot-reuse-self");

        Set<String> pkNames = new HashSet<>();
        Set<String> piNames = new HashSet<>();
        Set<String> vlNames = new HashSet<>();
        for (EdgeLabelEntity e : entity.getElEntities()) {
            pkNames.addAll(e.getPropNames());
            piNames.addAll(e.getIndexProps());
            vlNames.addAll(e.getLinkLabels());
        }
        List<VertexLabelEntity> vlEntities;
        HugeClient client = this.authClient(graphSpace, graph);
        HugeClient reusedClient = this.authClient(reusedGraphSpace,
                                                  reusedGraph);
        vlEntities = this.vlService.list(vlNames, reusedClient, false);
        for (VertexLabelEntity e : vlEntities) {
            pkNames.addAll(e.getPropNames());
            piNames.addAll(e.getIndexProps());
        }

        entity.setPkEntities(this.pkService.list(pkNames, reusedClient, false));
        entity.setPiEntities(this.piService.list(piNames, reusedClient, false));
        entity.setVlEntities(vlEntities);
        return this.elService.checkConflict(entity, client, false);
    }

    @PostMapping("recheck_conflict")
    public ConflictDetail recheckConflict(
                          @PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.cannot-be-empty", "edgelabels");
        HugeClient client = this.authClient(graphSpace, graph);
        return this.elService.checkConflict(entity, client, true);
    }

    @PostMapping("reuse")
    public void reuse(@PathVariable("graphspace") String graphSpace,
                      @PathVariable("graph") String graph,
                      @RequestBody ConflictDetail detail) {
        HugeClient client = this.authClient(graphSpace, graph);
        this.elService.reuse(detail, client);
    }

    @PutMapping("{name}")
    public void update(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @PathVariable("name") String name,
                       @RequestBody EdgeLabelUpdateEntity entity) {
        Ex.check(!StringUtils.isEmpty(name),
                 "common.param.cannot-be-null-or-empty", name);
        entity.setName(name);
        HugeClient client = this.authClient(graphSpace, graph);

        this.elService.checkExist(name, client);
        checkParamsValid(this.pkService, entity, client);
        this.elService.update(entity, client);
    }

    /**
     * Delete edge label doesn't need check checkUsing
     */
    @DeleteMapping
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @RequestParam("names") List<String> names) {
        HugeClient client = this.authClient(graphSpace, graph);
        for (String name : names) {
            this.elService.checkExist(name, client);
            this.elService.remove(name, client);
        }
    }

    private void checkParamsValid(EdgeLabelEntity entity, HugeClient client,
                                  boolean checkCreateTime) {
        String name = entity.getName();
        Ex.check(name != null, "common.param.cannot-be-null", "name");
        Ex.check(Constant.SCHEMA_NAME_PATTERN.matcher(name).matches(),
                 "schema.edgelabel.unmatch-regex");
        Ex.check(checkCreateTime, () -> entity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");

        EdgeLabelType type = EdgeLabelType.valueOf(entity.getEdgeLabelType());
        // skip check for PARENT type
        if (type.parent()) {
            return ;
        }
        // Check source label and target label
        checkRelation(entity, client);
        // Check properties
        checkProperties(this.pkService, entity.getProperties(), false, client);
        // Check sort keys
        checkSortKeys(entity);
        // Check property index
        checkPropertyIndexes(entity, client);
        // Check display fields and join symbols
        checkDisplayFields(entity);
    }

    private void checkRelation(EdgeLabelEntity entity, HugeClient client) {
        String sourceLabel = entity.getSourceLabel();
        String targetLabel = entity.getTargetLabel();
        Ex.check(!StringUtils.isEmpty(sourceLabel),
                 "common.param.cannot-be-null-or-empty",
                 "edgelabel.source_label");
        Ex.check(!StringUtils.isEmpty(targetLabel),
                 "common.param.cannot-be-null-or-empty",
                 "edgelabel.target_label");

        this.vlService.checkExist(sourceLabel, client);
        this.vlService.checkExist(targetLabel, client);
    }

    private void checkSortKeys(EdgeLabelEntity entity) {
        List<String> sortKeys = entity.getSortKeys();
        if (entity.isLinkMultiTimes()) {
            Ex.check(!CollectionUtils.isEmpty(entity.getProperties()),
                     "schema.edgelabel.property.cannot-be-null-or-empty",
                     entity.getName());
            Ex.check(!CollectionUtils.isEmpty(sortKeys),
                     "schema.edgelabel.sortkey.cannot-be-null-or-empty",
                     entity.getName());
            // All sort keys must belong to properties
            Set<String> propNames = entity.getPropNames();
            Ex.check(propNames.containsAll(sortKeys),
                     "schema.edgelabel.sortkey.must-belong-to.property",
                     entity.getName(), sortKeys, propNames);
            // Any sort key can't be nullable
            Ex.check(!CollectionUtil.hasIntersection(sortKeys,
                                                     entity.getNullableProps()),
                     "schmea.edgelabel.sortkey.cannot-be-nullable",
                     entity.getName());
        } else {
            Ex.check(CollectionUtils.isEmpty(sortKeys),
                     "schema.edgelabel.sortkey.should-be-null-or-empty",
                     entity.getName());
        }
    }

    /**
     * TODO：merge with VertexLabelController.checkDisplayFields
     */
    private static void checkDisplayFields(EdgeLabelEntity entity) {
        EdgeLabelStyle style = entity.getStyle();
        List<String> displayFields = style.getDisplayFields();
        if (!CollectionUtils.isEmpty(displayFields)) {
            Set<String> nullableProps = entity.getNullableProps();
            Ex.check(!CollectionUtil.hasIntersection(displayFields,
                                                     nullableProps),
                     "schema.display-fields.cannot-be-nullable");
        }
    }

    private void checkEntityUnique(EdgeLabelEntity newEntity, HugeClient client,
                                   boolean creating) {
        // The name must be unique
        String name = newEntity.getName();
        this.elService.checkNotExist(name, client);
    }
}
