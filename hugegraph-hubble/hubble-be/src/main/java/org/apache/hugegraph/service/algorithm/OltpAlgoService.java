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

package org.apache.hugegraph.service.algorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.TraverserManager;
import org.apache.hugegraph.entity.algorithm.ShortestPath;
import org.apache.hugegraph.entity.enums.AsyncTaskStatus;
import org.apache.hugegraph.entity.enums.ExecuteStatus;
import org.apache.hugegraph.entity.enums.ExecuteType;
import org.apache.hugegraph.entity.query.ExecuteHistory;
import org.apache.hugegraph.entity.query.GraphView;
import org.apache.hugegraph.entity.query.GremlinResult;
import org.apache.hugegraph.entity.query.JsonView;
import org.apache.hugegraph.entity.query.TableView;
import org.apache.hugegraph.service.HugeClientPoolService;
import org.apache.hugegraph.service.query.ExecuteHistoryService;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Path;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.util.HubbleUtil;
import com.google.common.collect.ImmutableMap;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class OltpAlgoService {

    @Autowired
    private HugeClientPoolService poolService;
    @Autowired
    private ExecuteHistoryService historyService;

    private HugeClient getClient(int connId) {
        return this.poolService.getOrCreate(connId);
    }

    public GremlinResult shortestPath(int connId, ShortestPath body) {
        HugeClient client = this.getClient(connId);
        TraverserManager traverser = client.traverser();
        Path result = traverser.shortestPath(body.getSource(), body.getTarget(),
                                             body.getDirection(), body.getLabel(),
                                             body.getMaxDepth(), body.getMaxDegree(),
                                             body.getSkipDegree(), body.getCapacity());
        JsonView jsonView = new JsonView();
        jsonView.setData(result.objects());
        Date createTime = HubbleUtil.nowDate();
        TableView tableView = this.buildPathTableView(result);
        GraphView graphView = this.buildPathGraphView(result);
        // Insert execute history
        ExecuteStatus status = ExecuteStatus.SUCCESS;
        ExecuteHistory history;
        history = new ExecuteHistory(null, connId, 0L, ExecuteType.ALGORITHM,
                                     body.toString(), status,
                                     AsyncTaskStatus.UNKNOWN, -1L, createTime);
        this.historyService.save(history);
        return GremlinResult.builder()
                            .type(GremlinResult.Type.PATH)
                            .jsonView(jsonView)
                            .tableView(tableView)
                            .graphView(graphView)
                            .build();
    }

    private TableView buildPathTableView(Path result) {
        List<Object> elements = result.objects();
        List<Object> paths = new ArrayList<>(elements.size());
        List<Object> ids = new ArrayList<>();
        elements.forEach(element -> {
            if (element instanceof Vertex) {
                ids.add(((Vertex) element).id());
            } else if (element instanceof Edge) {
                ids.add(((Edge) element).id());
            } else {
                ids.add(element);
            }
        });
        paths.add(ImmutableMap.of("path", ids));
        return new TableView(TableView.PATH_HEADER, paths);
    }

    private GraphView buildPathGraphView(Path result) {
        Map<Object, Vertex> vertices = new HashMap<>();
        Map<String, Edge> edges = new HashMap<>();

        List<Object> elements = result.objects();
        for (Object element : elements) {
            if (element instanceof Vertex) {
                Vertex vertex = (Vertex) element;
                vertices.put(vertex.id(), vertex);
            } else if (element instanceof Edge) {
                Edge edge = (Edge) element;
                edges.put(edge.id(), edge);
            } else {
                return GraphView.EMPTY;
            }
        }
        return new GraphView(vertices.values(), new ArrayList<>());
    }
}
