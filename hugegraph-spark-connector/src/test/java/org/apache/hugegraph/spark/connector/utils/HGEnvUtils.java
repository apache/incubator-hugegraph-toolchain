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

package org.apache.hugegraph.spark.connector.utils;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.SchemaManager;

public class HGEnvUtils {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_PORT = "8080";
    public static final String DEFAULT_GRAPH = "hugegraph";
    public static final String DEFAULT_URL = "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT;

    private static HugeClient hugeClient;

    public static void createEnv() {

        hugeClient = HugeClient.builder(DEFAULT_URL, DEFAULT_GRAPH).build();

        hugeClient.graphs().clearGraph(DEFAULT_GRAPH, "I'm sure to delete all data");

        SchemaManager schema = hugeClient.schema();

        // Define schema
        schema.propertyKey("name").asText().ifNotExist().create();
        schema.propertyKey("age").asInt().ifNotExist().create();
        schema.propertyKey("city").asText().ifNotExist().create();
        schema.propertyKey("weight").asDouble().ifNotExist().create();
        schema.propertyKey("lang").asText().ifNotExist().create();
        schema.propertyKey("date").asText().ifNotExist().create();
        schema.propertyKey("price").asDouble().ifNotExist().create();

        schema.vertexLabel("person")
              .properties("name", "age", "city")
              .useCustomizeStringId()
              .nullableKeys("age", "city")
              .ifNotExist()
              .create();

        schema.vertexLabel("software")
              .properties("name", "lang", "price")
              .primaryKeys("name")
              .ifNotExist()
              .create();

        schema.edgeLabel("knows")
              .sourceLabel("person")
              .targetLabel("person")
              .properties("date", "weight")
              .ifNotExist()
              .create();

        schema.edgeLabel("created")
              .sourceLabel("person")
              .targetLabel("software")
              .properties("date", "weight")
              .ifNotExist()
              .create();
    }

    public static void destroyEnv() {
        hugeClient.close();
    }

    public static HugeClient getHugeClient() {
        return hugeClient;
    }
}
