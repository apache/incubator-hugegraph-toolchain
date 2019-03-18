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
import java.util.List;

import com.baidu.hugegraph.driver.GraphManager;
import com.baidu.hugegraph.driver.HugeClient;
import com.baidu.hugegraph.driver.SchemaManager;
import com.baidu.hugegraph.driver.TaskManager;

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
}
