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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.hugegraph.common.OptionType;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.loader.util.JsonUtil;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import org.apache.hugegraph.common.Constant;
import org.apache.hugegraph.controller.BaseController;
import org.apache.hugegraph.entity.graph.EdgeEntity;
import org.apache.hugegraph.entity.graph.VertexEntity;
import org.apache.hugegraph.entity.graph.VertexQueryEntity;
import org.apache.hugegraph.entity.query.ElementEditHistory;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.service.graph.GraphService;
import org.apache.hugegraph.service.query.EditElementHistoryService;
import org.apache.hugegraph.service.schema.EdgeLabelService;
import org.apache.hugegraph.service.schema.VertexLabelService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(Constant.API_VERSION + "graphspaces/{graphspace}/graphs" +
        "/{graph}")
public class GraphController extends BaseController {

    @Autowired
    private VertexLabelService vlService;
    @Autowired
    private EdgeLabelService elService;
    @Autowired
    private GraphService graphService;
    @Autowired
    private EditElementHistoryService editEleHisService;

    @PostMapping("vertex")
    public GraphView addVertex(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @RequestBody VertexEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);
        GraphView graphView = this.graphService.addVertex(client, entity);

        assert graphView.getVertices().size() == 1;
        VertexQueryEntity v = graphView.getVertices().iterator().next();
        String vertexId = v.id().toString();
        addEditEleHistory(graphSpace, graph, vertexId, entity.getLabel(),
                          v.properties().size(),
                          OptionType.ADD, JsonUtil.toJson(entity));
        return graphView;
    }

    @PutMapping("vertex/{id}")
    public Vertex updateVertex(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @PathVariable("id") String vertexId,
                               @RequestBody VertexEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);
        vertexId = UriUtils.decode(vertexId, Constant.CHARSET);
        this.checkIdSameAsBody(vertexId, entity);
        Vertex result =
                this.graphService.updateVertex(client, vertexId, entity);
        addEditEleHistory(graphSpace, graph, vertexId,
                          entity.getLabel(), result.properties().size(),
                          OptionType.UPDATE, JsonUtil.toJson(entity));
        return result;
    }

    @DeleteMapping("vertex/{id}")
    public void deleteVertex(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @PathVariable("id") String vertexId) {
        HugeClient client = this.authClient(graphSpace, graph);
        vertexId = UriUtils.decode(vertexId, Constant.CHARSET);
        Vertex vertex = client.graph().getVertex(vertexId);
        String label = vertex.label();

        this.graphService.deleteVertex(client, vertexId);
        addEditEleHistory(graphSpace, graph, vertexId, label,
                          vertex.properties().size(), OptionType.DELETE, "");
    }

    @PostMapping("edge")
    public GraphView addEdge(@PathVariable("graphspace") String graphSpace,
                             @PathVariable("graph") String graph,
                             @RequestBody EdgeEntity entity) {
        HugeClient client = this.authClient(graphSpace, graph);
        GraphView edge = this.graphService.addEdge(client, entity);
        assert edge.getEdges().size() == 1;
        Edge e = edge.getEdges().iterator().next();
        String edgeId = e.id().toString();
        addEditEleHistory(graphSpace, graph,
                          edgeId, entity.getLabel(),
                          e.properties().size(), OptionType.ADD,
                          JsonUtil.toJson(entity));
        return edge;
    }

    @PutMapping("edge/{id}")
    public Edge updateEdge(@PathVariable("graphspace") String graphSpace,
                           @PathVariable("graph") String graph,
                           @PathVariable("id") String edgeId,
                           @RequestBody EdgeEntity entity) {
        edgeId = UriUtils.decode(edgeId, Constant.CHARSET);
        HugeClient client = this.authClient(graphSpace, graph);
        this.checkIdSameAsBody(edgeId, entity);
        Edge edge = this.graphService.updateEdge(client, edgeId, entity);
        addEditEleHistory(graphSpace, graph, edgeId, entity.getLabel(),
                          edge.properties().size(), OptionType.UPDATE,
                          JsonUtil.toJson(entity));
        return edge;
    }

    @DeleteMapping("edge/{id}")
    public void deleteEdge(@PathVariable("graphspace") String graphSpace,
                           @PathVariable("graph") String graph,
                           @PathVariable("id") String edgeId) {
        edgeId = UriUtils.decode(edgeId, Constant.CHARSET);
        HugeClient client = this.authClient(graphSpace, graph);
        Edge edge = client.graph().getEdge(edgeId);
        String label = edge.label();

        this.graphService.deleteEdge(client, edgeId);
        addEditEleHistory(graphSpace, graph, edgeId, label,
                          edge.properties().size(), OptionType.DELETE, "");
    }

    @DeleteMapping("element/batch")
    public void deleteElements(@PathVariable("graphspace") String graphSpace,
                               @PathVariable("graph") String graph,
                               @RequestParam(name = "type",
                                             required = true) String type,
                               @RequestParam(name = "ids", required = true)
                               List<String> elementIds) {
        HugeClient client = this.authClient(graphSpace, graph);
        ArrayList<ElementEditHistory> list = new ArrayList<>();
        HashSet<String> set = new HashSet<>(elementIds);
        if ("VERTEX".equals(type)) {
            for (String vertexId : set) {
                vertexId = UriUtils.decode(vertexId, Constant.CHARSET);
                Vertex vertex = client.graph().getVertex(vertexId);
                String label = vertex.label();
                this.graphService.deleteVertex(client, vertexId);
                list.add(getEditEleHistory(graphSpace, graph, vertexId, label,
                                  vertex.properties().size(), OptionType.DELETE, ""));
            }
        } else if ("EDGE".equals(type)) {
            for (String edgeId : elementIds) {
                edgeId = UriUtils.decode(edgeId, Constant.CHARSET);
                Edge edge = client.graph().getEdge(edgeId);
                String label = edge.label();
                this.graphService.deleteEdge(client, edgeId);
                list.add(getEditEleHistory(graphSpace, graph, edgeId, label,
                                           edge.properties().size(),
                                           OptionType.DELETE, ""));
            }
        } else {
            throw new IllegalArgumentException(
                    "type must in [VERTEX, EDGE], but got '" + type + "'");
        }
        editEleHisService.add(list);
    }

    @GetMapping("edgelabel/{label}")
    public HashMap<String, Object> getEdgeProperties(@PathVariable("graphspace") String graphSpace,
                                                     @PathVariable("graph") String graph,
                                                     @PathVariable("label") String label) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.graphService.getEdgeProperties(client, label);
    }

    @GetMapping("vertexlabel/{label}")
    public HashMap<String, Object> getVertexProperties(@PathVariable("graphspace") String graphSpace,
                                                       @PathVariable("graph") String graph,
                                                       @PathVariable("label") String label) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.graphService.getVertexProperties(client, label);
    }
    @PostMapping("edgestype")
    public HashMap<String, Object> getEdgeStyle(@PathVariable("graphspace") String graphSpace,
                                                @PathVariable("graph") String graph,
                                                @RequestBody List<String> labels) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.graphService.getEdgeStyle(client, labels);
    }

    @PostMapping("vertexstyle")
    public HashMap<String, Object> getVertexStyle(@PathVariable("graphspace") String graphSpace,
                                                  @PathVariable("graph") String graph,
                                                  @RequestBody List<String> labels) {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.graphService.getVertexStyle(client, labels);
    }


    @PostMapping("import")
    public GraphView importJson(@PathVariable("graphspace") String graphSpace,
                                @PathVariable("graph") String graph,
                                @RequestParam("file") MultipartFile jsonFile) throws IOException {
        HugeClient client = this.authClient(graphSpace, graph);
        return this.graphService.importJson(client, jsonFile);
    }

    private void addEditEleHistory(String graphSpace,
                                   String graph,
                                   String elementId,
                                   String label,
                                   int propertyNum,
                                   OptionType optionType,
                                   String content) {
        this.editEleHisService.add(graphSpace, graph, elementId, label,
                                   propertyNum, optionType.name(),
                                   new Date(), getUser(), content);
    }

    private ElementEditHistory getEditEleHistory(String graphSpace,
                                                 String graph,
                                                 String elementId,
                                                 String label,
                                                 int propertyNum,
                                                 OptionType optionType,
                                                 String content){
        return ElementEditHistory.builder()
                                 .graphspace(graphSpace)
                                 .graph(graph)
                                 .elementId(elementId)
                                 .label(label)
                                 .propertyNum(propertyNum)
                                 .optionType(optionType.name())
                                 .optionTime(new Date())
                                 .optionPerson(getUser())
                                 .content(content)
                                 .build();
    }
}
