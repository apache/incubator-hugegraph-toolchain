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
import org.apache.hugegraph.exception.NotSupportException;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.SchemaElement;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.constant.WriteType;
import org.apache.hugegraph.structure.schema.PropertyKey;
import org.apache.hugegraph.util.E;

import com.google.common.collect.ImmutableMap;

public class PropertyKeyAPI extends SchemaElementAPI {

    public PropertyKeyAPI(RestClient client, String graph) {
        super(client, graph);
    }

    @Override
    protected String type() {
        return HugeType.PROPERTY_KEY.string();
    }

    public PropertyKey.PropertyKeyWithTask create(PropertyKey propertyKey) {
        Object pkey = this.checkCreateOrUpdate(propertyKey);
        RestResult result = this.client.post(this.path(), pkey);
        if (this.client.apiVersionLt("0.65")) {
            return new PropertyKey.PropertyKeyWithTask(result.readObject(PropertyKey.class), 0L);
        }
        return result.readObject(PropertyKey.PropertyKeyWithTask.class);
    }

    public PropertyKey.PropertyKeyWithTask append(PropertyKey propertyKey) {
        String id = propertyKey.name();
        Map<String, Object> params = ImmutableMap.of("action", "append");
        Object pkey = this.checkCreateOrUpdate(propertyKey);
        RestResult result = this.client.put(this.path(), id, pkey, params);
        return result.readObject(PropertyKey.PropertyKeyWithTask.class);
    }

    public PropertyKey.PropertyKeyWithTask eliminate(PropertyKey propertyKey) {
        String id = propertyKey.name();
        Map<String, Object> params = ImmutableMap.of("action", "eliminate");
        Object pkey = this.checkCreateOrUpdate(propertyKey);
        RestResult result = this.client.put(this.path(), id, pkey, params);
        return result.readObject(PropertyKey.PropertyKeyWithTask.class);
    }

    public PropertyKey.PropertyKeyWithTask clear(PropertyKey propertyKey) {
        if (this.client.apiVersionLt("0.65")) {
            throw new NotSupportException("action clear on property key");
        }
        String id = propertyKey.name();
        Map<String, Object> params = ImmutableMap.of("action", "clear");
        Object pkey = this.checkCreateOrUpdate(propertyKey);
        RestResult result = this.client.put(this.path(), id, pkey, params);
        return result.readObject(PropertyKey.PropertyKeyWithTask.class);
    }

    public PropertyKey get(String name) {
        RestResult result = this.client.get(this.path(), name);
        return result.readObject(PropertyKey.class);
    }

    public List<PropertyKey> list() {
        RestResult result = this.client.get(this.path());
        return result.readList(this.type(), PropertyKey.class);
    }

    public List<PropertyKey> list(List<String> names) {
        this.client.checkApiVersion("0.48", "getting schema by names");
        E.checkArgument(names != null && !names.isEmpty(),
                        "The property key names can't be null or empty");
        Map<String, Object> params = ImmutableMap.of("names", names);
        RestResult result = this.client.get(this.path(), params);
        return result.readList(this.type(), PropertyKey.class);
    }

    public long delete(String name) {
        if (this.client.apiVersionLt("0.65")) {
            this.client.delete(this.path(), name);
            return 0L;
        }
        RestResult result = this.client.delete(this.path(), name);
        @SuppressWarnings("unchecked")
        Map<String, Object> task = result.readObject(Map.class);
        return TaskAPI.parseTaskId(task);
    }

    @Override
    protected Object checkCreateOrUpdate(SchemaElement schemaElement) {
        PropertyKey propertyKey = (PropertyKey) schemaElement;
        Object pkey = propertyKey;
        if (this.client.apiVersionLt("0.47")) {
            E.checkArgument(propertyKey.aggregateType().isNone(),
                            "Not support aggregate property until " +
                            "api version 0.47");
            pkey = propertyKey.switchV46();
        } else if (this.client.apiVersionLt("0.59")) {
            E.checkArgument(propertyKey.writeType() == WriteType.OLTP,
                            "Not support olap property key until " +
                            "api version 0.59");
            pkey = propertyKey.switchV58();
        }
        return pkey;
    }
}
