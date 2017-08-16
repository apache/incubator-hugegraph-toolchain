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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baidu.hugegraph.api.schema;

import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.client.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.google.common.collect.ImmutableMap;

public class EdgeLabelAPI extends SchemaAPI {

    public EdgeLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    public String type() {
        return HugeType.EDGE_LABEL.string();
    }

    public void create(EdgeLabel edgeLabel) {
        this.client.post(path(), edgeLabel);
    }

    public void append(EdgeLabel edgeLabel) {
        final Map<String, Object> params = ImmutableMap.of("action", "append");
        this.client.put(path(), edgeLabel, params);
    }

    public void eliminate(EdgeLabel edgeLabel) {
        final Map<String, Object> params = ImmutableMap.of("action",
                                                           "eliminate");
        this.client.put(path(), edgeLabel, params);
    }

    public EdgeLabel get(String name) {
        RestResult result = this.client.get(path(), name);
        return result.readObject(EdgeLabel.class);
    }

    public List<EdgeLabel> list() {
        RestResult result = this.client.get(path());
        return result.readList(type(), EdgeLabel.class);
    }

    public void delete(String name) {
        this.client.delete(path(), name);
    }
}
