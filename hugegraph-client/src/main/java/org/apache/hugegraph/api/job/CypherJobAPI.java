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

public class CypherJobAPI extends JobAPI {

    private static final String JOB_TYPE = "cypher";

    public CypherJobAPI(RestClient client, String graphSpace, String graph) {
        super(client, graphSpace, graph);
    }

    @Override
    protected String jobType() {
        return JOB_TYPE;
    }

    public long execute(String cypher) {
        RestResult result = this.client.post(this.path(), cypher);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = result.readObject(Map.class);
        return TaskAPI.parseTaskId(task);
    }
}
