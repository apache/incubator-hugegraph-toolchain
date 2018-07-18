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

package com.baidu.hugegraph.driver;

import com.baidu.hugegraph.api.job.RebuildAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public class JobManager {

    private RebuildAPI rebuildAPI;

    public JobManager(RestClient client, String graph) {
        this.rebuildAPI = new RebuildAPI(client, graph);
    }

    public long rebuild(VertexLabel vertexLabel) {
        return this.rebuildAPI.rebuild(vertexLabel);
    }

    public long rebuild(EdgeLabel edgeLabel) {
        return this.rebuildAPI.rebuild(edgeLabel);
    }

    public long rebuild(IndexLabel indexLabel) {
        return this.rebuildAPI.rebuild(indexLabel);
    }
}
