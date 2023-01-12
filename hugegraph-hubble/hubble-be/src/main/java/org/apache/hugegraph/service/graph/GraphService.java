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

package org.apache.hugegraph.service.graph;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.graph.EdgeEntity;
import org.apache.hugegraph.entity.graph.VertexEntity;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.PropertyKeyEntity;
import org.apache.hugegraph.entity.schema.SchemaLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.file.ListFormat;
import org.apache.hugegraph.loader.util.DataTypeUtil;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.util.Ex;
import com.google.common.collect.ImmutableSet;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class GraphService {

    @Autowired
    private HugeClientPoolService poolService;
    @Autowired
    private PropertyKeyService pkService;
    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;

    public HugeClient client(int connId) {
        return this.poolService.getOrCreate(connId);
    }

    public GraphView addVertex(int connId, VertexEntity entity) {
        HugeClient client = this.client(connId);
        Vertex vertex = this.buildVertex(connId, entity);
        vertex = client.graph().addVertex(vertex);
        return GraphView.builder()
                        .vertices(ImmutableSet.of(vertex))
                        .edges(ImmutableSet.of())
                        .build();
    }

    public Vertex updateVertex(int connId, VertexEntity entity) {
        HugeClient client = this.client(connId);
        GraphManager graph = client.graph();
        Vertex vertex = this.buildVertex(connId, entity);
        // TODO: client should add updateVertex() method
        return graph.addVertex(vertex);
    }

    private Vertex buildVertex(int connId, VertexEntity entity) {
        Vertex vertex = new Vertex(entity.getLabel());
        VertexLabelEntity vl = this.vlService.get(entity.getLabel(), connId);
        // Allowed front-end always pass id
        if (vl.getIdStrategy().isCustomize()) {
            Object vid = this.convertVertexId(vl.getIdStrategy(), entity.getId());
            vertex.id(vid);
        }
        this.fillProperties(connId, vl, vertex, entity.getProperties());
        return vertex;
    }

    public GraphView addEdge(int connId, EdgeEntity entity) {
        HugeClient client = this.client(connId);
        GraphManager graph = client.graph();

        EdgeHolder edgeHolder = this.buildEdge(connId, entity);
        Edge edge = graph.addEdge(edgeHolder.edge);
        Vertex source = edgeHolder.source;
        Vertex target = edgeHolder.target;
        return GraphView.builder()
                        .vertices(ImmutableSet.of(source, target))
                        .edges(ImmutableSet.of(edge))
                        .build();
    }

    public Edge updateEdge(int connId, EdgeEntity entity) {
        HugeClient client = this.client(connId);
        GraphManager graph = client.graph();
        EdgeHolder edgeHolder = this.buildEdge(connId, entity);
        // TODO: client should add updateEdge()
        return graph.addEdge(edgeHolder.edge);
    }

    private EdgeHolder buildEdge(int connId, EdgeEntity entity) {
        HugeClient client = this.client(connId);
        GraphManager graph = client.graph();
        EdgeLabelEntity el = this.elService.get(entity.getLabel(), connId);
        VertexLabelEntity sourceVl = this.vlService.get(el.getSourceLabel(),
                                                        connId);
        VertexLabelEntity targetVl = this.vlService.get(el.getTargetLabel(),
                                                        connId);
        Object realSourceId = this.convertVertexId(sourceVl.getIdStrategy(),
                                                   entity.getSourceId());
        Object realTargetId = this.convertVertexId(targetVl.getIdStrategy(),
                                                   entity.getTargetId());
        Vertex sourceVertex = graph.getVertex(realSourceId);
        Vertex targetVertex = graph.getVertex(realTargetId);

        Ex.check(el.getSourceLabel().equals(sourceVertex.label()) &&
                 el.getTargetLabel().equals(targetVertex.label()),
                 "graph.edge.link-unmatched-vertex", entity.getLabel(),
                 el.getSourceLabel(), el.getTargetLabel(),
                 sourceVertex.label(), targetVertex.label());

        Edge edge = new Edge(entity.getLabel());
        edge.source(sourceVertex);
        edge.target(targetVertex);
        this.fillProperties(connId, el, edge, entity.getProperties());
        return new EdgeHolder(edge, sourceVertex, targetVertex);
    }

    private Object convertVertexId(IdStrategy idStrategy, String rawId) {
        if (idStrategy.isCustomizeString() || idStrategy.isPrimaryKey()) {
            return rawId;
        } else if (idStrategy.isCustomizeNumber()) {
            return DataTypeUtil.parseNumber("id", rawId);
        } else {
            assert idStrategy.isCustomizeUuid();
            return DataTypeUtil.parseUUID("id", rawId);
        }
    }

    private void fillProperties(int connId, SchemaLabelEntity schema,
                                GraphElement element,
                                Map<String, Object> properties) {
        HugeClient client = this.client(connId);
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            // Skip nullable property
            if (schema.getNullableProps().contains(key)) {
                if (rawValue instanceof String &&
                    StringUtils.isEmpty((String) rawValue)) {
                    continue;
                }
            }
            PropertyKeyEntity pkEntity = this.pkService.get(key, connId);
            PropertyKey propertyKey = PropertyKeyService.convert(pkEntity,
                                                                 client);
            assert propertyKey != null;
            Object value;
            try {
                // DataTypeUtil.convert in loader need param InputSource
                FileSource source = new FileSource();
                ListFormat listFormat = new ListFormat("", "", ",");
                source.listFormat(listFormat);
                value = DataTypeUtil.convert(rawValue, propertyKey, source);
            } catch (IllegalArgumentException e) {
                throw new ExternalException("graph.property.convert.failed",
                                            e, key, rawValue);
            }
            element.property(key, value);
        }
    }

    private static class EdgeHolder {

        private Edge edge;
        private Vertex source;
        private Vertex target;

        public EdgeHolder(Edge edge, Vertex source, Vertex target) {
            this.edge = edge;
            this.source = source;
            this.target = target;
        }
    }
}
