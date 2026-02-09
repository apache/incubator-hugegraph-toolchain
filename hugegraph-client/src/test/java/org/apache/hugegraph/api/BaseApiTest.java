/*
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

package org.apache.hugegraph.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.hugegraph.BaseClientTest;
import org.apache.hugegraph.api.graph.EdgeAPI;
import org.apache.hugegraph.api.graph.VertexAPI;
import org.apache.hugegraph.api.graphs.GraphsAPI;
import org.apache.hugegraph.api.job.RebuildAPI;
import org.apache.hugegraph.api.schema.EdgeLabelAPI;
import org.apache.hugegraph.api.schema.IndexLabelAPI;
import org.apache.hugegraph.api.schema.PropertyKeyAPI;
import org.apache.hugegraph.api.schema.SchemaAPI;
import org.apache.hugegraph.api.schema.VertexLabelAPI;
import org.apache.hugegraph.api.task.TaskAPI;
import org.apache.hugegraph.api.variables.VariablesAPI;
import org.apache.hugegraph.api.version.VersionAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.VersionUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class BaseApiTest extends BaseClientTest {

    protected static RestClient client;

    protected static VersionAPI versionAPI;
    protected static GraphsAPI graphsAPI;

    protected static PropertyKeyAPI propertyKeyAPI;
    protected static VertexLabelAPI vertexLabelAPI;
    protected static EdgeLabelAPI edgeLabelAPI;
    protected static IndexLabelAPI indexLabelAPI;
    protected static SchemaAPI schemaAPI;

    protected static VertexAPI vertexAPI;
    protected static EdgeAPI edgeAPI;

    protected static VariablesAPI variablesAPI;
    protected static TaskAPI taskAPI;
    protected static RebuildAPI rebuildAPI;

    protected static RestClient initClient() {
        client = new RestClient(BASE_URL, USERNAME, PASSWORD, TIMEOUT);
        client.setSupportGs(true);
        return client;
    }

    @BeforeClass
    public static void init() {
        BaseClientTest.init();
        if (client == null) {
            initClient();
        }

        versionAPI = new VersionAPI(client);
        client.apiVersion(VersionUtil.Version.of(versionAPI.get().get("api")));

        graphsAPI = new GraphsAPI(client, GRAPHSPACE);
        propertyKeyAPI = new PropertyKeyAPI(client, GRAPHSPACE, GRAPH);
        vertexLabelAPI = new VertexLabelAPI(client, GRAPHSPACE, GRAPH);
        edgeLabelAPI = new EdgeLabelAPI(client, GRAPHSPACE, GRAPH);
        indexLabelAPI = new IndexLabelAPI(client, GRAPHSPACE, GRAPH);
        schemaAPI = new SchemaAPI(client, GRAPHSPACE, GRAPH);

        vertexAPI = new VertexAPI(client, GRAPHSPACE, GRAPH);
        edgeAPI = new EdgeAPI(client, GRAPHSPACE, GRAPH);

        variablesAPI = new VariablesAPI(client, GRAPHSPACE, GRAPH);
        taskAPI = new TaskAPI(client, GRAPHSPACE, GRAPH);
        rebuildAPI = new RebuildAPI(client, GRAPHSPACE, GRAPH);
    }

    @AfterClass
    public static void clear() throws Exception {
        Assert.assertNotNull("Not opened client", client);

        clearData();
        client.close();
        client = null;

        BaseClientTest.clear();
    }

    protected static void clearData() {
        // Clear edge
        edgeAPI.list(-1).results().forEach(edge -> edgeAPI.delete(edge.id()));

        // Clear vertex
        vertexAPI.list(-1).results().forEach(vertex -> vertexAPI.delete(vertex.id()));

        // Clear schema (order matters: index -> edge -> vertex -> property)
        List<Long> ilTaskIds = new ArrayList<>();
        indexLabelAPI.list().forEach(il -> ilTaskIds.add(indexLabelAPI.delete(il.name())));
        ilTaskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        List<Long> elTaskIds = new ArrayList<>();
        edgeLabelAPI.list().forEach(el -> elTaskIds.add(edgeLabelAPI.delete(el.name())));
        elTaskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        List<Long> vlTaskIds = new ArrayList<>();
        vertexLabelAPI.list().forEach(vl -> vlTaskIds.add(vertexLabelAPI.delete(vl.name())));
        // Vertex label deletion may take longer, use extended timeout
        vlTaskIds.forEach(taskId -> waitUntilTaskCompleted(taskId, 30));

        List<Long> pkTaskIds = new ArrayList<>();
        propertyKeyAPI.list().forEach(pk -> pkTaskIds.add(propertyKeyAPI.delete(pk.name())));
        pkTaskIds.forEach(BaseApiTest::waitUntilTaskCompleted);

        // Clear all tasks (cancel running ones first)
        cleanupTasks();
    }

    protected static void cleanupTasks() {
        taskAPI.list(null, -1).forEach(task -> {
            if (!task.completed()) {
                try {
                    taskAPI.cancel(task.id());
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                    // Task may have completed during cancellation
                }
            }
            try {
                taskAPI.delete(task.id());
            } catch (Exception ignored) {
                // Task may have been deleted by another process
            }
        });
    }

    protected static void waitUntilTaskCompleted(long taskId) {
        if (taskId == 0L) {
            return;
        }
        taskAPI.waitUntilTaskSuccess(taskId, TIMEOUT);
    }

    protected static void waitUntilTaskCompleted(long taskId, long timeout) {
        if (taskId == 0L) {
            return;
        }
        try {
            taskAPI.waitUntilTaskSuccess(taskId, timeout);
        } catch (Exception e) {
            // Cleanup should be resilient - log warning but continue
            System.err.println("Warning: Task " + taskId +
                               " did not complete successfully: " + e.getMessage());
        }
    }

    protected RestClient client() {
        return client;
    }
}
