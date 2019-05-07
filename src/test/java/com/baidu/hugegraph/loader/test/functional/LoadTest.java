/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.loader.test.functional;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.driver.TaskManager;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.graph.Edge;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.util.E;

public class LoadTest {

    protected static final String CONFIG_PATH_PREFIX = "target/test-classes";
    protected static final String GRAPH = "hugegraph";
    protected static final String SERVER = "127.0.0.1";
    protected static final String PORT = "8080";
    protected static final String URL = String.format("http://%s:%s",
                                                      SERVER, PORT);
    protected static final HugeClient CLIENT = new HugeClient(URL, GRAPH);

    public static String configPath(String fileName) {
        return Paths.get(CONFIG_PATH_PREFIX, fileName).toString();
    }

    public static void clearServerData() {
        SchemaManager schema = CLIENT.schema();
        GraphManager graph = CLIENT.graph();
        TaskManager task = CLIENT.task();
        // Clear edge
        graph.listEdges().forEach(e -> graph.removeEdge(e.id()));
        // Clear vertex
        graph.listVertices().forEach(v -> graph.removeVertex(v.id()));

        // Clear schema
        List<Long> taskIds = new ArrayList<>();
        schema.getIndexLabels().forEach(il -> {
            taskIds.add(schema.removeIndexLabelAsync(il.name()));
        });
        taskIds.forEach(id -> task.waitUntilTaskCompleted(id, 5L));
        taskIds.clear();
        schema.getEdgeLabels().forEach(el -> {
            taskIds.add(schema.removeEdgeLabelAsync(el.name()));
        });
        taskIds.forEach(id -> task.waitUntilTaskCompleted(id, 5L));
        taskIds.clear();
        schema.getVertexLabels().forEach(vl -> {
            taskIds.add(schema.removeVertexLabelAsync(vl.name()));
        });
        taskIds.forEach(id -> task.waitUntilTaskCompleted(id, 5L));
        taskIds.clear();
        schema.getPropertyKeys().forEach(pk -> {
            schema.removePropertyKey(pk.name());
        });
    }

    protected static void assertContains(List<Vertex> vertices, String label,
                                         Object... keyValues) {
        boolean matched = false;
        for (Vertex v : vertices) {
            if (v.label().equals(label) &&
                v.properties().equals(toMap(keyValues))) {
                matched = true;
                break;
            }
        }
        Assert.assertTrue(matched);
    }

    protected static void assertContains(List<Edge> edges, String label,
                                         Object sourceId, Object targetId,
                                         String sourceLabel, String targetLabel,
                                         Object... keyValues) {
        boolean matched = false;
        for (Edge e : edges) {
            if (e.label().equals(label) &&
                e.sourceId().equals(sourceId) &&
                e.targetId().equals(targetId) &&
                e.sourceLabel().equals(sourceLabel) &&
                e.targetLabel().equals(targetLabel) &&
                e.properties().equals(toMap(keyValues))) {
                matched = true;
                break;
            }
        }
        Assert.assertTrue(matched);
    }

    private static Map<String, Object> toMap(Object... properties) {
        E.checkArgument((properties.length & 0x01) == 0,
                        "The number of properties must be even");
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < properties.length; i = i + 2) {
            if (!properties[i].equals(T.id) && !properties[i].equals(T.label)) {
                map.put((String) properties[i], properties[i + 1]);
            }
        }
        return map;
    }
}
