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

package org.apache.hugegraph.api.auth;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.auth.Project;
import org.apache.hugegraph.structure.constant.HugeType;

import com.google.common.collect.ImmutableMap;

public class ProjectAPI extends AuthAPI {

    private static final String ACTION_ADD_GRAPH = "add_graph";
    private static final String ACTION_REMOVE_GRAPH = "remove_graph";

    public ProjectAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.PROJECT.string();
    }

    public Project create(Project project) {
        RestResult result = this.client.post(this.path(), project);
        return result.readObject(Project.class);
    }

    public Project get(Object id) {
        RestResult result = this.client.get(this.path(), formatEntityId(id));
        return result.readObject(Project.class);
    }

    public List<Project> list(long limit) {
        checkLimit(limit, "Limit");
        Map<String, Object> params = ImmutableMap.of("limit", limit);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), Project.class);
    }

    public Project update(Project project) {
        String id = formatEntityId(project.id());
        RestResult result = this.client.put(this.path(), id, project);
        return result.readObject(Project.class);
    }

    public void delete(Object id) {
        this.client.delete(this.path(), formatEntityId(id));
    }

    public Project addGraphs(Object projectId, Set<String> graphs) {
        Project project = new Project();
        project.graphs(graphs);
        RestResult result = this.client.put(this.path(),
                                            formatEntityId(projectId),
                                            project,
                                            ImmutableMap.of("action",
                                                            ACTION_ADD_GRAPH));
        return result.readObject(Project.class);
    }

    public Project removeGraphs(Object projectId, Set<String> graphs) {
        Project project = new Project();
        project.graphs(graphs);
        RestResult result = this.client.put(this.path(),
                                            formatEntityId(projectId),
                                            project,
                                            ImmutableMap.of("action",
                                                            ACTION_REMOVE_GRAPH));
        return result.readObject(Project.class);
    }
}
