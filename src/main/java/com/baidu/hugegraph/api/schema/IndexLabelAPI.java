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

import com.baidu.hugegraph.api.task.TaskAPI;
import com.baidu.hugegraph.client.RestClient;
import com.baidu.hugegraph.rest.RestResult;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IndexType;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.util.E;
import com.google.common.collect.ImmutableMap;

public class IndexLabelAPI extends SchemaAPI {

    public IndexLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.INDEX_LABEL.string();
    }

    public IndexLabel.CreatedIndexLabel create(IndexLabel indexLabel) {
        if (indexLabel.indexType() == IndexType.SHARD) {
            this.client.checkApiVersion("0.43", "shard index");
        } else if (indexLabel.indexType() == IndexType.UNIQUE) {
            this.client.checkApiVersion("0.44", "unique index");
        }
        RestResult result = this.client.post(this.path(), indexLabel);
        return result.readObject(IndexLabel.CreatedIndexLabel.class);
    }

    public IndexLabel append(IndexLabel indexLabel) {
        String id = indexLabel.name();
        Map<String, Object> params = ImmutableMap.of("action", "append");
        RestResult result = this.client.put(this.path(), id,
                                            indexLabel, params);
        return result.readObject(IndexLabel.class);
    }

    public IndexLabel eliminate(IndexLabel indexLabel) {
        String id = indexLabel.name();
        Map<String, Object> params = ImmutableMap.of("action", "eliminate");
        RestResult result = this.client.put(this.path(), id,
                                            indexLabel, params);
        return result.readObject(IndexLabel.class);
    }

    public IndexLabel get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(IndexLabel.class);
    }

    public List<IndexLabel> list() {
        RestResult result = this.client.get(this.path());
        return result.readList(this.type(), IndexLabel.class);
    }

    public List<IndexLabel> list(List<String> names) {
        this.client.checkApiVersion("0.48", "getting schema by names");
        E.checkArgument(names != null && !names.isEmpty(),
                        "The index label names can't be null or empty");
        Map<String, Object> params = ImmutableMap.of("names", names);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), IndexLabel.class);
    }

    public long delete(String name) {
        RestResult result = this.client.delete(this.path(), name);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = result.readObject(Map.class);
        return TaskAPI.parseTaskId(task);
    }
}
