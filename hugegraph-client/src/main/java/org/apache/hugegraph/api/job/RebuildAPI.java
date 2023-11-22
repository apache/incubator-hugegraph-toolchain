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

package org.apache.hugegraph.api.job;

import java.util.Map;

import org.apache.hugegraph.api.task.TaskAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.structure.schema.IndexLabel;
import org.apache.hugegraph.structure.schema.VertexLabel;
import org.apache.hugegraph.util.E;

public class RebuildAPI extends JobAPI {

    private static final String JOB_TYPE = "rebuild";

    public RebuildAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String jobType() {
        return JOB_TYPE;
    }

    public long rebuild(VertexLabel vertexLabel) {
        return this.rebuildIndex(vertexLabel);
    }

    public long rebuild(EdgeLabel edgeLabel) {
        return this.rebuildIndex(edgeLabel);
    }

    public long rebuild(IndexLabel indexLabel) {
        return this.rebuildIndex(indexLabel);
    }

    private long rebuildIndex(SchemaElement element) {
        E.checkArgument(element instanceof VertexLabel ||
                        element instanceof EdgeLabel ||
                        element instanceof IndexLabel,
                        "Only VertexLabel, EdgeLabel and IndexLabel support " +
                        "rebuild, but got '%s'", element);
        String path = String.join(PATH_SPLITOR, this.path(), element.type());
        RestResult result = this.client.put(path, element.name(), element);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = result.readObject(Map.class);
        return TaskAPI.parseTaskId(task);
    }
}
