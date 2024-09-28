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

import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hugegraph.driver.GraphManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.entity.graph.EdgeEntity;
import org.apache.hugegraph.entity.graph.VertexEntity;
import org.apache.hugegraph.entity.graph.VertexQueryEntity;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.schema.EdgeLabelEntity;
import org.apache.hugegraph.entity.schema.PropertyKeyEntity;
import org.apache.hugegraph.entity.schema.SchemaLabelEntity;
import org.apache.hugegraph.entity.schema.VertexLabelEntity;
import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.loader.source.file.FileSource;
import org.apache.hugegraph.loader.source.file.ListFormat;
import org.apache.hugegraph.loader.util.DataTypeUtil;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.service.auth.UserService;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.PropertyKeyService;
import org.apache.hugegraph.service.schema.VertexLabelService;
import org.apache.hugegraph.structure.GraphElement;
import org.apache.hugegraph.structure.constant.IdStrategy;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.util.Ex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Log4j2
@Service
public class GraphService {

    @Autowired
    private PropertyKeyService pkService;
    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;

    @Autowired
    private UserService userService;
    public GraphView addVertex(HugeClient client, VertexEntity entity) {
        this.checkParamsValid(client, entity, true);
        Vertex vertex = this.buildVertex(client, entity);
        vertex = client.graph().addVertex(vertex);
        return GraphView.builder()
                        .vertices(ImmutableSet.of(
                                VertexQueryEntity.fromVertex(vertex)))
                        .edges(ImmutableSet.of())
                        .build();
    }

    public Vertex updateVertex(HugeClient client, String vertexId, VertexEntity entity) {
        this.checkParamsValid(client, entity, false);
        GraphManager graph = client.graph();
        Vertex vertex = this.buildVertex(client, entity);
        return graph.updateVertexProperty(vertexId, vertex);
    }

    public void deleteVertex(HugeClient client, Object vertexId) {
        client.graph().deleteVertex(vertexId);
    }

    private Vertex buildVertex(HugeClient client, VertexEntity entity) {
        Vertex vertex = new Vertex(entity.getLabel());
        VertexLabelEntity vl = this.vlService.get(entity.getLabel(), client);
        // Allowed front-end always pass id
        if (vl.getIdStrategy().isCustomize()) {
            Object vid = this.convertVertexId(vl.getIdStrategy(), entity.getId());
            vertex.id(vid);
        }
        this.fillProperties(client, vl, vertex, entity.getProperties());
        return vertex;
    }

    public GraphView addEdge(HugeClient client, EdgeEntity entity) {
        this.checkParamsValid(client, entity, true);
        GraphManager graph = client.graph();

        EdgeHolder edgeHolder = this.buildEdge(client, entity);
        Edge edge = graph.addEdge(edgeHolder.edge);
        Vertex source = edgeHolder.source;
        Vertex target = edgeHolder.target;
        return GraphView.builder()
                        .vertices(ImmutableSet.of(
                                VertexQueryEntity.fromVertex(source),
                                VertexQueryEntity.fromVertex(target)))
                        .edges(ImmutableSet.of(edge))
                        .build();
    }

    public void deleteEdge(HugeClient client, String edgeId) {
        client.graph().deleteEdge(edgeId);
    }

    public Edge updateEdge(HugeClient client, String edgeId, EdgeEntity entity) {
        this.checkParamsValid(client, entity, false);
        GraphManager graph = client.graph();
        EdgeHolder edgeHolder = this.buildEdge(client, entity);
        return graph.updateEdgeProperty(edgeId, edgeHolder.edge);
    }

    public HashMap<String, Object> getVertexProperties(HugeClient client, String label) {
        VertexLabelEntity vlEntity = this.vlService.get(label,
                client);
        HashMap<String, Object> vertexPropertiesMap= new HashMap<>();
        vertexPropertiesMap.put("nonNullableProps", vlEntity.getNonNullableProps());
        vertexPropertiesMap.put("NullableProps", vlEntity.getNullableProps());
        vertexPropertiesMap.put("primaryKeys", vlEntity.getPrimaryKeys());
        return vertexPropertiesMap;
    }

    public HashMap<String, Object> getEdgeProperties(HugeClient client, String label) {
        EdgeLabelEntity elEntity = this.elService.get(label,
                client);
        HashMap<String, Object> edgePropertiesMap= new HashMap<>();
        edgePropertiesMap.put("nonNullableProps", elEntity.getNonNullableProps());
        edgePropertiesMap.put("NullableProps", elEntity.getNullableProps());
        edgePropertiesMap.put("sortKeys", elEntity.getSortKeys());
        return edgePropertiesMap;
    }

    public HashMap<String, Object> getVertexStyle(HugeClient client, List<String> labels) {
        HashMap<String, Object> vertexStyles = new HashMap<>();
        for(String label : labels){
            VertexLabelEntity vlEntity = this.vlService.get(label,
                    client);
            vertexStyles.put(label, vlEntity.getStyle());
        }
        return vertexStyles;
    }

    public HashMap<String, Object> getEdgeStyle(HugeClient client, List<String> labels) {
        HashMap<String, Object> edgeStyles = new HashMap<>();
        for(String label : labels){
            EdgeLabelEntity elEntity = this.elService.get(label,
                    client);
            edgeStyles.put(label, elEntity.getStyle());
        }
        return edgeStyles;
    }

    public GraphView importJson(HugeClient client, MultipartFile jsonFile) throws IOException {
        File file = userService.multipartFileToFile(jsonFile);
        String content= FileUtils.readFileToString(file, "UTF-8");
        JSONObject jsonObject=new JSONObject(content);
        Map<String, Edge> edges = new HashMap<>();
        Map<Object, Vertex> vertices = new HashMap<>();
        if (jsonObject.has("vertices")) {
            JSONArray verticesArray = jsonObject.getJSONArray("vertices");
            List<VertexEntity> vertexEntities = JsonUtil.convertList(verticesArray.toString(), VertexEntity.class);
            for (VertexEntity entity : vertexEntities) {
                this.checkParamsValid(client, entity, true);
                Vertex vertex = this.buildVertex(client, entity);
                vertex = client.graph().addVertex(vertex);
                vertices.put(vertex.id(), vertex);
            }
        }
        if (jsonObject.has("edges")) {
            JSONArray edgesArray = jsonObject.getJSONArray("edges");
            List<EdgeEntity> edgeEntities = JsonUtil.convertList(edgesArray.toString(), EdgeEntity.class);
            for (EdgeEntity entity : edgeEntities) {
                this.checkParamsValid(client, entity, false);
                GraphManager graph = client.graph();
                EdgeHolder edgeHolder = this.buildEdge(client, entity);
                Edge edge = graph.addEdge(edgeHolder.edge);
                Vertex source = edgeHolder.source;
                Vertex target = edgeHolder.target;
                edges.put(edge.id(), edge);
            }
        }
        return GraphView.builder()
                .vertices(VertexQueryEntity.fromVertices(vertices.values()))
                .edges(edges.values())
                .build();
    }

    private void checkParamsValid(HugeClient client, VertexEntity entity,
                                  boolean create) {
        Ex.check(!StringUtils.isEmpty(entity.getLabel()),
                "common.param.cannot-be-null-or-empty", "label");
        // If schema doesn't exist, it will throw exception
        VertexLabelEntity vlEntity = this.vlService.get(entity.getLabel(),
                client);
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
        if (create) {
            Ex.check(properties.keySet().containsAll(nonNullableProps),
                    "graph.vertex.all-nonnullable-prop.should-be-setted");
        }
    }

    private void checkParamsValid(HugeClient client, EdgeEntity entity,
                                  boolean create) {
        Ex.check(!StringUtils.isEmpty(entity.getLabel()),
                "common.param.cannot-be-null-or-empty", "label");
        // If schema doesn't exist, it will throw exception
        EdgeLabelEntity elEntity = this.elService.get(entity.getLabel(), client);
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
        if (create) {
            Ex.check(properties.keySet().containsAll(nonNullableProps),
                    "graph.edge.all-nonnullable-prop.should-be-setted");
        }
    }

    private EdgeHolder buildEdge(HugeClient client, EdgeEntity entity) {
        GraphManager graph = client.graph();
        EdgeLabelEntity el = this.elService.get(entity.getLabel(), client);
        VertexLabelEntity sourceVl = this.vlService.get(el.getSourceLabel(),
                                                        client);
        VertexLabelEntity targetVl = this.vlService.get(el.getTargetLabel(),
                                                        client);
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
        this.fillProperties(client, el, edge, entity.getProperties());
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

    private void fillProperties(HugeClient client, SchemaLabelEntity schema,
                                GraphElement element,
                                Map<String, Object> properties) {
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
            PropertyKeyEntity pkEntity = this.pkService.get(key, client);
            PropertyKey propertyKey = PropertyKeyService.convert(pkEntity,
                                                                 client);
            assert propertyKey != null;
            Object value;
            try {
                // DataTypeUtil.convert in loader need param InputSource
                FileSource source = new FileSource();
                List<String> extraDateFormats = new ArrayList<>();
                extraDateFormats.add("yyyy-MM-dd HH:mm:ss.SSS");
                //source.extraDateFormats(extraDateFormats);//TODO C Deleted
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
