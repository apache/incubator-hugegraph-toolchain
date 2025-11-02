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

package org.apache.hugegraph.api.space;

import com.google.common.collect.ImmutableMap;

import org.apache.hugegraph.api.API;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.rest.RestResult;
import org.apache.hugegraph.structure.constant.HugeType;
import org.apache.hugegraph.structure.space.OLTPService;

import java.util.List;
import java.util.Map;

public class ServiceAPI extends API {

    private static final String PATH = "graphspaces/%s/services";
    private static final String CONFIRM_MESSAGE = "confirm_message";
    private static final String DELIMITER = "/";

    public ServiceAPI(RestClient client, String graphSpace) {
        super(client);
        this.path(String.format(PATH, graphSpace));
    }

    private static String joinPath(String path, String id) {
        return String.join(DELIMITER, path, id);
    }

    @Override
    protected String type() {
        return HugeType.SERVICES.string();
    }

    public List<String> list() {
        RestResult result = this.client.get(this.path());

        return result.readList(this.type(), String.class);
    }

    public Object add(OLTPService.OLTPServiceReq req) {
        RestResult result
                = this.client.post(this.path(), req);
        return result.readObject(Map.class);
    }

    public void delete(String service, String message) {
        this.client.delete(joinPath(this.path(), service),
                           ImmutableMap.of(CONFIRM_MESSAGE, message));
    }

    public OLTPService get(String serviceName) {
        RestResult result = this.client.get(this.path(), serviceName);

        return result.readObject(OLTPService.class);
    }

    public void startService(String serviceName) {
        this.client.put(joinPath(this.path(), "start"), serviceName,
                        ImmutableMap.of());
    }

    public void stopService(String serviceName) {
        this.client.put(joinPath(this.path(), "stop"), serviceName,
                        ImmutableMap.of());
    }
}

