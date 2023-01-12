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

package org.apache.hugegraph.controller.graph;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.entity.graph.EdgeEntity;
import org.apache.hugegraph.entity.graph.VertexEntity;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.service.graph.GraphService;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.Ex;

@RestController
@RequestMapping(Constant.API_VERSION + "graph-connections/{connId}/graph")
public class GraphController extends BaseController {

    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;
    @Autowired
    private GraphService graphService;

    @PostMapping("vertex")
    public GraphView addVertex(@PathVariable("connId") int connId,
                               @RequestBody VertexEntity entity) {
        this.checkParamsValid(connId, entity, true);
        return this.graphService.addVertex(connId, entity);
    }

    @PutMapping("vertex/{id}")
    public Vertex updateVertex(@PathVariable("connId") int connId,
                               @PathVariable("id") String vertexId,
                               @RequestBody VertexEntity entity) {
        vertexId = UriUtils.decode(vertexId, Constant.CHARSET);
        this.checkParamsValid(connId, entity, false);
        this.checkIdSameAsBody(vertexId, entity);
        return this.graphService.updateVertex(connId, entity);
    }

    @PostMapping("edge")
    public GraphView addEdge(@PathVariable("connId") int connId,
                             @RequestBody EdgeEntity entity) {
        this.checkParamsValid(connId, entity, true);
        return this.graphService.addEdge(connId, entity);
    }

    @PutMapping("edge/{id}")
    public Edge updateEdge(@PathVariable("connId") int connId,
                           @PathVariable("id") String edgeId,
                           @RequestBody EdgeEntity entity) {
        edgeId = UriUtils.decode(edgeId, Constant.CHARSET);
        this.checkParamsValid(connId, entity, false);
        this.checkIdSameAsBody(edgeId, entity);
        return this.graphService.updateEdge(connId, entity);
    }

    private void checkParamsValid(int connId, VertexEntity entity,
                                  boolean create) {
        Ex.check(!StringUtils.isEmpty(entity.getLabel()),
                 "common.param.cannot-be-null-or-empty", "label");
        // If schema doesn't exist, it will throw exception
        VertexLabelEntity vlEntity = this.vlService.get(entity.getLabel(),
                                                        connId);
        IdStrategy idStrategy = vlEntity.getIdStrategy();
        if (create) {
            Ex.check(idStrategy.isCustomize(), () -> entity.getId() != null,
                     "common.param.cannot-be-null", "id");
        } else {
            Ex.check(entity.getId() != null,
                     "common.param.cannot-be-null", "id");
        }

        Set<String> nonNullableProps = vlEntity.getNonNullableProps();
        Map<String, Object> properties = entity.getProperties();
        Ex.check(properties.keySet().containsAll(nonNullableProps),
                 "graph.vertex.all-nonnullable-prop.should-be-setted");
    }

    private void checkParamsValid(int connId, EdgeEntity entity,
                                  boolean create) {
        Ex.check(!StringUtils.isEmpty(entity.getLabel()),
                 "common.param.cannot-be-null-or-empty", "label");
        // If schema doesn't exist, it will throw exception
        EdgeLabelEntity elEntity = this.elService.get(entity.getLabel(), connId);
        if (create) {
            Ex.check(entity.getId() == null,
                     "common.param.must-be-null", "id");
        } else {
            Ex.check(entity.getId() != null,
                     "common.param.cannot-be-null", "id");
        }
        Ex.check(entity.getSourceId() != null,
                 "common.param.must-be-null", "source_id");
        Ex.check(entity.getTargetId() != null,
                 "common.param.must-be-null", "target_id");

        Set<String> nonNullableProps = elEntity.getNonNullableProps();
        Map<String, Object> properties = entity.getProperties();
        Ex.check(properties.keySet().containsAll(nonNullableProps),
                 "graph.edge.all-nonnullable-prop.should-be-setted");
    }
}
