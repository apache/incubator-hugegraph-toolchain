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

package org.apache.hugegraph.driver;

import org.apache.hugegraph.api.job.RebuildAPI;
import org.apache.hugegraph.api.task.TaskAPI;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.client.RestClient;

public class JobManager {

    private RebuildAPI rebuildAPI;
    private TaskAPI taskAPI;

    public JobManager(RestClient client, String graph) {
        this.rebuildAPI = new RebuildAPI(client, graph);
        this.taskAPI = new TaskAPI(client, graph);
    }

    public void rebuild(VertexLabel vertexLabel) {
        this.rebuild(vertexLabel, TaskAPI.TASK_TIMEOUT);
    }

    public void rebuild(VertexLabel vertexLabel, long seconds) {
        long task = this.rebuildAPI.rebuild(vertexLabel);
        this.taskAPI.waitUntilTaskSuccess(task, seconds);
    }

    public long rebuildAsync(VertexLabel vertexLabel) {
        return this.rebuildAPI.rebuild(vertexLabel);
    }

    public void rebuild(EdgeLabel edgeLabel) {
        this.rebuild(edgeLabel, TaskAPI.TASK_TIMEOUT);
    }

    public void rebuild(EdgeLabel edgeLabel, long seconds) {
        long task = this.rebuildAPI.rebuild(edgeLabel);
        this.taskAPI.waitUntilTaskSuccess(task, seconds);
    }

    public long rebuildAsync(EdgeLabel edgeLabel) {
        return this.rebuildAPI.rebuild(edgeLabel);
    }

    public void rebuild(IndexLabel indexLabel) {
        this.rebuild(indexLabel, TaskAPI.TASK_TIMEOUT);
    }

    public void rebuild(IndexLabel indexLabel, long seconds) {
        long task = this.rebuildAPI.rebuild(indexLabel);
        this.taskAPI.waitUntilTaskSuccess(task, seconds);
    }

    public long rebuildAsync(IndexLabel indexLabel) {
        return this.rebuildAPI.rebuild(indexLabel);
    }
}
