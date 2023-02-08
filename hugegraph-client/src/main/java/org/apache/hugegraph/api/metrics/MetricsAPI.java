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

package org.apache.hugegraph.api.metrics;

import java.util.Map;

import org.apache.hugegraph.util.CommonUtil;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;

public class MetricsAPI extends API {

    public MetricsAPI(RestClient client) {
        super(client);
        this.path(this.type());
    }

    @Override
    protected String type() {
        return HugeType.METRICS.string();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> system() {
        RestResult result = this.client.get(this.path(), "system");
        Map<?, ?> map = result.readObject(Map.class);
        CommonUtil.checkMapClass(map, String.class, Map.class);
        for (Object mapValue : map.values()) {
            CommonUtil.checkMapClass(mapValue, String.class, Object.class);
        }
        return (Map<String, Map<String, Object>>) map;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> backend() {
        RestResult result = this.client.get(this.path(), "backend");
        Map<?, ?> map = result.readObject(Map.class);
        CommonUtil.checkMapClass(map, String.class, Map.class);
        for (Object mapValue : map.values()) {
            CommonUtil.checkMapClass(mapValue, String.class, Object.class);
        }
        return (Map<String, Map<String, Object>>) map;
    }

    public Map<String, Object> backend(String graph) {
        return this.backend().get(graph);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> all() {
        RestResult result = this.client.get(this.path());
        Map<?, ?> map = result.readObject(Map.class);
        CommonUtil.checkMapClass(map, String.class, Map.class);
        for (Object mapValue : map.values()) {
            CommonUtil.checkMapClass(mapValue, String.class, Object.class);
        }
        return (Map<String, Map<String, Object>>) map;
    }
}
