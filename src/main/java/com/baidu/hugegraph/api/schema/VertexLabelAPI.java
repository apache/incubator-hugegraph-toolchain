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

package com.baidu.hugegraph.api.schema;

import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.VertexLabel;
import com.google.common.collect.ImmutableMap;

public class VertexLabelAPI extends SchemaAPI {

    public VertexLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.VERTEX_LABEL.string();
    }

    public VertexLabel create(VertexLabel vertexLabel) {
        RestResult result = this.client.post(this.path(), vertexLabel);
        return result.readObject(VertexLabel.class);
    }

    public VertexLabel append(VertexLabel vertexLabel) {
        String id = vertexLabel.name();
        Map<String, Object> params = ImmutableMap.of("action", "append");
        RestResult result = this.client.put(this.path(), id,
                                            vertexLabel, params);
        return result.readObject(VertexLabel.class);
    }

    public VertexLabel eliminate(VertexLabel vertexLabel) {
        String id = vertexLabel.name();
        Map<String, Object> params = ImmutableMap.of("action", "eliminate");
        RestResult result = this.client.put(this.path(), id,
                                            vertexLabel, params);
        return result.readObject(VertexLabel.class);
    }

    public VertexLabel get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(VertexLabel.class);
    }

    public List<VertexLabel> list() {
        RestResult result = this.client.get(this.path());
        return result.readList(this.type(), VertexLabel.class);
    }

    public void delete(String name) {
        this.client.delete(this.path(), name);
    }
}
