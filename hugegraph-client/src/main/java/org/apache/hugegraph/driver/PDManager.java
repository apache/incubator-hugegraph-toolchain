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

package org.apache.hugegraph.driver;

import java.util.List;

import org.apache.hugegraph.api.space.PDAPI;
import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.structure.space.PDNodeInfo;

public class PDManager {

    private final PDAPI pdAPI;

    public PDManager(RestClient client) {
        this.pdAPI = new PDAPI(client);
    }

    public List<PDNodeInfo> status() {
        return this.pdAPI.list();
    }
}
