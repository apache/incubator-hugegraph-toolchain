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

package org.apache.hugegraph.loader.filter.util;

import java.lang.reflect.Field;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;
import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.loader.executor.LoadOptions;
import org.apache.hugegraph.structure.schema.BuilderProxy;
import org.apache.hugegraph.structure.schema.VertexLabel;

public class SchemaManagerProxy extends SchemaManager {

    private LoadOptions options;
    public SchemaManagerProxy(RestClient client, String graphSpace, String graph, LoadOptions options) {
        super(client, graphSpace, graph);
        this.options = options;
    }

    public static void proxy(HugeClient client, LoadOptions options){
        try {
            Field clientField = HugeClient.class.getDeclaredField("client");
            clientField.setAccessible(true);
            RestClient restClient = (RestClient) (clientField.get(client));
            SchemaManager schemaManager  = new SchemaManagerProxy(restClient,
                                                                  client.getGraphSpaceName(),
                                                                  client.getGraphName(),
                                                                  options);
            Field schemaField = HugeClient.class.getDeclaredField("schema");
            schemaField.setAccessible(true);
            schemaField.set(client, schemaManager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new LoadException("create schema proxy fail", e);
        }
    }

    @Override
    public VertexLabel.Builder vertexLabel(String name) {
        VertexLabel.Builder builder = new VertexLabelBuilderProxy(name, this, options);
        BuilderProxy<VertexLabel.Builder> proxy = new BuilderProxy<>(builder);
        return proxy.proxy();
    }
}

