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

package org.apache.hugegraph.spark.connector.mapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.hugegraph.testutil.Assert;
import org.junit.Test;

public class VertexMappingTest {

    @Test
    public void testBasicVertexMapping() {
        VertexMapping vertexMapping = new VertexMapping("unique-id");
        vertexMapping.label("person");

        vertexMapping.check();

        Assert.assertEquals("vertex", vertexMapping.type().dataType());
        Assert.assertEquals("unique-id", vertexMapping.idField());
        Assert.assertEquals("person", vertexMapping.label());
        Assert.assertEquals("vertex-mapping(label=person)", vertexMapping.toString());
    }

    @Test
    public void testAdvancedVertexMapping() {
        VertexMapping vertexMapping = new VertexMapping("unique-id");
        vertexMapping.label("person");
        vertexMapping.batchSize(100);
        Assert.assertEquals(100, vertexMapping.batchSize());

        vertexMapping.selectedFields(new HashSet<>(Arrays.asList("id", "name", "lang", "price")));
        vertexMapping.ignoredFields(new HashSet<>(Arrays.asList("ISBN", "age")));

        String[] selectFields = vertexMapping.selectedFields().toArray(new String[0]);
        Assert.assertArrayEquals(new String[]{"price", "name", "id", "lang"}, selectFields);
        String[] ignoredFields = vertexMapping.ignoredFields().toArray(new String[0]);
        Assert.assertArrayEquals(new String[]{"ISBN", "age"}, ignoredFields);

        HashMap<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("source_name", "name");
        fieldMapping.put("target_name", "name");
        vertexMapping.mappingFields(fieldMapping);
    }

    @Test
    public void testMappingFieldValueNotExists() {
        VertexMapping vertexMapping = new VertexMapping("unique-id");
        vertexMapping.label("person");

        HashMap<String, String> fieldMapping = new HashMap<>();
        fieldMapping.put("source_name", "name");
        vertexMapping.mappingFields(fieldMapping);

        Assert.assertEquals("new_source_name", vertexMapping.mappingField("new_source_name"));
    }

    @Test
    public void testMappingFieldValueIsNull() {
        VertexMapping vertexMapping = new VertexMapping("unique-id");
        vertexMapping.label("person");

        HashMap<String, String> fieldMapping = new HashMap<>();
        // The value in field_mapping can't be null
        fieldMapping.put("source_name", null);
        vertexMapping.mappingFields(fieldMapping);

        Assert.assertThrows(IllegalArgumentException.class, vertexMapping::check);
    }

    @Test
    public void testMappingValue() {
        VertexMapping vertexMapping = new VertexMapping("unique-id");
        vertexMapping.label("person");

        Map<String, Object> cityMap = new HashMap<>();
        cityMap.put("1", "Beijing");
        cityMap.put("2", "Shanghai");
        Map<String, Object> ageMap = new HashMap<>();
        ageMap.put("young", 18);
        ageMap.put("middle", 35);

        Map<String, Map<String, Object>> valueMapping = new HashMap<>();
        valueMapping.put("city", cityMap);
        valueMapping.put("age", ageMap);

        vertexMapping.mappingValues(valueMapping);

        Assert.assertEquals("Beijing", vertexMapping.mappingValue("city", "1"));
        Assert.assertEquals("Shanghai", vertexMapping.mappingValue("city", "2"));
        Assert.assertEquals(18, vertexMapping.mappingValue("age", "young"));
        Assert.assertEquals(35, vertexMapping.mappingValue("age", "middle"));

        // not exist raw value
        Assert.assertEquals("Wuhan", vertexMapping.mappingValue("city", "Wuhan"));
        Assert.assertEquals("old", vertexMapping.mappingValue("age", "old"));
    }

    @Test
    public void testMappingValueIsNull() {
        VertexMapping vertexMapping = new VertexMapping("unique-id");
        vertexMapping.label("person");

        Map<String, Object> cityMap = new HashMap<>();
        // The value in value_mapping can't be null
        cityMap.put("1", null);
        cityMap.put("2", "Shanghai");

        Map<String, Map<String, Object>> valueMapping = new HashMap<>();
        valueMapping.put("city", cityMap);
        vertexMapping.mappingValues(valueMapping);

        Assert.assertThrows(IllegalArgumentException.class, vertexMapping::check);
    }
}
