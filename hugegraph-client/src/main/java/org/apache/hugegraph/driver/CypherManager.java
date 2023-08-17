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

import org.apache.hugegraph.api.gremlin.CypherAPI;
import org.apache.hugegraph.structure.gremlin.Response;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.apache.hugegraph.client.RestClient;

public class CypherManager {

    private final GraphManager graphManager;
    private final CypherAPI cypherAPI;

    public CypherManager(RestClient client, String graph,
                         GraphManager graphManager) {
        this.graphManager = graphManager;
        this.cypherAPI = new CypherAPI(client, graph);
    }

    public ResultSet execute(String cypher) {
        Response response = this.cypherAPI.post(cypher);
        response.graphManager(this.graphManager);
        // TODO: Can add some checks later
        return response.result();
    }
}
