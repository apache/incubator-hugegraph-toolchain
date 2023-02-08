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

package org.apache.hugegraph.api.schema;

import java.util.List;
import java.util.Map;

import org.apache.hugegraph.api.task.TaskAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.schema.EdgeLabel;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableMap;

public class EdgeLabelAPI extends SchemaElementAPI {

    public EdgeLabelAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.EDGE_LABEL.string();
    }

    public EdgeLabel create(EdgeLabel edgeLabel) {
        Object el = this.checkCreateOrUpdate(edgeLabel);
        RestResult result = this.client.post(this.path(), el);
        return result.readObject(EdgeLabel.class);
    }

    public EdgeLabel append(EdgeLabel edgeLabel) {
        String id = edgeLabel.name();
        Map<String, Object> params = ImmutableMap.of("action", "append");
        Object el = this.checkCreateOrUpdate(edgeLabel);
        RestResult result = this.client.put(this.path(), id, el, params);
        return result.readObject(EdgeLabel.class);
    }

    public EdgeLabel eliminate(EdgeLabel edgeLabel) {
        String id = edgeLabel.name();
        Map<String, Object> params = ImmutableMap.of("action", "eliminate");
        Object el = this.checkCreateOrUpdate(edgeLabel);
        RestResult result = this.client.put(this.path(), id, el, params);
        return result.readObject(EdgeLabel.class);
    }

    public EdgeLabel get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(EdgeLabel.class);
    }

    public List<EdgeLabel> list() {
        RestResult result = this.client.get(this.path());
        return result.readList(this.type(), EdgeLabel.class);
    }

    public List<EdgeLabel> list(List<String> names) {
        this.client.checkApiVersion("0.48", "getting schema by names");
        E.checkArgument(names != null && !names.isEmpty(),
                        "The edge label names can't be null or empty");
        Map<String, Object> params = ImmutableMap.of("names", names);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), EdgeLabel.class);
    }

    public long delete(String name) {
        RestResult result = this.client.delete(this.path(), name);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = result.readObject(Map.class);
        return TaskAPI.parseTaskId(task);
    }

    @Override
    protected Object checkCreateOrUpdate(SchemaElement schemaElement) {
        EdgeLabel edgeLabel = (EdgeLabel) schemaElement;
        Object el = edgeLabel;
        if (this.client.apiVersionLt("0.54")) {
            E.checkArgument(edgeLabel.ttl() == 0L &&
                            edgeLabel.ttlStartTime() == null,
                            "Not support ttl until api version 0.54");
            el = edgeLabel.switchV53();
        }
        return el;
    }
}
