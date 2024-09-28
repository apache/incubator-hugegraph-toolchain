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

//import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.schema.vertexlabel.ParamEntity;
import org.apache.hugegraph.entity.schema.vertexlabel.ParamStyle;
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
@RequestMapping(Constant.API_V1_3 + "graphspaces/{graphspace}/graphs" +
        "/{graph}/schema/vertexlabels")
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
    public List<String> getOptionalColors() {
        return PRESET_COLORS;
    }

    @GetMapping("{name}/link")
    public List<String> getLinkEdgeLabels(@PathVariable("graphspace") String graphSpace,
                                          @PathVariable("graph") String graph,
                                          @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, graph);
        this.vlService.checkExist(name, client);
        return this.vlService.getLinkEdgeLabels(name, client);
    }

    @GetMapping
    public IPage<VertexLabelEntity> list(@PathVariable("graphspace") String graphSpace,
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
        return this.listInPage(c -> this.vlService.list(c),
                               client, content, nameOrder, pageNo, pageSize);
    }

    @GetMapping("{name}")
    public VertexLabelEntity get(@PathVariable("graphspace") String graphSpace,
                                 @PathVariable("graph") String graph,
                                 @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.vlService.get(name, client);
    }

    @PostMapping
    public void create(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @RequestBody VertexLabelEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);

        this.checkParamsValid(entity, client, true);
        this.checkEntityUnique(entity, client, true);
        entity.setCreateTime(HubbleUtil.nowDate());
        this.vlService.add(entity, client);
    }

    @PostMapping("check_conflict")
    public ConflictDetail checkConflicts(
                          @PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestParam("reused_graphspace") String reusedGraphSpace,
                          @RequestParam("reused_graph") String reusedGraph,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.cannot-be-empty", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getPkEntities()),
                 "common.param.must-be-null", "propertykeys");
        Ex.check(CollectionUtils.isEmpty(entity.getPiEntities()),
                 "common.param.must-be-null", "propertyindexes");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");
        Ex.check(graphSpace != reusedGraphSpace && graph != reusedGraph,
                 "schema.conn.cannot-reuse-self");

        Set<String> pkNames = new HashSet<>();
        Set<String> piNames = new HashSet<>();
        for (VertexLabelEntity e : entity.getVlEntities()) {
            pkNames.addAll(e.getPropNames());
            piNames.addAll(e.getIndexProps());
        }

        HugeClient client = this.authClient(graphSpace, graph);
        HugeClient reusedClient = this.authClient(reusedGraphSpace, reusedGraph);

        entity.setPkEntities(this.pkService.list(pkNames, reusedClient, false));
        entity.setPiEntities(this.piService.list(piNames, reusedClient, false));
        return this.vlService.checkConflict(entity, client, false);
    }

    @PostMapping("recheck_conflict")
    public ConflictDetail recheckConflicts(
                          @PathVariable("graphspace") String graphSpace,
                          @PathVariable("graph") String graph,
                          @RequestBody ConflictCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getVlEntities()),
                 "common.param.cannot-be-empty", "vertexlabels");
        Ex.check(CollectionUtils.isEmpty(entity.getElEntities()),
                 "common.param.must-be-null", "edgelabels");

        HugeClient client = this.authClient(graphSpace, graph);
        return this.vlService.checkConflict(entity, client, true);
    }

    @PostMapping("reuse")
    public void reuse(@PathVariable("graphspace") String graphSpace,
                      @PathVariable("graph") String graph,
                      @RequestBody ConflictDetail detail) {
        HugeClient client = this.authClient(graphSpace, graph);
        this.vlService.reuse(detail, client);
    }

    @PutMapping("{name}")
    public void update(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @PathVariable("name") String name,
                       @RequestBody VertexLabelUpdateEntity entity) {
        Ex.check(!StringUtils.isEmpty(name),
                 "common.param.cannot-be-null-or-empty", name);
        entity.setName(name);
        HugeClient client = this.authClient(graphSpace, graph);

        this.vlService.checkExist(name, client);
        checkParamsValid(this.pkService, entity, client);
        this.vlService.update(entity, client);
    }

    @PostMapping("check_using")
    public Map<String, Boolean> checkUsing(
                                @PathVariable("graphspace") String graphSpace,
                                @PathVariable("graph") String graph,
                                @RequestBody UsingCheckEntity entity) {
        Ex.check(!CollectionUtils.isEmpty(entity.getNames()),
                 "common.param.cannot-be-empty", "names");
        Map<String, Boolean> inUsing = new LinkedHashMap<>();
        HugeClient client = this.authClient(graphSpace, graph);
        for (String name : entity.getNames()) {
            this.vlService.checkExist(name, client);
            inUsing.put(name, this.vlService.checkUsing(name, client));
        }
        return inUsing;
    }

    @DeleteMapping
    public void delete(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @RequestParam("names") List<String> names,
                       @RequestParam(name = "skip_using",
                                     defaultValue = "false")
                       boolean skipUsing) {
        HugeClient client = this.authClient(graphSpace, graph);
        for (String name : names) {
            this.vlService.checkExist(name, client);
            if (this.vlService.checkUsing(name, client)) {
                if (skipUsing) {
                    continue;
                } else {
                    throw new ExternalException("schema.vertexlabel.in-using",
                                                name);
                }
            }
            this.vlService.remove(name, client);
        }
    }

    private void checkParamsValid(VertexLabelEntity entity, HugeClient client,
                                  boolean checkCreateTime) {
        String name = entity.getName();
        Ex.check(name != null, "common.param.cannot-be-null", "name");
        Ex.check(Constant.SCHEMA_NAME_PATTERN.matcher(name).matches(),
                 "schema.vertexlabel.unmatch-regex");
        Ex.check(checkCreateTime, () -> entity.getCreateTime() == null,
                 "common.param.must-be-null", "create_time");
        // Check properties
        checkProperties(this.pkService, entity.getProperties(), false, client);
        // Check primary keys
        checkPrimaryKeys(entity);
        // Check property index
        checkPropertyIndexes(entity, client);
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

    private void checkEntityUnique(VertexLabelEntity newEntity,
                                   HugeClient client,
                                   boolean creating) {
        // The name must be unique
        String name = newEntity.getName();
        this.vlService.checkNotExist(name, client);
    }

    @GetMapping("{name}/new")
    public ParamEntity getNew(@PathVariable("graphspace") String graphSpace,
                                 @PathVariable("graph") String graph,
                                 @PathVariable("name") String name) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.labelDataToParam(this.vlService.get(name, client));
    }

    @PutMapping("{name}/new")
    public void updateNew(@PathVariable("graphspace") String graphSpace,
                       @PathVariable("graph") String graph,
                       @PathVariable("name") String name,
                       @RequestBody VertexLabelUpdateEntity entity) {
        Ex.check(!StringUtils.isEmpty(name),
                "common.param.cannot-be-null-or-empty", name);
        entity.setName(name);
        HugeClient client = this.authClient(graphSpace, graph);

        this.vlService.checkExist(name, client);
        checkParamsValid(this.pkService, entity, client);
        this.vlService.update(entity, client);
    }

    @PostMapping("create_new")
    public void createNew(@PathVariable("graphspace") String graphSpace,
                            @PathVariable("graph") String graph,
                            @RequestBody ParamEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);

        VertexLabelEntity formatEntity = this.paramToLabelData(entity);

        this.checkParamsValid(formatEntity, client, true);
        this.checkEntityUnique(formatEntity, client, true);
        formatEntity.setCreateTime(HubbleUtil.nowDate());
        this.vlService.add(formatEntity, client);
    }

    private VertexLabelEntity paramToLabelData(ParamEntity entity) {
        List<String> displayFields = entity.getDisplayFields();

        if (!CollectionUtils.isEmpty(displayFields)) {
            displayFields.add("~id");
        } else {
            displayFields = ImmutableList.of("~id");
        }

        VertexLabelStyle style = new VertexLabelStyle();
        style.setColor(entity.getStyle().getColor());
        style.setSize(entity.getStyle().getSize());
        style.setDisplayFields(displayFields);

        VertexLabelEntity newEntity = new VertexLabelEntity();
        newEntity.setIdStrategy(entity.getIdStrategy());
        newEntity.setName(entity.getName());
        newEntity.setProperties(entity.getProperties());
        newEntity.setPropertyIndexes(entity.getPropertyIndexes());
        newEntity.setOpenLabelIndex(entity.getOpenLabelIndex());
        newEntity.setPrimaryKeys(entity.getPrimaryKeys());
        newEntity.setStyle(style);

        return newEntity;
    }

    // format backend data to front
    private ParamEntity labelDataToParam(VertexLabelEntity entity) {
//        Logger Logger = LoggerFactory.getLogger(getClass());

        ParamStyle style = new ParamStyle();
        style.setColor(entity.getStyle().getColor());
        style.setSize(entity.getStyle().getSize());

        List<String> displayFields = entity.getStyle().getDisplayFields().stream()
                .filter(v -> !("~id".equals(v))).collect(Collectors.toList());

        ParamEntity newEntity = new ParamEntity();
        newEntity.setIdStrategy(entity.getIdStrategy());
        newEntity.setName(entity.getName());
        newEntity.setProperties(entity.getProperties());
        newEntity.setPropertyIndexes(entity.getPropertyIndexes());
        newEntity.setOpenLabelIndex(entity.getOpenLabelIndex());
        newEntity.setPrimaryKeys(entity.getPrimaryKeys());
        newEntity.setStyle(style);
        newEntity.setDisplayFields(displayFields);

        return newEntity;
    }
}
