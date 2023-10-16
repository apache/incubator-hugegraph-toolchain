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

import java.util.HashMap;
import java.util.Map;

import org.apache.hugegraph.spark.connector.mapping.EdgeMapping;
import org.apache.hugegraph.spark.connector.mapping.VertexMapping;
import org.apache.hugegraph.spark.connector.options.HGOptions;
import org.junit.Assert;
import org.junit.Test;

public class HGUtilsTest {

    @Test
    public void testVertexMappingFromConf() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "vertex");
        configs.put("label", "person");
        configs.put("id", "name");
        configs.put("batch-size", "100");
        configs.put("ignored-fields", "price,ISBN");

        HGOptions options = new HGOptions(configs);
        VertexMapping vertexMapping = HGUtils.vertexMappingFromConf(options);

        Assert.assertEquals("vertex", vertexMapping.type().dataType());
        Assert.assertEquals("person", vertexMapping.label());
        Assert.assertEquals("name", vertexMapping.idField());
        Assert.assertEquals(100, vertexMapping.batchSize());
        Assert.assertArrayEquals(new String[]{"ISBN", "price"},
                                 vertexMapping.ignoredFields().toArray());
    }

    @Test
    public void testEdgeMappingFromConf() {
        Map<String, String> configs = new HashMap<>();
        configs.put("data-type", "edge");
        configs.put("label", "buy");
        configs.put("source-name", "id,name,age");
        configs.put("target-name", "price,ISBN");
        configs.put("batch-size", "200");
        configs.put("selected-fields", "price,pages");

        HGOptions options = new HGOptions(configs);
        EdgeMapping edgeMapping = HGUtils.edgeMappingFromConf(options);

        Assert.assertEquals("edge", edgeMapping.type().dataType());
        Assert.assertEquals("buy", edgeMapping.label());
        Assert.assertEquals(200, edgeMapping.batchSize());

        Assert.assertArrayEquals(new String[]{"id", "name", "age"},
                                 edgeMapping.sourceFields().toArray(new String[0]));
        Assert.assertArrayEquals(new String[]{"price", "ISBN"},
                                 edgeMapping.targetFields().toArray(new String[0]));
        Assert.assertArrayEquals(new String[]{"pages", "price"},
                                 edgeMapping.selectedFields().toArray(new String[0]));
    }
}
